from flask import Flask
from flask_socketio import SocketIO, emit
from flask_cors import CORS
from config import SECRET_KEY

app = Flask('DeepNavi')
app.config['SECRET_KEY'] = SECRET_KEY

CORS(app)
socket_io = SocketIO(app)


