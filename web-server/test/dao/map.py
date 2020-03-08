import unittest
from unittest import TestCase
from dao.map import MapDao, Map
from bson import ObjectId
class TestMapDao(TestCase):
    mapDao = MapDao()
    def _test_saveMap(self):
        m = Map()
        m = self.mapDao.saveMap(m)
        self.assertTrue(isinstance(m.id, ObjectId))
    def _test_updateMap(self):
        m = Map({
            'id': '5e64cfce8aeb3647322e0880',
            'name': '超算6楼',
        })
        self.mapDao.updateMap(m)
        m = Map({
            'id': '5e64cfce8aeb3647322e0887',
            'name': '超算7楼'
        })
        m = self.mapDao.updateMap(m)
        self.assertTrue(isinstance(m.id, ObjectId))
    def test_dropMap(self):
        mid = '5e64d10df181a381cd28ca77'
        result = self.mapDao.deleteById(mid)
        self.assertEqual(result, 1)
if __name__ == "__main__":
    unittest.main()