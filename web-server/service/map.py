from model import Map
from dao import MapDao

mapDao = MapDao()
class MapService:
    def saveMap(self, m: Map) -> Map:
        m = mapDao.saveMap(m)
        return m
    
    def findById(self, id: str) -> Map:
        m = mapDao.findById(id)
        return m