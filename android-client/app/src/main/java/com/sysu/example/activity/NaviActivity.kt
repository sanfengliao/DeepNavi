package com.sysu.example.activity

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.liang.example.map.bean.DeepNaviEdge
import com.liang.example.map.bean.DeepNaviMap
import com.liang.example.map.bean.DeepNaviPoint
import com.liang.example.map.bean.quaternionToEuler
import com.liang.example.map.map_view.LineEdge
import com.liang.example.map.map_view.MapContainer
import com.liang.example.map.map_view.Marker
import com.sysu.deepnavi.DeepNaviManager
import com.sysu.example.R
import com.sysu.example.activity.MapTestData.angleSet
import com.sysu.example.activity.MapTestData.posSet
import com.sysu.example.fragment.SearchMapDialogFragment
import com.sysu.example.fragment.UploadMapDialogFragment
import com.sysu.example.utils.returnToast3
import com.sysu.example.utils.toFloatList
import kotlinx.android.synthetic.main.activity_navi.bottom_sheet
import kotlinx.android.synthetic.main.activity_navi.cancel_navigation
import kotlinx.android.synthetic.main.activity_navi.map_container
import kotlinx.android.synthetic.main.activity_navi.search
import kotlinx.android.synthetic.main.activity_navi.start_navigation
import kotlinx.android.synthetic.main.activity_navi.stop_navigation
import kotlinx.android.synthetic.main.bottom_sheet_point_info.as_end_point
import kotlinx.android.synthetic.main.bottom_sheet_point_info.as_start_point
import kotlinx.android.synthetic.main.bottom_sheet_point_info.point_actual_location
import kotlinx.android.synthetic.main.bottom_sheet_point_info.point_id
import kotlinx.android.synthetic.main.bottom_sheet_point_info.point_map_id
import kotlinx.android.synthetic.main.bottom_sheet_point_info.point_name
import kotlinx.android.synthetic.main.bottom_sheet_point_info.point_plan_location

class NaviActivity : AppCompatActivity(), MapContainer.OnMarkerClickListener, UploadMapDialogFragment.UpdateMapBitmap, UploadMapDialogFragment.UpdateMapInfo,
    SearchMapDialogFragment.GetAllPoints {
    private var bitmap: Bitmap? = null
    private var mapInfo: DeepNaviMap? = null
        set(value) {
            field = value
            allPoints = null
            allEdges = null
            map_container.mMarkers = null
            map_container.mEdges = null
            map_container.pos(-1f, -1f)
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
                if (deepNaviManager.pathId == null) {
                    value.forEachIndexed { i1, p1 ->
                        val p1Id = p1.id ?: return@forEachIndexed
                        p1.adjacence.forEach { p2Id ->
                            val i2 = value.indexOfFirst { it.id == p2Id }
                            if (i2 > i1) {
                                edges.add(DeepNaviEdge(p1.mapId, p1Id, p2Id))
                            }
                        }
                    }
                } else {
                    var p1Id = value.first().id ?: return
                    for (index in 1 until value.size) {
                        val p2Id = value[index].id ?: continue
                        edges.add(DeepNaviEdge(mapInfo.id!!, p1Id, p2Id))
                        p1Id = p2Id
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

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<RelativeLayout>

    private lateinit var deepNaviManager: DeepNaviManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navi)

        initMapAndInfo()
        initNavigation()

        // test()
    }

    private fun initMapAndInfo() {
        map_container.onMarkerClickListener = this
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
    }

    private fun initNavigation() {
        deepNaviManager = TrainActivity.configByWS(this, null, null) {
            if (it.flag == 1) {
                return@configByWS returnToast3("navigation finished")
            }
            val mapInfo = this.mapInfo ?: return@configByWS
            val coordinate = mapInfo.modelToWorld(listOf(it.coor.x, it.coor.y, it.coor.z), true)
            map_container.pos(coordinate[0] / mapInfo.actualSize[0], coordinate[1] / mapInfo.actualSize[1])
            map_container.rotate(
                if (mapInfo.isClockWise) {
                    it.rotation - mapInfo.offsetAngle
                } else {
                    mapInfo.offsetAngle - it.rotation
                }
            )
        }
        search.setOnClickListener {
            SearchMapDialogFragment(this, this, this).show(supportFragmentManager, "searchMap")
        }
        cancel_navigation.setOnClickListener {
            deepNaviManager.pathId = null
            DeepNaviManager.stop()
            this.allPoints = null
        }
        val configDataSet = ConfigProperty.SIGNAL_CONFIG_SET.value?.split(',')?.toSet() ?: emptySet()
        stop_navigation.setOnClickListener { DeepNaviManager.stop() }
        start_navigation.setOnClickListener { DeepNaviManager.start(ConfigProperty.getRates(), configDataSet) }
    }

    private fun test() {
        bitmap = ((resources.getDrawable(R.drawable.map_example)) as? BitmapDrawable)?.bitmap
        map_container.mMapView.setImageBitmap(bitmap)
        mapInfo = DeepNaviMap().apply {
            id = "5e6e410ff143bf7cc1ca344a"
            planPath = "http://127.0.0.1:5000/static/860bfe6266cc11eaa125185e0ff2d49d"
            name = "超算5楼"
            planUnit = "px"
            planSize = floatArrayOf(577f, 370f, 0f)
            actualUnit = "m"
            actualSize = floatArrayOf(70f, 40f, 0f)
            originInPlan = floatArrayOf(17.31f, 362.6f, 0f)
            originInActual = floatArrayOf(1.4f, 39.2f, 0f)
            rotationAngle = floatArrayOf(180f, 0f, 90f)
            standardVector = "0,1"
        }
        // val first = mapInfo!!.modelToWorld(posSet[40].toFloatList(), true)
        allPoints = mutableListOf(
            // mapInfo!!.actualPoint(first[0], first[1]).apply {
            //     this.id = "0"
            //     this.adjacence = arrayOf("1")
            //     this.name = "a"
            // },
            mapInfo!!.percentagePoint(0.98f, 0.98f).apply {
                this.id = "1"
                this.adjacence = arrayOf("0", "2")
                this.name = "b"
            },
            mapInfo!!.percentagePoint(0.98f, 0.02f).apply {
                this.id = "2"
                this.adjacence = arrayOf("1", "3")
                this.name = "c"
            },
            mapInfo!!.percentagePoint(0.02f, 0.02f).apply {
                this.id = "3"
                this.adjacence = arrayOf("2")
                this.name = "d"
            }
        )
        var index = 40
        var task: Runnable? = null
        task = Runnable {
            if (index < 90 /*posSet.size*/) {
                val data = mapInfo?.modelToWorld(posSet[index].toFloatList(), true) ?: return@Runnable
                map_container.pos(data[0] / mapInfo!!.actualSize[0], data[1] / mapInfo!!.actualSize[1])
                map_container.rotate(-quaternionToEuler(angleSet[index].toFloatList())[2])
                index++
                if (task != null) {
                    map_container.postDelayed(task, 200)
                }
            }
        }
        map_container.postDelayed(task, 200)
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

    override fun update(bitmap: Bitmap, load: Boolean) {
        this.bitmap = bitmap
        map_container.mMapView.setImageBitmap(bitmap)
        this.mapInfo = null
    }

    override fun update(mapInfo: DeepNaviMap) {
        this.mapInfo = mapInfo
    }

    override fun onAllPointsGot(points: MutableList<DeepNaviPoint>, pathId: String?) {
        deepNaviManager.pathId = pathId
        this.allPoints = points
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        DeepNaviManager.onRequestPermissionsResult(requestCode, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}

// TODO: 每个地图模型应该对应一份配置参数，用于训练和导航，同时可以修改，不过需要重新训练
