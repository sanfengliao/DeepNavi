from flask_socketio import emit
import os
from app import socketIO
from proto_model.basic_pb2 import DeepNaviReq, DeepNaviRes
from config import ResponseCode

@socketIO.on('deepNavi',)
def onDeepNavi(data=b''):
    print('=======' + os.getcwd())
    deepNaviReq = DeepNaviReq()
    deepNaviReq.ParseFromString(data)
    print(deepNaviReq)
    f = open(os.getcwd() + '/upload/' + str(deepNaviReq.time) + '.jpg', 'wb+')
    f.write(deepNaviReq.image)
    f.close()
    res = DeepNaviRes()
    res.result = ResponseCode.OK
    emit('deepNavi', res.SerializeToString())