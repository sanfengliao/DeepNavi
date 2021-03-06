import math
import os
import typing
from io import BytesIO, StringIO

import torch
import torchvision.transforms as transforms
from PIL import Image

import logging
from navi.datasets import models

from navi.models import ImageEncoder, MagEncoder, Fusion, Decoder, MainModel
from naviservice.ttypes import NaviModel

defaultModelPath = os.path.join(os.path.dirname(__file__), 'model_weights/sc/sc.pth.tar')

class DeepNaviModel:
    def predict(self, naviModel: NaviModel) -> torch.Tensor: 
        # init encoder
        encoders = []
        if naviModel.image is not None:
            encoders.append(ImageEncoder(models.resnet18(pretrained=True)))
        if naviModel.magneticList is not None and len(naviModel.magneticList) == 16:
            encoders.append( MagEncoder(3 * 16, 256, 4))
        
        fusion = Fusion(input_size=[256, 256], output_size=1024)
        decoder = Decoder(input_size=1024, output_size=[3, 4])

        # init model
        modelPath = defaultModelPath
        if naviModel.modelPath is not None and len(naviModel.modelPath) > 0:
            modelPath = naviModel.modelPath
        device = torch.device('cpu')
        model = MainModel(encoders, fusion, decoder)
        model_weights = torch.load(modelPath, map_location='cpu')
        model.load_state_dict(model_weights["state_dict"])
        model = model.to(device)

        ######################################
        # Dataset
        ######################################
        normalize = transforms.Normalize(
            mean=[0.485, 0.456, 0.406],
            std=[0.229, 0.224, 0.225]
        )
        image_transforms = transforms.Compose([
            transforms.Resize(256),
            transforms.CenterCrop(224),
            transforms.ToTensor(),
            normalize
        ])
        imgs = Image.open(BytesIO(naviModel.image)).convert('RGB')
        imgs = image_transforms(imgs)
        imgs = imgs.view(1, 1, *imgs.size())
        magList = []
        for item in naviModel.magneticList:
            magList.append(item.x)
            magList.append(item.y)
            magList.append(item.z)
        
        mags = torch.tensor(magList)
        mags = mags.view(1, 1, *mags.size())

        model.eval()

        with torch.no_grad():
            batch_size = mags.size(0)

            imgs, mags = imgs.to(device), mags.to(device)
            mags = mags.view(batch_size, 1, -1)

            trans_output, rotation_output = model([imgs, mags])
            logging.info('trans_output: %s rotation_out: %s'%(trans_output[0], rotation_output[0]))
            logging.info(self.quaternion_to_euler(rotation_output[0]))
            return trans_output[0], self.quaternion_to_euler(rotation_output[0])
    
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