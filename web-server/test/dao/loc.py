import unittest
from model import Loc
from dao import LocDao
locDao = LocDao()
class TestLocDao(unittest.TestCase):
    def _test_save(self):
        loc = Loc('5e64cfce8aeb3647322e0880', '超算5楼', {
            'planCoordinate': {'x': 0, 'y': 0, 'z': 0},
            'actualCoordinate': {'x': 0, 'y': 0, 'z': 0}
        })
        loc = locDao.saveLoc(loc)
        self.assertEqual(len(loc.id), 24)
    
    def _test_update(self):
        loc = Loc('5e64cfce8aeb3647322e0880', '超算5楼', {
            'planCoordinate': {'x': 0, 'y': 0, 'z': 0},
            'actualCoordinate': {'x': 0, 'y': 0, 'z': 0}
        })
        loc = locDao.saveLoc(loc)
        self.assertEqual(len(loc.id), 24)
        loc.name = '超算六楼'
        loc = locDao.updateLoc(loc)
    
    def test_searchLocByPointName(self):
        result = locDao.searchLocByPointName('超算')
        for item in result:
            print(item.toJsonMap())

    def test_searchLocByPointNameAndMid(self):
        result = locDao.searchLocByPointNameAndMid('超算', '5e64cfce8aeb3647322e0880')
        for item in result:
            print(item.toJsonMap())