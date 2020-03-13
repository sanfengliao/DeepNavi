import unittest
from service import MapService
mapService = MapService()

class TestService(unittest.TestCase):
    def _testNavi(self):
        print(mapService.navi('5e6b0be0e4f26f3c4a8dadc0', '5e6b0c1ae4f26f3c4a8dadc6', '5e68ca6e148277137d1c62b1'))
    def _testFindEdgeWherePointIn(self):
        edges = mapService.findEdgeWherePointIn({'x': 0, 'y': -50}, '5e68ca6e148277137d1c62b1')
        for item in edges:
            print(item.toJsonMap())
    def testNavi(self):
        print(mapService.navi({'x': 0, 'y': 0}, {'x': 70, 'y': -50}, '5e68ca6e148277137d1c62b1'))
        print(mapService.navi({'x': 50, 'y': 0}, {'x': 70, 'y': -50}, '5e68ca6e148277137d1c62b1'))
        print(mapService.navi({'x': 50, 'y': 0}, {'x': 50, 'y': -50}, '5e68ca6e148277137d1c62b1'))