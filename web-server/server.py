from tornado.web import Application, RequestHandler
import tornado.ioloop
from handler import ConfigHandler,DeepNaviWebSocket, NotFoundHandler
from config import SERVER_PORT
import tornado.log
import tornado
import logging
from tornado.options import options, define
import os

class LogFormatter(tornado.log.LogFormatter):
    def __init__(self):
        super(LogFormatter, self).__init__(
             fmt='%(color)s[%(asctime)s %(filename)s:%(funcName)s:%(lineno)d %(levelname)s]%(end_color)s %(message)s',  
            datefmt='%Y-%m-%d %H:%M:%S'
        )

def initLog():
    options.log_file_prefix = os.path.join(os.path.dirname(__file__), 'logs/tornado_main.log')
    options.logging = 'debug'
    tornado.options.parse_command_line()
    [i.setFormatter(LogFormatter()) for i in logging.getLogger().handlers]




if __name__ == "__main__":
    initLog()
    logging.debug('这是测试日志')
    # application.listen(SERVER_PORT)
    application = Application([
        (r'/config', ConfigHandler),
        (r'/', DeepNaviWebSocket)
    ],  debug=True, 
        default_handler_class=NotFoundHandler,
        default_handler_args=dict(status_code=404)
    )
    httpserver = tornado.httpserver.HTTPServer(application)
    httpserver.listen(SERVER_PORT)
    tornado.ioloop.IOLoop.current().start()
