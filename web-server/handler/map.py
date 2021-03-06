from tornado.web import RequestHandler
from model import Map
from service import MapService, PointService, EdgeService, LocService
from adapter import LocalFileAdapter
import logging
import uuid
import json
import os

mapService = MapService()
pointService = PointService()
edgeService = EdgeService()
localFileAdapter = LocalFileAdapter()
locService = LocService()

class MapHandler(RequestHandler):
    def get(self):
        mapId = self.get_query_argument('mapId')
        includePoint = int(self.get_query_argument('includePoint', 0))
        includeEdge = int(self.get_query_argument('includeEdge', 0))
        includeLoc = int(self.get_query_argument('includeEdge', 0))
        result = {'code': 0, 'data': {}}
        m = mapService.findById(mapId)
        result['data']['map'] = m.toJsonMap()
        if includePoint == 1:
            points = pointService.findAll(mapId)
            result['data']['points'] = [item.toJsonMap() for item in points]
        if includeEdge == 1:
            edges = edgeService.findAll(mapId)
            result['data']['edges'] = [item.toJsonMap() for item in edges]
        if includeLoc == 1:
            locs = locService.findByMapId(mapId)
            result['data']['locs'] = [item.toJsonMap() for item in locs]
        self.write(result)

    def post(self):
        name = self.get_body_argument('name')
        planSize = self.get_body_arguments('planSize')
        planUnix = self.get_body_argument('planUnit')
        actualSize = self.get_body_arguments('actualSize')
        actualUnit = self.get_body_argument('actualUnit')
        originInPlan = self.get_body_arguments('originInPlan')
        rotationAngle = self.get_body_arguments('rotationAngle')
        originInActual = self.get_body_arguments('originInActual')
        isClockwise = bool(int(self.get_body_argument('isClockwise', False)))
        
        standardVector = self.get_body_argument('standardVector', '0,1')
        standardVector = [float(item) for item in standardVector.split(',')]

        if len(planSize) > 1:
            actualSize = [float(item) for item in actualSize]
        else:
            actualSize = [float(item) for item in actualSize[0].split(',')]

        if len(planSize) > 1:
            planSize = [float(item) for item in planSize]
        else:
             planSize = [float(item) for item in planSize[0].split(',')]

        if len(originInPlan) > 1:
            originInPlan = [float(item) for item in originInPlan]
        else:
            originInPlan = [float(item) for item in originInPlan[0].split(',')]
        
        if len(rotationAngle) > 1:
            rotationAngle = [float(item) for item in rotationAngle]
        else:
            rotationAngle = [float(item) for item in rotationAngle[0].split(',')]
        
        if len(originInActual) > 1:
            originInActual = [float(item) for item in originInActual]
        else:
            originInActual = [float(item) for item in originInActual[0].split(',')]


           

        planImage = self.request.files['planImage']
        originName = planImage[0]['filename']
        filename = name + os.path.splitext(originName)[-1]
        fileBody = planImage[0]['body']
        planPath = localFileAdapter.save(filename, fileBody)

        mapDict = {
            'name': name,
            'planSize': planSize,
            'planUnix': planUnix,
            'actualSize': actualSize,
            'actualUnit': actualUnit,
            'originInPlan': originInPlan,
            'planPath': planPath,
            'originInActual': originInActual,
            'rotationAngle': rotationAngle,
            'isClockwise': isClockwise,
            'standardVector': standardVector
        }
        logging.info(mapDict)
        m = Map(mapDict)
        m = mapService.saveMap(m)
        self.write({'code': 0, 'data': m.toJsonMap()})

class MapNaviHandler(RequestHandler):
    def post(self):
        data = json.loads(self.request.body)
        mid = data['mapId']
        src = data['src']['actualCoordinate']
        dst = data['dst']['actualCoordinate']
        result = mapService.navi(src, dst, mid)
        self.write({'code': 0, 'data': result})
