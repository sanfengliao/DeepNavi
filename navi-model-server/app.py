from naviservice import NaviModelService
from naviservice.ttypes import NaviModel
from thrift.transport import TSocket, TTransport
from thrift.protocol import TBinaryProtocol
from thrift.server import TServer
from PIL import Image
from io import BytesIO
import torch
from navi import DeepNaviModel

IP = '127.0.0.1'
PORT = 1234

import socket

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

if __name__ == "__main__":
    naviModelServiceHandler = NaviModelServiceHandler()
    processor = NaviModelService.Processor(naviModelServiceHandler)
    transport = TSocket.TServerSocket('127.0.0.1', port=PORT)
    tfactory = TTransport.TBufferedTransportFactory()
    pfactory = TBinaryProtocol.TBinaryProtocolFactory()

    server = TServer.TSimpleServer(processor, transport, tfactory, pfactory)

    server.serve()