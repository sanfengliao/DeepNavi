from autobahn.twisted.websocket import WebSocketServerProtocol
import sys
from twisted.python import log
from twisted.internet import reactor
from proto_model.basic_pb2 import DeepNaviReq, DeepNaviRes
from autobahn.twisted.websocket import WebSocketServerFactory

class WebSocketServer(WebSocketServerProtocol):
    def onMessage(self, payload, isBinary):
        if isBinary:
            deepNaviReq = DeepNaviReq()
            deepNaviReq.ParseFromString(payload)
            f = open('./upload' + '/' + str(deepNaviReq.time) + '.jpg', 'wb+')
            f.write(deepNaviReq.image)
            f.close()

            deepNaviRes = DeepNaviRes()
            deepNaviRes.result = 'OK'
            self.sendMessage(deepNaviRes.SerializeToString(), True)
        else:
            pass

    def onClose(self, wasClean, code, reason):
        pass

    def onConnect(self, request):
        return super().onConnect(request)


def runWebSocket(port):
    log.startLogging(sys.stdout)
    factory = WebSocketServerFactory()
    factory.protocol = WebSocketServer

    reactor.listenTCP(5000, factory)
    reactor.run()