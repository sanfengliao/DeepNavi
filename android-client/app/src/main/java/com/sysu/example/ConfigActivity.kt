package com.sysu.example

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.setPadding
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sysu.deepnavi.impl.SensorListeners
import com.sysu.example.ConfigProperty.DEEPNAVI_FREQUENCY
import com.sysu.example.ConfigProperty.DEEPNAVI_SENSOR_RATE
import com.sysu.example.ConfigProperty.DEEPNAVI_URL
import com.sysu.example.ConfigProperty.DEFAULT_VALUE_FREQUENCY
import com.sysu.example.ConfigProperty.DEFAULT_VALUE_SENSOR_RATE
import com.sysu.example.ConfigProperty.DEFAULT_VALUE_URL
import com.sysu.example.ConfigProperty.SIGNAL_CONFIG_SET
import com.sysu.example.block.Block
import com.sysu.example.block.BlockActivity
import com.sysu.example.block.BlockGroup
import com.sysu.example.block.BlockManager
import com.sysu.example.utils.AppSpProperty
import com.sysu.example.utils.dp2px
import com.sysu.example.utils.setDrawableRight
import com.sysu.example.utils.str2IdMap
import com.sysu.example.utils.strId
import android.view.ViewGroup.LayoutParams.MATCH_PARENT as MP
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT as WC

open class ImageSize(val width: Int = 0, val height: Int = 0)

object ConfigProperty {
    const val DEFAULT_VALUE_FREQUENCY = 3L
    const val DEFAULT_VALUE_URL = "192.168.43.47:5000"
    const val DEFAULT_VALUE_SENSOR_RATE = 1000000 / 50  // 1s = 1000ms = 1000 * 1000us ，然后每秒50次
    val DEFAULT_PICTURE_SIZE: ImageSize = ImageSize(1080, 1920)

    val SIGNAL_CONFIG_SET = AppSpProperty("SIGNAL_CONFIG_SET", "", "SIGNAL_CONFIG_SET")
    val DEEPNAVI_URL = AppSpProperty("DEEPNAVI_URL", DEFAULT_VALUE_URL, "DEEPNAVI_URL")
    val DEEPNAVI_FREQUENCY = AppSpProperty("DEEPNAVI_FREQUENCY", DEFAULT_VALUE_FREQUENCY, "DEEPNAVI_FREQUENCY")
    val DEEPNAVI_SENSOR_RATE = AppSpProperty("DEEPNAVI_SENSOR_RATE", DEFAULT_VALUE_SENSOR_RATE, "DEEPNAVI_SENSOR_RATE")
    val DEEPNAVI_IMAGE_SIZE = AppSpProperty("DEEPNAVI_IMAGE_SIZE", DEFAULT_PICTURE_SIZE, "DEEPNAVI_IMAGE_SIZE")
}

class ConfigActivity : BlockActivity() {
    companion object {
        const val TAG = "Config"

        const val BLOCK_ID_RADIO_CAMERA = "radio_camera"
        const val BLOCK_ID_RADIO_CAMERA2 = "radio_camera2"
        const val BLOCK_ID_FREQUENCY = "frequency"
        const val BLOCK_ID_SENSOR_RATE = "sensor_rate"
        const val BLOCK_ID_BACK_END_URL = "backend_url"
        const val BLOCK_ID_BEGIN = "begin"
        const val BLOCK_ID_DEEPNAVI_CONFIG_LIST = "deepnavi_config_list"
    }

    private var cls: Class<*> = MainActivity2::class.java

    @SuppressLint("SetTextI18n")
    override fun getBlockManagerList(): List<BlockManager>? {
        val dp20 = dp2px(20f)
        val dp10 = dp2px(10f)
        val blockManager = BlockManager(this, R.layout.layout_linear).apply {
            parent = window.decorView.findViewById(android.R.id.content)
            setInflatedCallback<LinearLayout> {
                it.gravity = Gravity.CENTER_HORIZONTAL
                it.orientation = LinearLayout.VERTICAL
                it.setPadding(dp20)
            }

            addBlock(BlockGroup(this, R.layout.view_radiogroup).setInflatedCallback<RadioGroup> {
                val lp = LinearLayout.LayoutParams(MP, WC)
                lp.bottomMargin = dp10
                it.layoutParams = lp
                it.orientation = RadioGroup.HORIZONTAL
                it.setOnCheckedChangeListener { _, checkedId ->
                    cls = when (checkedId) {
                        str2IdMap[BLOCK_ID_RADIO_CAMERA] -> MainActivity::class.java
                        str2IdMap[BLOCK_ID_RADIO_CAMERA2] -> MainActivity2::class.java
                        else -> MainActivity2::class.java
                    }
                }
            }
                .addBlock(makeRadioButton(BLOCK_ID_RADIO_CAMERA, "camera", false))
                .addBlock(makeRadioButton(BLOCK_ID_RADIO_CAMERA2, "camera2", true))
            )

            addBlock(Block(R.layout.view_edit).setInflatedCallback { editText: EditText ->
                editText.strId = BLOCK_ID_FREQUENCY
                val lp = LinearLayout.LayoutParams(MP, WC)
                lp.bottomMargin = dp10
                editText.layoutParams = lp
                editText.hint = "please input frequency"
                editText.inputType = 0x00000002
                editText.setDrawableRight(R.drawable.frequency_select)
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
            })

            addBlock(Block(R.layout.view_edit).setInflatedCallback { editText: EditText ->
                editText.strId = BLOCK_ID_SENSOR_RATE
                val lp = LinearLayout.LayoutParams(MP, WC)
                lp.bottomMargin = dp10
                editText.layoutParams = lp
                editText.hint = "please input sensor rate"
                editText.inputType = 0x00000002
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
            })

            addBlock(Block(R.layout.view_edit).setInflatedCallback { editText: EditText ->
                editText.strId = BLOCK_ID_BACK_END_URL
                val lp = LinearLayout.LayoutParams(MP, WC)
                lp.bottomMargin = dp10
                editText.layoutParams = lp
                editText.hint = "please input backend s url"
                editText.inputType = 0x00000011
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
            })

            addBlock(Block(R.layout.view_button).setInflatedCallback<Button> {
                it.strId = BLOCK_ID_BEGIN
                val lp = LinearLayout.LayoutParams(MP, WC)
                lp.bottomMargin = dp10
                it.layoutParams = lp
                it.text = "begin"

                it.setOnClickListener begin@{
                    val editUrl = DEEPNAVI_URL.value
                    if (editUrl.isNullOrEmpty()) {
                        Toast.makeText(this@ConfigActivity, "Url should not be empty", Toast.LENGTH_LONG).show()
                        return@begin
                    }
                    startActivity(Intent(this@ConfigActivity, cls))
                }
            })

            addBlock(Block(R.layout.layout_recycler).setInflatedCallback { recyclerView: RecyclerView ->
                recyclerView.strId = BLOCK_ID_DEEPNAVI_CONFIG_LIST

                val configListData = SensorListeners.DEFAULT_VALUE_SENSOR_CONFIG.toMutableList()
                val holders = mutableSetOf<BaseRecyclerViewHolder>()
                SIGNAL_CONFIG_SET.observe(this@ConfigActivity, Observer { data: String ->
                    if (data.isNotEmpty()) {
                        val dataSet = data.split(',')
                        holders.forEach { holder -> holder.itemView.isSelected = holder.data in dataSet }
                    }
                })
                val baseAdapter = BaseRecyclerAdapter<String>(
                    this@ConfigActivity,
                    configListData,
                    R.layout.item_config,
                    object : BaseRecyclerAdapter.AdapterAction<String> {
                        override fun onBindViewHolder(holder: BaseRecyclerViewHolder, data: String, position: Int) {
                            if (holder.itemView is TextView) {
                                holder.itemView.text = data
                                holder.itemView.isSelected = SIGNAL_CONFIG_SET.value?.split(',')?.contains(data) ?: false
                                holders.add(holder)
                            }
                        }
                    })
                baseAdapter.setItemClickListener(object : BaseRecyclerAdapter.ItemClickListener<String> {
                    override fun onItemClick(holder: BaseRecyclerViewHolder, data: String, position: Int) {
                        val view = holder.itemView
                        view.isSelected = !view.isSelected
                        if (view is TextView) {
                            val dataSet = when {
                                SIGNAL_CONFIG_SET.value.isNullOrEmpty() -> mutableSetOf()
                                else -> SIGNAL_CONFIG_SET.value?.split(',')!!.toMutableSet()
                            }
                            if (view.isSelected && data !in dataSet) {
                                dataSet.add(data)
                                SIGNAL_CONFIG_SET.value = dataSet.joinToString(",")
                            } else if (!view.isSelected && data in dataSet) {
                                dataSet.remove(data)
                                SIGNAL_CONFIG_SET.value = dataSet.joinToString(",")
                            }
                            Log.d(TAG, "ItemClick -- view: ${view.text}, isSelected changed: ${view.isSelected}")
                        } else Log.d(TAG, "ItemClick -- view isn't a TextView")
                    }
                })
                recyclerView.adapter = baseAdapter
                recyclerView.layoutManager = LinearLayoutManager(this@ConfigActivity, LinearLayoutManager.VERTICAL, false)
            })
        }
        return listOf(blockManager)
    }

    private fun makeRadioButton(strId: String, text: String, isChecked: Boolean): Block {
        return Block(R.layout.view_radiobutton).setInflatedCallback<RadioButton> {
            it.strId = strId
            it.layoutParams = RadioGroup.LayoutParams(WC, WC, 1f)
            it.text = text
            it.isChecked = isChecked
        }
    }
}
