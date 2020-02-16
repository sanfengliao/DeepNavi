from flask import Flask

app = Flask('DeepNavi')
from .controller import * 

@app.route('/')
def helloworld():
    return '{"DEEPNAVI_FREQUENCY": 3, "DEEPNAVI_URL": "192.168.43.47:5000", "DEEPNAVI_IMAGE_SIZE": {"width": 720, "height": 1080}, "SIGNAL_CONFIG_SET": "image,wifiList"}'
