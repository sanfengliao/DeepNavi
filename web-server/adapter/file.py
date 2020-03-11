import uuid
import os
from config import SERVER_IP, SERVER_PORT
class LocalFileAdapter:
    def save(self, name: str, byte) -> str:
        fileDir = os.path.join(os.getcwd(), 'upload')
        filename = str(uuid.uuid1()).replace('-', '') + os.path.splitext(name)[-1]
        filepath = os.path.join(fileDir, filename)
        f = open(filepath, 'wb')
        f.write(byte)
        return 'http://%s:%d/static/%s'%(SERVER_IP, SERVER_PORT, filename)
        