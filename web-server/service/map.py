from model import Map, Point, Edge
from dao import MapDao, PointDao, EdgeDao
import copy
import math
import typing
import uuid

mapDao = MapDao()
pointDao = PointDao()
edgeDao = EdgeDao()

def calDis(src: dict, dist: dict) -> float:
    return math.sqrt((src['x'] - dist['x']) * (src['x'] - dist['x']) + (src['y'] - dist['y']) * (src['y'] - dist['y']))

class MapService:
    def saveMap(self, m: Map) -> Map:
        m = mapDao.saveMap(m)
        return m
    
    def findById(self, id: str) -> Map:
        m = mapDao.findById(id)
        return m

    def navi(self, src:dict, dst:dict, mid:str) -> typing.Dict[str, dict]:
        srcIds = self.findClosePointIds(src, mid)
        dstIds = self.findClosePointIds(dst, mid)
        pointList = self.findMapPoints(mid)
        pointDict = self.pointsToDict(pointList)
        minDis = -1
        pathResult = None
        for srcId in srcIds:
            for dstId in dstIds:
                pathInfo = self.findPath(srcId, dstId, pointDict)
                path = pathInfo['path']
                srcDis = calDis(src, pointDict[path[0]].actualCoordinate)
                dstDis = calDis(dst, pointDict[path[-1]].actualCoordinate)
                newDis = srcDis + dstDis + pathInfo['dis']
                if minDis == -1 or newDis < minDis:
                    minDis = newDis
                    pathResult = {
                        'dis': minDis,
                        'path': path
                    }
        pathResult = [pointDict[item].toJsonMap() for item in pathResult['path']]
        pathId = str(uuid.uuid1()).replace('-', '')
        return {
            'pathId': pathId,
            'path': pathResult
        }
    
    def findClosePointIds(self, actualCoordinate:dict, mid:str) -> typing.List[str]:
        edges = self.findEdgeWherePointIn(actualCoordinate, mid)
        pointIds = None
        if len(edges) > 1:
            pointIds = [self.findCommonPointId(edges)]
        elif len(edges) == 1:
            edge = edges[0]
            if calDis(edge.pointA['actualCoordinate'], actualCoordinate) <= edge.edgeWidth:
                pointIds = [edge.pointA['id']]
            elif calDis(edge.pointB['actualCoordinate'], actualCoordinate) <= edge.edgeWidth:
                pointIds = [edge.pointB['id']]
            else:
                pointIds = [edge.pointA['id'], edge.pointB['id']]
        return pointIds
    
    def findCommonPointId(self, edges: typing.List[Edge]) -> str:
        pointMap = {}
        for edge in edges:
            if edge.pointA['id'] in pointMap:
                return edge.pointA['id']
            else:
                pointMap[edge.pointA['id']] = 1
            if edge.pointB['id'] in pointMap:
                return edge.pointB['id']
            else:
                pointMap[edge.pointB['id']] = 1
        return None


    def findPath(self, srcId:str, dstId:str, pointDict: typing.Dict[str, Point])->typing.Dict[str, dict]:
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
        if srcId == dstId:
            return {
                'dis': 0,
                'path': [srcId]
            }
        pathDict = self.initPathDict(srcId, pointDict)
        findIds = {}
        n = len(pathDict) - 1
        while n > 0:
            minDis = -1
            minId = ''
            for k, v in pathDict.items():
                if k not in findIds and (v['dis'] > 0 and v['dis'] < minDis or minDis < 0) :
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
        return pathDict[dstId]
    
    def findEdgeWherePointIn(self, actualCoordinate: dict, mapId: str) -> typing.List[Edge]:
        return edgeDao.findEdgeWherePointIn(actualCoordinate, mapId)