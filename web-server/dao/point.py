from model import Point
from dao.mongodb import db
from bson import ObjectId
import typing
DBPREFIX = 'point'

class PointDao:
    def savePoint(self, point: Point) -> Point:
        col = self.getColl(point.mapId)
        result = col.insert_one(point.toDBMap())
        point.id = str(result.inserted_id)
        return point
    

    def updatePoint(self, point: Point) -> Point:
        if len(point.id) != 24:
            return point
        col = self.getColl(point.mapId)
        result = col.update_one({'_id': ObjectId(point.id)}, {'$set': point.toDBMap()}, upsert=True)
        if result.upserted_id:
            point.id = str(result.upserted_id)
        path = pathDao.findPathByPointAId(point.id, point.mapId)
        for item in path:
            item.pointA = {
                'id': point.id,
                'planCoordinate': point.planCoordinate,
                'actualCoordinate': point.actualCoordinate
            }
            pathDao.updatePath(item)
        path = pathDao.findPathByPointBId(point.id, point.mapId)
        for item in path:
            item.pointB = {
                'id': point.id,
                'planCoordinate': point.planCoordinate,
                'actualCoordinate': point.actualCoordinate
            }
            pathDao.updatePath(item)
        return point
    
    def dropPoint(self, point: Point) -> int:
        col = self.getColl(point.mId)
        result = col.delete_one({'_id': ObjectId(point.id)})
        pathDao.dropPathByPid(point.id, point.mapId)
        return result.deleted_count
        
    def dropPointById(self, pid:str, mapId:str) -> int:
        col = self.getColl(mapId)
        result = col.delete_one({'_id': ObjectId(pid)})
        pathDao.dropPathByPid(pid, mapId)
        return result.deleted_count
    
    def findById(self, id: str, mid: str) -> Point:
        col = self.getColl(mid)
        result = col.find_one({'_id': ObjectId(id)})
        if result is not None:
            point = Point(mid, result)
            point.id = id
            return point
        return None

    def findAll(self, mid: str) -> typing.List[Point]:
        col = self.getColl(mid)
        cursor = col.find()
        result = []
        for item in cursor:
            p = Point(item['mapId'], item)
            p.id = str(item['_id'])
            result.append(p)
        return result
    
    def isPidExistMap(self, pid: str, mid: str) -> bool:
        if len(pid) < 24:
            return False
        col = self.getColl(mid)
        return col.count_documents({'_id': ObjectId(pid)}) > 0
    
    def addAdjacence(self, pid: str, mid: str, *args) -> Point:
        point = self.findById(pid, mid)
        if point is not None:
            for item in args:
                pAddId = ''
                if isinstance(item, str) and len(item) == 24:
                    pAddId = item
                elif isinstance(item, Point) and len(item.id) == 24:
                    pAddId = item.id
                else:
                    continue
                if self.isPidExistMap(pAddId, mid):
                    point.addAdjacence(pAddId)
        point = self.updatePoint(point)
        return point

    def getAdjacence(self, pid: str, mid: str) -> typing.List[Point]:
        col = self.getColl(mid)
        point = self.findById(pid, mid)
        adjacenceIds = point.adjacence
        adjacenceIds = [ObjectId(id) for id in adjacenceIds]
        cursor = col.find({'_id': {'$in': adjacenceIds}})
        result = []
        for item in cursor:
            result.append(self.assemblePoint(item))
        return result
    
    def findPointsIn(self, ids: typing.List[str], mapId: str) -> typing.List[Point]:
        ids = [ObjectId(id) for id in ids]
        col = self.getColl(mapId)
        cursor = col.find({'_id': {'$in': ids}})
        result = []
        for item in cursor:
            result.append(self.assemblePoint(item))
        return result

    def findClosePoint(self, actualCoordinate: dict, mapId: str) -> typing.List[Point]:
        if len(mapId) != 24:
            return []
        paths = pathDao.findPathWherePointIn(actualCoordinate, mapId)
        pointIds = []
        for item in paths:
            pointIds.append(item.pointA['id'])
            pointIds.append(item.pointB['id'])
        return self.findPointsIn(pointIds, mapId)

    def assemblePoint(self, item: dict) -> Point:
        p = Point(item['mapId'], item)
        p.id = str(item['_id'])
        return p
    
    def getColl(self, mId: str):
        return db.get_collection(DBPREFIX + '_' + mId)



from .path import PathDao
pathDao = PathDao()