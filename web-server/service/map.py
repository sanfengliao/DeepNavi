from model import Map, Point
from dao import MapDao, PointDao
import copy
import math
import typing

mapDao = MapDao()
pointDao = PointDao()
class MapService:
    def saveMap(self, m: Map) -> Map:
        m = mapDao.saveMap(m)
        return m
    
    def findById(self, id: str) -> Map:
        m = mapDao.findById(id)
        return m

    def navi(self, srcId:str, dstId:str, mid:str)->typing.Dict[str, dict]:
        pointList = self.findMapPoints(mid)
        pointDict = self.pointsToDict(pointList)
        return self.dijkstra(srcId, dstId, pointDict)

    def findMapPoints(self, id:str) -> typing.List[Point]:
        return pointDao.findAll(id)
    
    def pointsToDict(self, points: typing.List[Point]) -> typing.Dict[str, Point]:
        pointDict = {}
        for item in points:
            pointDict[item.id] = item
        return pointDict
    
    def initPathDict(self, srcId, pointDict: typing.Dict[str, Point]) -> typing.Dict[str, dict]:
        pathDict = {}
        srcPoint = pointDict[srcId]
        for k in pointDict:
            if k != srcId:
                pathDict[k] = {
                    'dis': -1,
                    'path': []
                }
        adjacence = srcPoint.adjacence
        for item in adjacence:
            adjacencePoint = pointDict[item]
            x1 = adjacencePoint.actualCoordinate['x']
            y1 = adjacencePoint.actualCoordinate['y']
            x2 = srcPoint.actualCoordinate['x']
            y2 = srcPoint.actualCoordinate['y']
            pathDict[item]['dis'] = math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2))
            pathDict[item]['path'].append(srcId)
            pathDict[item]['path'].append(item)
        return pathDict

    def dijkstra(self, srcId:str, dstId:str, pointDict: typing.Dict[str,Point]):
        pathDict = self.initPathDict(srcId, pointDict)
        findIds = {}
        n = len(pathDict) - 1
        while n > 0:
            minDis = -1
            minId = ''
            print(findIds)
            print(pathDict)
            for k, v in pathDict.items():
                if (v['dis'] < minDis or minDis < 0) and k not in findIds:
                    minId = k
                    minDis = v['dis']
            if minId == dstId:
                break
            findIds[minId] = True
            minPoint = pointDict[minId]
            adjacence = minPoint.adjacence
           
            for item in adjacence:
                if item not in findIds and item != srcId:
                    adjacencePoint = pointDict[item]
                    x1 = adjacencePoint.actualCoordinate['x']
                    y1 = adjacencePoint.actualCoordinate['y']
                    x2 = minPoint.actualCoordinate['x']
                    y2 = minPoint.actualCoordinate['y']
                    dis = math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2))
                    adjacenceDis = pathDict[minId]['dis'] + dis
                    if pathDict[item]['dis'] < 0 or adjacenceDis < pathDict[item]['dis']:
                        pathDict[item]['dis'] = adjacenceDis
                        pathDict[item]['path'] = copy.deepcopy(pathDict[minId]['path'])
                        pathDict[item]['path'].append(item)
            n -= 1
        print(pathDict)
        return pathDict[dstId]