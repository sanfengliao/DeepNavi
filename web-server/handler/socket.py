from tornado.websocket import WebSocketHandler
from model.basic_pb2 import DeepNaviReq, DeepNaviRes
from service.navi import NaviService
naviService = NaviService()
import logging
class DeepNaviWebSocket(WebSocketHandler):
    def open(self):
        pass
    def on_message(self, payload):
        req = DeepNaviReq()
        req.ParseFromString(payload)
        result = naviService.predict(req)
        self.write_message(result.SerializeToString(), binary=True)
