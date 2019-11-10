from flask import Flask
from flask_socketio import SocketIO, emit
from config import SECRET_KEY

app = Flask('DeepNavi')
app.config['SECRET_KEY'] = SECRET_KEY

socketIO = SocketIO(app, binary=True)

@app.route('/')
def helloworld():
    return 'hello world'


