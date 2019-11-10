package com.sysu.example

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.sysu.deepnavi.DeepNaviManager
import com.sysu.deepnavi.bean.Basic
import com.sysu.deepnavi.impl.AudioListener
import com.sysu.deepnavi.impl.SensorListeners
import com.sysu.deepnavi.impl.WifiListener
import com.sysu.deepnavi.inter.DataCollectorInter
import com.sysu.deepnavi.inter.SocketInter
import io.socket.client.IO
import io.socket.client.Socket

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        DeepNaviManager.get().init(this, object : SocketInter<Basic.DeepNaviReq, Basic.DeepNaviRes> {
            var socket: Socket? = null

            init {
                val url: String = "http://127.0.0.1:9001"
                socket = IO.socket(url)
                DeepNaviManager.logger?.d("MainActivity", "socket.constructor -- url: %s", url)
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
                DeepNaviManager.logger?.d("MainActivity", "socket.send('deepNavi' -- %s)", String(req.toByteArray()))
                socket?.emit("deepNavi", req.toByteArray())
            }

            override fun onMessage(res: Basic.DeepNaviRes) {
                DeepNaviManager.get().onMessage(res)
            }
        }, 1000 / 3)
        SensorListeners().initAll()
        DeepNaviManager.get().addDataCollector(AudioListener() as DataCollectorInter<Any>)
        DeepNaviManager.get().addDataCollector(WifiListener() as DataCollectorInter<Any>)
    }

    override fun onResume() {
        super.onResume()
        DeepNaviManager.get().loop()
    }

    override fun onPause() {
        super.onPause()
        DeepNaviManager.get().stop()
    }
}
