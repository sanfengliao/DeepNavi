import os
import sys
# 关于模块路径的问题很恶心，我没办法解决，有空研究一下
path = os.path.dirname(os.path.abspath(__file__))
sys.path.append(path)
from navi_socket import runWebSocket
if __name__ == "__main__":
    runWebSocket(5000)