package com.sysu.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sysu.deepnavi.bean.Basic

@SuppressLint("CI_ByteDanceKotlinRules_Not_Allow_findViewById_Invoked_In_UI")
class ConfigActivity : AppCompatActivity() {
    companion object {
        const val TAG = "Config"
        const val SP_NAME_CONFIG = "config"
        const val SP_KEY_SIGNAL_CONFIG = "deepNavi_signal_config"

        const val DEFAULT_VALUE_FREQUENCY = 3
        const val DEFAULT_VALUE_URL = "10.95.40.1:5000"

        const val EXTRA_KEY_URL = "url"
        const val EXTRA_KEY_FREQUENCY = "frequency"
        const val EXTRA_KEY_SIGNAL_CONFIG = "signalConfig"
    }

    private lateinit var sp: SharedPreferences
    private var cls: Class<*> = MainActivity2::class.java
    private lateinit var urlEdit: EditText
    private lateinit var frequencyEdit: EditText

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        findViewById<RadioGroup>(R.id.camera_select).setOnCheckedChangeListener { _, checkedId ->
            cls = when (checkedId) {
                R.id.radio_camera -> MainActivity::class.java
                R.id.radio_camera2 -> MainActivity2::class.java
                R.id.radio_media_recorder -> MainActivity3::class.java
                else -> MainActivity2::class.java
            }
        }
        urlEdit = findViewById(R.id.backend_url)
        urlEdit.setText(DEFAULT_VALUE_URL)
        frequencyEdit = findViewById(R.id.frequency)
        frequencyEdit.setText("" + DEFAULT_VALUE_FREQUENCY)

        sp = getSharedPreferences(SP_NAME_CONFIG, Context.MODE_PRIVATE)
        val signalConfigSet = sp.getStringSet(SP_KEY_SIGNAL_CONFIG, mutableSetOf()) ?: mutableSetOf()
        val configListData = Basic.DeepNaviReq.getDescriptor().fields.filter { it != null && it.name != "time" }.map { it.name }.toMutableList()

        findViewById<RecyclerView>(R.id.deepnavi_config_list).run {
            val baseAdapter = BaseRecyclerAdapter<String>(
                this@ConfigActivity,
                configListData,
                R.layout.item_config,
                object : BaseRecyclerAdapter.AdapterAction<String> {
                    override fun onBindViewHolder(holder: BaseRecyclerViewHolder, data: String, position: Int) {
                        if (holder.itemView is TextView) {
                            holder.itemView.text = data
                            holder.itemView.isSelected = signalConfigSet.contains(data)
                        }
                    }
                })
            baseAdapter.setItemClickListener(object : BaseRecyclerAdapter.ItemClickListener<String> {
                override fun onItemClick(holder: BaseRecyclerViewHolder, data: String, position: Int) {
                    val view = holder.itemView
                    view.isSelected = !view.isSelected
                    if (view is TextView) {
                        val text = view.text.toString()
                        if (view.isSelected && !signalConfigSet.contains(text)) {
                            signalConfigSet.add(text)
                        } else if (!view.isSelected && signalConfigSet.contains(text)) {
                            signalConfigSet.remove(text)
                        }
                        sp.edit().putStringSet(SP_KEY_SIGNAL_CONFIG, signalConfigSet).run {
                            apply()
                            commit()
                        }
                        Log.d(TAG, "ItemClick -- view: ${view.text}, isSelected changed: ${view.isSelected}")
                    } else Log.d(TAG, "ItemClick -- view isn't a TextView")
                }
            })
            adapter = baseAdapter
            layoutManager = LinearLayoutManager(this@ConfigActivity, LinearLayoutManager.VERTICAL, false)
            addItemDecoration(DividerItemDecoration(this@ConfigActivity, DividerItemDecoration.VERTICAL))
        }

        Log.d(
            TAG, "onCreate -- initial config -- ${configListData.joinToString()} --" +
                    "${Basic.DeepNaviReq.getDescriptor().fields.size} -- ${signalConfigSet.joinToString()}"
        )

        findViewById<Button>(R.id.begin).setOnClickListener begin@{
            val url = urlEdit.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(this, "Url should not be empty", Toast.LENGTH_LONG).show()
                return@begin
            }
            val frequency = frequencyEdit.text.toString().trim()
            if (frequency.isEmpty()) {
                Toast.makeText(this, "Frequency should not be empty", Toast.LENGTH_LONG).show()
                return@begin
            }
            val frequencyNumber = try {
                frequency.toInt()
            } catch (e: java.lang.NumberFormatException) {
                Toast.makeText(this, "Frequency can't be transferred to a number", Toast.LENGTH_LONG).show()
                return@begin
            }
            startActivity(
                Intent(this, cls)
                    .putExtra(EXTRA_KEY_URL, url)
                    .putExtra(EXTRA_KEY_FREQUENCY, frequencyNumber)
                    .putExtra(EXTRA_KEY_SIGNAL_CONFIG, signalConfigSet.joinToString())
            )
        }

        // setListView()
    }

    private fun setListView() {
        //     sp = applicationContext.getSharedPreferences(SP_NAME_CONFIG, Context.MODE_PRIVATE)
        //     val signalConfigSet = sp.getStringSet(SP_KEY_SIGNAL_CONFIG, mutableSetOf()) ?: mutableSetOf()
        //     val configListData = Basic.DeepNaviReq.getDescriptor().fields.filter { it != null && it.name != "time" }.map {
        //         mapOf(Pair("item_select", it.name))
        //     }.toMutableList()
        //
        //     val configList = findViewById<ListView>(R.id.deepnavi_config_list)
        //     configList.adapter = SimpleAdapter(this, configListData, R.layout.item_config, arrayOf("item_select"), intArrayOf(R.id.item_select))
        //     configList.setOnItemClickListener { _, view, _, _ ->
        //         view.isSelected = !view.isSelected
        //         if (view is TextView) {
        //             val text = view.text.toString()
        //             if (view.isSelected && !signalConfigSet.contains(text)) {
        //                 signalConfigSet.add(text)
        //             } else if (!view.isSelected && signalConfigSet.contains(text)) {
        //                 signalConfigSet.remove(text)
        //             }
        //             sp.edit().putStringSet(SP_KEY_SIGNAL_CONFIG, signalConfigSet).commit()
        //             Log.d(TAG, "ItemClick -- view: ${view.text}, isSelected changed: ${view.isSelected}")
        //         } else Log.d(TAG, "ItemClick -- view isn't a TextView")
        //     }
        //     configList.post { configList.forEach { if (it is TextView && signalConfigSet.contains(it.text)) it.isSelected = true } }
        //
        //     Log.d(
        //         TAG,
        //         "onCreate -- initial config -- ${configListData.joinToString()} -- ${Basic.DeepNaviReq.getDescriptor().fields.size} -- ${signalConfigSet.joinToString()}"
        //     )
        //
        //     // val listener = object : View.OnTouchListener {
        //     //     override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        //     //         v?.performClick()
        //     //         // final int DRAWABLE_LEFT = 0;
        //     //         // final int DRAWABLE_TOP = 1;
        //     //         // final int DRAWABLE_RIGHT = 2;
        //     //         // final int DRAWABLE_BOTTOM = 3;
        //     //         if (event?.action == MotionEvent.ACTION_UP && v != null && v is TextView) {
        //     //             if (event.rawX >= (v.right - v.compoundDrawables[2].bounds.width())) {
        //     //                 v.isSelected = true
        //     //                 return true
        //     //             }
        //     //         }
        //     //         return false
        //     //     }
        //     // }
        //     // configList.post { configList.forEach { it.setOnTouchListener(listener) } }
    }
}