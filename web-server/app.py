from tornado.web import Application
import tornado.ioloop
from src.handler import ConfigHandler,DeepNaviWebSocket
from src.config import SERVER_PORT

if __name__ == "__main__":
    application = Application([
        (r'/config', ConfigHandler),
        (r'/', DeepNaviWebSocket)
    ], debug=True)
    application.listen(SERVER_PORT)
    tornado.ioloop.IOLoop.instance().start()
