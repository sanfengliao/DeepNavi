# 存数据库
class Map:
	DB_KEY = ['name', 'planPath', 'planSize', 'planUnit', 'actualSize', 'actualUnit', 'modelPath','orginInPlan']
	def __init__(self, info:dict=None, **kwargs):
		# id
		self.id = ''
		# 地图名
		self.name = '超算5楼'
		# 平面图所在位置
		self.planPath = 'image/test.png'
		# 平面图大小
		self.planSize = [70, 50, 0] # x 70 y 50
		# 平面图单位
		self.planUnit = 'px'
		# 地图实际大小
		self.actualSize = [70, 50, 0] # x 70 y 50
		# 地图实际单位
		self.actualUnit = 'm'
		# 模型位置
		self.modelPath = 'modelPath'
		# 原点在平面图的位置 单位 planUnit
		self.orginInPlan = [0, 0, 0]
		for k, v in kwargs.items():
			self[k] = v
		if isinstance(info, dict):
			for k, v in info.items():
				self[k] = v
		
	def __setitem__(self, name, value):
		self.__dict__[name] = value

	def __getitem__(self, name):
		return self.__dict__[name]
	
	def toJsonMap(self) -> dict:
		 return self.__dict__
	
	def toDBMap(self) -> dict:
		DBMap = {}
		for key in self.DB_KEY:
			DBMap[key] = self[key]
		return DBMap



