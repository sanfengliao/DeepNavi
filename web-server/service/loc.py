from model import Loc

from dao import LocDao, MapDao

locDao = LocDao()
mapDao = MapDao()

class LocService:
    def save(self, loc: Loc) -> Loc:
        return locDao.saveLoc(loc)

    def search(self, name: str, mapId: str):
        locs = []
        if mapId is None:
            locs = locDao.searchLocByPointName(name)
        else:
            locs = locDao.searchLocByPointNameAndMid(name, mapId)
        result = []
        for item in locs:
            m = mapDao.findById(item.mapId)
            result.append({'loc': item.toJsonMap(), 'map': m.toJsonMap()})
        return result
    def findByMapId(self, mid:str) -> typing.List[Loc]:
        return locDao.findByMapId(mid)
