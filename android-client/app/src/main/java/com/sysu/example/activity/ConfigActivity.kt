package com.sysu.example.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sysu.deepnavi.impl.SensorListeners.DEFAULT_VALUE_SENSOR_CONFIG
import com.sysu.deepnavi.util.MutableSize
import com.sysu.example.BaseRecyclerAdapter
import com.sysu.example.BaseRecyclerViewHolder
import com.sysu.example.R
import com.sysu.example.activity.ConfigProperty.DEEPNAVI_FREQUENCY
import com.sysu.example.activity.ConfigProperty.DEEPNAVI_SENSOR_RATE
import com.sysu.example.activity.ConfigProperty.DEEPNAVI_URL
import com.sysu.example.activity.ConfigProperty.DEFAULT_VALUE_FREQUENCY
import com.sysu.example.activity.ConfigProperty.DEFAULT_VALUE_SENSOR_RATE
import com.sysu.example.activity.ConfigProperty.DEFAULT_VALUE_URL
import com.sysu.example.activity.ConfigProperty.SIGNAL_CONFIG_RATES
import com.sysu.example.activity.ConfigProperty.SIGNAL_CONFIG_SET
import com.sysu.example.utils.AppSpProperty

object ConfigProperty {
    const val DEFAULT_VALUE_FREQUENCY = 3L
    const val DEFAULT_VALUE_URL = "192.168.43.47:5000"
    const val DEFAULT_VALUE_SENSOR_RATE = 1000000 / 50  // 1s = 1000ms = 1000 * 1000us ，然后每秒50次
    val DEFAULT_PICTURE_SIZE: MutableSize = MutableSize(1080, 1920)

    val SIGNAL_CONFIG_SET = AppSpProperty("SIGNAL_CONFIG_SET", "", "SIGNAL_CONFIG_SET")
    val SIGNAL_CONFIG_RATES = AppSpProperty("SIGNAL_CONFIG_RATES", "", "SIGNAL_CONFIG_RATES")
    val DEEPNAVI_URL = AppSpProperty("DEEPNAVI_URL", DEFAULT_VALUE_URL, "DEEPNAVI_URL")
    val DEEPNAVI_FREQUENCY = AppSpProperty("DEEPNAVI_FREQUENCY", DEFAULT_VALUE_FREQUENCY, "DEEPNAVI_FREQUENCY")
    val DEEPNAVI_SENSOR_RATE = AppSpProperty("DEEPNAVI_SENSOR_RATE", DEFAULT_VALUE_SENSOR_RATE, "DEEPNAVI_SENSOR_RATE")
    val DEEPNAVI_IMAGE_SIZE = AppSpProperty("DEEPNAVI_IMAGE_SIZE", DEFAULT_PICTURE_SIZE, "DEEPNAVI_IMAGE_SIZE")

    fun getRates(): Map<String, Int> {
        val sensorRate = DEEPNAVI_SENSOR_RATE.value ?: DEFAULT_VALUE_SENSOR_RATE
        val configRateSet = SIGNAL_CONFIG_RATES.value?.split(',')?.map {
            val temp = it.split(':')
            when {
                temp.size != 2 -> "" to null
                else -> temp[0] to temp[1].toIntOrNull()
            }
        }?.toMap()?.toMutableMap()
        return DEFAULT_VALUE_SENSOR_CONFIG.map { it to (configRateSet?.get(it) ?: sensorRate) }.toMap()
    }
}

class ConfigActivity : AppCompatActivity() {
    companion object {
        const val TAG = "Config"
        const val IS_MAP_MODE = "IS_MAP_MODE"
    }

    private var cls: Class<*> = MainActivity2::class.java
    private var isMapMode: Boolean = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        findViewById<RadioGroup>(R.id.camera_select).setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_camera -> cls = MainActivity::class.java
                R.id.radio_camera2 -> cls = MainActivity2::class.java
            }
        }
        findViewById<RadioGroup>(R.id.mode_select).setOnCheckedChangeListener { _, checkedId -> isMapMode = checkedId == R.id.map_mode }

        var editText = findViewById<EditText>(R.id.frequency)
        editText.setText((DEEPNAVI_FREQUENCY.value ?: DEFAULT_VALUE_FREQUENCY).toString())
        editText.addTextChangedListener { text ->
            if (DEEPNAVI_FREQUENCY.value.toString() != text.toString()) {
                DEEPNAVI_FREQUENCY.value = when {
                    text.isNullOrEmpty() -> DEFAULT_VALUE_FREQUENCY
                    else -> text.toString().toLong()
                }
            }
        }
        DEEPNAVI_FREQUENCY.observe(this@ConfigActivity, Observer {
            if (it.toString() != editText.text.toString()) {
                editText.setText(it.toString())
            }
        })

        editText = findViewById(R.id.sensor_rate)
        editText.setText((DEEPNAVI_SENSOR_RATE.value ?: DEFAULT_VALUE_SENSOR_RATE).toString())
        editText.addTextChangedListener { text ->
            if (DEEPNAVI_SENSOR_RATE.value.toString() != text.toString()) {
                DEEPNAVI_SENSOR_RATE.value = when {
                    text.isNullOrEmpty() -> DEFAULT_VALUE_SENSOR_RATE
                    else -> text.toString().toInt()
                }
            }
        }
        DEEPNAVI_SENSOR_RATE.observe(this@ConfigActivity, Observer {
            if (it.toString() != editText.text.toString()) {
                editText.setText(it.toString())
            }
        })

        editText = findViewById(R.id.backend_url)
        editText.setText(DEEPNAVI_URL.value ?: DEFAULT_VALUE_URL)
        editText.addTextChangedListener { text ->
            if (text?.toString() != DEEPNAVI_URL.value) {
                DEEPNAVI_URL.setData(text?.toString().orEmpty())
            }
        }
        DEEPNAVI_URL.observe(this@ConfigActivity, Observer {
            if (it.toString() != editText.text.toString()) {
                editText.setText(it)
            }
        })

        findViewById<Button>(R.id.begin).setOnClickListener begin@{
            val editUrl = DEEPNAVI_URL.value
            if (editUrl.isNullOrEmpty()) {
                Toast.makeText(this@ConfigActivity, "Url should not be empty", Toast.LENGTH_LONG).show()
                return@begin
            }
            startActivity(Intent(this@ConfigActivity, cls))
        }

        findViewById<Button>(R.id.train).setOnClickListener train@{
            startActivity(Intent(this, MapActivity::class.java).putExtra(IS_MAP_MODE, isMapMode))
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
                    editText = holder.itemView.findViewById(R.id.item_frequency)
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
        val recyclerView = findViewById<RecyclerView>(R.id.deepnavi_config_list)
        recyclerView.adapter = baseAdapter
        recyclerView.layoutManager = LinearLayoutManager(this@ConfigActivity, LinearLayoutManager.VERTICAL, false)
    }
}

/**
 * 手机标准状态下正(z)方向向量（0, 0, 1），右(x)方向（1, 0, 0），上(y)方向（0, 1, 0）
 */
