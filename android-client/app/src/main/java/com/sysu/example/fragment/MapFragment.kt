package com.sysu.example.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.core.view.setPadding
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.liang.example.json_ktx.JsonStyle
import com.liang.example.json_ktx.ReflectJsonApi
import com.liang.example.json_ktx.SimpleJsonArray
import com.liang.example.json_ktx.SimpleJsonObject
import com.liang.example.json_ktx.SimpleJsonParser
import com.liang.example.json_ktx.SimpleJsonString
import com.sysu.deepnavi.util.bitmapToByteArray
import com.sysu.example.App.Companion.BASE_URL
import com.sysu.example.BaseRecyclerAdapter
import com.sysu.example.BaseRecyclerAdapter.AdapterAction
import com.sysu.example.BaseRecyclerViewHolder
import com.sysu.example.KeyUrls.ADD_EDGE
import com.sysu.example.KeyUrls.ADD_KEY_POINT
import com.sysu.example.KeyUrls.ADD_POINT
import com.sysu.example.KeyUrls.LOAD_MAP
import com.sysu.example.KeyUrls.SEARCH_BY_END_POINT
import com.sysu.example.KeyUrls.SEARCH_BY_START_POINT
import com.sysu.example.KeyUrls.UPLOAD_MAP
import com.sysu.example.R
import com.sysu.example.utils.ContextApi
import com.sysu.example.utils.HttpResult
import com.sysu.example.utils.doGetMainAsync
import com.sysu.example.utils.doPostMainAsync
import com.sysu.example.utils.returnToast2
import com.sysu.example.utils.returnToast3
import com.sysu.example.utils.sendMultiPart
import com.sysu.example.utils.setOnCompoundDrawableClickListener
import com.sysu.example.utils.toInt
import kotlinx.android.synthetic.main.fragment_add_edge.add_end_point
import kotlinx.android.synthetic.main.fragment_add_edge.add_start_point
import kotlinx.android.synthetic.main.fragment_add_edge.point_choice
import kotlinx.android.synthetic.main.fragment_add_point.is_key_point
import kotlinx.android.synthetic.main.fragment_add_point.is_percentage_mode
import kotlinx.android.synthetic.main.fragment_add_point.point_actual_x
import kotlinx.android.synthetic.main.fragment_add_point.point_actual_y
import kotlinx.android.synthetic.main.fragment_add_point.point_actual_z
import kotlinx.android.synthetic.main.fragment_add_point.point_name
import kotlinx.android.synthetic.main.fragment_add_point.point_x
import kotlinx.android.synthetic.main.fragment_add_point.point_y
import kotlinx.android.synthetic.main.fragment_add_point.point_z
import kotlinx.android.synthetic.main.fragment_search.end_point
import kotlinx.android.synthetic.main.fragment_search.include_edges
import kotlinx.android.synthetic.main.fragment_search.include_points
import kotlinx.android.synthetic.main.fragment_search.search_choice
import kotlinx.android.synthetic.main.fragment_search.search_result
import kotlinx.android.synthetic.main.fragment_search.start_point
import kotlinx.android.synthetic.main.fragment_upload_map.map_actual_size_x
import kotlinx.android.synthetic.main.fragment_upload_map.map_actual_size_y
import kotlinx.android.synthetic.main.fragment_upload_map.map_actual_size_z
import kotlinx.android.synthetic.main.fragment_upload_map.map_bitmap_size_x
import kotlinx.android.synthetic.main.fragment_upload_map.map_bitmap_size_y
import kotlinx.android.synthetic.main.fragment_upload_map.map_bitmap_size_z
import kotlinx.android.synthetic.main.fragment_upload_map.map_name
import kotlinx.android.synthetic.main.fragment_upload_map.select_map_drawable
import kotlinx.android.synthetic.main.fragment_upload_map.set_origin_point

// [DialogFragment使用小结](https://www.jianshu.com/p/0861ee5b9028)

/**
 * 一个point中的x,y,z可以用百分比表示，也可以用int px表示，暂时不提供m的选项
 */
fun parsePoint(text: String?): Float? {
    if (text.isNullOrEmpty()) {
        return null
    }
    val text2 = text.trim()
    if (text2.endsWith('%')) {
        return text2.substring(0, text2.length - 1).toFloatOrNull()
    }
    return text2.toFloatOrNull()
}

open class BaseDialogFragment(
    @LayoutRes open val layoutResId: Int,
    open var fullScreen: Boolean = true
) : DialogFragment() {
    // init {
    //     if (fullScreen) {
    //         setStyle(STYLE_NORMAL, R.style.Dialog_FullScreen)
    //     }
    // }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        if (fullScreen) {
            dialog.window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                decorView.setPadding(0)
                val wlp: WindowManager.LayoutParams = attributes
                wlp.width = WindowManager.LayoutParams.MATCH_PARENT
                wlp.height = WindowManager.LayoutParams.MATCH_PARENT
                attributes = wlp
            }
        }
        dialog.setOnShowListener(showListener)
        // dialog.setOnDismissListener(dismissListener)  // 无效，dialogFragment中只能走onDismiss
        dialog.setOnCancelListener(cancelListener)
        dialog.setOnKeyListener(keyListener)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layoutResId, container, false)
    }

    override fun onResume() {
        if (fullScreen) {
            val params: ViewGroup.LayoutParams = dialog!!.window!!.attributes
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.height = WindowManager.LayoutParams.MATCH_PARENT
            dialog!!.window!!.attributes = params as WindowManager.LayoutParams
        }
        super.onResume()
    }

    open var showListener: DialogInterface.OnShowListener? = null
    open var dismissListener: DialogInterface.OnDismissListener? = null
    open var cancelListener: DialogInterface.OnCancelListener? = null
    open var keyListener: DialogInterface.OnKeyListener? = null
    // open var multiChoiceClickListener: DialogInterface.OnMultiChoiceClickListener? = null
    // open var clickListener: DialogInterface.OnClickListener? = null

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        dismissListener?.onDismiss(dialog)
    }
}

open class UploadMapDialogFragment(
    protected open var bitmapUpdater: UpdateMapBitmap,
    protected open var originPointUpdater: AddPointDialogFragment.UpdateMapPoint,
    protected open var mapInfoUpdater: UpdateMapInfo
) : BaseDialogFragment(R.layout.fragment_upload_map) {
    protected open var bitmap: Bitmap? = null
    protected open val uploadMap = UploadMap()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        select_map_drawable.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), IMAGE_RESULT_CODE)
        }
        set_origin_point.setOnClickListener {
            if (getPlanSize() || getActualSize()) {
                return@setOnClickListener
            }
            var final = false
            val addPointDialogFragment = AddPointDialogFragment(uploadMap.toDeepNaviMap(), object : AddPointDialogFragment.UpdateMapPoint {
                override fun update(x: Float, y: Float, z: Float, flag: Int, name: String?) {
                    originPointUpdater.update(x, y, z, flag)
                    if (final || flag == 2) {
                        return
                    }
                    uploadMap.originInPlan = if (flag == 1 || x < 0f || y < 0f) {
                        uploadMap.originInActual = INVALID_FLOAT_ARRAY
                        INVALID_FLOAT_ARRAY
                    } else if (x < 1f) {
                        uploadMap.originInActual = floatArrayOf(x * uploadMap.actualSize[0], y * uploadMap.actualSize[1], z * uploadMap.actualSize[2])
                        floatArrayOf(x * uploadMap.planSize[0], y * uploadMap.planSize[1], z * uploadMap.planSize[2])
                    } else {
                        uploadMap.originInActual = floatArrayOf(
                            x / uploadMap.planSize[0] * uploadMap.actualSize[0],
                            y / uploadMap.planSize[1] * uploadMap.actualSize[1],
                            z / uploadMap.planSize[2] * uploadMap.actualSize[2]
                        )
                        floatArrayOf(x, y, z)
                    }
                }

                override fun update(x: Float, y: Float, z: Float, xF: Float, yF: Float, zF: Float, flag: Int, name: String?) {
                    originPointUpdater.update(x, y, z, xF, yF, zF, flag)
                    if (final || flag == 2) {
                        return
                    }
                    uploadMap.originInPlan = if (flag == 1 || x < 0f || y < 0f) {
                        uploadMap.originInActual = INVALID_FLOAT_ARRAY
                        INVALID_FLOAT_ARRAY
                    } else {
                        uploadMap.originInActual = floatArrayOf(xF, yF, zF)
                        floatArrayOf(x, y, z)
                    }
                }
            })
            addPointDialogFragment.dismissListener = DialogInterface.OnDismissListener {
                this.dialog?.show()
                final = true
                addPointDialogFragment.updatePoint(1)
            }
            addPointDialogFragment.show(childFragmentManager, "addPoint")
            dialog?.hide()
        }
        view.findViewById<Button>(R.id.ok).setOnClickListener ok@{
            val bitmap = this.bitmap ?: return@ok returnToast3("map's bitmap should be select")
            uploadMap.name = map_name.text?.toString()?.trim() ?: return@ok returnToast3("map's name should not be empty")
            if (getPlanSize() || getActualSize()) {
                return@ok
            }
            if (uploadMap.originInPlan === INVALID_FLOAT_ARRAY) {
                return@ok returnToast3("you should select the origin point")
            }
            sendMultiPart(
                UPLOAD_MAP,
                mapOf(
                    "name" to uploadMap.name,
                    "planSize" to uploadMap.planSize.joinToString(","),
                    "planUnit" to uploadMap.planUnit,
                    "actualSize" to uploadMap.actualSize.joinToString(","),
                    "actualUnit" to uploadMap.actualUnit,
                    "originInPlan" to uploadMap.originInPlan.joinToString(",")
                ),
                mapOf("planImage" to bitmapToByteArray(bitmap))
            ) uploadMap@{
                val content = it ?: return@uploadMap returnToast3("no response while upload map")
                val jsonObj = SimpleJsonParser.fromJson(String(content), JsonStyle.STANDARD) as? SimpleJsonObject
                    ?: return@uploadMap returnToast3("jsonObj parse error occurred while upload map")
                val flag = "msg" in jsonObj
                ContextApi.handler.post {
                    Toast.makeText(
                        ContextApi.appContext, if (flag) {
                            jsonObj["msg"]!!.string()
                        } else {
                            dismiss()
                            "Create map successfully"
                        }, Toast.LENGTH_LONG
                    ).show()
                }
                if (!flag) {
                    val data = jsonObj["data"] as? SimpleJsonObject ?: return@uploadMap returnToast3("mapInfo cannot be updated")
                    val mapInfo = uploadMap.toDeepNaviMap()
                    mapInfo.id = (data["id"] as? SimpleJsonString)?.string() ?: INVALID_STR
                    mapInfo.planPath = (data["planPath"] as? SimpleJsonString)?.string() ?: INVALID_STR
                    mapInfo.modelPath = (data["modelPath"] as? SimpleJsonString)?.string() ?: INVALID_STR
                    mapInfoUpdater.update(mapInfo)
                }
            }
        }
        view.findViewById<Button>(R.id.cancel)?.setOnClickListener {
            dismiss()
        }
    }

    protected open fun getPlanSize(): Boolean {
        val x1 = map_bitmap_size_x.text?.toString()?.toFloatOrNull() ?: return returnToast2("bitmap's width should be integer", true)
        val y1 = map_bitmap_size_y.text?.toString()?.toFloatOrNull() ?: return returnToast2("bitmap's height should be integer", true)
        val z1 = map_bitmap_size_z.text?.toString()?.toFloatOrNull() ?: 0f
        uploadMap.planSize = floatArrayOf(x1, y1, z1)
        return false
    }

    protected open fun getActualSize(): Boolean {
        val x2 = map_actual_size_x.text?.toString()?.toFloatOrNull() ?: return returnToast2("actual's width should be floating number or percentage", true)
        val y2 = map_actual_size_y.text?.toString()?.toFloatOrNull() ?: return returnToast2("actual's height should be floating number or percentage", true)
        val z2 = map_actual_size_z.text?.toString()?.toFloatOrNull() ?: -1f
        uploadMap.actualSize = floatArrayOf(x2, y2, z2)
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            IMAGE_RESULT_CODE -> if (resultCode == Activity.RESULT_OK) {
                val uri: Uri = data?.data ?: return returnToast3("data.data is null Uri")
                bitmap = getBitmapFromUri(uri, this.context ?: return returnToast3("getBitmapFromUri failed because context is null"))
                    ?: return returnToast3("getBitmapFromUri failed")
                map_bitmap_size_x.setText(bitmap!!.width.toString())
                map_bitmap_size_y.setText(bitmap!!.height.toString())
                uploadMap.planSize = floatArrayOf(bitmap!!.width.toFloat(), bitmap!!.height.toFloat(), 0f)
                bitmapUpdater.update(bitmap!!)
            }
        }
    }

    interface UpdateMapBitmap {
        fun update(bitmap: Bitmap, load: Boolean = false)
    }

    interface UpdateMapInfo {
        fun update(mapInfo: DeepNaviMap)
    }

    companion object {
        const val IMAGE_RESULT_CODE = 1

        fun getBitmapFromUri(uri: Uri?, mContext: Context): Bitmap? = try {
            MediaStore.Images.Media.getBitmap(mContext.contentResolver, uri)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

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
        is_percentage_mode.setOnCheckedChangeListener { _, isChecked ->
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
        view.findViewById<Button>(R.id.ok).setOnClickListener ok@{
            val flag = is_key_point.isChecked
            if (flag && name == null) {
                return@ok returnToast3("point's name should not be empty while it's breaking point")
            }
            val addPoint = AddPoint()
            addPoint.name = name
            x = parsePoint(point_x.text?.toString()) ?: return@ok returnToast3("point's x should be floating number or percentage")
            y = parsePoint(point_y.text?.toString()) ?: return@ok returnToast3("point's y should be floating number or percentage")
            z = parsePoint(point_z.text?.toString()) ?: -1f
            if (isPercentageMode) {
                addPoint.planCoordinate = DeepNaviCoordinator(x * mapInfo.planSize[0], y * mapInfo.planSize[1], z * mapInfo.planSize[2])
                addPoint.actualCoordinate = DeepNaviCoordinator(x * mapInfo.actualSize[0], y * mapInfo.actualSize[1], z * mapInfo.actualSize[2])
            } else {
                xF = point_actual_x.text?.toString()?.toFloatOrNull() ?: return@ok returnToast3("point's actual x should be floating number")
                yF = point_actual_y.text?.toString()?.toFloatOrNull() ?: return@ok returnToast3("point's actual y should be floating number")
                zF = point_actual_z.text?.toString()?.toFloatOrNull() ?: -1f
                addPoint.planCoordinate = DeepNaviCoordinator(x, y, z)
                addPoint.actualCoordinate = DeepNaviCoordinator(xF, yF, zF)
            }
            if (mapInfo.id != INVALID_STR) {
                addPoint.mapId = mapInfo.id
                doPostMainAsync(
                    when (name) {
                        null -> ADD_POINT
                        else -> ADD_KEY_POINT
                    }, null, ReflectJsonApi.toJsonOrNull(addPoint)?.toByteArray()
                        ?: return@ok returnToast3("error occurred while parse AddPoint object to json")
                ) addPoint@{
                    val content = it?.content ?: return@addPoint returnToast3("no response while add point: $it")
                    val jsonObj = SimpleJsonParser.fromJson(String(content), JsonStyle.STANDARD) as? SimpleJsonObject
                        ?: return@addPoint returnToast3("jsonObj parse error occurred while add point: $it")
                    Toast.makeText(
                        ContextApi.appContext, if ("msg" in jsonObj) {
                            jsonObj["msg"]!!.string()
                        } else {
                            dismiss()
                            updatePoint(2)
                            "Add point successfully"
                        }, Toast.LENGTH_LONG
                    ).show()
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

    open fun updatePoint(flag: Int = 0) {
        if (!innerChanged) {
            if (isPercentageMode) {
                pointUpdater.update(x, y, z, flag, name)
            } else {
                pointUpdater.update(x, y, z, xF, yF, zF, flag, name)
            }
        }
    }

    interface UpdateMapPoint {
        /**
         * flag == 0，用于新建和更新marker
         * flag == 1，用于删除marker
         * flag == 2，用于确定marker
         */
        fun update(x: Float, y: Float, z: Float, flag: Int = 0, name: String? = null)
        fun update(x: Float, y: Float, z: Float, xF: Float, yF: Float, zF: Float, flag: Int = 0, name: String? = null)
    }
}

// 这个被唤起前应该 请求map的所有点
open class AddEdgeDialogFragment(
    protected open var mapId: String,
    protected open var points: List<DeepNaviPoint>,
    protected open var edgeUpdater: UpdateMapEdge
) : BaseDialogFragment(R.layout.fragment_add_edge) {
    protected open var pId1: String? = null
    protected open var pId2: String? = null
    protected open var innerChanged: Boolean = false
    protected open var pointChoice: Int = 2

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        point_choice.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.point_id -> pointChoice = 0
                R.id.point_name -> pointChoice = 1
                R.id.point_index -> pointChoice = 2
            }
            innerChanged = true
            add_start_point.text = null
            add_end_point.text = null
            innerChanged = false
        }
        add_start_point.addTextChangedListener {
            pId1 = parsePointId(it?.toString()?.trim())
            if (!innerChanged) {
                edgeUpdater.update(pId1, pId2)
            }
        }
        add_end_point.addTextChangedListener {
            pId2 = parsePointId(it?.toString()?.trim())
            if (!innerChanged) {
                edgeUpdater.update(pId1, pId2)
            }
        }
        view.findViewById<Button>(R.id.ok)?.setOnClickListener ok@{
            pId1 = parsePointId(add_start_point.text?.toString()?.trim())
            pId2 = parsePointId(add_end_point.text?.toString()?.trim())
            if (pId1 == null || pId2 == null) {
                return@ok returnToast3("start point's id and end point's id should not be empty")
            }
            // val addEdge = AddEdge()
            // addEdge.mapId = mapId
            // addEdge.pointAId = pId1
            // addEdge.pointBId = pId2
            // val params = ReflectJsonApi.toJsonOrNull(addEdge) ?: return@ok returnToast3("error occurred while parse AddEdge object to json")
            val params = "mapId=$mapId&pointAId=$pId1&pointBId=$pId2"
            doPostMainAsync(ADD_EDGE, null, params.toByteArray()) addEdge@{
                val content = it?.content ?: return@addEdge returnToast3("no response for adding edge")
                val jsonObj = SimpleJsonParser.fromJson(String(content), JsonStyle.STANDARD) as? SimpleJsonObject
                    ?: return@addEdge returnToast3("jsonObj parse error occurred while adding edge")
                Toast.makeText(
                    ContextApi.appContext, if ("msg" in jsonObj) {
                        jsonObj["msg"]!!.string()
                    } else {
                        edgeUpdater.update(pId1, pId2, 2)
                        dismiss()
                        "Add edge successfully"
                    }, Toast.LENGTH_LONG
                ).show()
            }
        }
        view.findViewById<Button>(R.id.cancel)?.setOnClickListener {
            edgeUpdater.update(pId1, pId2, 1)
            dismiss()
        }
    }

    protected open fun parsePointId(text: String?): String? {
        if (text == null) {
            return null
        }
        return when (pointChoice) {
            0 -> text
            1 -> points.find { it.name == text }?.id
            else -> when (val index = text.toIntOrNull()) {
                null -> null
                else -> points[index].id
            }
        }
    }

    interface UpdateMapEdge {
        fun update(pId1: String?, pId2: String?, flag: Int = 0)
    }
}

open class SearchMapDialogFragment(
    protected open var bitmapUpdater: UploadMapDialogFragment.UpdateMapBitmap,
    protected open var mapInfoUpdater: UploadMapDialogFragment.UpdateMapInfo,
    protected open var pointsUpdater: GetAllPoints
) : BaseDialogFragment(R.layout.fragment_search) {
    protected open var mapInfo: DeepNaviMap? = null
    // protected open var allPoints: MutableList<DeepNaviPoint>? = null
    // protected open var allEdges: MutableList<DeepNaviEdge>? = null

    protected open lateinit var adapter: BaseRecyclerAdapter<Pair<DeepNaviPoint, DeepNaviMap>>
    protected open var searchChoice: Int = 2

    protected open var startPointName: String? = null
    protected open var endPointName: String? = null
    protected open var startPoint = INVALID_DEEPNAVI_POINT
    protected open var endPoint = INVALID_DEEPNAVI_POINT

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (initMapList()) {
            return returnToast3("cannot make adapter: context is null")
        }

        search_choice.setOnCheckedChangeListener { _, checkedId ->
            end_point.visibility = View.GONE
            include_points.visibility = View.VISIBLE
            include_edges.visibility = View.VISIBLE
            when (checkedId) {
                R.id.is_search_map_by_point -> {
                    searchChoice = 0
                    start_point.hint = resources.getString(R.string.please_input_key_point)
                }
                R.id.is_search_path -> {
                    searchChoice = 1
                    end_point.visibility = View.VISIBLE
                    start_point.hint = resources.getString(R.string.please_input_starting_point)
                    include_points.visibility = View.GONE
                    include_edges.visibility = View.GONE
                }
                R.id.is_search_map_by_id -> {
                    searchChoice = 2
                    start_point.hint = resources.getString(R.string.please_input_map_id)
                }
                R.id.is_search_map_by_name -> {
                    searchChoice = 3
                    start_point.hint = resources.getString(R.string.please_input_map_name)
                }
            }
            mapInfo = null
        }
        start_point.setOnCompoundDrawableClickListener sp@{ t, _ ->
            startPointName = t.text?.toString()?.trim() ?: return@sp returnToast2(
                "${when (searchChoice) {
                    0 -> "key point's name"
                    1 -> "starting point's name"
                    2 -> "map id"
                    else -> "map name"
                }} should not be empty", false
            )
            when (searchChoice) {
                0 -> Unit // TODO("not implemented")
                1 -> doGetMainAsync("$SEARCH_BY_START_POINT?name=$startPointName") searchMapsByStartPoint@{
                    val content = it?.content ?: return@searchMapsByStartPoint returnToast3("no response for searching map by start point's name")
                    val jsonObj = SimpleJsonParser.fromJson(String(content), JsonStyle.STANDARD) as? SimpleJsonObject
                        ?: return@searchMapsByStartPoint returnToast3("jsonObj parse error occurred while searching map by start point's name")
                    val flag = "msg" in jsonObj
                    Toast.makeText(
                        ContextApi.appContext, if (flag) {
                            jsonObj["msg"]!!.string()
                        } else {
                            "search map list successfully"
                        }, Toast.LENGTH_LONG
                    ).show()
                    if (!flag) {
                        val iterator = (jsonObj["data"] as? SimpleJsonArray)?.iterator()
                            ?: return@searchMapsByStartPoint returnToast3("get map list error occurred while searching map by start point's name")
                        adapter.clear()
                        while (iterator.hasNext()) {
                            val next = iterator.next() as? SimpleJsonObject ?: continue
                            val loc = ReflectJsonApi.fromJsonOrNull<DeepNaviPoint>(
                                next["loc"] as? SimpleJsonObject ?: continue, DeepNaviPoint::class.java
                            ) ?: continue
                            val map = ReflectJsonApi.fromJsonOrNull<DeepNaviMap>(
                                next["map"] as? SimpleJsonObject ?: continue, DeepNaviMap::class.java
                            ) ?: continue
                            adapter.addData(loc to map)
                        }
                    }
                }
                2 -> doGetMainAsync(
                    "$LOAD_MAP?mapId=${startPointName!!}&includePoint=${include_points.isChecked.toInt()}&includeEdge=${include_edges.isChecked.toInt()}"
                ) searchMapByMapId@{
                    val content = it?.content ?: return@searchMapByMapId returnToast3("no response for searching map by id")
                    val jsonObj = SimpleJsonParser.fromJson(String(content), JsonStyle.STANDARD) as? SimpleJsonObject
                        ?: return@searchMapByMapId returnToast3("jsonObj parse error occurred while searching map by id")
                    val flag = "msg" in jsonObj
                    Toast.makeText(
                        ContextApi.appContext, if (flag) {
                            jsonObj["msg"]!!.string()
                        } else {
                            dismiss()
                            "search map successfully"
                        }, Toast.LENGTH_LONG
                    ).show()
                    if (!flag) {
                        val data = jsonObj["data"] as? SimpleJsonObject
                            ?: return@searchMapByMapId returnToast3("map info cannot be updated while searching map by id")
                        mapInfo = ReflectJsonApi.fromJsonOrNull<DeepNaviMap>(
                            data["map"] as? SimpleJsonObject
                                ?: return@searchMapByMapId returnToast3("json error: no map item in data, while updating map info searched by id"),
                            DeepNaviMap::class.java
                        ) ?: return@searchMapByMapId returnToast3("jsonObj parse error occurred while updating map info searched by id")
                        mapInfoUpdater.update(mapInfo!!)
                        val imagePath = mapInfo!!.planPath.replace("http://127.0.0.1:5000", BASE_URL)
                        doGetMainAsync(imagePath) getBitmap@{ res ->
                            val bytes = res?.content ?: return@getBitmap returnToast3("getBitmap failed after searching map by id: $imagePath")
                            bitmapUpdater.update(BitmapFactory.decodeByteArray(bytes, 0, bytes.size), true)
                        }
                        mapInfo = null
                        if ("points" in data) {
                            val iterator = (data["points"] as? SimpleJsonArray)?.iterator()
                            if (iterator != null && iterator.hasNext()) {
                                val points = mutableListOf<DeepNaviPoint>()
                                while (iterator.hasNext()) {
                                    val next = iterator.next() ?: continue
                                    val point = ReflectJsonApi.fromJsonOrNull<DeepNaviPoint>(next, DeepNaviPoint::class.java) ?: continue
                                    points.add(point)
                                }
                                pointsUpdater.onAllPointsGot(points)
                            }
                        }
                        // if ("edges" in jsonObj || "edge" in jsonObj) {}
                    }
                }
                3 -> Unit // TODO("not implemented")
            }
            true
        }
        end_point.setOnCompoundDrawableClickListener ep@{ t, _ ->
            val mapId = mapInfo?.id ?: return@ep returnToast2("your should input start point's name", true)
            endPointName = t.text?.toString()?.trim() ?: return@ep returnToast2("end point's name should not be empty", false)
            doGetMainAsync("$SEARCH_BY_END_POINT?name=endPointName&mapId=$mapId") { res: HttpResult? ->
                // TODO:
            }
            true
        }

        view.findViewById<Button>(R.id.ok)?.setOnClickListener ok@{
            if (endPointName == null || startPointName == null) {
                return@ok returnToast3("start point's name and end point's name should not be empty")
            }
            // doPostMainAsync(
            //     GET_PATH, null,
            //     ReflectJsonApi.toJsonOrNull(searchByPoints)?.toByteArray()
            //         ?: return@ok returnToast3("error occurred while parse AddEdge object to json")
            // ) getPath@{
            //     val content = it?.content ?: return@getPath
            //     val jsonObj = SimpleJsonParser.fromJson(String(content), JsonStyle.STANDARD) as? SimpleJsonObject ?: return@getPath
            //     Toast.makeText(
            //         ContextApi.appContext, if ("msg" in jsonObj) {
            //             jsonObj["msg"]!!.string()
            //         } else {
            //             // TODO
            //             dismiss()
            //             "Create map successfully"
            //         }, Toast.LENGTH_LONG
            //     ).show()
            // }
        }
        view.findViewById<Button>(R.id.cancel)?.setOnClickListener { dismiss() }
    }

    protected open fun initMapList(): Boolean {
        val selectMapListener = View.OnClickListener {
            val data = it.tag as? Pair<DeepNaviPoint, DeepNaviMap> ?: return@OnClickListener
            if (startPoint === INVALID_DEEPNAVI_POINT) {
                startPoint = data.first
                mapInfo = data.second
                end_point.requestFocus()
                end_point.requestFocusFromTouch()
            } else {
                // TODO()
            }
            adapter.clear()
        }
        val consultMapListener = View.OnClickListener {
            // TODO()
        }
        adapter = BaseRecyclerAdapter(
            this.context ?: return true,
            mutableListOf(), R.layout.item_map_search,
            object : AdapterAction<Pair<DeepNaviPoint, DeepNaviMap>> {
                override fun onBindViewHolder(holder: BaseRecyclerViewHolder, data: Pair<DeepNaviPoint, DeepNaviMap>, position: Int) {
                    holder.itemView.tag = data
                    holder.itemView.setOnClickListener(selectMapListener)
                    // holder.findViewById<TextView>(R.id.map_name).text = String(Base64.decode(data.second.name, Base64.DEFAULT))
                    holder.findViewById<TextView>(R.id.map_name).text = data.second.name
                    val a = data.second.actualSize
                    holder.findViewById<TextView>(R.id.map_actual_size).text = "${a[0]}m x ${a[1]}m x ${a[2]}m"
                    holder.findViewById<TextView>(R.id.map_bitmap).setOnClickListener(consultMapListener)
                }
            }
        )
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            private fun change() {
                this@SearchMapDialogFragment.view?.setBackgroundColor(
                    when (adapter.size) {
                        0 -> android.R.color.transparent
                        else -> android.R.color.white
                    }
                )
            }

            override fun onChanged() = change()
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) = change()
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) = change()
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = change()
            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) = change()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = change()
        })
        search_result.adapter = adapter
        search_result.layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
        return false
    }

    interface GetAllPoints {
        fun onAllPointsGot(points: MutableList<DeepNaviPoint>)
    }
}

// TODO: search map vs / search path -- ok button vs search icon
