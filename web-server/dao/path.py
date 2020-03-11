from model import Path
from dao.mongodb import db
from bson import ObjectId
import math
import typing 
DBPREFIX = 'path'

def isIn(a: int, x1: int, x2: int) -> bool:
    return x1 <= a <= x2 or x2 <= a <= x1

def calDistance(src: dict, pointA: dict, pointB: dict) -> float:
    a = pointB['y'] - pointA['y'] # y2 - y1
    b = pointA['x'] - pointB['x'] # x1 - x2
    c = pointB['x'] * pointA['y'] - pointA['x'] * pointB['y']
    x = src['x']
    y = src['y']
    dis = abs(a * x + b * y + c) / math.sqrt(a * a + b * b)
    return dis

def isInPath(src: dict, pointA: dict, pointB: dict, planWidth: float) -> bool:
    return calDistance(src, pointA, pointB) <= planWidth and (isIn(src['x'], pointA['x'], pointB['x']) or isIn(src['y'], pointA['y'], pointB['y']))


class PathDao:
  
    def savePath(self, path: Path) -> Path:
        col = self.getColl(path.mapId)
        result = col.insert_one(path.toDBMap())
        path.id = str(result.inserted_id)
        pointDao.addAdjacence(path.pointA['id'], path.mapId, path.pointB['id'])
        pointDao.addAdjacence(path.pointB['id'], path.mapId, path.pointA['id'])
        return path
  
    def findAll(self, mapId: str) -> typing.List[Path]:
        col = self.getColl(mapId)
        cursor = col.find()
        result = []
        for item in cursor:
            result.append(self.asseblePath(item))
        return result

    def findPathByPointAId(self, pid: str, mid: str) -> typing.List[Path]:
        col = self.getColl(mid)
        cursor = col.find({'pointA.id': pid})
        result = []
        for item in cursor:
            result.append(self.asseblePath(item))
        return result

    def findPathWherePointIn(self, actualCoordinate: dict, mapId: str) -> typing.List[Path]:
        if len(mapId) != 24:
            return []
        items = self.findAll(mapId)
        result = []
        for item in items:
            actualCoordinateA = item.pointA['actualCoordinate']
            actualCoordinateB = item.pointB['actualCoordinate']
            if isInPath(actualCoordinate, actualCoordinateA, actualCoordinateB, item.planWidth):
                result.append(item)
        return result

    def findPathByPointBId(self, pid: str, mid: str) -> typing.List[Path]:
        col = self.getColl(mid)
        cursor = col.find({'pointB.id': pid})
        result = []
        for item in cursor:
            result.append(self.asseblePath(item))
        return result
    
    def updatePath(self, path: Path) -> Path:
        if len(path.id) != 24:
            return path
        col = self.getColl(path.mapId)
        result = col.update_one({'_id': ObjectId(path.id)}, {'$set': path.toDBMap()}, upsert=True)
        if result.upserted_id:
            path.id = str(result.upserted_id)
        return path
  
    def dropPath(self, path: Path) -> int:
        col = self.getColl(path.mapId)
        result = col.delete_one({'_id': ObjectId(path.id)})
        return result.deleted_count
  
    def dropPathById(self, pathId:str, mid:str) -> int:
        col = self.getColl(mid)
        result = col.delete_one({'_id': ObjectId(pathId)})
        return result.deleted_count
  
    def dropPathByPid(self, pid: str, mapId: str) -> int:
        col = self.getColl(mapId)
        result = col.delete_many({'$or': [{'pointA.id': pid}, {'pointB.id': pid}]})
        return result.deleted_count
    
    def asseblePath(self, item: dict) -> Path:
        path = Path(item['mapId'], item['pointA'], item['pointB'])
        path.id = str(item['_id'])
        return path

    def getColl(self, mid: str):
        return db.get_collection(DBPREFIX + '_' + mid)

from .point import PointDao
pointDao = PointDao()