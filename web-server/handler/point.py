from tornado.web import RequestHandler
from service import PointService, LocService
from model import Point, Loc
import logging
import json

pointService = PointService()
locService = LocService()
class PointHandler(RequestHandler):
    def get(self):
        mid = self.get_query_argument('mapId')
        pid = self.get_query_argument('pid', None)
        result = {'code': 0}
        if pid is not None:
            point = pointService.findById(pid, mid)
            result['data'] = point.toJsonMap()
        else:
            points = pointService.findAll(mid)
            result['data'] = [item.toJsonMap() for item in points]
        self.write(result)
    def post(self):
        pointJson = json.loads(self.request.body)
        point = Point(pointJson['mapId'], pointJson)
        point = pointService.save(point)
        if 'name' in pointJson:
            loc = Loc(pointJson['mapId'], pointJson['name'], pointJson)
            locService.save(loc)
        self.write({'code': 0, 'data': point.toJsonMap()})



