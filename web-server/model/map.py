# 存数据库
import copy
class Map:
	DB_KEY = ['name', 'planPath', 'planSize', 'planUnit', 'actualSize', 'actualUnit', 'modelPath','originInPlan', 'rotationAngle', 'originInActual', 'isClockwise', 'standardVector']
	def __init__(self, info:dict=None, **kwargs):
		# id
		self.id = ''
		# 地图名
		self.name = ''
		# 平面图所在位置
		self.planPath = ''
		# 平面图大小
		self.planSize = [] # x 70 y 50
		# 平面图单位
		self.planUnit = 'px'
		# 地图实际大小
		self.actualSize = [] # x 70 y 50
		# 地图实际单位
		self.actualUnit = 'm'
		# 模型位置
		self.modelPath = ''
		# 原点在平面图的位置 单位 planUnit
		self.originInPlan = [0, 0, 0]
		self.rotationAngle = [0, 0, 0]
		# 是否顺时针旋转为正 计算偏移量会用到
		self.isClockwise = False
		self.standardVector = [0, 1]
		for k, v in kwargs.items():
			self[k] = v
		if isinstance(info, dict):
			for k, v in info.items():
				self[k] = v
		
	def __setitem__(self, name, value):
		if name == '_id':
			return
		self.__dict__[name] = value

	def __getitem__(self, name):
		return self.__dict__[name]
	
	def toJsonMap(self) -> dict:
		# jsonDict = copy.deepcopy(self.__dict__)
		# jsonDict.pop('planPath')
		# return jsonDict
		return self.__dict__
	
	def toDBMap(self) -> dict:
		DBMap = {}
		for key in self.DB_KEY:
			DBMap[key] = self[key]
		return DBMap



