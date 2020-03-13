package com.sysu.example.activity

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.liang.example.json_ktx.ReflectJsonApi
import com.liang.example.json_ktx.SimpleJsonArray
import com.liang.example.json_ktx.SimpleJsonObject
import com.liang.example.json_ktx.SimpleJsonParser
import com.liang.example.json_ktx.SimpleJsonString
import com.sysu.example.KeyUrls.GET_POINTS
import com.sysu.example.R
import com.sysu.example.fragment.AddEdgeDialogFragment
import com.sysu.example.fragment.AddPointDialogFragment
import com.sysu.example.fragment.DeepNaviCoordinator
import com.sysu.example.fragment.DeepNaviEdge
import com.sysu.example.fragment.DeepNaviMap
import com.sysu.example.fragment.DeepNaviPoint
import com.sysu.example.fragment.SearchMapDialogFragment
import com.sysu.example.fragment.UploadMapDialogFragment
import com.sysu.example.map.LineEdge
import com.sysu.example.map.MapContainer
import com.sysu.example.map.Marker
import com.sysu.example.utils.add
import com.sysu.example.utils.doGetMainAsync
import com.sysu.example.utils.returnToast3
import kotlinx.android.synthetic.main.activity_map.bottom_sheet
import kotlinx.android.synthetic.main.activity_map.map_container
import kotlinx.android.synthetic.main.activity_map.true_container
import kotlinx.android.synthetic.main.bottom_sheet_point_info.as_end_point
import kotlinx.android.synthetic.main.bottom_sheet_point_info.as_start_point
import kotlinx.android.synthetic.main.bottom_sheet_point_info.point_actual_location
import kotlinx.android.synthetic.main.bottom_sheet_point_info.point_id
import kotlinx.android.synthetic.main.bottom_sheet_point_info.point_map_id
import kotlinx.android.synthetic.main.bottom_sheet_point_info.point_name
import kotlinx.android.synthetic.main.bottom_sheet_point_info.point_plan_location

class MapActivity : AppCompatActivity(), MapContainer.OnMarkerClickListener, UploadMapDialogFragment.UpdateMapBitmap,
    AddPointDialogFragment.UpdateMapPoint, UploadMapDialogFragment.UpdateMapInfo, AddEdgeDialogFragment.UpdateMapEdge, SearchMapDialogFragment.GetAllPoints {
    private var bitmap: Bitmap? = null
    private var mapInfo: DeepNaviMap? = null  // search 之后会有，与mapInfo不能同时存在
        set(value) {
            field = value
            allPoints = null
        }
    private var allPoints: MutableList<DeepNaviPoint>? = null
        set(value) {
            field = value
            val mapInfo = this.mapInfo ?: return
            if (value.isNullOrEmpty()) {
                map_container.mMarkers = null
                allEdges = null
            } else {
                map_container.mMarkers = value.map {
                    Marker(it.planCoordinate.x / mapInfo.planSize[0], it.planCoordinate.y / mapInfo.planSize[1], name = it.id)
                }.toMutableList()
                val edges = mutableListOf<DeepNaviEdge>()
                value.forEachIndexed { i1, p1 ->
                    p1.adjacence.forEach { p2Id ->
                        val i2 = value.indexOfFirst { it.id == p2Id }
                        if (i2 > i1) {
                            val edge = DeepNaviEdge()
                            edge.mapId = p1.mapId
                            edge.startPointId = p1.id
                            edge.endPointId = p2Id
                            edges.add(edge)
                        }
                    }
                }
                allEdges = edges
            }
        }
    private var allEdges: MutableList<DeepNaviEdge>? = null
        set(value) {
            field = value
            if (value == null) {
                map_container.mEdges = null
            } else {
                val markers = map_container.mMarkers ?: return
                map_container.mEdges = value.map { deepNaviEdge ->
                    LineEdge(markers.indexOfFirst { deepNaviEdge.startPointId == it.name }, markers.indexOfFirst { deepNaviEdge.endPointId == it.name })
                }.toMutableList()
            }
        }

    private var marker: Marker? = null
    private var edge: LineEdge? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<RelativeLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        map_container.onMarkerClickListener = this

        findViewById<Button>(R.id.upload).setOnClickListener {
            UploadMapDialogFragment(this, this, this).show(supportFragmentManager, "uploadMap")
        }
        findViewById<Button>(R.id.search).setOnClickListener {
            SearchMapDialogFragment(this, this, this).show(supportFragmentManager, "searchMap")
        }
        findViewById<Button>(R.id.add_point).setOnClickListener {
            if (mapInfo == null) {
                return@setOnClickListener returnToast3("map info is null, your should create a map or load a map")
            }
            AddPointDialogFragment(mapInfo!!, this).show(supportFragmentManager, "addPoint")
        }
        findViewById<Button>(R.id.add_edge).setOnClickListener {
            if (mapInfo == null) {
                return@setOnClickListener returnToast3("map info is null, your should create a map or load a map")
            }
            if (allPoints == null) {
                doGetMainAsync("$GET_POINTS?mapId=${mapInfo!!.id}") getAllPoints@{
                    val jsonObj = SimpleJsonParser.fromJsonOrNull(
                        String(it?.content ?: return@getAllPoints returnToast3("no response for getting all points"))
                    ) as? SimpleJsonObject ?: return@getAllPoints returnToast3("getting all points failed while parse json")
                    val iterator = (jsonObj["data"] as? SimpleJsonArray)?.iterator()
                        ?: return@getAllPoints returnToast3("getting all points failed: ${(jsonObj["msg"] as? SimpleJsonString)?.string() ?: "unknown reason"}")
                    val points = mutableListOf<DeepNaviPoint>()
                    while (iterator.hasNext()) {
                        val next = iterator.next() ?: continue
                        val point = ReflectJsonApi.fromJsonOrNull<DeepNaviPoint>(next, DeepNaviPoint::class.java) ?: continue
                        points.add(point)
                    }
                    allPoints = points
                    AddEdgeDialogFragment(mapInfo!!.id, points, this).show(supportFragmentManager, "addEdge")
                }
                Toast.makeText(this, "please wait for points' loading", Toast.LENGTH_LONG).show()
            } else {
                AddEdgeDialogFragment(mapInfo!!.id, allPoints!!, this).show(supportFragmentManager, "addEdge")
            }
        }

        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet).apply {
            state = BottomSheetBehavior.STATE_COLLAPSED
            // addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            //     override fun onSlide(bottomSheet: View, slideOffset: Float) {
            //         // TODO("Not yet implemented")
            //     }
            //
            //     override fun onStateChanged(bottomSheet: View, newState: Int) {
            //         // TODO("Not yet implemented")
            //     }
            // })
        }
        as_start_point.setOnClickListener {
            val data = it.tag as Pair<DeepNaviPoint, Int>
            // TODO()
        }
        as_end_point.setOnClickListener {
            val data = it.tag as Pair<DeepNaviPoint, Int>
            // TODO()
        }
        val hideBottomSheetListener = View.OnClickListener {
            if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
        bottom_sheet.setOnClickListener(hideBottomSheetListener)

        // bitmap = ((resources.getDrawable(R.drawable.map_example)) as? BitmapDrawable)?.bitmap
        // map_container.mMapView.setImageBitmap(bitmap)
        // map_container.mMarkers = mutableListOf(
        //     Marker(0.02f, 0.02f, R.drawable.point),
        //     Marker(0.98f, 0.02f, R.drawable.point),
        //     Marker(0.98f, 0.98f, R.drawable.point),
        //     Marker(0.02f, 0.98f, R.drawable.point)
        // )
        // map_container.mEdges = mutableListOf(
        //     LineEdge(0, 1),
        //     LineEdge(1, 2),
        //     LineEdge(2, 3),
        //     LineEdge(3, 0)
        // )
        // map_container.pos(0.5f, 0.98f)
        // map_container.rotate(-60f)
    }

    @SuppressLint("SetTextI18n")
    override fun onMarkerClick(view: View?, position: Int) {
        val point = allPoints?.get(position) ?: return
        point_name.text = "${point.name} (index: $position)"
        var t = point.planCoordinate
        point_plan_location.text = "show: ${t.x}px x ${t.y}px x ${t.z}px"
        t = point.actualCoordinate
        point_actual_location.text = "actual: ${t.x}m x ${t.y}m x ${t.z}m"
        point_id.text = "id: ${point.id}"
        point_map_id.text = "id: ${point.mapId}"
        val data = point to position
        as_start_point.tag = data
        as_end_point.tag = data
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        Toast.makeText(this, "你点击了第" + position + "个marker", Toast.LENGTH_SHORT).show()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_COLLAPSED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun update(bitmap: Bitmap, load: Boolean) {
        this.bitmap = bitmap
        map_container.mMapView.setImageBitmap(bitmap)
    }

    override fun update(x: Float, y: Float, z: Float, flag: Int, name: String?) {
        val mapInfo = this.mapInfo ?: return returnToast3("map info is null, so cannot locate percentage point")
        update(
            mapInfo.planSize[0] * x, mapInfo.planSize[1] * y, mapInfo.planSize[2] * z,
            mapInfo.actualSize[0] * x, mapInfo.actualSize[1] * y, mapInfo.actualSize[2] * z,
            flag, name
        )
        /*if (flag == 1 || x < 0f || y < 0f) {
            if (marker != null) {
                map_container.removeMarker(marker!!)
                marker = null
            }
        } else if (flag == 2) {
            if (marker != null) {
                val deepNaviPoint = DeepNaviPoint()
                deepNaviPoint.mapId = mapInfo.id
                deepNaviPoint.planCoordinate = intArrayOf()
                marker!!.data = deepNaviPoint
                marker = null
            }
        } else if (marker == null) {
            marker = if (x < 1f) {
                Marker(x, y, R.drawable.point)
            } else {
                Marker(x / bitmap!!.width, y / bitmap!!.height, R.drawable.point)
            }
            map_container.addMarker(marker!!)
        } else if (x < 1f) {
            marker!!.scaleX = x
            marker!!.scaleY = y
        } else {
            marker!!.scaleX = x / bitmap!!.width
            marker!!.scaleY = y / bitmap!!.height
        }*/
    }

    override fun update(x: Float, y: Float, z: Float, xF: Float, yF: Float, zF: Float, flag: Int, name: String?) {
        val mapInfo = this.mapInfo ?: return returnToast3("map info is null, so cannot locate plan point and actual point")
        if (flag == 1 || x < 0f || y < 0f) {
            if (marker != null) {
                map_container.removeMarker(marker!!)
                marker = null
            }
        } else if (flag == 2) {
            if (marker != null) {
                val data = DeepNaviPoint()
                data.mapId = mapInfo.id
                data.planCoordinate = DeepNaviCoordinator(x, y, z)
                data.actualCoordinate = DeepNaviCoordinator(xF, yF, zF)
                data.id = name ?: marker!!.index.toString()
                marker!!.data = data
                marker = null
            }
        } else if (marker == null) {
            marker = Marker(x / bitmap!!.width, y / bitmap!!.height, R.drawable.point)
            marker!!.name = name
            map_container.addMarker(marker!!)
        } else {
            marker!!.name = name
            marker!!.scaleX = x / bitmap!!.width
            marker!!.scaleY = y / bitmap!!.height
        }
    }

    override fun update(mapInfo: DeepNaviMap) {
        this.mapInfo = mapInfo
    }

    override fun update(pId1: String?, pId2: String?, flag: Int) {
        if (mapInfo == null) {
            return returnToast3("map info is null, so cannot locate edge")
        }
        val markers = map_container.mMarkers ?: return returnToast3("map's points are empty")
        if (flag == 1 || pId1.isNullOrEmpty() || pId2.isNullOrEmpty()) {
            if (edge != null) {
                map_container.removeEdge(edge!!)
                edge = null
            }
        } else if (flag == 2) {
            if (edge != null) {
                val first = allPoints!!.find { it.id == pId1 }!!
                first.adjacence = first.adjacence.add(pId1)
                val second = allPoints!!.find { it.id == pId2 }!!
                second.adjacence = second.adjacence.add(pId2)
                edge = null
            }
        } else if (edge == null) {
            edge = LineEdge(markers.find { it.name == pId1 }?.index ?: return, markers.find { it.name == pId2 }?.index ?: return)
            map_container.addEdge(edge!!)
        } else {
            edge!!.startPointIndex = markers.find { it.name == pId1 }?.index ?: return
            edge!!.endPointIndex = markers.find { it.name == pId2 }?.index ?: return
        }
    }

    override fun onAllPointsGot(points: MutableList<DeepNaviPoint>) {
        this.allPoints = points
    }
}

// TODO: 图片格式
// TODO: fragment scrollView与软键盘的冲突
