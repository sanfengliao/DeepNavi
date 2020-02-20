from tornado.web import ErrorHandler, RequestHandler
import traceback

class NotFoundHandler(ErrorHandler):
    """
        Default handler gonna to be used in case of 404 error
    """
    """Generates an error response with ``status_code`` for all requests."""
    def initialize(self, status_code):
        self.set_status(status_code)

    def write_error(self, status_code, **kwargs):
        self.set_status(status_code)
        self.write({'error': status_code, 'msg': 'Not Found'})

def write_error(self, status_code, **kwargs):
        self.set_status(status_code)
        print(self.settings.get('serve_traceback'))
        if self.settings.get("serve_traceback") and "exc_info" in kwargs:
            # in debug mode, try to send a traceback
            lines = []
            for line in traceback.format_exception(*kwargs["exc_info"]):
                lines.append(line)
            self.write({
                'error': {
                    'code': status_code,
                    'message': self._reason,
                    'traceback': lines
                }
            })
        else:
           self.write({
                'error': {
                    'code': status_code,
                    'message': self._reason,
                }
            })


def globalErrorHandle():
    setattr(RequestHandler, 'write_error', write_error)

globalErrorHandle()