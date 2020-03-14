from service.naviservice import NaviModelService
from service.naviservice.ttypes import *
from service.naviservice.constants import *

from config import RPC_IP, RPC_PORT
from model.basic_pb2 import DeepNaviReq, DeepNaviRes
from util.bean import deepNaviReqToNaviModel


from thrift import Thrift
from thrift.transport import TSocket, TTransport
from thrift.protocol import TBinaryProtocol


transocket = TSocket.TSocket(RPC_IP, RPC_PORT)
transport = TTransport.TBufferedTransport(transocket)
protocol = TBinaryProtocol.TBinaryProtocol(transport)
naviModelService = NaviModelService.Client(protocol)
transport.open()

MAGNETIC_LIST_MAX_LEN = 48
class NaviService:
    def predict(self, req: DeepNaviReq) -> DeepNaviRes:
        naviModel = deepNaviReqToNaviModel(req)
        result = naviModelService.predict(naviModel)
        rCoor = result.coor
        res = DeepNaviRes()
        res.coor.x = rCoor.x
        res.coor.y = rCoor.y
        res.coor.z = rCoor.z
        res.rotation = result.rotation
        return res