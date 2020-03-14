from model import Loc
from .mongodb import db
from bson import ObjectId
import typing
locCol = db.get_collection('navi_loc')
class LocDao:
    def saveLoc(self, loc: Loc) -> Loc:
        result = locCol.insert_one(loc.toDBMap())
        loc.id = str(result.inserted_id)
        return loc
    
    def updateLoc(self, loc:Loc) -> Loc:
        if len(loc.id) != 24:
            return loc
        result = locCol.update_one({'_id': ObjectId(loc.id)}, {'$set': loc.toDBMap()}, upsert=True)
        if result.upserted_id:
            loc.id = str(result.upserted_id)
        return loc
    
    def findByMapId(self, mid:str) -> typing.List[Loc]:
        result = locCol.find({'mapId': mid})
        result = [self.assembleLoc(item) for item in result]
        return result

    def deleteLocById(self, locId: str) -> int:
        if len(locId) != 24:
            return 0
        result = locCol.delete_one({'_id': ObjectId(locId)})
        return result.deleted_count
    
    def searchLocByPointName(self, name: str) -> typing.List[Loc]:
        cursor = locCol.find({'name': {'$regex': name}})
        result = []
        for item in cursor:
            result.append(self.assembleLoc(item))
        return result


    
    def searchLocByPointNameAndMid(self, name:str, mid:str) -> typing.List[Loc]:
        cursor = locCol.find({'$and': [{'name': {'$regex': name}}, {'mapId': mid}]})
        result = []
        for item in cursor:
            result.append(self.assembleLoc(item))
        return result


    def assembleLoc(self, item:dict):
        loc = Loc(item['mapId'], item['name'], item)
        loc.id = str(item['_id'])
        return loc