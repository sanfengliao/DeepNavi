package com.sysu.example.fragment

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.liang.example.map.bean.DeepNaviMap
import com.liang.example.map.bean.DeepNaviPoint
import com.liang.example.map.bean.GetPath
import com.liang.example.map.bean.INVALID_DEEPNAVI_POINT
import com.liang.example.map.net.MapApi.searchMapByMapId
import com.liang.example.map.net.MapApi.searchMapsByPoint
import com.liang.example.map.net.MapApi.searchPath
import com.sysu.example.App
import com.sysu.example.BaseRecyclerAdapter
import com.sysu.example.BaseRecyclerViewHolder
import com.sysu.example.KeyUrls
import com.sysu.example.KeyUrls.GET_PATH
import com.sysu.example.KeyUrls.LOAD_MAP
import com.sysu.example.R
import com.sysu.example.utils.ContextApi
import com.sysu.example.utils.doGetMainAsync
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
                    searchMapsByPoint("${KeyUrls.SEARCH_BY_START_POINT}?name=${startPointName!!}") { msg, dataList ->
                        if (dataList != null) {
                            adapter.setDataSet(dataList)
                        }
                        ContextApi.toast(msg)
                    }
                }
                2 -> searchMapByMapId(LOAD_MAP, startPointName!!, include_points.isChecked.toInt(), include_edges.isChecked.toInt()) { msg, mapInfo, points ->
                    if (mapInfo != null) {
                        updateMap(mapInfo, mapInfoUpdater)
                    }
                    if (points != null) {
                        pointsUpdater.onAllPointsGot(points)
                    }
                    ContextApi.toast(msg)
                }
                3 -> Unit // TODO("not implemented")
            }
            true
        }
        end_point.setOnCompoundDrawableClickListener ep@{ t, _ ->
            adapter.clear()
            endPoint = INVALID_DEEPNAVI_POINT
            val mapId = mapInfo?.id ?: return@ep returnToast2("your should input start point's name", true)
            endPointName = t.text?.toString()?.trim() ?: return@ep returnToast2("end point's name should not be empty", false)
            searchMapsByPoint("${KeyUrls.SEARCH_BY_END_POINT}?name=$endPointName&mapId=$mapId") searchMapsByPointRes@{ msg, dataList ->
                if (dataList != null) {
                    adapter.setDataSet(dataList)
                }
                ContextApi.toast(msg)
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
            val mapInfo = mapInfo ?: return@ok returnToast3("mapInfo is null, and search path before search map is impossible")
            if (searchPath(GET_PATH, getPath, mapInfo) { msg, pathId, points ->
                    if (pathId != null && points != null) {
                        dismiss()
                        pointsUpdater.onAllPointsGot(points, pathId)
                    }
                    ContextApi.toast(msg)
                }) return@ok returnToast3("error occurred while parse GetPath object to json")
        }
        view.findViewById<Button>(R.id.cancel)?.setOnClickListener { dismiss() }
    }

    protected open fun searchChoiceChangedListener() = search_choice.setOnCheckedChangeListener { _, checkedId ->
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

    protected open fun updateMap(mapInfo: DeepNaviMap, mapInfoUpdater: UploadMapDialogFragment.UpdateMapInfo) {
        mapInfoUpdater.update(mapInfo)
        val imagePath = mapInfo.planPath?.replace("http://127.0.0.1:5000", App.BASE_URL) ?: return returnToast3("no planPath")
        doGetMainAsync(imagePath) getBitmap@{ res ->
            val bytes = res?.content ?: return@getBitmap returnToast3("getBitmap failed after searching map by id: $imagePath")
            bitmapUpdater.update(BitmapFactory.decodeByteArray(bytes, 0, bytes.size), true)
        }
    }

    interface GetAllPoints {
        fun onAllPointsGot(points: MutableList<DeepNaviPoint>, pathId: String? = null)
    }
}

// TODO: search map vs / search path -- ok button vs search icon
