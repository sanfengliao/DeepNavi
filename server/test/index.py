import socketio
import sys
sys.path.append('../')
import time
from proto_model.basic_pb2 import DeepNaviRes, DeepNaviReq



def generateReq():
    req = DeepNaviReq()
    req.time = int(time.time() * 1000)
    f = open('./images/timg.jpeg', 'rb')
    req.image = f.read()
    magnetic = req.magneticList.add()
    magnetic.x = 1
    magnetic.y = 2
    magnetic.z = 3
    accelerometer = req.accelerometerList.add()
    accelerometer.x = 1
    accelerometer.y = 2
    accelerometer.z = 3
    rientation = req.orientationList.add()
    rientation.x = 1
    rientation.y = 2
    rientation.z = 3
    gyroscope = req.gyroscopeList.add()
    gyroscope.x = 1
    gyroscope.y = 2
    gyroscope.z = 3
    gravity = req.gravityList.add()
    gravity.x = 1
    gravity.y = 2
    gravity.z = 3
    linearAcceleration = req.linearAccelerationList.add()
    linearAcceleration.x = 1
    linearAcceleration.y = 2
    linearAcceleration.z = 3
    ambientTemperature = req.ambientTemperatureList.add()
    ambientTemperature.value = 20
    light = req.lightList.add()
    light.value = 20
    pressure = req.pressureList.add()
    pressure.value = 20
    proximity = req.proximityList.add()
    proximity.value = 20
    return req


socketIO = socketio.Client()
socketIO.connect('http://localhost:5000')

@socketIO.on('deepNavi')
def onDeepNav(resData):
    res = DeepNaviRes()
    res.ParseFromString(resData)
    print(res)

def startSocket():
    req = generateReq() 
    print(type(req.SerializeToString()))
    socketIO.emit('deepNavi', req.SerializeToString())
    socketIO.wait()
startSocket()