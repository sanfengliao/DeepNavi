package com.sysu.example

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT as MP
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT as WC
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sysu.deepnavi.impl.SensorListeners
import com.sysu.example.block.Block
import com.sysu.example.block.BlockActivity
import com.sysu.example.block.BlockGroup
import com.sysu.example.block.BlockManager
import com.sysu.example.utils.AppSpProperty
import com.sysu.example.utils.add
import com.sysu.example.utils.contains
import com.sysu.example.utils.dp2px
import com.sysu.example.utils.remove
import com.sysu.example.utils.setDrawableRight
import com.sysu.example.utils.str2IdMap
import com.sysu.example.utils.strId

class ConfigActivity : BlockActivity() {
    companion object {
        const val TAG = "Config"

        const val BLOCK_ID_RADIO_CAMERA = "radio_camera"
        const val BLOCK_ID_RADIO_CAMERA2 = "radio_camera2"
        const val BLOCK_ID_RADIO_MEDIA_RECORDER = "radio_media_recorder"
        const val BLOCK_ID_FREQUENCY = "frequency"
        const val BLOCK_ID_BACK_END_URL = "backend_url"
        const val BLOCK_ID_BEGIN = "begin"
        const val BLOCK_ID_DEEPNAVI_CONFIG_LIST = "deepnavi_config_list"

        const val DEFAULT_VALUE_FREQUENCY = 3L
        const val DEFAULT_VALUE_URL = "192.168.43.47:5000"

        val SIGNAL_CONFIG_SET = AppSpProperty<HashSet<String>>("SIGNAL_CONFIG_SET", HashSet(), "SIGNAL_CONFIG_SET")
        val DEEPNAVI_URL = AppSpProperty("DEEPNAVI_URL", DEFAULT_VALUE_URL, "DEEPNAVI_URL")
        val DEEPNAVI_FREQUENCY = AppSpProperty("DEEPNAVI_FREQUENCY", DEFAULT_VALUE_FREQUENCY, "DEEPNAVI_FREQUENCY")
    }

    private var cls: Class<*> = MainActivity2::class.java

    @SuppressLint("SetTextI18n")
    override fun getBlockManagerList(): List<BlockManager>? {
        val dp20 = dp2px(20f)
        val dp10 = dp2px(10f)
        val blockManager = BlockManager(this, R.layout.layout_linear).apply {
            parent = window.decorView.findViewById(android.R.id.content)
            inflateBlocksAsync = false
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
                        str2IdMap[BLOCK_ID_RADIO_MEDIA_RECORDER] -> MainActivity3::class.java
                        else -> MainActivity2::class.java
                    }
                }
            }
                .addBlock(makeRadioButton(BLOCK_ID_RADIO_CAMERA, "camera", false))
                .addBlock(makeRadioButton(BLOCK_ID_RADIO_CAMERA2, "camera2", true))
                .addBlock(makeRadioButton(BLOCK_ID_RADIO_MEDIA_RECORDER, "media recorder", false))
            )

            addBlock(Block(R.layout.view_edit).setInflatedCallback<EditText> {
                it.strId = BLOCK_ID_FREQUENCY
                val lp = LinearLayout.LayoutParams(MP, WC)
                lp.bottomMargin = dp10
                it.layoutParams = lp
                it.hint = "please input frequency"
                it.inputType = 0x00000002
                it.setDrawableRight(R.drawable.frequency_select)
                it.setText((DEEPNAVI_FREQUENCY.getData() ?: DEFAULT_VALUE_FREQUENCY).toString())
                it.addTextChangedListener { text ->
                    DEEPNAVI_FREQUENCY.value = if (text.isNullOrEmpty()) DEFAULT_VALUE_FREQUENCY else text.toString().toLong()
                }
            })

            addBlock(Block(R.layout.view_edit).setInflatedCallback<EditText> {
                it.strId = BLOCK_ID_BACK_END_URL
                val lp = LinearLayout.LayoutParams(MP, WC)
                lp.bottomMargin = dp10
                it.layoutParams = lp
                it.hint = "please input backend s url"
                it.inputType = 0x00000011
                it.setText(DEEPNAVI_URL.getData() ?: DEFAULT_VALUE_URL)
                it.addTextChangedListener { text -> DEEPNAVI_URL.setData(text?.toString().orEmpty()) }
            })

            addBlock(Block(R.layout.view_button).setInflatedCallback<Button> {
                it.strId = BLOCK_ID_BEGIN
                val lp = LinearLayout.LayoutParams(MP, WC)
                lp.bottomMargin = dp10
                it.layoutParams = lp
                it.text = "begin"

                it.setOnClickListener begin@{
                    val editUrl = DEEPNAVI_URL.value ?: ""
                    if (editUrl.isEmpty()) {
                        Toast.makeText(this@ConfigActivity, "Url should not be empty", Toast.LENGTH_LONG).show()
                        return@begin
                    }
                    startActivity(Intent(this@ConfigActivity, cls))
                }
            })

            addBlock(Block(R.layout.layout_recycler).setInflatedCallback<RecyclerView> {
                it.strId = BLOCK_ID_DEEPNAVI_CONFIG_LIST
                val lp = LinearLayout.LayoutParams(MP, WC)

                val configListData = SensorListeners.DEFAULT_VALUE_SENSOR_CONFIG.toMutableList()
                val baseAdapter = BaseRecyclerAdapter<String>(
                    this@ConfigActivity,
                    configListData,
                    R.layout.item_config,
                    object : BaseRecyclerAdapter.AdapterAction<String> {
                        override fun onBindViewHolder(holder: BaseRecyclerViewHolder, data: String, position: Int) {
                            if (holder.itemView is TextView) {
                                holder.itemView.text = data
                                holder.itemView.isSelected = SIGNAL_CONFIG_SET.contains(data)
                            }
                        }
                    })
                baseAdapter.setItemClickListener(object : BaseRecyclerAdapter.ItemClickListener<String> {
                    override fun onItemClick(holder: BaseRecyclerViewHolder, data: String, position: Int) {
                        val view = holder.itemView
                        view.isSelected = !view.isSelected
                        if (view is TextView) {
                            val text = view.text.toString()
                            if (view.isSelected && !SIGNAL_CONFIG_SET.contains(data)) {
                                SIGNAL_CONFIG_SET.add(data)
                            } else if (!view.isSelected && SIGNAL_CONFIG_SET.contains(data)) {
                                SIGNAL_CONFIG_SET.remove(data)
                            }
                            Log.d(TAG, "ItemClick -- view: ${view.text}, isSelected changed: ${view.isSelected}")
                        } else Log.d(TAG, "ItemClick -- view isn't a TextView")
                    }
                })
                it.adapter = baseAdapter
                it.layoutManager = LinearLayoutManager(this@ConfigActivity, LinearLayoutManager.VERTICAL, false)
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
