from tornado.websocket import WebSocketHandler
from model.basic_pb2 import DeepNaviReq, DeepNaviRes
from service.navi import Navi
navi = Navi()
import logging
class DeepNaviWebSocket(WebSocketHandler):
    def open(self):
        pass
    def on_message(self, payload):
        deepNaviReq = DeepNaviReq()
        deepNaviReq.ParseFromString(payload)
        result = navi.predictByImageAndMag(deepNaviReq)
        logging.info('predict result is %s'%result)
        deepNaviRes = DeepNaviRes()
        deepNaviRes.result = str(result)
        self.write_message(deepNaviRes.SerializeToString(), binary=True)
