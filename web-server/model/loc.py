class Loc:
    def __init__(self, mapId:str, name: str, info:dict=None, **kwargs):
        self.id = ''
        self.mapId = mapId
        self.name = name
        self.planCoordinate = {'x': 0, 'y': 0, 'z': 0}
        self.actualCoordinate = {'x': 0, 'y': 0, 'z': 0}
        for k, v in kwargs.items():
            self[k] = v
        for k, v in info.items():
            self[k] = v
        
    def __setitem__(self, name, value):
        if name == '_id':
            return
        self.__dict__[name] = value
    
    def __getitem__(self, name):
        return self.__dict__[name]
    
    def toDBMap(self):
        return {
            'mapId': self.mapId,
            'name': self.name,
            'planCoordinate': self.planCoordinate,
            'actualCoordinate': self.planCoordinate
        }
    
    def toJsonMap(self):
        return {
            'id': self.id,
            'mapId': self.mapId,
            'name': self.name,
            'planCoordinate': self.planCoordinate,
            'actualCoordinate': self.planCoordinate
        }
    