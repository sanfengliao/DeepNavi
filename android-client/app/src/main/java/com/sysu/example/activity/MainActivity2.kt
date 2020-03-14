package com.sysu.example.activity

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.sysu.deepnavi.DeepNaviManager
import com.sysu.deepnavi.bean.Basic
import com.sysu.deepnavi.impl.AudioListener2
import com.sysu.deepnavi.impl.SensorListeners
import com.sysu.deepnavi.impl.WifiListener
import com.sysu.deepnavi.inter.DataCollectorInter
import com.sysu.deepnavi.inter.SocketInter
import com.sysu.deepnavi.util.AndroidLogLogger
import com.sysu.deepnavi.util.DEFAULT_TAG
import com.sysu.deepnavi.util.PERMISSION_CAMERA_AND_STORAGE_REQUEST_CODE
import com.sysu.example.R
import org.java_websocket.client.WebSocketClient
import org.java_websocket.enums.ReadyState
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI
import java.nio.ByteBuffer

@Suppress("UNCHECKED_CAST")
@SuppressLint("CI_ByteDanceKotlinRules_Not_Allow_findViewById_Invoked_In_UI")
class MainActivity2 : AppCompatActivity() {
    private lateinit var deepNaviManager: DeepNaviManager
    private var audioListener2: AudioListener2? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (DeepNaviManager.logger == null) {
            DeepNaviManager.logger = AndroidLogLogger()
        }
        deepNaviManager = DeepNaviManager.get()

        deepNaviManager.forceInit(this, object : SocketInter<Basic.DeepNaviReq, Basic.DeepNaviRes> {
            var socket: WebSocketClient? = null

            override fun connect() {
                val url = "ws://" + (ConfigProperty.DEEPNAVI_URL.getValue2())
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

            override val isConnected: Boolean
                get() = socket?.isOpen ?: false
        }, 1000 / ConfigProperty.DEEPNAVI_FREQUENCY.getValue2())

        val configDataSet = ConfigProperty.SIGNAL_CONFIG_SET.value?.split(',')?.toSet() ?: emptySet()
        val previewView: View = findViewById(R.id.test_textureview) ?: findViewById(R.id.test_surfaceview)
        deepNaviManager.view(findViewById(R.id.direction_panel))
        if ("image" in configDataSet) {
            val imageSize = ConfigProperty.DEEPNAVI_IMAGE_SIZE.value ?: ConfigProperty.DEFAULT_PICTURE_SIZE
            audioListener2 = AudioListener2(this, previewView, pictureSize = Size(imageSize.width, imageSize.height))
            deepNaviManager.addDataCollector(audioListener2 as DataCollectorInter<Any>)
        }
        if ("wifiList" in configDataSet) {
            deepNaviManager.addDataCollector(WifiListener(this) as DataCollectorInter<Any>)
        }

        SensorListeners.initAll(configDataSet)
        findViewById<Button>(R.id.start_preview).setOnClickListener {
            if (audioListener2 != null) {
                if (!audioListener2!!.haveOpenCamera) {
                    audioListener2!!.cameraUtil.openCamera()
                }
                audioListener2!!.cameraUtil.startPreview()
            }
            SensorListeners.registerAll(ConfigProperty.getRates(), configDataSet)
            deepNaviManager.loop()
        }
        findViewById<Button>(R.id.stop_preview).setOnClickListener {
            audioListener2?.cameraUtil?.stopPreview()
            deepNaviManager.stop()
            SensorListeners.unregisterAll()
        }
    }

    override fun onDestroy() {
        deepNaviManager.stop()
        SensorListeners.unregisterAll()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_CAMERA_AND_STORAGE_REQUEST_CODE
            && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
            && grantResults[1] == PackageManager.PERMISSION_GRANTED
            && grantResults[2] == PackageManager.PERMISSION_GRANTED
        ) {
            if (audioListener2 != null && !audioListener2!!.haveOpenCamera) {
                audioListener2!!.cameraUtil.openCamera()
                audioListener2!!.cameraUtil.startPreview()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // override fun onResume() {
    //     audioListener2?.cameraUtil?.onResume()
    //     super.onResume()
    // }
    //
    // override fun onPause() {
    //     audioListener2?.cameraUtil?.onPause()
    //     super.onPause()
    // }
}