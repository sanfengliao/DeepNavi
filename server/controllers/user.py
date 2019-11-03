from app import app, socket_io
from flask_socketio import emit

@app.route('/user')
def user_hello():
  return 'hello world'

@socket_io.on('message')
def msg(message=''):
  emit('aaa_response', 'message' + message)
  return 'msg1'