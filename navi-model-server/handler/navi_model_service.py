import torch
from PIL import Image
from naviservice.ttypes import NaviModel
from io import BytesIO

from navi import DeepNaviModel

class NaviModelServiceHandler:
    def __init__(self):
        self.deepNaviModel = DeepNaviModel()

    def predictByImageAndWifi(self, naviModel: NaviModel):
        return [1.23, 4.56]
    
    def predictByImageAndMag(self, naviModel: NaviModel):
        image = naviModel.image
        mags = naviModel.magneticList
        magList = []
        for mag in mags:
            magList.append(mag.x)
            magList.append(mag.y)
            magList.append(mag.z)
        magTensor = torch.Tensor(magList)
        image = Image.open(BytesIO(image)).convert('RGB')
        return self.deepNaviModel.predictByImageAndMags(image, magTensor)     