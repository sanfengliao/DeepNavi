from model import Path
from dao.mongodb import db
from bson import ObjectId
import typing 
DBPREFIX = 'path'

class PathDao:
  
    def savePath(self, path: Path) -> Path:
        col = self.getColl(path.mapId)
        result = col.insert_one(path.toDBMap())
        path.id = str(result.inserted_id)
        return path
  
    def findPathByPointAId(self, pid: str, mid: str) -> typing.List[Path]:
        col = self.getColl(mid)
        cursor = col.find({'pointA.id': pid})
        result = []
        for item in cursor:
            result.append(self.asseblePath(item))
        return result

    def findPathByPointBId(self, pid: str, mid: str) -> typing.List[Path]:
        col = self.getColl(mid)
        cursor = col.find({'pointB.id': pid})
        result = []
        for item in cursor:
            result.append(self.asseblePath(item))
        return result
    
    def updatePath(self, path: Path) -> Path:
        if len(path.id) != 24:
            return path
        col = self.getColl(path.mapId)
        result = col.update_one({'_id': ObjectId(path.id)}, {'$set': path.toDBMap()}, upsert=True)
        if result.upserted_id:
            path.id = str(result.upserted_id)
        return path
  
    def dropPath(self, path: Path) -> int:
        col = self.getColl(path.mapId)
        result = col.delete_one({'_id': ObjectId(path.id)})
        return result.deleted_count
  
    def dropPathById(self, pathId:str, mid:str) -> int:
        col = self.getColl(mid)
        result = col.delete_one({'_id': ObjectId(pathId)})
        return result.deleted_count
  
    def dropPathByPid(self, pid: str, mapId: str) -> int:
        col = self.getColl(mapId)
        result = col.delete_many({'$or': [{'pointA.id': pid}, {'pointB.id': pid}]})
        return result.deleted_count
    
    def asseblePath(self, item: dict) -> Path:
        path = Path(item['mapId'], item['pointA'], item['pointB'])
        path.id = str(item['_id'])
        return path

    def getColl(self, mid: str):
        return db.get_collection(DBPREFIX + '_' + mid)