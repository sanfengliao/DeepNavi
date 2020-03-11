from model import Point
from dao import PointDao
from decorator import transaction
import typing

pointDao = PointDao()
class PointService:
    def save(self, point: Point) -> Point:
        return pointDao.savePoint(point)

    def findAll(self, mid: str) -> typing.List[Point]:
        return pointDao.findAll(mid)
    
    def findById(self, pid: str, mid: str) -> Point:
        return pointDao.findById(pid, mid)