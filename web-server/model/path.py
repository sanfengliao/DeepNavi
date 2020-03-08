from .point import Point
# 存数据库
class Path:
	def __init__(self, pointA:Point=None, pointB:Point=None, **kwargs):
		if pointA is not None and pointB is not None and pointA.mapId != pointB.mapId:
			raise Exception('pointA and pointB are not in the same map')
		if pointA is not None:
			self.pointA = {
				'pointId': pointA.id,
				'planCoordinate': pointA.planCoordinate,
				'actualCoordinate': pointA.actualCoordinate
			}
		if pointB is not None:
			self.pointB = {
				'pointId': pointB.id,
				'planCoordinate': pointB.planCoordinate,
				'actualCoordinate': pointB.actualCoordinate
			}
		self.id = ''
		if 'id' in kwargs:
			self.id = kwargs['id']
		if 'mapId' in kwargs:
			self.mapId = kwargs['mapId']
		if pointA is not None:
			self.mapId = pointA.mapId
		elif pointB is not None:
			self.mapId = pointB.mapId
	def toDBMap(self) -> dict:
		return {
			'pointA': self.pointA,
			'pointB': self.pointB,
			'mapId': self.mapId,
		}
	
	def toJsonMap(self) -> dict:
		return {
			'id': self.id,
			'pointA': self.pointA,
			'pointB': self.pointB,
			'mapId': self.mapId,
		}