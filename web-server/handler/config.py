from tornado.web import RequestHandler
from config import DEEPNAVI_CLIENT_CONFIG
class ConfigHandler(RequestHandler):
    def get(self):
        self.write(DEEPNAVI_CLIENT_CONFIG)