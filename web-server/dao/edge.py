from model import Edge
from dao.mongodb import db
from bson import ObjectId
import math
import typing 
DBPREFIX = 'edge'
from decorator import transaction

def isNotIn(a: int, x1: int, x2: int) -> bool:
    return ((a > x1 and a > x2) or (a < x1 and a < x2))

def calDistance(src: dict, pointA: dict, pointB: dict) -> float:
    a = pointB['y'] - pointA['y'] # y2 - y1
    b = pointA['x'] - pointB['x'] # x1 - x2
    c = pointB['x'] * pointA['y'] - pointA['x'] * pointB['y']
    x = src['x']
    y = src['y']
    dis = abs(a * x + b * y + c) / math.sqrt(a * a + b * b)
    return dis

def isInEdge(src: dict, pointA: dict, pointB: dict, edgeWidth: float) -> bool:
    return calDistance(src, pointA, pointB) <= edgeWidth and (not (isNotIn(src['x'], pointA['x'], pointB['x']) or isNotIn(src['y'], pointA['y'], pointB['y'])))


class EdgeDao:
    @transaction
    def saveEdge(self, edge: Edge) -> Edge:
        col = self.getColl(edge.mapId)
        result = col.insert_one(edge.toDBMap())
        edge.id = str(result.inserted_id)
        pointDao.addAdjacence(edge.pointA['id'], edge.mapId, edge.pointB['id'])
        pointDao.addAdjacence(edge.pointB['id'], edge.mapId, edge.pointA['id'])
        return edge
  
    def findAll(self, mapId: str) -> typing.List[Edge]:
        col = self.getColl(mapId)
        cursor = col.find()
        result = []
        for item in cursor:
            result.append(self.assebleEdge(item))
        return result

    def findEdgeByPointAId(self, pid: str, mid: str) -> typing.List[Edge]:
        col = self.getColl(mid)
        cursor = col.find({'pointA.id': pid})
        result = []
        for item in cursor:
            result.append(self.assebleEdge(item))
        return result

    def findEdgeWherePointIn(self, actualCoordinate: dict, mapId: str) -> typing.List[Edge]:
        if len(mapId) != 24:
            return []
        items = self.findAll(mapId)
        result = []
        for item in items:
            actualCoordinateA = item.pointA['actualCoordinate']
            actualCoordinateB = item.pointB['actualCoordinate']
            if isInEdge(actualCoordinate, actualCoordinateA, actualCoordinateB, item.edgeWidth):
                result.append(item)
        return result

    def findEdgeByPointBId(self, pid: str, mid: str) -> typing.List[Edge]:
        col = self.getColl(mid)
        cursor = col.find({'pointB.id': pid})
        result = []
        for item in cursor:
            result.append(self.assebleEdge(item))
        return result
    
    def updateEdge(self, edge: Edge) -> Edge:
        if len(edge.id) != 24:
            return edge
        col = self.getColl(edge.mapId)
        result = col.update_one({'_id': ObjectId(edge.id)}, {'$set': edge.toDBMap()}, upsert=True)
        if result.upserted_id:
            edge.id = str(result.upserted_id)
        return edge
  
    def dropEdge(self, edge: Edge) -> int:
        col = self.getColl(edge.mapId)
        result = col.delete_one({'_id': ObjectId(edge.id)})
        return result.deleted_count
  
    def dropEdgeById(self, edgeId:str, mid:str) -> int:
        col = self.getColl(mid)
        result = col.delete_one({'_id': ObjectId(edgeId)})
        return result.deleted_count
  
    def dropEdgeByPid(self, pid: str, mapId: str) -> int:
        col = self.getColl(mapId)
        result = col.delete_many({'$or': [{'pointA.id': pid}, {'pointB.id': pid}]})
        return result.deleted_count
    
    def assebleEdge(self, item: dict) -> Edge:
        edge = Edge(item['mapId'], item['pointA'], item['pointB'])
        edge.id = str(item['_id'])
        return edge

    def getColl(self, mid: str):
        return db.get_collection(DBPREFIX + '_' + mid)

from .point import PointDao
pointDao = PointDao()