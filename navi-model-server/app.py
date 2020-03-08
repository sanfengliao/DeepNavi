from naviservice import NaviModelService
from naviservice.ttypes import NaviModel
from thrift.transport import TSocket, TTransport
from thrift.protocol import TBinaryProtocol
from thrift.server import TServer
from PIL import Image
from io import BytesIO
import torch
from navi import DeepNaviModel
from handler import NaviModelServiceHandler
import logging
IP = '127.0.0.1'
PORT = 1234

import socket   

class NaviModelServer:
    def __initLog(self):
        logging.basicConfig(format='[%(asctime)s %(filename)s:%(funcName)s:%(lineno)d %(levelname)s] %(message)s', filemode='a', filename=None, level=logging.DEBUG)
        naviModelServiceHandler = NaviModelServiceHandler()
        processor = NaviModelService.Processor(naviModelServiceHandler)
        transport = TSocket.TServerSocket('127.0.0.1', port=PORT)
        tfactory = TTransport.TBufferedTransportFactory()
        pfactory = TBinaryProtocol.TBinaryProtocolFactory()
        self.server = TServer.TSimpleServer(processor, transport, tfactory, pfactory)

    def __init__(self):
        self.__initLog()
    def serve(self):
        logging.info('NaviModelServier start')
        self.server.serve()


if __name__ == "__main__":
    naviModelServer = NaviModelServer()
    naviModelServer.serve()