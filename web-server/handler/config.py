from tornado.web import RequestHandler
from config import DEEPNAVI_CLIENT_CONFIG
class ConfigHandler(RequestHandler):
    def get(self):
        a = 1 / 0
        self.write(DEEPNAVI_CLIENT_CONFIG)