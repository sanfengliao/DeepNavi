import os
import sys
# 关于模块路径的问题很恶心，我没办法解决，有空研究一下
path = os.path.dirname(os.path.abspath(__file__))
sys.path.append(path)
# from web.controller import * 
from web import app
if __name__ == "__main__":
    app.run(port=3000, debug=True)