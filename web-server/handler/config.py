from tornado.web import RequestHandler
import  logging
from config import DEEPNAVI_CLIENT_CONFIG
class ConfigHandler(RequestHandler):
    def get(self):
        logging.info('DEEPNAVI_CLIENT_CONFIG: %s'%DEEPNAVI_CLIENT_CONFIG)
        self.write(DEEPNAVI_CLIENT_CONFIG)