from naviservice import NaviModelService
from naviservice.ttypes import *
from thrift.transport import TSocket, TTransport
from thrift.protocol import TBinaryProtocol
from thrift.server import TServer

IP = '127.0.0.1'
PORT = 1234

import socket

class NaviModelServiceHandler:
    def __init__(self):
        pass

    def predictByImageAndWifi(self, naviModel: NaviModel):
        return [1.23, 4.56]
    
    def predictByImageAndMag(self, naviModel: NaviModel):
        return [1.23, 4.56]

naviModelServiceHandler = NaviModelServiceHandler()
processor = NaviModelService.Processor(naviModelServiceHandler)
transport = TSocket.TServerSocket('127.0.0.1', port=PORT)
tfactory = TTransport.TBufferedTransportFactory()
pfactory = TBinaryProtocol.TBinaryProtocolFactory()

server = TServer.TSimpleServer(processor, transport, tfactory, pfactory)

server.serve()