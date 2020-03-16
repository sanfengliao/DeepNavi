import unittest
from model import Map
from service.naviservice.ttypes import Coor

class TestMap(unittest.TestCase):
    def testMap(self):
        coor = Coor(x=0, y=0, z=0)