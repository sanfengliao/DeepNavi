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
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.liang.example.map.bean.DeepNaviCoordinator
import com.liang.example.map.bean.DeepNaviEdge
import com.liang.example.map.bean.DeepNaviMap
import com.liang.example.map.bean.DeepNaviPoint
import com.liang.example.map.bean.INVALID_STR
import com.liang.example.map.map_view.LineEdge
import com.liang.example.map.map_view.MapContainer
import com.liang.example.map.map_view.Marker
import com.liang.example.map.net.PointApi.getPoints
import com.sysu.deepnavi.DeepNaviManager
import com.sysu.example.KeyUrls.GET_POINTS
import com.sysu.example.R
import com.sysu.example.fragment.AddEdgeDialogFragment
import com.sysu.example.fragment.AddPointDialogFragment
import com.sysu.example.fragment.SearchMapDialogFragment
import com.sysu.example.fragment.UploadMapDialogFragment
import com.sysu.example.utils.ContextApi
import com.sysu.example.utils.add
import com.sysu.example.utils.returnToast3
import kotlinx.android.synthetic.main.activity_map.bottom_sheet
import kotlinx.android.synthetic.main.activity_map.cancel_navigation
import kotlinx.android.synthetic.main.activity_map.config_map_bar
import kotlinx.android.synthetic.main.activity_map.map_container
import kotlinx.android.synthetic.main.activity_map.navigation_bar
import kotlinx.android.synthetic.main.activity_map.start_navigation
import kotlinx.android.synthetic.main.activity_map.stop_navigation
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

    private var marker: Marker? = null
    private var edge: LineEdge? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<RelativeLayout>

    private lateinit var deepNaviManager: DeepNaviManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        initMapConfigBar()
        initMapAndInfo()
        initNavigation()

        // test()
    }

    private fun test() {
        bitmap = ((resources.getDrawable(R.drawable.map_example)) as? BitmapDrawable)?.bitmap
        map_container.mMapView.setImageBitmap(bitmap)
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
        allPoints = mutableListOf(
            mapInfo!!.percentagePoint(0.02f, 0.98f).apply {
                this.id = "0"
                this.adjacence = arrayOf("1", "3")
                this.name = "a"
            },
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
                this.adjacence = arrayOf("0", "2")
                this.name = "d"
            }
        )
        // var index = 0
        // var task: Runnable? = null
        // task = Runnable {
        //     if (index < posSet.size) {
        //         val data = mapInfo?.modelToWorld(posSet[index].toFloatList(), true) ?: return@Runnable
        //         map_container.pos(data[0] / mapInfo!!.actualSize[0], data[1] / mapInfo!!.actualSize[1])
        //         map_container.rotate(-quaternionToEuler(angleSet[index].toFloatList())[2])
        //         index++
        //         if (task != null) {
        //             map_container.postDelayed(task, 200)
        //         }
        //     }
        // }
        // map_container.postDelayed(task, 200)
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

    private fun initMapConfigBar() {
        findViewById<Button>(R.id.upload).setOnClickListener {
            UploadMapDialogFragment(this, this, this).show(supportFragmentManager, "uploadMap")
        }
        findViewById<Button>(R.id.add_point).setOnClickListener {
            if (mapInfo == null) {
                return@setOnClickListener returnToast3("map info is null, your should create a map or load a map")
            }
            AddPointDialogFragment(mapInfo!!, this).show(supportFragmentManager, "addPoint")
        }
        findViewById<Button>(R.id.add_edge).setOnClickListener {
            if (mapInfo == null || mapInfo!!.id == null) {
                return@setOnClickListener returnToast3("map info is null or map id is null, your should create a map or load a map")
            }
            if (allPoints == null) {
                getPoints(GET_POINTS, mapInfo!!) getAllPoints@{ msg, points ->
                    if (points != null) {
                        ContextApi.handler.post {
                            allPoints = points
                            AddEdgeDialogFragment(mapInfo!!.id!!, points, this).show(supportFragmentManager, "addEdge")
                        }
                    }
                    if (msg != null) {
                        ContextApi.toast(msg)
                    }
                }
                Toast.makeText(this, "please wait for points' loading", Toast.LENGTH_LONG).show()
            } else {
                AddEdgeDialogFragment(mapInfo!!.id!!, allPoints!!, this).show(supportFragmentManager, "addEdge")
            }
        }
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
        cancel_navigation.setOnClickListener {
            navigation_bar.isVisible = false
            config_map_bar.isVisible = true
            deepNaviManager.pathId = null
            DeepNaviManager.stop()
            this.allPoints = null
        }
        val configDataSet = ConfigProperty.SIGNAL_CONFIG_SET.value?.split(',')?.toSet() ?: emptySet()
        stop_navigation.setOnClickListener { DeepNaviManager.stop() }
        start_navigation.setOnClickListener { DeepNaviManager.start(ConfigProperty.getRates(), configDataSet) }
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
        this.mapInfo = null
    }

    override fun update(x: Float, y: Float, z: Float, flag: Int, name: String?, id: String?) {
        val mapInfo = this.mapInfo ?: return returnToast3("map info is null, so cannot locate percentage point")
        /*update(
            mapInfo.planSize[0] * x, mapInfo.planSize[1] * y, mapInfo.planSize[2] * z,
            mapInfo.actualSize[0] * x, mapInfo.actualSize[1] * y, mapInfo.actualSize[2] * z,
            flag, name
        )*/
        if (flag == 1 || x < 0f || y < 0f) {
            if (marker != null) {
                map_container.removeMarker(marker!!)
                marker = null
            }
        } else if (flag == 2) {
            if (marker != null) {
                val data = DeepNaviPoint()
                data.mapId = mapInfo.id ?: return returnToast3("map id is null")
                data.planCoordinate = DeepNaviCoordinator(x * mapInfo.planSize[0], y * mapInfo.planSize[1], z * mapInfo.planSize[2])
                data.actualCoordinate = DeepNaviCoordinator(x * mapInfo.actualSize[0], y * mapInfo.actualSize[1], z * mapInfo.actualSize[2])
                // data.name = name ?: marker!!.index.toString()
                data.id = id ?: INVALID_STR
                if (allPoints == null) {
                    allPoints = mutableListOf(data)
                } else {
                    allPoints!!.add(data)
                }
                marker!!.data = data
                marker!!.name = id
                marker = null
            }
        } else if (marker == null) {
            marker = Marker(x, y, R.drawable.point)
            marker!!.name = name
            map_container.addMarker(marker!!)
        } else if (x < 1f) {
            marker!!.name = name
            marker!!.scaleX = x
            marker!!.scaleY = y
        } else {
            marker!!.name = name
            marker!!.scaleX = x / bitmap!!.width
            marker!!.scaleY = y / bitmap!!.height
        }
    }

    override fun update(x: Float, y: Float, z: Float, xF: Float, yF: Float, zF: Float, flag: Int, name: String?, id: String?) {
        val mapInfo = this.mapInfo ?: return returnToast3("map info is null, so cannot locate plan point and actual point")
        if (flag == 1 || x < 0f || y < 0f) {
            if (marker != null) {
                map_container.removeMarker(marker!!)
                marker = null
            }
        } else if (flag == 2) {
            if (marker != null) {
                val data = DeepNaviPoint()
                data.mapId = mapInfo.id ?: return returnToast3("map id is null")
                data.planCoordinate = DeepNaviCoordinator(x, y, z)
                data.actualCoordinate = DeepNaviCoordinator(xF, yF, zF)
                // data.name = name ?: marker!!.index.toString()
                data.id = id ?: INVALID_STR
                if (allPoints == null) {
                    allPoints = mutableListOf(data)
                } else {
                    allPoints!!.add(data)
                }
                marker!!.data = data
                marker!!.name = id
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

    override fun onAllPointsGot(points: MutableList<DeepNaviPoint>, pathId: String?) {
        deepNaviManager.pathId = pathId
        this.allPoints = points
        if (pathId != null) {
            config_map_bar.isVisible = false
            navigation_bar.isVisible = true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        DeepNaviManager.onRequestPermissionsResult(requestCode, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}

// TODO: 图片格式
// TODO: fragment scrollView与软键盘的冲突
// TODO: 坐标系转换

// [三维空间坐标系变换-旋转矩阵](https://blog.csdn.net/fireflychh/article/details/82352710?depth_1-utm_source=distribute.pc_relevant.none-task&utm_source=distribute.pc_relevant.none-task)
// [如何通俗地解释欧拉角？之后为何要引入四元数？](https://www.zhihu.com/question/47736315)
// [旋转矩阵*百度百科](https://baike.baidu.com/item/%E6%97%8B%E8%BD%AC%E7%9F%A9%E9%98%B5/3265181?fr=aladdin)
// [【Unity编程】欧拉角与万向节死锁（图文版）](https://blog.csdn.net/AndrewFan/article/details/60981437#)
