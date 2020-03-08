import math
import os
import typing
from io import BytesIO, StringIO

import torch
import torchvision.transforms as transforms
from PIL import Image
from sklearn.preprocessing import StandardScaler

from datasets import *
from models import *


def use_deepnavi(imgs: Image.Image, mags: torch.Tensor, model_path='./model_weights/sc/sc.pth.tar', device_index=0) -> torch.Tensor:
    ######################################
    # Congifuration
    ######################################
    # os.environ["CUDA_VISIBLE_DEVICES"] = str(device_index)
    # device = torch.device('cuda')
    device = torch.device('cpu')

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
    # mag_scaler.fit_transform(mags)

    ######################################
    # Model
    ######################################
    image_encoder = ImageEncoder(models.resnet18(pretrained=True))
    mag_encoder = MagEncoder(3 * 16, 256, 4)
    fusion = Fusion(input_size=[256, 256], output_size=1024)
    decoder = Decoder(input_size=1024, output_size=[3, 4])
    model = MainModel([image_encoder, mag_encoder], fusion, decoder)

    # load model weights
    # [PyTorch使用cpu调用gpu训练的模型](https://blog.csdn.net/c654528593/article/details/81539441)
    # model_weights = torch.load(model_path)
    model_weights = torch.load(model_path, map_location='cpu')
    model.load_state_dict(model_weights["state_dict"])
    model = model.to(device)

    ######################################
    # Test
    ######################################
    model.eval()

    with torch.no_grad():
        batch_size = mags.size(0)

        imgs, mags = imgs.to(device), mags.to(device)
        mags = mags.view(batch_size, 1, -1)

        trans_output, rotation_output = model([imgs, mags])
        print(trans_output[0], rotation_output[0],
              quaternion_to_euler(rotation_output[0]), rotate_vector(rotation_output[0], [1, 1, 1]))
        return rotation_output


def quaternion_to_euler(rotation_output: torch.Tensor) -> typing.List:
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


def rotate_vector(rotation_output: torch.Tensor, v: typing.List) -> typing.List:
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


# def img_byte_to_tensor(img: bytes) -> torch.Tensor:
#     # [Pillow 之frombytes从二进制中读取图片](https://blog.csdn.net/wang785994599/article/details/96425280)
#     transform1 = transforms.Compose(
#         [transforms.ToTensor()])  # should be global
#     return transform1(Image.open(BytesIO(img)).convert('RGB'))


def main(path):
    mags = torch.tensor([37.857056, 16.542053, -11.161804, 37.857056, 16.542053, -11.161804,
                         35.585022, 10.758972, -10.366821, 35.585022, 10.758972, -10.366821,
                         36.091614, 15.016174, -11.819458, 36.091614, 15.016174, -11.819458,
                         34.526062, 12.641907, -10.768127, 34.526062, 12.641907, -10.768127,
                         36.524963, 12.173462, -9.135437, 37.857056, 16.542053, -11.161804,
                         37.857056, 16.542053, -11.161804, 37.088013, 12.394714, -9.571838,
                         34.96704, 14.585876, -12.930298, 34.96704, 14.585876, -12.930298,
                         36.643982, 16.670227, -11.070251, 36.643982, 16.670227, -11.070251])
    img_file = open(path, 'rb')
    img = img_file.read()
    img_file.close()
    img = Image.open(BytesIO(img)).convert('RGB')
    use_deepnavi(img, mags)
if __name__ == "__main__":
    main('./dataset/office/sensor_data/images/1527408657595803863.png')
