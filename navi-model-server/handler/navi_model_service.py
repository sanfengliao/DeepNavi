import torch
from PIL import Image
from naviservice.ttypes import NaviModel, LocationResult, Coor
from io import BytesIO

from navi import DeepNaviModel

class NaviModelServiceHandler:
    def __init__(self):
        self.deepNaviModel = DeepNaviModel()

    def predict(self, model: NaviModel) -> LocationResult:
        loc, angle = self.deepNaviModel.predict(model)
        result = LocationResult()
        result.coor = Coor(x=loc[0], y=loc[1], z=loc[2])
        result.rotation = angle[0]
        return result