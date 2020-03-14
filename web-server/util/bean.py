from model import DeepNaviReq
from service.naviservice.ttypes import NaviModel, Coor, FeelSensor
def deepNaviReqToNaviModel(req: DeepNaviReq) -> NaviModel:
    image = req.image
    magneticList = req.magneticList
    magneticList = [Coor(x=item.x, y=item.y, z=item.z) for item in magneticList]

    accelerometerList = req.accelerometerList
    accelerometerList = [Coor(x=item.x, y=item.y, z=item.z) for item in accelerometerList]

    orientationList = req.orientationList
    orientationList = [Coor(x=item.x, y=item.y, z=item.z) for item in orientationList]

    gyroscopeList = req.gyroscopeList
    gyroscopeList = [Coor(x=item.x, y=item.y, z=item.z) for item in gyroscopeList]

    gravityList = req.gravityList
    gravityList = [Coor(x=item.x, y=item.y, z=item.z) for item in gravityList]

    linearAccelerationList = req.linearAccelerationList
    linearAccelerationList = [Coor(x=item.x, y=item.y, z=item.z) for item in linearAccelerationList]

    ambientTemperatureList = req.ambientTemperatureList
    ambientTemperatureList = [FeelSensor(value=item.value) for item in ambientTemperatureList]

    lightList = req.lightList
    lightList = [FeelSensor(value=item.value) for item in lightList]

    pressureList = req.pressureList
    pressureList = [FeelSensor(value=item.value) for item in pressureList]

    proximityList = req.proximityList
    proximityList = [FeelSensor(value=item.value) for item in proximityList]

    wifiList = req.wifiList
    wifiList = [FeelSensor(value=item.value) for item in wifiList]
    return NaviModel(image=image, magneticList=magneticList, accelerometerList=accelerometerList, orientationList=orientationList, gyroscopeList=gyroscopeList, gravityList=gravityList, linearAccelerationList=linearAccelerationList, ambientTemperatureList=ambientTemperatureList,lightList=lightList, proximityList=proximityList, wifiList=wifiList)