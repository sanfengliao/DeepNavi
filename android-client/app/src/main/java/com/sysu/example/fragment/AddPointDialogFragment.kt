package com.sysu.example.fragment

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.liang.example.json_ktx.JsonStyle
import com.liang.example.json_ktx.ReflectJsonApi
import com.liang.example.json_ktx.SimpleJsonObject
import com.liang.example.json_ktx.SimpleJsonParser
import com.liang.example.json_ktx.SimpleJsonString
import com.sysu.example.KeyUrls
import com.sysu.example.R
import com.sysu.example.bean.DeepNaviCoordinator
import com.sysu.example.bean.DeepNaviMap
import com.sysu.example.bean.DeepNaviPoint
import com.sysu.example.utils.ContextApi
import com.sysu.example.utils.doPostMainAsync
import com.sysu.example.utils.returnToast3
import kotlinx.android.synthetic.main.fragment_add_point.is_key_point
import kotlinx.android.synthetic.main.fragment_add_point.is_percentage_mode
import kotlinx.android.synthetic.main.fragment_add_point.is_point_and_loc
import kotlinx.android.synthetic.main.fragment_add_point.point_actual_x
import kotlinx.android.synthetic.main.fragment_add_point.point_actual_y
import kotlinx.android.synthetic.main.fragment_add_point.point_actual_z
import kotlinx.android.synthetic.main.fragment_add_point.point_name
import kotlinx.android.synthetic.main.fragment_add_point.point_x
import kotlinx.android.synthetic.main.fragment_add_point.point_y
import kotlinx.android.synthetic.main.fragment_add_point.point_z

open class AddPointDialogFragment(
    protected open var mapInfo: DeepNaviMap,
    protected open var pointUpdater: UpdateMapPoint
) : BaseDialogFragment(R.layout.fragment_add_point) {
    protected open var name: String? = null
    protected open var x: Float = -1f
    protected open var y: Float = -1f
    protected open var z: Float = -1f
    protected open var xF: Float = -1f
    protected open var yF: Float = -1f
    protected open var zF: Float = -1f

    protected open var isPercentageMode = true
    protected open var innerChanged = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        is_percentage_mode.setOnCheckedChangeListener { _, isChecked -> changeMode(isChecked) }
        is_key_point.setOnCheckedChangeListener { _, isChecked ->
            is_point_and_loc.isVisible = isChecked
            is_point_and_loc.isChecked = false
        }
        createPointInfoListener()
        view.findViewById<Button>(R.id.ok).setOnClickListener ok@{
            val flag = is_key_point.isChecked
            if (flag && name == null) {
                return@ok returnToast3("point's name should not be empty while it's breaking point")
            }
            val addPoint = DeepNaviPoint()
            addPoint.name = name
            x = parsePoint(point_x.text?.toString()) ?: return@ok returnToast3("point's x should be floating number or percentage")
            y = parsePoint(point_y.text?.toString()) ?: return@ok returnToast3("point's y should be floating number or percentage")
            z = parsePoint(point_z.text?.toString()) ?: -1f
            if (isPercentageMode) {
                addPoint.planCoordinate =
                    DeepNaviCoordinator(x * mapInfo.planSize[0], y * mapInfo.planSize[1], z * mapInfo.planSize[2])
                addPoint.actualCoordinate = DeepNaviCoordinator(
                    x * mapInfo.actualSize[0],
                    y * mapInfo.actualSize[1],
                    z * mapInfo.actualSize[2]
                )
            } else {
                xF = point_actual_x.text?.toString()?.toFloatOrNull() ?: return@ok returnToast3("point's actual x should be floating number")
                yF = point_actual_y.text?.toString()?.toFloatOrNull() ?: return@ok returnToast3("point's actual y should be floating number")
                zF = point_actual_z.text?.toString()?.toFloatOrNull() ?: -1f
                addPoint.planCoordinate = DeepNaviCoordinator(x, y, z)
                addPoint.actualCoordinate = DeepNaviCoordinator(xF, yF, zF)
            }
            if (mapInfo.id != null) {
                addPoint.mapId = mapInfo.id!!
                if (addPoint(addPoint, mapInfo)) {
                    return@ok returnToast3("error occurred while parse AddPoint object to json")
                }
                if (flag && is_point_and_loc.isChecked) {
                    addPoint.name = null
                    if (addPoint(addPoint, mapInfo)) {
                        return@ok returnToast3("error occurred while parse AddPoint2 object to json")
                    }
                }
            } else {
                dismiss()
            }
        }
        view.findViewById<Button>(R.id.cancel)?.setOnClickListener {
            updatePoint(1)
            dismiss()
        }
    }

    // can be static
    protected open fun addPoint(addPoint: DeepNaviPoint, mapInfo: DeepNaviMap): Boolean {
        val flag = addPoint.name == null
        doPostMainAsync(
            when (flag) {
                true -> KeyUrls.ADD_POINT
                else -> KeyUrls.ADD_KEY_POINT
            }, null, ReflectJsonApi.toJsonOrNull(addPoint)?.toByteArray()
                ?: return true
        ) addPointRes@{
            addPoint.worldToModel(mapInfo)
            val content = it?.content ?: return@addPointRes returnToast3("no response while add point: $it")
            val jsonObj = SimpleJsonParser.fromJson(String(content), JsonStyle.STANDARD) as? SimpleJsonObject
                ?: return@addPointRes returnToast3("jsonObj parse error occurred while add point: $it")
            val text = if ("msg" in jsonObj) {
                jsonObj["msg"]!!.string()
            } else {
                val pointId = ((jsonObj["data"] as? SimpleJsonObject)?.get("id") as? SimpleJsonString)?.value()
                    ?: return@addPointRes returnToast3("no point id")
                if (flag) {
                    updatePoint(2, pointId)
                } else {
                    updatePoint(1)
                }
                dismiss()
                "Add point successfully"
            }
            Toast.makeText(ContextApi.appContext, text, Toast.LENGTH_LONG).show()
        }
        return false
    }

    protected open fun createPointInfoListener() {
        point_x.addTextChangedListener {
            x = parsePoint(it?.toString()) ?: -1f
            updatePoint()
        }
        point_y.addTextChangedListener {
            y = parsePoint(it?.toString()) ?: -1f
            updatePoint()
        }
        point_z.addTextChangedListener {
            z = parsePoint(it?.toString()) ?: -1f
            updatePoint()
        }
        point_actual_x.addTextChangedListener {
            xF = it?.toString()?.toFloatOrNull() ?: -1f
            updatePoint()
        }
        point_actual_y.addTextChangedListener {
            yF = it?.toString()?.toFloatOrNull() ?: -1f
            updatePoint()
        }
        point_actual_z.addTextChangedListener {
            zF = it?.toString()?.toFloatOrNull() ?: -1f
            updatePoint()
        }
        point_name.addTextChangedListener {
            name = point_name.text?.toString()?.trim()
            updatePoint()
        }
    }

    protected open fun changeMode(isChecked: Boolean) {
        isPercentageMode = isChecked
        val visibility = if (isPercentageMode) {
            View.GONE
        } else {
            View.VISIBLE
        }
        point_actual_x.visibility = visibility
        point_actual_y.visibility = visibility
        point_actual_z.visibility = visibility
        updatePoint(1)
        innerChanged = true
        point_x.setText("")
        point_y.setText("")
        point_z.setText("")
        point_actual_x.setText("")
        point_actual_y.setText("")
        point_actual_z.setText("")
        innerChanged = false
    }

    /**
     * 一个point中的x,y,z可以用百分比表示，也可以用int px表示，暂时不提供m的选项
     */
    protected open fun parsePoint(text: String?): Float? {
        if (text.isNullOrEmpty()) {
            return null
        }
        val text2 = text.trim()
        if (text2.endsWith('%')) {
            return text2.substring(0, text2.length - 1).toFloatOrNull()
        }
        return text2.toFloatOrNull()
    }

    open fun updatePoint(flag: Int = 0, id: String? = null) {
        if (!innerChanged) {
            if (isPercentageMode) {
                pointUpdater.update(x, y, z, flag, name, id)
            } else {
                pointUpdater.update(x, y, z, xF, yF, zF, flag, name, id)
            }
        }
    }

    interface UpdateMapPoint {
        /**
         * flag == 0，用于新建和更新marker
         * flag == 1，用于删除marker
         * flag == 2，用于确定marker
         */
        fun update(x: Float, y: Float, z: Float, flag: Int = 0, name: String? = null, id: String? = null)
        fun update(x: Float, y: Float, z: Float, xF: Float, yF: Float, zF: Float, flag: Int = 0, name: String? = null, id: String? = null)
    }
}
