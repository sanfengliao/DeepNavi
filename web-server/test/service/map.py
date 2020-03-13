import unittest
from service import MapService
mapService = MapService()

class TestService(unittest.TestCase):
    def testNavi(self):
        print(mapService.navi('5e6b0be0e4f26f3c4a8dadc0', '5e6b0c1ae4f26f3c4a8dadc6', '5e68ca6e148277137d1c62b1'))