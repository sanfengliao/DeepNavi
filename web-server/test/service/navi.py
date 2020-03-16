import unittest
from service import NaviService
from model import DeepNaviReq
import time
import os

service = NaviService()
imagePath = os.path.join(os.path.dirname(__file__), 'images/1527408657595803863.png')
mags = [37.857056, 16.542053, -11.161804, 37.857056, 16.542053, -11.161804,
                         35.585022, 10.758972, -10.366821, 35.585022, 10.758972, -10.366821,
                         36.091614, 15.016174, -11.819458, 36.091614, 15.016174, -11.819458,
                         34.526062, 12.641907, -10.768127, 34.526062, 12.641907, -10.768127,
                         36.524963, 12.173462, -9.135437, 37.857056, 16.542053, -11.161804,
                         37.857056, 16.542053, -11.161804, 37.088013, 12.394714, -9.571838,
                         34.96704, 14.585876, -12.930298, 34.96704, 14.585876, -12.930298,
                         36.643982, 16.670227, -11.070251, 36.643982, 16.670227, -11.070251]
def generateReq():
    req = DeepNaviReq()
    req.time = int(time.time() * 1000)
    f = open(imagePath, 'rb')
    req.image = f.read()
    i = 0
    while i < len(mags):
        magnetic = req.magneticList.add()
        magnetic.x = mags[i]
        magnetic.y = mags[i + 1]
        magnetic.z = mags[i + 2]
        i += 3
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
    req.id = '52aeb9c0672f11eab7b6001e64cce6eb'
    return req

class TestNavi(unittest.TestCase):
    def testLoc(self):
        req = generateReq()
        print(service.predict(req))