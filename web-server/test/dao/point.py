from dao.point import PointDao
from model import Point

import unittest
from unittest import TestCase
from bson import ObjectId
class TesPointDao(TestCase):
    pointDao = PointDao()
    def _test_savePoint(self):
        p = Point('5e64cfce8aeb3647322e0880', {
            'planCoordinate': {'x': 70, 'y': 70},
            'actualCoordinate': {'x': 70, 'y': 70},
        })
        p = self.pointDao.savePoint(p)
        self.assertTrue(len(p.id) == 24)
    def _test_updatePoint(self):
        p = Point('5e64cfce8aeb3647322e0880', {
            'planCoordinate': {'x': 0, 'y': 70},
            'actualCoordinate': {'x': 0, 'y': 70},
        })
        p.id = '5e64d614b40dda58e5e7b190'
        p = self.pointDao.updatePoint(p)
        print(p.toJsonMap())
    def _test_dropPoint(self):
        p = Point('5e64cfce8aeb3647322e0880', {
            'id': '5e64da47be70ccf2eb974ebf'
        })
        print(p.id)
        result = self.pointDao.dropPoint(p)
        self.assertEqual(result, 1)
    def test_dropPointById(self):
        result = self.pointDao.dropPointById('5e64d614b40dda58e5e7b190', '5e64cfce8aeb3647322e0880')
        print(result)
    def _testFindById(self):
        p = self.pointDao.findById('5e64de9265bf8bca60db5865', '5e64cfce8aeb3647322e0880')
        print(p.toJsonMap())
    def _testFindAll(self):
        ps = self.pointDao.findAll('5e64cfce8aeb3647322e0880')
        print(ps[0])
        self.assertEqual(len(ps), 4)
    def _testAddAdjacence(self):
        point = self.pointDao.addAdjacence('5e64d614b40dda58e5e7b190', '5e64cfce8aeb3647322e0880', '5e64de9265bf8bca60db5865', '5e64deb3bd4b95cd35f1430f')
        self.assertEqual(len(point.adjacence), 2)
    def _testGetAdjacence(self):
        result = self.pointDao.getAdjacence('5e64d614b40dda58e5e7b190', '5e64cfce8aeb3647322e0880')
        for item in result:
            print(item.id)
        self.assertEqual(len(result), 2)
if __name__ == "__main__":
    unittest.main()