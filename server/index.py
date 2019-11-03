#!/usr/bin/python
# -*- coding: UTF-8 -*-
# 文件名: index.py


from app import socket_io, app
import controllers.user

if __name__ == '__main__':
    socket_io.run(app,port=3000,debug=True)
