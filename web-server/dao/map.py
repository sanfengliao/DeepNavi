from model import Map
from dao.mongodb import db
from bson import ObjectId

mapCol = db.get_collection('navi_map')

class MapDao:

  def saveMap(self, m: Map) -> Map:
    result = mapCol.insert_one(m.toDBMap())
    m.id = str(result.inserted_id)
    return m
  def updateMap(self, m: Map) -> Map:
    if len(m.id) != 24:
      return m
    result = mapCol.update_one({'_id': ObjectId(m.id)}, {'$set': m.toDBMap()}, upsert=True)
    if result.upserted_id:
      m.id = str(result.upserted_id)
    return m

  def deleteById(self, id:str) -> int:
    if len(id) != 24:
      return 0
    result = mapCol.delete_one({'_id': ObjectId(id)})
    return result.deleted_count