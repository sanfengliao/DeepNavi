import unittest
from model import Map

class TestMap(unittest.TestCase):
    def testMap(self):
        m = Map()
        print(m.__dict__)