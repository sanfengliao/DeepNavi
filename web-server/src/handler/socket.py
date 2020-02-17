from tornado.websocket import WebSocketHandler
from ..model.basic_pb2 import DeepNaviReq, DeepNaviRes

class DeepNaviWebSocket(WebSocketHandler):
    def open(self):
        pass
    def on_message(self, payload):
        deepNaviReq = DeepNaviReq()
        deepNaviReq.ParseFromString(payload)
        print(deepNaviReq)
        f = open('./upload' + '/' + str(deepNaviReq.time) + '.jpg', 'wb+')
        f.write(deepNaviReq.image)
        f.close()

        deepNaviRes = DeepNaviRes()
        deepNaviRes.result = 'OK'
        self.write_message(deepNaviRes.SerializeToString(), binary=True)
