from model import Map, Point, Edge
from dao import MapDao, PointDao, EdgeDao, RedisDao
import copy
import math
import typing
import uuid

mapDao = MapDao()
pointDao = PointDao()
edgeDao = EdgeDao()
redisDao = RedisDao()

def calDis(src: dict, dist: dict) -> float:
    return math.sqrt((src['x'] - dist['x']) * (src['x'] - dist['x']) + (src['y'] - dist['y']) * (src['y'] - dist['y']))

def calAngle(v1: dict, v2: dict, isClockWise:bool=False) -> float:
    a = v1['x'] * v2['x'] + v1['y'] * v2['y']
    v1l = math.sqrt(v1['x'] * v1['x'] + v1['y'] * v1['y'])
    v2l = math.sqrt(v2['x'] * v2['x'] + v2['y'] * v2['y'])
    v1fv2 = v2['x'] * v1['y'] - v1['x']*v2['y']
    angle = math.acos(a / (v1l * v2l)) * 180 / math.pi
    if v1fv2 < 0 and isClockWise:
        angle = -angle
    return angle

class MapService:
    def saveMap(self, m: Map) -> Map:
        m = mapDao.saveMap(m)
        return m
    
    def findById(self, id: str) -> Map:
        m = mapDao.findById(id)
        return m

    
    def isInCommonEdge(self,srcIds:typing.List[str], dstIds: typing.List[str], srcEdge: Edge, dstEdge: Edge):
        if len(srcIds) == 2 and len(dstIds) == 2 and srcIds[0] == dstIds[0] and srcIds[1] == dstIds[1]:
            return 2
        
        if len(srcIds) == 1 and len(dstIds) == 2 and (srcIds[0] == dstIds[0] or srcIds[0] == dstIds[1]):
            return 2
        if len(dstIds) == 1 and len(srcIds) == 2 and (dstIds[0] == srcIds[0] or dstIds[0] == srcIds[1]):
            return 2
        
        if len(dstIds) == 1 and len(srcIds) == 1 and dstIds[0] == srcIds[0]:
            return 1
        return 0

    def calAngles(self, pathResult: typing.List[dict], standardVector: dict, isClockWise:bool) -> typing.List[float]:
        i = 0
        angles = []
        while i < len(pathResult) - 1:
            point1 = pathResult[i]['actualCoordinate']
            point2 = pathResult[i + 1]['actualCoordinate']
            v1 = {
                'x': point2['x'] - point1['x'],
                'y': point2['y'] - point1['y']
            }
            angles.append(calAngle(v1, standardVector, isClockWise))
            i += 1
        return angles
    def navi(self, src:dict, dst:dict, mid:str) -> typing.Dict[str, dict]:
        
        srcIds, srcEdge = self.findClosePointIds(src, mid)
        dstIds, dstEdge = self.findClosePointIds(dst, mid)
        pointList = self.findMapPoints(mid)
        pointDict = self.pointsToDict(pointList)
        pathId = str(uuid.uuid1()).replace('-', '')
        isInCommonEdge = self.isInCommonEdge(srcIds, dstIds, srcEdge, dstEdge)
        pathResult = None
        angles = []
        if isInCommonEdge == 2:
            if len(srcIds) == 2 and len(dstIds) == 2:
                pathResult = [{'actualCoordinate': src}, {'actualCoordinate': dst}]
            elif len(srcIds) == 1:
                pathResult = [pointDict[srcIds[0]].toJsonMap(), {'actualCoordinate': dst}]
            elif len(dstIds) == 1:
                pathResult = [{'actualCoordinate': src}, pointDict[dstIds[0]].toJsonMap()]
        elif isInCommonEdge == 1:
           return {
               'pathId': pathId,
               'path': []
           }
        else:
            minDis = -1
            finalPath = None
            for srcId in srcIds:
                for dstId in dstIds:
                    pathInfo = self.findPath(srcId, dstId, pointDict)
                    path = pathInfo['path']
                    srcDis = calDis(src, pointDict[path[0]].actualCoordinate)
                    dstDis = calDis(dst, pointDict[path[-1]].actualCoordinate)
                    newDis = srcDis + dstDis + pathInfo['dis']
                    if minDis == -1 or newDis < minDis:
                        minDis = newDis
                        finalPath = {
                            'dis': minDis,
                            'path': path
                        }
            pathResult = [pointDict[item].toJsonMap() for item in finalPath['path']]
            if len(srcIds) > 1:
                pathResult.insert(0, {'actualCoordinate': src})
        
            if len(dstIds) > 1:
                pathResult.append({'actualCoordinate': dst})
        m = mapDao.findById(mid)
        standardVector = {
            'x': m.standardVector[0],
            'y': m.standardVector[1]
        }
        angles = self.calAngles(pathResult, standardVector, m.isClockwise)
        saveResult = {
            'mapId': mid,
            'index': 0,
            'currentAngle': angles[0],
            'toPoint': pathResult[1],
            'angles': angles,
            'pathResult': pathResult

        }
        self.savePathResult(pathId, saveResult)
        return {
            'pathId': pathId,
            'path': pathResult
        }
    
    def savePathResult(self, pathId:str, result:dict):
        redisDao.setDict(pathId, result)

    def findClosePointIds(self, actualCoordinate:dict, mid:str) -> typing.Tuple[typing.List[str], Edge]:
        edges = self.findEdgeWherePointIn(actualCoordinate, mid)
        pointIds = None
        if len(edges) > 1:
            pointIds = [self.findCommonPointId(edges)]
            return pointIds, None
        elif len(edges) == 1:
            edge = edges[0]
            if calDis(edge.pointA['actualCoordinate'], actualCoordinate) <= edge.edgeWidth:
                pointIds = [edge.pointA['id']]
                return pointIds, None
            elif calDis(edge.pointB['actualCoordinate'], actualCoordinate) <= edge.edgeWidth:
                pointIds = [edge.pointB['id']]
                return pointIds, None
            else:
                pointIds = [edge.pointA['id'], edge.pointB['id']]
                return pointIds, edges[0]
    
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