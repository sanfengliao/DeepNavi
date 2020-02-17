from model.basic_pb2 import DeepNaviReq, DeepNaviRes
from autobahn.asyncio.websocket import WebSocketClientProtocol
import time
try:
    import asyncio
except ImportError:
    pass

class MyClientProtocol(WebSocketClientProtocol):
    def onOpen(self):
        req = generateReq()
        self.sendMessage(req.SerializeToString(), True)
    def onMessage(self, payload, isBinary):
        if isBinary:
            deepNaviRes = DeepNaviRes()
            deepNaviRes.ParseFromString(payload)
            print(deepNaviRes)

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

if __name__ == "__main__":
    from autobahn.asyncio.websocket import WebSocketClientFactory
    factory = WebSocketClientFactory()
    factory.protocol = MyClientProtocol

    loop = asyncio.get_event_loop()
    coro = loop.create_connection(factory, '127.0.0.1', 8000)
    loop.run_until_complete(coro)
    loop.run_forever()
    loop.close()