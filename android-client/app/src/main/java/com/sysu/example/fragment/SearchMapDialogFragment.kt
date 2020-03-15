package com.sysu.example.fragment

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.liang.example.json_ktx.JsonStyle
import com.liang.example.json_ktx.ReflectJsonApi
import com.liang.example.json_ktx.SimpleJsonArray
import com.liang.example.json_ktx.SimpleJsonObject
import com.liang.example.json_ktx.SimpleJsonParser
import com.liang.example.json_ktx.SimpleJsonString
import com.liang.example.json_ktx.SimpleJsonValue
import com.sysu.example.App
import com.sysu.example.BaseRecyclerAdapter
import com.sysu.example.BaseRecyclerViewHolder
import com.sysu.example.KeyUrls
import com.sysu.example.KeyUrls.GET_PATH
import com.sysu.example.R
import com.sysu.example.bean.DeepNaviMap
import com.sysu.example.bean.DeepNaviPoint
import com.sysu.example.bean.GetPath
import com.sysu.example.bean.INVALID_DEEPNAVI_POINT
import com.sysu.example.utils.ContextApi
import com.sysu.example.utils.doGetMainAsync
import com.sysu.example.utils.doPostMainAsync
import com.sysu.example.utils.returnToast2
import com.sysu.example.utils.returnToast3
import com.sysu.example.utils.setOnCompoundDrawableClickListener
import com.sysu.example.utils.toInt
import kotlinx.android.synthetic.main.fragment_search.end_point
import kotlinx.android.synthetic.main.fragment_search.include_edges
import kotlinx.android.synthetic.main.fragment_search.include_points
import kotlinx.android.synthetic.main.fragment_search.search_choice
import kotlinx.android.synthetic.main.fragment_search.search_result
import kotlinx.android.synthetic.main.fragment_search.start_point

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

        searchChoiceChangedListener()
        start_point.setOnCompoundDrawableClickListener sp@{ t, _ ->
            adapter.clear()
            startPoint = INVALID_DEEPNAVI_POINT
            startPointName = t.text?.toString()?.trim() ?: return@sp returnToast2(
                "${when (searchChoice) {
                    0 -> "key point's name"
                    1 -> "starting point's name"
                    2 -> "map id"
                    else -> "map name"
                }} should not be empty", false
            )
            when (searchChoice) {
                0, 1 -> {
                    adapter.clear()
                    searchMapsByPoint("${KeyUrls.SEARCH_BY_START_POINT}?name=${startPointName!!}") { adapter.setDataSet(it) }
                }
                2 -> searchMapByMapId(startPointName!!, include_points.isChecked.toInt(), include_edges.isChecked.toInt(), pointsUpdater, mapInfoUpdater)
                3 -> Unit // TODO("not implemented")
            }
            true
        }
        end_point.setOnCompoundDrawableClickListener ep@{ t, _ ->
            adapter.clear()
            endPoint = INVALID_DEEPNAVI_POINT
            val mapId = mapInfo?.id ?: return@ep returnToast2("your should input start point's name", true)
            endPointName = t.text?.toString()?.trim() ?: return@ep returnToast2("end point's name should not be empty", false)
            searchMapsByPoint("${KeyUrls.SEARCH_BY_END_POINT}?name=$endPointName&mapId=$mapId") searchMapsByPointRes@{
                adapter.setDataSet(it)
            }
            true
        }

        view.findViewById<Button>(R.id.ok)?.setOnClickListener ok@{
            if (endPointName == null || startPointName == null) {
                return@ok returnToast3("start point's name and end point's name should not be empty")
            }
            val getPath = GetPath()
            getPath.mapId = startPoint.mapId
            getPath.src.actualCoordinate = startPoint.actualCoordinate
            getPath.dst.actualCoordinate = endPoint.actualCoordinate
            if (searchPath(getPath)) return@ok returnToast3("error occurred while parse GetPath object to json")
        }
        view.findViewById<Button>(R.id.cancel)?.setOnClickListener { dismiss() }
    }

    protected open fun searchPath(getPath: GetPath): Boolean {
        doPostMainAsync(
            GET_PATH, null,
            ReflectJsonApi.toJsonOrNull(getPath)?.toByteArray()
                ?: return true
        ) getPath@{
            val content = it?.content ?: return@getPath returnToast3("no response")
            val jsonObj = SimpleJsonParser.fromJson(String(content), JsonStyle.STANDARD) as? SimpleJsonObject
                ?: return@getPath returnToast3("parse json failed")
            val text = if ("msg" in jsonObj) {
                jsonObj["msg"]!!.string()
            } else {
                val data = jsonObj["data"] as? SimpleJsonObject ?: return@getPath returnToast3("no data")
                val pathId = ((data["pathId"]) as? SimpleJsonString)?.value() ?: return@getPath returnToast3("no pathId")
                updatePoints((data["path"] as? SimpleJsonArray)?.iterator(), pathId)
                dismiss()
                "Get path successfully"
            }
            Toast.makeText(ContextApi.appContext, text, Toast.LENGTH_LONG).show()
        }
        return false
    }

    protected open fun updatePoints(iterator: MutableIterator<SimpleJsonValue<*>?>?, pathId: String?) {
        if (iterator != null && iterator.hasNext()) {
            val points = mutableListOf<DeepNaviPoint>()
            while (iterator.hasNext()) {
                val next = iterator.next() ?: continue
                val point = ReflectJsonApi.fromJsonOrNull<DeepNaviPoint>(next, DeepNaviPoint::class.java) ?: continue
                point.modelToWorld(mapInfo!!)
                points.add(point)
            }
            pointsUpdater.onAllPointsGot(points, pathId)
        }
    }

    protected open fun searchChoiceChangedListener() {
        search_choice.setOnCheckedChangeListener { _, checkedId ->
            end_point.visibility = View.GONE
            include_points.visibility = View.VISIBLE
            include_edges.visibility = View.VISIBLE
            mapInfo = null
            adapter.clear()
            startPoint = INVALID_DEEPNAVI_POINT
            endPoint = INVALID_DEEPNAVI_POINT
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
        }
    }

    protected open fun searchMapsByPoint(url: String, callback: (MutableList<Pair<DeepNaviPoint, DeepNaviMap>>) -> Unit) {
        doGetMainAsync(url) searchMapsByPointRes@{
            val content = it?.content ?: return@searchMapsByPointRes returnToast3("no response for searching map")
            val jsonObj = SimpleJsonParser.fromJson(String(content), JsonStyle.STANDARD) as? SimpleJsonObject
                ?: return@searchMapsByPointRes returnToast3("jsonObj parse error occurred while searching map")
            val flag = "msg" in jsonObj
            val text = when {
                flag -> jsonObj["msg"]!!.string()
                else -> "search map list successfully"
            }
            Toast.makeText(ContextApi.appContext, text, Toast.LENGTH_LONG).show()
            val result = mutableListOf<Pair<DeepNaviPoint, DeepNaviMap>>()
            if (!flag) {
                val iterator = (jsonObj["data"] as? SimpleJsonArray)?.iterator()
                    ?: return@searchMapsByPointRes returnToast3("get map list error occurred while searching map by start point's name")
                while (iterator.hasNext()) {
                    val next = iterator.next() as? SimpleJsonObject ?: continue
                    val loc = ReflectJsonApi.fromJsonOrNull<DeepNaviPoint>(
                        next["loc"] as? SimpleJsonObject ?: continue, DeepNaviPoint::class.java
                    ) ?: continue
                    val map = ReflectJsonApi.fromJsonOrNull<DeepNaviMap>(
                        next["map"] as? SimpleJsonObject ?: continue, DeepNaviMap::class.java
                    ) ?: continue
                    result.add(loc to map)
                }
            }
            callback(result)
        }
    }

    protected open fun searchMapByMapId(
        mapId: String, includePoints: Int, includeEdges: Int,
        pointsUpdater: GetAllPoints, mapInfoUpdater: UploadMapDialogFragment.UpdateMapInfo
    ) {
        doGetMainAsync(
            "${KeyUrls.LOAD_MAP}?mapId=${mapId}&includePoint=${includePoints}&includeEdge=${includeEdges}"
        ) searchMapByMapIdRes@{
            val content = it?.content ?: return@searchMapByMapIdRes returnToast3("no response for searching map by id")
            val jsonObj = SimpleJsonParser.fromJson(String(content), JsonStyle.STANDARD) as? SimpleJsonObject
                ?: return@searchMapByMapIdRes returnToast3("jsonObj parse error occurred while searching map by id")
            val flag = "msg" in jsonObj
            val text = if (flag) {
                jsonObj["msg"]!!.string()
            } else {
                dismiss()
                "search map successfully"
            }
            Toast.makeText(ContextApi.appContext, text, Toast.LENGTH_LONG).show()
            if (!flag) {
                val data = jsonObj["data"] as? SimpleJsonObject
                    ?: return@searchMapByMapIdRes returnToast3("map info cannot be updated while searching map by id")
                val mapInfo = ReflectJsonApi.fromJsonOrNull<DeepNaviMap>(
                    data["map"] as? SimpleJsonObject
                        ?: return@searchMapByMapIdRes returnToast3("json error: no map item in data, while updating map info searched by id"),
                    DeepNaviMap::class.java
                ) ?: return@searchMapByMapIdRes returnToast3("jsonObj parse error occurred while updating map info searched by id")
                updateMap(mapInfo, mapInfoUpdater)
                updatePoints((data["points"] as? SimpleJsonArray)?.iterator(), null)
                // if ("edges" in jsonObj || "edge" in jsonObj) {}
            }
        }
    }

    protected open fun updateMap(mapInfo: DeepNaviMap, mapInfoUpdater: UploadMapDialogFragment.UpdateMapInfo) {
        mapInfoUpdater.update(mapInfo)
        val imagePath = mapInfo.planPath?.replace("http://127.0.0.1:5000", App.BASE_URL) ?: return returnToast3("no planPath")
        doGetMainAsync(imagePath) getBitmap@{ res ->
            val bytes = res?.content ?: return@getBitmap returnToast3("getBitmap failed after searching map by id: $imagePath")
            bitmapUpdater.update(BitmapFactory.decodeByteArray(bytes, 0, bytes.size), true)
        }
    }

    protected open fun initMapList(): Boolean {
        val selectMapListener = View.OnClickListener {
            val data = it.tag as? Pair<DeepNaviPoint, DeepNaviMap> ?: return@OnClickListener
            if (startPoint === INVALID_DEEPNAVI_POINT) {
                startPoint = data.first
                mapInfo = data.second
                updateMap(data.second, mapInfoUpdater)
                if (searchChoice == 0) {
                    dismiss()
                } else {
                    end_point.requestFocus()
                    end_point.requestFocusFromTouch()
                    adapter.clear()
                }
            } else {
                endPoint = data.first
                adapter.clear()
            }
        }
        val consultMapListener = View.OnClickListener {
            // TODO()
        }
        adapter = BaseRecyclerAdapter(
            this.context ?: return true,
            mutableListOf(), R.layout.item_map_search,
            object : BaseRecyclerAdapter.AdapterAction<Pair<DeepNaviPoint, DeepNaviMap>> {
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
        // adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
        //     private fun change() {
        //         search_map_root.setBackgroundColor(
        //             when (adapter.size) {
        //                 0 -> android.R.color.transparent
        //                 else -> android.R.color.white
        //             }
        //         )
        //     }
        //
        //     override fun onChanged() = change()
        //     override fun onItemRangeChanged(positionStart: Int, itemCount: Int) = change()
        //     override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) = change()
        //     override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = change()
        //     override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) = change()
        //     override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = change()
        // })
        search_result.adapter = adapter
        search_result.layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
        return false
    }

    interface GetAllPoints {
        fun onAllPointsGot(points: MutableList<DeepNaviPoint>, pathId: String? = null)
    }
}

// TODO: search map vs / search path -- ok button vs search icon
