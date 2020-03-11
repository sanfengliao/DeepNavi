from model import Edge
from dao import EdgeDao, PointDao
from decorator import transaction
import typing
edgeDao = EdgeDao()
pointDao = PointDao()

class EdgeService:
    # @transaction
    def save(self, pointAId:str, pointBId:str, edgeWidth:float, mapId:str) -> Edge:
        pointA = pointDao.findById(pointAId, mapId)
        pointB = pointDao.findById(pointBId, mapId)
        if mapId is None:
            mapId = pointA.mapId
        edge = Edge(mapId, pointA.toJsonMap(), pointB.toJsonMap(), edgeWidth=edgeWidth)
        return edgeDao.saveEdge(edge)

    def findAll(self, mapId: str) -> typing.List[Edge]:
        return edgeDao.findAll(mapId)