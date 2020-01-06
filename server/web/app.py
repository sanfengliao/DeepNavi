from flask import Flask

app = Flask('DeepNavi')
from .controller import * 

@app.route('/')
def helloworld():
    return 'hello world'


