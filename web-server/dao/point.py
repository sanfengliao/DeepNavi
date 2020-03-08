from model import Point
from dao.mongodb import db
from bson import ObjectId
DBPREFIX = 'point'

class PointDao:
  
  def savePoint(self, point: Point) -> Point:
    col = db.get_collection(self.getCollName(point.mapId))
    result = col.insert_one(point.toDBMap())
    point.id = str(result.inserted_id)
    return point
  def updatePoint(self, point: Point) -> Point:
    if len(point.id) != 24:
      return point
    col = db.get_collection(self.getCollName(point.mapId))
    result = col.update_one({'_id': ObjectId(point.id)}, {'$set': point.toDBMap()}, upsert=True)
    if result.upserted_id:
      point.id = str(result.upserted_id)
    return point
  
  def dropPoint(self, point: Point) -> int:
    col = db.get_collection(self.getCollName(point))
    print(point.id)
    result = col.delete_one({'_id': ObjectId(point.id)})
    return result.deleted_count

  def dropPointById(self, pid:str, mapId:str) -> int:
    col = db.get_collection(self.getCollName(mapId))
    result = col.delete_one({'_id': ObjectId(pid)})
    return result.deleted_count

  def getCollName(self, mId: str) -> str:
    return DBPREFIX + '_' + mId