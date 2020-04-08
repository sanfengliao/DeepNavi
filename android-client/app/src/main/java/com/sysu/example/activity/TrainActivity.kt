package com.sysu.example.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.protobuf.ByteString
import com.sysu.deepnavi.DeepNaviManager
import com.sysu.deepnavi.DeepNaviManager.Companion.DEFAULT_PICTURE_SIZE
import com.sysu.deepnavi.DeepNaviManager.Companion.DEFAULT_VALUE_FREQUENCY
import com.sysu.deepnavi.DeepNaviManager.Companion.config
import com.sysu.deepnavi.bean.Basic
import com.sysu.deepnavi.inter.SocketInter
import com.sysu.deepnavi.util.DEFAULT_TAG
import com.sysu.example.R
import com.sysu.example.activity.ConfigProperty.DEEPNAVI_FREQUENCY
import com.sysu.example.activity.ConfigProperty.DEEPNAVI_IMAGE_SIZE
import com.sysu.example.utils.ContextApi
import kotlinx.android.synthetic.main.activity_train.direction_panel
import kotlinx.android.synthetic.main.activity_train.msg_panel
import kotlinx.android.synthetic.main.activity_train.start_preview
import kotlinx.android.synthetic.main.activity_train.stop_preview
import org.java_websocket.client.WebSocketClient
import org.java_websocket.enums.ReadyState
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer

@Suppress("UNCHECKED_CAST")
class TrainActivity : AppCompatActivity() {
    @SuppressLint("CI_ByteDanceKotlinRules_Not_Allow_findViewById_Invoked_In_UI")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_train)

        val configDataSet = ConfigProperty.SIGNAL_CONFIG_SET.value?.split(',')?.toSet() ?: emptySet()

        val previewView: View? = findViewById(R.id.test_textureview) ?: findViewById(R.id.test_surfaceview)
        val deepNaviManager = configByWS(this, previewView, null, null).directionPanel(direction_panel)
        deepNaviManager.setSignalsListener(object : DeepNaviManager.SignalsListener {
            override fun onSignals(req: Basic.DeepNaviReq) {
                ContextApi.handler.post {
                    msg_panel.text = configDataSet.map {
                        val fieldDescriptor = deepNaviManager.reqDescriptor.findFieldByName(it)
                        when {
                            it == "image" -> "image-size: ${(req.getField(fieldDescriptor) as ByteString).size()}"
                            fieldDescriptor.isRepeated -> "$it: [${(0 until req.getRepeatedFieldCount(fieldDescriptor)).map { i ->
                                transformSingleValue(req.getRepeatedField(fieldDescriptor, i))
                            }.joinToString()}]"
                            else -> "$it: ${transformSingleValue(req.getField(fieldDescriptor))}"
                        }
                    }.joinToString("\n")
                }
            }
        })

        start_preview.setOnClickListener { DeepNaviManager.start(ConfigProperty.getRates(), configDataSet) }
        stop_preview.setOnClickListener { DeepNaviManager.stop() }
    }

    private fun transformSingleValue(value: Any?) =
        when (value) {
            null -> "null"
            is Basic.Coor -> "[${value.x}, ${value.y}, ${value.z}]"
            is Basic.FeelSensorReq -> value.value.toString()
            else -> value.toString()
        }

    override fun onDestroy() {
        DeepNaviManager.stop()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        DeepNaviManager.onRequestPermissionsResult(requestCode, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        fun configByWS(
            activity: Activity, previewView: View?,
            callback: ((Basic.DeepNaviReq) -> Unit)? = null, callback2: ((Basic.DeepNaviRes) -> Unit)? = null
        ): DeepNaviManager {
            val configDataSet = ConfigProperty.SIGNAL_CONFIG_SET.value?.split(',')?.toSet() ?: emptySet()
            val imageSize = DEEPNAVI_IMAGE_SIZE.value ?: DEFAULT_PICTURE_SIZE
            val deepNaviManager = DeepNaviManager.get()
            return config(
                activity, configDataSet, imageSize, previewView,
                1000 / (DEEPNAVI_FREQUENCY.value ?: DEFAULT_VALUE_FREQUENCY),
                object : SocketInter<Basic.DeepNaviReq, Basic.DeepNaviRes> {
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
                                if (bytes != null) {
                                    onMessage(Basic.DeepNaviRes.parseFrom(bytes.array()))
                                }
                            }

                            override fun onMessage(message: String?) {
                                DeepNaviManager.logger?.d("MainActivity", "socket.receive('deepNavi' -- %s) -- onMessage(msg: String)", message)
                                if (message != null) {
                                    onMessage(Basic.DeepNaviRes.parseFrom(message.toByteArray()))
                                }
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
                        callback?.invoke(req)
                        DeepNaviManager.logger?.d(DEFAULT_TAG, "socket.state: ${socket?.readyState}, req_len: ${req.toByteArray().size}")
                        if (socket?.readyState == ReadyState.OPEN) {
                            socket?.send(req.toByteArray())
                        }
                    }

                    override fun onMessage(res: Basic.DeepNaviRes) {
                        deepNaviManager.onMessage(res)
                        callback2?.invoke(res)
                    }

                    override val isConnected: Boolean
                        get() = socket?.isOpen ?: false
                }
            )
        }
    }
}
