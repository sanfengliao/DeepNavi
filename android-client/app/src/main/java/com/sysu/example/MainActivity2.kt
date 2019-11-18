package com.sysu.example

import android.os.Bundle
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

            init {
                val url = "http://172.26.44.15:5000/"
                socket = object : WebSocketClient(URI.create(url)) {
                    override fun onOpen(handshakedata: ServerHandshake?) {
                        DeepNaviManager.logger?.d(DEFAULT_TAG, "EVENT_CONNECT")
                    }

                    override fun onClose(code: Int, reason: String?, remote: Boolean) {
                        DeepNaviManager.logger?.d(DEFAULT_TAG, "EVENT_DISCONNECT")
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
                    }

                    override fun onError(ex: Exception?) {
                        DeepNaviManager.logger?.d(DEFAULT_TAG, "EVENT_ERROR")
                    }
                }
                DeepNaviManager.logger?.d("MainActivity", "socket.constructor -- url: %s", url)
            }

            override fun connect() {
                socket?.connect()
            }

            override fun close() {
                socket?.close()
            }

            override fun send(req: Basic.DeepNaviReq) {
                DeepNaviManager.logger?.d(DEFAULT_TAG, "socket.state: ${socket?.readyState}")
                socket?.send(req.toByteArray())
            }

            override fun onMessage(res: Basic.DeepNaviRes) {
                deepNaviManager.onMessage(res)
            }
        }, 1000 / 3)
        // deepNaviManager.addDataCollector(AudioListener(this, findViewById(R.id.test_textureview)) as DataCollectorInter<Any>)
        deepNaviManager.addDataCollector(WifiListener(this) as DataCollectorInter<Any>)
    }

    override fun onResume() {
        super.onResume()
        sensorListeners.initAll()
        deepNaviManager.loop()
    }

    override fun onPause() {
        super.onPause()
        deepNaviManager.stop()
        deepNaviManager.unregisterListener()
    }
}
