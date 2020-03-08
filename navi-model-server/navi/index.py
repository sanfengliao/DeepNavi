import math
import os
import typing
from io import BytesIO, StringIO

import torch
import torchvision.transforms as transforms
from PIL import Image
from sklearn.preprocessing import StandardScaler
import logging
from .datasets import *
from .models import *


class DeepNaviModel:
    def __init__(self, model_path:str=None, device_index=0):
        if model_path is None:
            model_path = os.path.join(os.path.dirname(__file__), 'model_weights/sc/sc.pth.tar')
        ######################################
        # Congifuration
        ######################################
        # os.environ["CUDA_VISIBLE_DEVICES"] = str(device_index)
        # device = torch.device('cuda')
        self.device = device = torch.device('cpu')
        self.image_encoder = ImageEncoder(models.resnet18(pretrained=True))
        self.mag_encoder = MagEncoder(3 * 16, 256, 4)
        self.fusion = Fusion(input_size=[256, 256], output_size=1024)
        self.decoder = Decoder(input_size=1024, output_size=[3, 4])
        self.model = MainModel([self.image_encoder, self.mag_encoder], self.fusion, self.decoder)

        # load model weights
        # [PyTorch使用cpu调用gpu训练的模型](https://blog.csdn.net/c654528593/article/details/81539441)
        # model_weights = torch.load(model_path)
        self.model_weights = torch.load(model_path, map_location='cpu')
        self.model.load_state_dict(self.model_weights["state_dict"])
        self.model = self.model.to(device)
    
    def predictByImageAndMags(self, imgs: Image.Image, mags: torch.Tensor) -> torch.Tensor: 
        ######################################
        # Data Preprocess
        ######################################
        normalize = transforms.Normalize(
            mean=[0.485, 0.456, 0.406],
            std=[0.229, 0.224, 0.225]
        )
        mag_scaler = StandardScaler()

        ######################################
        # Dataset
        ######################################
        image_transforms = transforms.Compose([
            transforms.Resize(256),
            transforms.CenterCrop(224),
            transforms.ToTensor(),
            normalize
        ])
        imgs = image_transforms(imgs)
        imgs = imgs.view(1, 1, *imgs.size())
        mags = mags.view(1, 1, *mags.size())
        
        self.model.eval()

        with torch.no_grad():
            batch_size = mags.size(0)

            imgs, mags = imgs.to(self.device), mags.to(self.device)
            mags = mags.view(batch_size, 1, -1)

            trans_output, rotation_output = self.model([imgs, mags])
            logging.info('trans_output: %s rotation_out: %s'%(trans_output[0], rotation_output[0]))
            return self.rotate_vector(rotation_output[0], [1, 1, 1])
    
    def quaternion_to_euler(self,rotation_output: torch.Tensor) -> typing.List:
        rotation_w = rotation_output[0]
        rotation_x = rotation_output[1]
        rotation_y = rotation_output[2]
        rotation_z = rotation_output[3]
        x = math.atan2(2 * (rotation_y * rotation_z + rotation_w * rotation_x),
                    (rotation_w * rotation_w - rotation_x * rotation_x - rotation_y * rotation_y + rotation_z * rotation_z))
        #    1 - 2 * (rotation_x * rotation_x + rotation_y * rotation_y))
        y = math.asin(2 * (rotation_w * rotation_y - rotation_x * rotation_z))
        z = math.atan2(2 * (rotation_x * rotation_y + rotation_w * rotation_z),
                    (rotation_w * rotation_w + rotation_x * rotation_x - rotation_y * rotation_y - rotation_z * rotation_z))
        #    1 - 2 * (rotation_y * rotation_y + rotation_z * rotation_z))
        return [x * 180 / math.pi, y * 180 / math.pi, z * 180 / math.pi]


    def rotate_vector(self, rotation_output: torch.Tensor, v: typing.List) -> typing.List:
        w = rotation_output[0]
        x = rotation_output[1]
        y = rotation_output[2]
        z = rotation_output[3]

        x22 = 2 * x * x
        y22 = 2 * y * y
        z22 = 2 * z * z
        w22 = 2 * w * w
        xy2 = 2 * x * y
        zw2 = 2 * z * w
        xz2 = 2 * x * z
        yw2 = 2 * y * w
        yz2 = 2 * y * z
        xw2 = 2 * x * w

        vx = v[0]
        vy = v[1]
        vz = v[2]
        result = []
        result.append(vx * (x22 + w22 - 1) + vy * (xy2 - zw2) + vz * (xz2 + yw2))
        result.append(vx * (xy2 + zw2) + vy * (y22 + w22 - 1) + vz * (yz2 - xw2))
        result.append(vx * (xz2 - yw2) + vy * (yz2 + xw2) + vz * (z22 + w22 - 1))
        return result