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
import typing

transocket = TSocket.TSocket(RPC_IP, RPC_PORT)
transport = TTransport.TBufferedTransport(transocket)
protocol = TBinaryProtocol.TBinaryProtocol(transport)
naviModelService = NaviModelService.Client(protocol)
transport.open()

MAGNETIC_LIST_MAX_LEN = 48

redisDao = RedisDao()
mapDao = MapDao()

import csv
import os
import math
def loadLoc(filename) -> typing.List[dict]:
    data = csv.reader(open(filename, 'r'))
    data = list(data)
    return [{'x': float(item[0]), 'y': float(item[1]), 'z': float(item[2])} for item in data]


def quaternion_to_euler(rotation_output) -> typing.List:
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

def loadRotation(filename) -> typing.List[float]:
    data = csv.reader(open(filename, 'r'))
    data = list(data)
    data = [[float(item[0]), float(item[1]), float(item[2]), float(item[3])] for item in data]
    data = [quaternion_to_euler(item) for item in data]
    return [item[2] for item in data]

locData = loadLoc(os.path.join(os.path.dirname(__file__), 'loc.csv'))
rotationData = loadRotation(os.path.join(os.path.dirname(__file__), 'rotation.csv'))


def calDis(v1: dict, v2: dict) -> float:
    a = v1['x'] - v2['x']
    b = v1['y'] - v2['y']
    return math.sqrt(a * a + b * b)



i = 0
class NaviService:
    def predict(self, req: DeepNaviReq) -> DeepNaviRes:
        global i
        redisValue = redisDao.getDict(req.id)
        mapId = redisValue['mapId']
        m = mapDao.findById(mapId)
        # naviModel = deepNaviReqToNaviModel(req)
        # naviModel.modelPath = m.modelPath
        # result = naviModelService.predict(naviModel)

        # rCoor = result.coor
        res = DeepNaviRes()
        # res.coor.x = rCoor.x
        # res.coor.y = rCoor.y
        # res.coor.z = rCoor.z
        rCoor = locData[i]
        res.coor.x = rCoor['x']
        res.coor.y = rCoor['y']
        res.coor.z = rCoor['z']

       
        isClockwise = m.isClockwise
        currentAngle = redisValue['currentAngle']
        print(currentAngle)
        print(redisValue['angles'])
        print(rotationData[i])
        rotation = 0
        if not isClockwise:
            rotation = rotationData[i] - currentAngle
        else:
            rotation = currentAngle - rotationData[i]
        i += 1
        res.rotation = rotation
        toPoint = redisValue['toPoint']['actualCoordinate']
        # v2 = {'x': rCoor.x, 'y': rCoor.y}
        v2 = {'x': rCoor['x'], 'y': rCoor['y']}
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