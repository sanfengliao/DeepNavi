package com.sysu.example.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.sysu.deepnavi.DeepNaviManager.Companion.DEFAULT_PICTURE_SIZE
import com.sysu.deepnavi.DeepNaviManager.Companion.DEFAULT_VALUE_FREQUENCY
import com.sysu.deepnavi.DeepNaviManager.Companion.DEFAULT_VALUE_SENSOR_RATE
import com.sysu.deepnavi.impl.SensorListeners.DEFAULT_VALUE_SENSOR_CONFIG
import com.sysu.deepnavi.util.camera2SupportInfo
import com.sysu.deepnavi.util.requestCameraPermissions
import com.sysu.example.BaseRecyclerAdapter
import com.sysu.example.BaseRecyclerViewHolder
import com.sysu.example.KeyUrls.CONFIG
import com.sysu.example.KeyUrls.TRAIN_MAP
import com.sysu.example.R
import com.sysu.example.activity.ConfigProperty.DEEPNAVI_FREQUENCY
import com.sysu.example.activity.ConfigProperty.DEEPNAVI_SENSOR_RATE
import com.sysu.example.activity.ConfigProperty.DEEPNAVI_URL
import com.sysu.example.activity.ConfigProperty.SIGNAL_CONFIG_RATES
import com.sysu.example.activity.ConfigProperty.SIGNAL_CONFIG_SET
import com.sysu.example.utils.AppSpProperty
import com.sysu.example.utils.ContextApi
import com.sysu.example.utils.StrPropertyHelper
import kotlinx.android.synthetic.main.activity_config.backend_url
import kotlinx.android.synthetic.main.activity_config.begin
import kotlinx.android.synthetic.main.activity_config.camera_select
import kotlinx.android.synthetic.main.activity_config.deepnavi_config_list
import kotlinx.android.synthetic.main.activity_config.frequency
import kotlinx.android.synthetic.main.activity_config.get_config_from_server
import kotlinx.android.synthetic.main.activity_config.radio_camera
import kotlinx.android.synthetic.main.activity_config.radio_camera2
import kotlinx.android.synthetic.main.activity_config.sensor_rate

object ConfigProperty {
    val SIGNAL_CONFIG_SET = AppSpProperty("SIGNAL_CONFIG_SET", "", "SIGNAL_CONFIG_SET")
    val SIGNAL_CONFIG_RATES = AppSpProperty("SIGNAL_CONFIG_RATES", "", "SIGNAL_CONFIG_RATES")
    val DEEPNAVI_URL = AppSpProperty("DEEPNAVI_URL", TRAIN_MAP, "DEEPNAVI_URL")
    val DEEPNAVI_FREQUENCY = AppSpProperty("DEEPNAVI_FREQUENCY", DEFAULT_VALUE_FREQUENCY, "DEEPNAVI_FREQUENCY")
    val DEEPNAVI_SENSOR_RATE = AppSpProperty("DEEPNAVI_SENSOR_RATE", DEFAULT_VALUE_SENSOR_RATE, "DEEPNAVI_SENSOR_RATE")
    val DEEPNAVI_IMAGE_SIZE = AppSpProperty("DEEPNAVI_IMAGE_SIZE", DEFAULT_PICTURE_SIZE, "DEEPNAVI_IMAGE_SIZE")

    fun getRates(): Map<String, Int> {
        val sensorRate = 1000 * 1000 / (DEEPNAVI_SENSOR_RATE.value ?: DEFAULT_VALUE_SENSOR_RATE)
        val configRateSet = SIGNAL_CONFIG_RATES.value?.split(',')?.map {
            val temp = it.split(':')
            when {
                temp.size != 2 -> "" to null
                else -> {
                    var rate = temp[1].toIntOrNull()
                    if (rate != null) {
                        rate = 1000 * 1000 / rate
                    }
                    temp[0] to rate
                }
            }
        }?.toMap()?.toMutableMap()
        return DEFAULT_VALUE_SENSOR_CONFIG.map { it to (configRateSet?.get(it) ?: sensorRate) }.toMap()
    }
}

class ConfigActivity : AppCompatActivity() {
    companion object {
        const val TAG = "Config"

        val GET_CONFIG_FROM_NETWORK = AppSpProperty("GET_CONFIG_FROM_NETWORK", Boolean::class.java, "GET_CONFIG_FROM_NETWORK")
        val USE_CAMERA2 = AppSpProperty("USE_CAMERA2", true, "USE_CAMERA2")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        requestCameraPermissions(this)

        when (camera2SupportInfo(this)) {
            0 -> radio_camera2.apply {
                isClickable = false
                isFocusable = false
                isFocusableInTouchMode = false
                isChecked = false
                radio_camera.isChecked = true
                Toast.makeText(ContextApi.appContext, "This devices cannot support camera2 API", Toast.LENGTH_LONG).show()
            }
            1 -> {
                radio_camera.isChecked = true
                radio_camera2.isChecked = false
                Toast.makeText(
                    ContextApi.appContext,
                    "This devices doesn't support camera2 API very well. It's recommended to use camera API",
                    Toast.LENGTH_LONG
                ).show()
            }
            2 -> Toast.makeText(
                ContextApi.appContext,
                "This devices support camera2 API very well. It's recommended to use camera2 API",
                Toast.LENGTH_LONG
            ).show()
        }
        USE_CAMERA2.value = radio_camera2.isChecked
        camera_select.setOnCheckedChangeListener { _, checkedId -> USE_CAMERA2.value = checkedId == R.id.radio_camera2 }
        get_config_from_server.apply {
            setOnCheckedChangeListener { _, isChecked ->
                GET_CONFIG_FROM_NETWORK.value = isChecked
                get_config_from_server.text = if (isChecked) {
                    (ContextApi.global["StrPropertyHelper"] as StrPropertyHelper).readFromJson(CONFIG)
                    this@ConfigActivity.resources.getString(R.string.get_config_from_server)
                } else {
                    this@ConfigActivity.resources.getString(R.string.don_t_get_config_from_server)
                }
            }
            isChecked = if (GET_CONFIG_FROM_NETWORK.value != null) {
                GET_CONFIG_FROM_NETWORK.value!!
            } else {
                GET_CONFIG_FROM_NETWORK.value = false
                false
            }
        }

        frequency.setText((DEEPNAVI_FREQUENCY.value ?: DEFAULT_VALUE_FREQUENCY).toString())
        frequency.addTextChangedListener { text ->
            if (DEEPNAVI_FREQUENCY.value.toString() != text.toString()) {
                DEEPNAVI_FREQUENCY.value = when {
                    text.isNullOrEmpty() -> DEFAULT_VALUE_FREQUENCY
                    else -> text.toString().toLong()
                }
            }
        }
        DEEPNAVI_FREQUENCY.observe(this@ConfigActivity, Observer {
            if (it.toString() != frequency.text.toString()) {
                frequency.setText(it.toString())
            }
        })

        sensor_rate.setText((DEEPNAVI_SENSOR_RATE.value ?: DEFAULT_VALUE_SENSOR_RATE).toString())
        sensor_rate.addTextChangedListener { text ->
            if (DEEPNAVI_SENSOR_RATE.value.toString() != text.toString()) {
                DEEPNAVI_SENSOR_RATE.value = when {
                    text.isNullOrEmpty() -> DEFAULT_VALUE_SENSOR_RATE
                    else -> text.toString().toInt()
                }
            }
        }
        DEEPNAVI_SENSOR_RATE.observe(this@ConfigActivity, Observer {
            if (it.toString() != sensor_rate.text.toString()) {
                sensor_rate.setText(it.toString())
            }
        })

        backend_url.setText(DEEPNAVI_URL.value ?: TRAIN_MAP)
        backend_url.addTextChangedListener { text ->
            if (text?.toString() != DEEPNAVI_URL.value) {
                DEEPNAVI_URL.setData(text?.toString().orEmpty())
            }
        }
        DEEPNAVI_URL.observe(this@ConfigActivity, Observer {
            if (it.toString() != backend_url.text.toString()) {
                backend_url.setText(it)
            }
        })

        begin.setOnClickListener begin@{
            val editUrl = DEEPNAVI_URL.value
            if (editUrl.isNullOrEmpty()) {
                Toast.makeText(this@ConfigActivity, "Url should not be empty", Toast.LENGTH_LONG).show()
                return@begin
            }
            startActivity(Intent(this@ConfigActivity, TrainActivity::class.java))
        }

        val configListData = DEFAULT_VALUE_SENSOR_CONFIG.toMutableList()
        val holders = mutableSetOf<BaseRecyclerViewHolder>()
        var configDataSet = SIGNAL_CONFIG_SET.value?.split(',') ?: emptyList()
        SIGNAL_CONFIG_SET.observe(this@ConfigActivity, Observer { data: String ->
            if (data.isNotEmpty()) {
                configDataSet = data.split(',')
                holders.forEach { holder ->
                    holder.findViewById<ImageView>(R.id.item_select).isSelected = holder.data in configDataSet
                }
            }
        })
        var configRateSet = SIGNAL_CONFIG_RATES.value?.split(',')?.map {
            val temp = it.split(':')
            when {
                temp.size != 2 -> "" to null
                else -> temp[0] to temp[1].toIntOrNull()
            }
        }?.toMap()?.toMutableMap() ?: mutableMapOf()
        SIGNAL_CONFIG_RATES.observe(this@ConfigActivity, Observer { data: String ->
            if (data.isNotEmpty() && configRateSet.map { "${it.key}:${it.value}" }.sorted().joinToString(",") != data) {
                configRateSet = data.split(',').map {
                    val temp = it.split(':')
                    when {
                        temp.size != 2 -> "" to null
                        else -> temp[0] to temp[1].toIntOrNull()
                    }
                }.toMap().toMutableMap()
                holders.forEach { holder ->
                    holder.findViewById<EditText>(R.id.item_frequency).setText(configRateSet[holder.data as? String]?.toString() ?: return@forEach)
                }
            }
        })
        val baseAdapter = BaseRecyclerAdapter<String>(
            this@ConfigActivity,
            configListData,
            R.layout.item_config,
            object : BaseRecyclerAdapter.AdapterAction<String> {
                override fun onBindViewHolder(holder: BaseRecyclerViewHolder, data: String, position: Int) {
                    holder.itemView.findViewById<TextView>(R.id.item_name).text = data
                    holder.itemView.findViewById<ImageView>(R.id.item_select).isSelected = data in configDataSet
                    val editText = holder.itemView.findViewById<EditText>(R.id.item_frequency)
                    if (configRateSet.containsKey(data)) {
                        editText.setText(configRateSet[data].toString())
                    }
                    editText.addTextChangedListener(afterTextChanged = { text: Editable? ->
                        val value = text?.toString()?.toIntOrNull()
                        if (value != null) {
                            configRateSet[data] = value
                        } else {
                            configRateSet.remove(data)
                        }
                        SIGNAL_CONFIG_RATES.value = configRateSet.map { "${it.key}:${it.value}" }.sorted().joinToString(",")
                    })
                    holders.add(holder)
                }
            })
        baseAdapter.setItemClickListener(object : BaseRecyclerAdapter.ItemClickListener<String> {
            override fun onItemClick(holder: BaseRecyclerViewHolder, data: String, position: Int) {
                val view = holder.itemView
                val selectView = view.findViewById<ImageView>(R.id.item_select)
                selectView.isSelected = !selectView.isSelected
                val dataSet = when {
                    SIGNAL_CONFIG_SET.value.isNullOrEmpty() -> mutableSetOf()
                    else -> SIGNAL_CONFIG_SET.value?.split(',')!!.toMutableSet()
                }
                if (selectView.isSelected && data !in dataSet) {
                    dataSet.add(data)
                    SIGNAL_CONFIG_SET.value = dataSet.joinToString(",")
                } else if (!selectView.isSelected && data in dataSet) {
                    dataSet.remove(data)
                    SIGNAL_CONFIG_SET.value = dataSet.joinToString(",")
                }
                Log.d(TAG, "ItemClick -- view: ${holder.data}, isSelected changed: ${view.isSelected}")
            }
        })
        deepnavi_config_list.adapter = baseAdapter
        deepnavi_config_list.layoutManager = LinearLayoutManager(this@ConfigActivity, LinearLayoutManager.VERTICAL, false)
    }
}

/**
 * 手机标准状态下正(z)方向向量（0, 0, 1），右(x)方向（1, 0, 0），上(y)方向（0, 1, 0）
 */
