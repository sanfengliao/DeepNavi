package com.sysu.example

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.sysu.deepnavi.DeepNaviManager
import com.sysu.deepnavi.bean.Basic
import com.sysu.deepnavi.impl.AudioListener
import com.sysu.deepnavi.impl.SensorListeners
import com.sysu.deepnavi.impl.WifiListener
import com.sysu.deepnavi.inter.DataCollectorInter
import com.sysu.deepnavi.inter.SocketInter
import com.sysu.deepnavi.util.AndroidLogLogger
import com.sysu.deepnavi.util.DEFAULT_TAG
import org.java_websocket.client.WebSocketClient
import org.java_websocket.enums.ReadyState
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI
import java.nio.ByteBuffer

class MainActivity2 : AppCompatActivity() {
    lateinit var deepNaviManager: DeepNaviManager
    lateinit var sensorListeners: SensorListeners

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        DeepNaviManager.logger = AndroidLogLogger()
        deepNaviManager = DeepNaviManager.get()
        sensorListeners = SensorListeners()

        deepNaviManager.init(this, object : SocketInter<Basic.DeepNaviReq, Basic.DeepNaviRes> {
            var socket: WebSocketClient? = null

            override fun connect() {
                val url = "ws://192.168.124.42:9001/"
                socket = object : WebSocketClient(URI.create(url)) {
                    override fun onOpen(handshakedata: ServerHandshake?) {
                        DeepNaviManager.logger?.d(DEFAULT_TAG, "EVENT_CONNECT")
                    }

                    override fun onClose(code: Int, reason: String?, remote: Boolean) {
                        DeepNaviManager.logger?.d(DEFAULT_TAG, "EVENT_DISCONNECT, code:$code, reason: $reason, remote: $remote")
                    }

                    override fun onMessage(bytes: ByteBuffer?) {
                        DeepNaviManager.logger?.d(
                            "MainActivity",
                            "socket.receive('deepNavi' -- %s) -- onMessage(msg: ByteBuffer)",
                            String(bytes?.array() ?: byteArrayOf())
                        )
                        if (bytes != null) onMessage(Basic.DeepNaviRes.parseFrom(bytes.array()))
                    }

                    override fun onMessage(message: String?) {
                        DeepNaviManager.logger?.d("MainActivity", "socket.receive('deepNavi' -- %s) -- onMessage(msg: String)", message)
                        if (message != null) onMessage(Basic.DeepNaviRes.parseFrom(message.toByteArray()))
                    }

                    override fun onError(ex: Exception?) {
                        DeepNaviManager.logger?.d(DEFAULT_TAG, "EVENT_ERROR")
                    }
                }
                DeepNaviManager.logger?.d("MainActivity", "socket.constructor -- url: %s", url)
                socket?.connect()
            }

            override fun close() {
                socket?.close()
                socket = null
            }

            override fun send(req: Basic.DeepNaviReq) {
                DeepNaviManager.logger?.d(DEFAULT_TAG, "socket.state: ${socket?.readyState}, req_len: ${req.toByteArray().size}")
                if (socket?.readyState == ReadyState.OPEN) socket?.send(req.toByteArray())
            }

            override fun onMessage(res: Basic.DeepNaviRes) {
                deepNaviManager.onMessage(res)
            }
        }, 1000 / 3)
        deepNaviManager.addDataCollector(AudioListener(this, findViewById(R.id.test_textureview)) as DataCollectorInter<Any>)
        deepNaviManager.addDataCollector(WifiListener(this) as DataCollectorInter<Any>)

        findViewById<Button>(R.id.start_preview).setOnClickListener {
            sensorListeners.initAll()
            deepNaviManager.loop()
        }
        findViewById<Button>(R.id.stop_preview).setOnClickListener {
            deepNaviManager.stop()
            deepNaviManager.unregisterListener()
        }
    }
}
