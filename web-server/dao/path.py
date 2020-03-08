from model import Path
from dao.mongodb import db
from bson import ObjectId
DBPREFIX = 'path'

class PathDao:
  
  def savePath(self, path: Path) -> Path:
    col = db.get_collection(self.getCollName(path.mapId))
    result = col.insert_one(path.toDBMap())
    path.id = str(result.inserted_id)
    return path
  def updatePath(self, path: Path) -> Path:
    if len(path.id) != 24:
      return path
    col = db.get_collection(self.getCollName(path.mapId))
    result = col.update_one({'_id': ObjectId(path.id)}, {'$set': path.toDBMap()}, upsert=True)
    if result.upserted_id:
      path.id = str(result.upserted_id)
    return path
  
  def dropPath(self, path: Path) -> int:
    col = db.get_collection(self.getCollName(path))
    result = col.delete_one({'_id': ObjectId(path.id)})
    return result.deleted_count
  def dropPathById(self, mapId:str, pathId:str) ->int:
    col = db.get_collection(self.getCollName(mapId))
    result = col.delete_one({'_id': ObjectId(pathId)})
    return result.deleted_count
  
  def getCollName(self, mid: str) -> str:
    return DBPREFIX + '_' + mid