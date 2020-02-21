from .naviservice import NaviModelService
from .naviservice.ttypes import *
from .naviservice.constants import *
from config import RPC_IP, RPC_PORT
from model.basic_pb2 import DeepNaviReq
from google.protobuf.json_format import MessageToJson, Parse, MessageToDict

from thrift import Thrift
from thrift.transport import TSocket, TTransport
from thrift.protocol import TBinaryProtocol

transocket = TSocket.TSocket(RPC_IP, RPC_PORT)
transport = TTransport.TBufferedTransport(transocket)
protocol = TBinaryProtocol.TBinaryProtocol(transport)
naviModelService = NaviModelService.Client(protocol)
transport.open()

class Navi:
    def predictByImageAndWifi(self, reqData: DeepNaviReq):
        naviModel = NaviModel()
        naviModel.image = reqData.image
        naviModel.wifiList = reqData.wifiList
        resultList = naviModelService.predictByImageAndWifi(naviModel)
        return resultList
    def predictByImageAndMag(self, reqData: DeepNaviReq):
        dic = MessageToDict(reqData)
        _magneticList = dic['magneticList']
        magneticList = []
        for magnetic in _magneticList:
            magneticList.append(CoorSensor(x=magnetic['x'], y=magnetic['y'], z=magnetic['z']))
        naviModel = NaviModel(image=reqData.image, magneticList=magneticList)
        resultList = naviModelService.predictByImageAndMag(naviModel)
        return resultList