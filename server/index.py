#!/usr/bin/python
# -*- coding: UTF-8 -*-
# 文件名: index.py


from app import socketIO, app
import controllers.socket_controller

if __name__ == '__main__':
    socketIO.run(app,port=5000, debug=True)
