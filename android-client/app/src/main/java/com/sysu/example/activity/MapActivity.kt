package com.sysu.example.activity

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Size
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.liang.example.json_ktx.ReflectJsonApi
import com.liang.example.json_ktx.SimpleJsonArray
import com.liang.example.json_ktx.SimpleJsonObject
import com.liang.example.json_ktx.SimpleJsonParser
import com.liang.example.json_ktx.SimpleJsonString
import com.sysu.deepnavi.DeepNaviManager
import com.sysu.deepnavi.impl.AudioListener2
import com.sysu.deepnavi.impl.SensorListeners
import com.sysu.deepnavi.impl.WifiListener
import com.sysu.deepnavi.inter.DataCollectorInter
import com.sysu.example.KeyUrls.GET_POINTS
import com.sysu.example.R
import com.sysu.example.fragment.AddEdgeDialogFragment
import com.sysu.example.fragment.AddPointDialogFragment
import com.sysu.example.bean.DeepNaviCoordinator
import com.sysu.example.bean.DeepNaviEdge
import com.sysu.example.bean.DeepNaviMap
import com.sysu.example.bean.DeepNaviPoint
import com.sysu.example.bean.INVALID_STR
import com.sysu.example.fragment.SearchMapDialogFragment
import com.sysu.example.fragment.UploadMapDialogFragment
import com.sysu.example.map_view.LineEdge
import com.sysu.example.map_view.MapContainer
import com.sysu.example.map_view.Marker
import com.sysu.example.utils.add
import com.sysu.example.utils.doGetMainAsync
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
import kotlin.math.cos
import kotlin.math.sin

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
    private var audioListener2: AudioListener2? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        initMapConfigBar()
        initMapAndInfo()
        initNavigation()

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
            if (mapInfo == null || mapInfo!!.id == null) {
                return@setOnClickListener returnToast3("map info is null or map id is null, your should create a map or load a map")
            }
            if (allPoints == null) {
                doGetMainAsync("$GET_POINTS?mapId=${mapInfo!!.id}") getAllPoints@{
                    val jsonObj = SimpleJsonParser.fromJsonOrNull(
                        String(it?.content ?: return@getAllPoints returnToast3("no response for getting all points"))
                    ) as? SimpleJsonObject ?: return@getAllPoints returnToast3("getting all points failed while parse json")
                    val iterator = (jsonObj["data"] as? SimpleJsonArray)?.iterator()
                        ?: return@getAllPoints returnToast3("getting all points failed: ${(jsonObj["msg"] as? SimpleJsonString)?.value() ?: "unknown reason"}")
                    val points = mutableListOf<DeepNaviPoint>()
                    while (iterator.hasNext()) {
                        val next = iterator.next() ?: continue
                        val point = ReflectJsonApi.fromJsonOrNull<DeepNaviPoint>(next, DeepNaviPoint::class.java) ?: continue
                        point.modelToWorld(mapInfo!!)
                        points.add(point)
                    }
                    allPoints = points
                    AddEdgeDialogFragment(mapInfo!!.id!!, points, this).show(supportFragmentManager, "addEdge")
                }
                Toast.makeText(this, "please wait for points' loading", Toast.LENGTH_LONG).show()
            } else {
                AddEdgeDialogFragment(mapInfo!!.id!!, allPoints!!, this).show(supportFragmentManager, "addEdge")
            }
        }
    }

    private fun initNavigation() {
        deepNaviManager = MainActivity.initDeepNaviManagerByWS(this) {
            if (it.flag == 1) {
                return@initDeepNaviManagerByWS returnToast3("navigation finished")
            }
            val coordinate = rotateCoordinate(
                mapInfo!!.rotationAngle[0], mapInfo!!.rotationAngle[1], mapInfo!!.rotationAngle[2],
                listOf(it.coor.x, it.coor.y, it.coor.z)
            )
            map_container.pos(coordinate[0] / mapInfo!!.actualSize[0], coordinate[1] / mapInfo!!.actualSize[1])
            map_container.rotate(it.rotation)
        }
        val configDataSet = ConfigProperty.SIGNAL_CONFIG_SET.value?.split(',')?.toSet() ?: emptySet()
        if ("image" in configDataSet) {
            val imageSize = ConfigProperty.DEEPNAVI_IMAGE_SIZE.value ?: ConfigProperty.DEFAULT_PICTURE_SIZE
            audioListener2 = AudioListener2(this, null, pictureSize = Size(imageSize.width, imageSize.height))
            deepNaviManager.addDataCollector(audioListener2 as DataCollectorInter<Any>)
        }
        if ("wifiList" in configDataSet) {
            deepNaviManager.addDataCollector(WifiListener(this) as DataCollectorInter<Any>)
        }
        SensorListeners.initAll(configDataSet)

        cancel_navigation.setOnClickListener {
            navigation_bar.isVisible = false
            config_map_bar.isVisible = true
            deepNaviManager.pathId = null
            this.allPoints = null
        }
        stop_navigation.setOnClickListener {
            audioListener2?.cameraUtil?.stopPreview()
            deepNaviManager.stop()
            SensorListeners.unregisterAll()
        }
        start_navigation.setOnClickListener {
            if (audioListener2 != null) {
                if (!audioListener2!!.haveOpenCamera) {
                    audioListener2!!.cameraUtil.openCamera()
                }
                audioListener2!!.cameraUtil.startPreview()
            }
            SensorListeners.registerAll(ConfigProperty.getRates(), configDataSet)
            deepNaviManager.loop()
        }
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

    companion object {
        /**
         * import numpy as np
         * def coordinateRotation(roll: float, pitch: float, yaw: float, coor: np.matrix) -> np.matrix:
         *     roll = np.pi * roll / 180
         *     pitch = np.pi * pitch / 180
         *     yaw = np.pi * yaw / 180
         *     return np.matrix([
         *         [np.cos(yaw) * np.cos(pitch), np.sin(yaw) * np.sin(pitch) * np.sin(roll) - np.sin(yaw) * np.cos(roll), np.sin(yaw) * np.sin(roll) + np.cos(yaw) * np.cos(roll) * np.sin(pitch)],
         *         [np.cos(pitch) * np.sin(yaw), np.cos(yaw) * np.cos(roll) + np.sin(yaw) * np.sin(roll) * np.sin(pitch), np.cos(roll) * np.sin(yaw) * np.sin(pitch) - np.cos(yaw) * np.sin(roll)],
         *         [-np.sin(pitch), np.cos(pitch) * np.sin(roll), np.cos(roll) * np.cos(pitch)]
         *     ]) * coor
         *
         * @param roll 坐标系绕x轴旋转的角度
         * @param pitch 坐标系绕y轴旋转的角度
         * @param yaw 坐标系绕z轴旋转的角度
         * @param coordinate 坐标(必须是x,y,z这样的)
         */
        fun rotateCoordinate(roll: Float, pitch: Float, yaw: Float, coordinate: List<Float>): List<Float> {
            val r = roll * Math.PI.toFloat() / 180
            val p = pitch * Math.PI.toFloat() / 180
            val y = yaw * Math.PI.toFloat() / 180
            val cr = cos(r)
            val sr = sin(r)
            val cp = cos(p)
            val sp = sin(p)
            val cy = cos(y)
            val sy = sin(y)
            val temp = matrixMultipleF(
                listOf(
                    listOf(cy * cp, sy * sp * sr - sy * cr, sy * sr + cy * cr * sp),
                    listOf(cp * sy, cy * cr + sy * sr * sp, cr * sy * sp - cy * sr),
                    listOf(-sp, cp * sr, cr * cp)
                ),
                listOf(listOf(coordinate[0]), listOf(coordinate[1]), listOf(coordinate[2]))
            )!!
            return listOf(temp[0][0], temp[1][0], temp[2][0])
        }

        /**
         * 简单的矩阵乘法
         */
        fun matrixMultipleF(m1: List<List<Float>>, m2: List<List<Float>>): List<List<Float>>? =
            if (m1.isNotEmpty() && m2.isNotEmpty() && m1[0].size == m2.size) {
                val len3 = m2[0].size
                m1.map { list1 ->
                    (0 until len3).map { list2Index ->
                        list1.mapIndexed { index, item -> item * m2[index][list2Index] }.reduce { acc, i -> acc + i }
                    }
                }
            } else {
                null
            }
    }
}

// TODO: 图片格式
// TODO: fragment scrollView与软键盘的冲突
// TODO: 坐标系转换

// [三维空间坐标系变换-旋转矩阵](https://blog.csdn.net/fireflychh/article/details/82352710?depth_1-utm_source=distribute.pc_relevant.none-task&utm_source=distribute.pc_relevant.none-task)
// [如何通俗地解释欧拉角？之后为何要引入四元数？](https://www.zhihu.com/question/47736315)
// [旋转矩阵*百度百科](https://baike.baidu.com/item/%E6%97%8B%E8%BD%AC%E7%9F%A9%E9%98%B5/3265181?fr=aladdin)
// [【Unity编程】欧拉角与万向节死锁（图文版）](https://blog.csdn.net/AndrewFan/article/details/60981437#)
