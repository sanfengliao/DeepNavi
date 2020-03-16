from .point import Point
# 存数据库
class Edge:
	def __init__(self,  mapId: str, pointA:dict=None, pointB:dict=None,**kwargs):
		if pointA is not None:
			self.pointA = {
				'id': pointA['id'],
				'planCoordinate': pointA['planCoordinate'],
				'actualCoordinate': pointA['actualCoordinate']
			}
		if pointB is not None:
			self.pointB = {
				'id': pointB['id'],
				'planCoordinate': pointB['planCoordinate'],
				'actualCoordinate': pointB['actualCoordinate']
			}
		self.id = ''
		self.mapId = mapId
		self.edgeWidth = 0.5
		if 'id' in kwargs:
			self.id = kwargs['id']
		if 'mapId' in kwargs:
			self.mapId = kwargs['mapId']
		if 'edgeWidth'in kwargs:
			self.edgeWidth = kwargs['edgeWidth']
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