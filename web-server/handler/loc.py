from tornado.web import RequestHandler
import json
from model import Loc
from service import LocService
locService = LocService()
class LocHandler(RequestHandler):
    def get(self):
        pass

    def post(self):
        locJson = json.loads(self.request.body)
        loc = Loc(locJson['mapId'], locJson['name'], locJson)
        loc = locService.save(loc)
        self.write({'code': 0, 'data': loc.toJsonMap()})


class LocSearchHandler(RequestHandler):
    def get(self):
        name = self.get_query_argument('name')
        mapId = self.get_query_argument('mapId', None)
        data = locService.search(name, mapId)
        result = {'code': 0, 'data': data}
        self.write(result)