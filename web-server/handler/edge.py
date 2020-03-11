from tornado.web import RequestHandler

from service import EdgeService

edgeService = EdgeService()

class EdgeHandler(RequestHandler):
    def get(self):
        pass

    def post(self):
        pointAId = self.get_body_argument('pointAId')
        pointBId = self.get_body_argument('pointBId')
        print(pointAId, pointBId)
        mapId = self.get_body_argument('mapId', None)
        edgeWidth = float(self.get_body_argument('edgeWidth', 1))
        edge = edgeService.save(pointAId, pointBId, edgeWidth, mapId)
        self.write({'code': 0, 'data': edge.toJsonMap()})