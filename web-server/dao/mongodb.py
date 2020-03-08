from pymongo import MongoClient

from config import MONGODB_IP, MONGODB_PORT, NAVI_DB_NAME

client = MongoClient(MONGODB_IP, MONGODB_PORT)

db = client.get_database(NAVI_DB_NAME)