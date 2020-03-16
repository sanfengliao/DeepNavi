import unittest
from dao.redis_client import redisClient
import json
class TestRedis(unittest.TestCase):
    def testR(self):
        redisClient.hset("hash1", "k1", "v1")
        redisClient.hset("hash1", "k2", "v2")
        print(redisClient.hkeys("hash1")) # 取hash中所有的key
        print(redisClient.hget("hash1", "k1"))    # 单个取hash的key对应的值
        print(redisClient.hmget("hash1", "k1", "k2")) # 多个取hash的key对应的值
        redisClient.hsetnx("hash1", "k2", "v3") # 只能新建
        print(redisClient.hget("hash1", "k2"))
        redisClient.set('k1', json.dumps({'k1': 'v1', 'k2': ['v2', 'v2']}))
        print(json.loads(redisClient.get('k1')))