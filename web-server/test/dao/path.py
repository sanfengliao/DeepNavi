import unittest

from dao import EdgeDao, PointDao

from model import Path, Point
pointDao = PointDao()
class TestEdgeDao(unittest.TestCase):
    EdgeDao = EdgeDao()
    def test_savePath(self):
        points = pointDao.findAll('5e64cfce8aeb3647322e0880')
        path = Path('5e64cfce8aeb3647322e0880', points[0].toJsonMap(), points[1].toJsonMap())
        self.EdgeDao.savePath(path)
        path = Path('5e64cfce8aeb3647322e0880', points[1].toJsonMap(), points[2].toJsonMap())
        self.EdgeDao.savePath(path)
        path = Path('5e64cfce8aeb3647322e0880', points[0].toJsonMap(), points[2].toJsonMap())
        self.EdgeDao.savePath(path)
    def _test_updatePath(self):
        path = Path('5e64cfce8aeb3647322e0880', {
            'id': '5e64de9265bf8bca60db5865',
            "planCoordinate" : { "x" : -50, "y" : 0 }, 
            "actualCoordinate" : { "x" : -50, "y" : -70 },
        }, {
            'id': '5e64deb3bd4b95cd35f1430f',
           "planCoordinate" : { "x" : 70, "y" : 70 }, 
           "actualCoordinate" : { "x" : 70, "y" : 70 }
        })
        path = self.EdgeDao.savePath(path)
        path.pointA['planCoordinate'] = { "x" : -50, "y" : 0 }
        self.assertEqual(len(path.id), 24)
    
    def _testDropPath(self):
        path = Path(mapId='5e64cfce8aeb3647322e0880', id='5e64e06d5f5e97bf5ead919b')
        print(path.id, path.mapId)
        result = self.EdgeDao.dropPath(path)
        self.assertEqual(result, 1)
    
    def _test_findPathByPointAId(self):
        result = self.EdgeDao.findPathByPointAId('5e64de9265bf8bca60db5865', '5e64cfce8aeb3647322e0880')
        for item in result:
            print(item.toJsonMap())
    def _test_dropPathByPid(self):
        result = self.EdgeDao.dropPathByPid('5e64de9265bf8bca60db5865', '5e64cfce8aeb3647322e0880')
        print(result)