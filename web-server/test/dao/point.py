from dao.point import PointDao
from model import Point

import unittest
from unittest import TestCase
from bson import ObjectId
class TesPointDao(TestCase):
    pointDao = PointDao()
    def test_savePoint(self):
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
        p = self.pointDao.savePoint(p)
        p.planCoordinate = {'x': -50, 'y': 0}
        p.actualCoordinate = {'x': -50, 'y': -70}
        p = self.pointDao.updatePoint(p)
        print(p.toJsonMap())
    def _test_dropPoint(self):
        p = Point('5e64cfce8aeb3647322e0880', {
            'id': '5e64da47be70ccf2eb974ebf'
        })
        print(p.id)
        result = self.pointDao.dropPoint(p)
        self.assertEqual(result, 1)
if __name__ == "__main__":
    unittest.main()