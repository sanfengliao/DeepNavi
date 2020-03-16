SECRET_KEY = 'secret'

class ResponseCode():
    OK = 'ok'
    ERROR = 'error'

DEEPNAVI_CLIENT_CONFIG = {
    "DEEPNAVI_FREQUENCY": 3, 
    "DEEPNAVI_URL": "192.168.43.47:5000", 
    "DEEPNAVI_IMAGE_SIZE": {
        "width": 720, 
        "height": 1080
    }, 
    "SIGNAL_CONFIG_SET": "image,wifiList"
}

SERVER_PORT = 5000
SERVER_IP = '127.0.0.1'

RPC_IP = '127.0.0.1'
RPC_PORT = 1234

MONGODB_IP = '127.0.0.1'
MONGODB_PORT = 27017
NAVI_DB_NAME = 'navi'

REDIS_IP = '127.0.0.1'
REDIS_PORT = 6379