from ..app import app
from config import DEEPNAVI_CLIENT_CONFIG
@app.route('/config')
def index():
    return {"code": 0, "data": DEEPNAVI_CLIENT_CONFIG}