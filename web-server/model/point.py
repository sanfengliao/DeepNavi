class Point:
	def __init__(self, mapId:str, info:dict=None, **kwargs):
		self.id = ''
		self.mapId  = mapId # 地图Id
		# 平面图的坐标，使用dict是为了方便查询和该point在同一条路线上的拐点
		self.planCoordinate = {'x': 0, 'y': 0, 'z': 0}
		# 实际坐标
		self.actualCoordinate = {'x': 0, 'y': 0, 'z': 0}
		# 相连的point
		self.adjacence = []
		for k, v in kwargs.items():
			self[k] = v
		if isinstance(info, dict):
			for k, v in info.items():
				self[k] = v
	
	def addAdjacence(self, pid: str):
		self.adjacence.append(pid)

	def __setitem__(self, name, value):
		if name == '_id':
			return
		self.__dict__[name] = value

	def __getitem__(self, name):
		return self.__dict__[name]
	

	def toDBMap(self) -> dict:
		dbMap =  {
			'mapId': self.mapId,
			'planCoordinate': self.planCoordinate,
			'actualCoordinate': self.actualCoordinate,
			'adjacence': self.adjacence
		}
		if 'name' in self.__dict__:
			dbMap['name'] = self.__dict__['name']
		return dbMap

	def toJsonMap(self) -> dict:
		return self.__dict__