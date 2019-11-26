package com.sysu.example;

import io.socket.client.IO;
import io.socket.client.Socket;


import java.net.URISyntaxException;

public class App {
    public static void main(String[] args) {
        try {
            Socket socket = IO.socket("http://127.0.0.1:5000");
            socket.on(Socket.EVENT_CONNECT, (objects) -> {
                System.out.println("连接成功");
            });
            socket.on("world", (objects) -> {
                System.out.println(objects[0]);
            });
            socket.connect();
            socket.emit("hello", "hello");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
