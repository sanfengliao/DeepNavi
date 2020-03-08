import unittest

from dao import PathDao

from model import Path, Point

class TestPathDao(unittest.TestCase):
    pathDao = PathDao()
    def _test_savePath(self):
        p1 = Point('5e64cfce8aeb3647322e0880', {
            'id': '5e64de9265bf8bca60db5865',
            "planCoordinate" : { "x" : -50, "y" : 0 }, 
            "actualCoordinate" : { "x" : -50, "y" : -70 },
        })
        p2 = Point('5e64cfce8aeb3647322e0880', {
            'id': '5e64deb3bd4b95cd35f1430f',
           "planCoordinate" : { "x" : 70, "y" : 70 }, 
           "actualCoordinate" : { "x" : 70, "y" : 70 }
        })
        path = Path(p1, p2)
        path = self.pathDao.savePath(path)
        self.assertEqual(len(path.id), 24)
    def _test_updatePath(self):
        p1 = Point('5e64cfce8aeb3647322e0880', {
            'id': '5e64de9265bf8bca60db5865',
            "planCoordinate" : { "x" : -50, "y" : 70 }, 
            "actualCoordinate" : { "x" : -50, "y" : 70 },
        })
        p2 = Point('5e64cfce8aeb3647322e0880', {
            'id': '5e64deb3bd4b95cd35f1430f',
           "planCoordinate" : { "x" : 70, "y" : 70 }, 
           "actualCoordinate" : { "x" : 70, "y" : 70 }
        })
        path = Path(p1, p2)
        path.id = '5e64e06d5f5e97bf5ead919b'
        path = self.pathDao.updatePath(path)
        self.assertEqual(len(path.id), 24)
    
    def testDropPath(self):
        path = Path(mapId='5e64cfce8aeb3647322e0880', id='5e64e06d5f5e97bf5ead919b')
        print(path.id, path.mapId)
        result = self.pathDao.dropPath(path)
        self.assertEqual(result, 1)