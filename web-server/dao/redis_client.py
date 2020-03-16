import redis
from config import REDIS_IP, REDIS_PORT

redisClient = redis.Redis(REDIS_IP, REDIS_PORT)
import json

class RedisDao:
    def setDict(self, key:str, value: dict):
        redisClient.set(key, json.dumps(value))
    def getDict(self, key:str):
        value = redisClient.get(key)
        return json.loads(value)