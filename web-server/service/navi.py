from service.naviservice import NaviModelService
from service.naviservice.ttypes import *
from service.naviservice.constants import *

from config import RPC_IP, RPC_PORT
from model.basic_pb2 import DeepNaviReq, DeepNaviRes
from util.bean import deepNaviReqToNaviModel
from dao import RedisDao, MapDao


from thrift import Thrift
from thrift.transport import TSocket, TTransport
from thrift.protocol import TBinaryProtocol

transocket = TSocket.TSocket(RPC_IP, RPC_PORT)
transport = TTransport.TBufferedTransport(transocket)
protocol = TBinaryProtocol.TBinaryProtocol(transport)
naviModelService = NaviModelService.Client(protocol)
transport.open()

MAGNETIC_LIST_MAX_LEN = 48

redisDao = RedisDao()
mapDao = MapDao()

import math
def calDis(v1: dict, v2: dict) -> float:
    a = v1['x'] - v2['x']
    b = v1['y'] - v2['y']
    return math.sqrt(a * a + b * b)



class NaviService:
    def predict(self, req: DeepNaviReq) -> DeepNaviRes:
        redisValue = redisDao.getDict(req.id)
        mapId = redisValue['mapId']
        m = mapDao.findById(mapId)
        naviModel = deepNaviReqToNaviModel(req)
        naviModel.modelPath = m.modelPath
        result = naviModelService.predict(naviModel)

        rCoor = result.coor
        res = DeepNaviRes()
        res.coor.x = rCoor.x
        res.coor.y = rCoor.y
        res.coor.z = rCoor.z

       

       
        isClockwise = m.isClockwise
        currentAngle = redisValue['currentAngle']
        rotation = 0
        if not isClockwise:
            rotation = result.rotation - currentAngle
        else:
            rotation = currentAngle - result.rotation
        res.rotation = rotation
        toPoint = redisValue['toPoint']['actualCoordinate']
        v2 = {'x': rCoor.x, 'y': rCoor.y}
        if calDis(toPoint, v2) < 0.5:
            index = redisValue['index']
            angles = redisValue['angles']
            if index == len(angles) - 1:
                res.flag = True
            else:
                res.flag = False
                redisValue['index'] = index + 1
                redisValue['currentAngle'] = angles[index]
                redisValue['toPoint'] = redisValue['pathResult'][index + 1]
                redisDao.setDict(req.id, redisValue)

        return res