package com.sysu.example

import android.annotation.SuppressLint
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
import io.socket.client.IO
import io.socket.client.Socket

@Suppress("UNCHECKED_CAST")
@Deprecated(message = "SocketIO isn't a good choice")
class MainActivity : AppCompatActivity() {
    private lateinit var deepNaviManager: DeepNaviManager
    private lateinit var sensorListeners: SensorListeners

    @SuppressLint("CI_ByteDanceKotlinRules_Not_Allow_findViewById_Invoked_In_UI")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        DeepNaviManager.logger = AndroidLogLogger()
        deepNaviManager = DeepNaviManager.get()
        sensorListeners = SensorListeners()

        deepNaviManager.init(this, object : SocketInter<Basic.DeepNaviReq, Basic.DeepNaviRes> {
            var socket: Socket? = null

            init {
                val url = "http://172.26.44.15:5000/"
                socket = IO.socket(url)
                DeepNaviManager.logger?.d("MainActivity", "socket.constructor -- url: %s", url)
                socket?.on(Socket.EVENT_CONNECT) {
                    DeepNaviManager.logger?.d(DEFAULT_TAG, "EVENT_CONNECT")
                }
                socket?.on(Socket.EVENT_DISCONNECT) {
                    DeepNaviManager.logger?.d(DEFAULT_TAG, "EVENT_DISCONNECT")
                }
                socket?.on(Socket.EVENT_ERROR) {
                    DeepNaviManager.logger?.d(DEFAULT_TAG, "EVENT_ERROR")
                }
                socket?.on("deepNavi") {
                    DeepNaviManager.logger?.d("MainActivity", "socket.receive('deepNavi' -- %s)", String(it[0] as ByteArray))
                    onMessage(Basic.DeepNaviRes.parseFrom(it[0] as ByteArray))
                }
            }

            override fun connect() {
                socket?.connect()
            }

            override fun close() {
                socket?.disconnect()
            }

            override fun send(req: Basic.DeepNaviReq) {
                // DeepNaviManager.logger?.d("MainActivity", "socket.send('deepNavi' -- %s)", String(req.toByteArray()))
                DeepNaviManager.logger?.d(DEFAULT_TAG, "socket.state: ${socket?.connected()}")
                socket?.emit("deepNavi", req.toByteArray())
            }

            override fun onMessage(res: Basic.DeepNaviRes) {
                deepNaviManager.onMessage(res)
            }
        }, 1000 / 3)
        deepNaviManager.addDataCollector(AudioListener(this, findViewById(R.id.test_textureview)) as DataCollectorInter<Any>)
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
