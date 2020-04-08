package com.liang.example.map.net

import android.graphics.Bitmap
import com.liang.example.json.JsonStyle
import com.liang.example.json.ReflectJsonApi
import com.liang.example.json.SimpleJsonArray
import com.liang.example.json.SimpleJsonObject
import com.liang.example.json.SimpleJsonParser
import com.liang.example.json.SimpleJsonString
import com.liang.example.json.SimpleJsonValue
import com.liang.example.map.bean.DeepNaviMap
import com.liang.example.map.bean.DeepNaviPoint
import com.liang.example.map.bean.GetPath
import com.liang.example.map.bean.INVALID_STR
import com.liang.example.net.doGetAsync
import com.liang.example.net.doPostAsync
import com.liang.example.net.doPostAsync3
import java.io.ByteArrayOutputStream

// 上传map
// TODO: 删除map
// TODO: 更改map
// 搜索map
// 搜索路径

object MapApi {
    fun bitmapToByteArray(bitmap: Bitmap, recycle: Boolean = true): ByteArray {
        val blob = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, blob)
        if (recycle) {
            bitmap.recycle()
        }
        return blob.toByteArray()
    }

    fun uploadMap(url: String, bitmap: Bitmap, mapInfo: DeepNaviMap, callback: ((msg: String, mapInfo: DeepNaviMap?) -> Unit)) {
        doPostAsync3(
            url, mapOf(
                "name" to mapInfo.name,
                "planSize" to mapInfo.planSize.joinToString(","),
                "planUnit" to mapInfo.planUnit,
                "actualSize" to mapInfo.actualSize.joinToString(","),
                "actualUnit" to mapInfo.actualUnit,
                "originInPlan" to mapInfo.originInPlan.joinToString(","),
                "originInActual" to mapInfo.originInActual.joinToString(","),
                "rotationAngle" to mapInfo.rotationAngle.joinToString(","),
                "isClockWise" to mapInfo.isClockWise.toString(),
                "standardVector" to mapInfo.standardVector,
                "offsetAngle" to mapInfo.offsetAngle.toString()
            ), mapOf("planImage" to ("bitmap${System.currentTimeMillis()}" to bitmapToByteArray(bitmap, false)))
        ) uploadMapRes@{
            val content = it?.content ?: return@uploadMapRes callback.invoke("no response while upload map", null)
            val jsonObj = SimpleJsonParser.fromJson(String(content), JsonStyle.STANDARD) as? SimpleJsonObject
                ?: return@uploadMapRes callback.invoke("jsonObj parse error occurred while upload map", null)
            if ("msg" !in jsonObj) {
                val data = jsonObj["data"] as? SimpleJsonObject
                    ?: return@uploadMapRes callback.invoke("mapInfo cannot be updated", null)
                mapInfo.id = (data["id"] as? SimpleJsonString)?.value() ?: INVALID_STR
                mapInfo.planPath = (data["planPath"] as? SimpleJsonString)?.value() ?: INVALID_STR
                mapInfo.modelPath = (data["modelPath"] as? SimpleJsonString)?.value() ?: INVALID_STR
                callback.invoke("Create map successfully", mapInfo)
            } else {
                callback.invoke((jsonObj["msg"]!! as SimpleJsonString).value2(), null)
            }
        }
    }

    fun parsePoints(iterator: MutableIterator<SimpleJsonValue<*>?>?, mapInfo: DeepNaviMap): MutableList<DeepNaviPoint>? {
        if (iterator != null && iterator.hasNext()) {
            val points = mutableListOf<DeepNaviPoint>()
            while (iterator.hasNext()) {
                val next = iterator.next() ?: continue
                val point = ReflectJsonApi.fromJsonOrNull<DeepNaviPoint>(next, DeepNaviPoint::class.java) ?: continue
                point.modelToWorld(mapInfo)
                points.add(point)
            }
            return points
        }
        return null
    }

    fun searchPath(
        url: String, getPath: GetPath, mapInfo: DeepNaviMap,
        callback: ((msg: String, pathId: String?, points: MutableList<DeepNaviPoint>?) -> Unit)
    ): Boolean {
        doPostAsync(
            url, null, ReflectJsonApi.toJsonOrNull(getPath)?.toByteArray() ?: return true
        ) getPath@{
            val content = it?.content ?: return@getPath callback.invoke("no response while searching path", null, null)
            val jsonObj = SimpleJsonParser.fromJson(String(content), JsonStyle.STANDARD) as? SimpleJsonObject
                ?: return@getPath callback.invoke("parse json failed while searching path", null, null)
            val text = if ("msg" in jsonObj) {
                callback.invoke((jsonObj["msg"]!! as SimpleJsonString).value2(), null, null)
            } else {
                val data = jsonObj["data"] as? SimpleJsonObject
                    ?: return@getPath callback.invoke("no data while searching path", null, null)
                val pathId = ((data["pathId"]) as? SimpleJsonString)?.value()
                    ?: return@getPath callback.invoke("no pathId while searching path", null, null)
                callback.invoke("Get path successfully", pathId, parsePoints((data["path"] as? SimpleJsonArray)?.iterator(), mapInfo))
            }
        }
        return false
    }

    fun searchMapsByPoint(url: String, callback: (msg: String, dataList: MutableList<Pair<DeepNaviPoint, DeepNaviMap>>?) -> Unit) {
        doGetAsync(url) searchMapsByPointRes@{
            val content = it?.content ?: return@searchMapsByPointRes callback.invoke("no response for searching map", null)
            val jsonObj = SimpleJsonParser.fromJson(String(content), JsonStyle.STANDARD) as? SimpleJsonObject
                ?: return@searchMapsByPointRes callback.invoke("jsonObj parse error occurred while searching map", null)
            if ("msg" !in jsonObj) {
                val result = mutableListOf<Pair<DeepNaviPoint, DeepNaviMap>>()
                val iterator = (jsonObj["data"] as? SimpleJsonArray)?.iterator()
                    ?: return@searchMapsByPointRes callback.invoke("get map list error occurred while searching map by start point's name", null)
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
                callback("search map list successfully", result)
            } else {
                callback((jsonObj["msg"]!! as SimpleJsonString).value2(), null)
            }
        }
    }

    fun searchMapByMapId(
        url: String, mapId: String, includePoints: Int, includeEdges: Int,
        callback: (msg: String, mapInfo: DeepNaviMap?, points: MutableList<DeepNaviPoint>?) -> Unit
    ) {
        doGetAsync("$url?mapId=${mapId}&includePoint=${includePoints}&includeEdge=${includeEdges}") searchMapByMapIdRes@{
            val content = it?.content ?: return@searchMapByMapIdRes callback("no response for searching map by id", null, null)
            val jsonObj = SimpleJsonParser.fromJson(String(content), JsonStyle.STANDARD) as? SimpleJsonObject
                ?: return@searchMapByMapIdRes callback("jsonObj parse error occurred while searching map by id", null, null)
            if ("msg" in jsonObj) {
                callback((jsonObj["msg"]!! as SimpleJsonString).value2(), null, null)
            } else {
                val data = jsonObj["data"] as? SimpleJsonObject
                    ?: return@searchMapByMapIdRes callback("map info cannot be updated while searching map by id", null, null)
                val mapInfo = ReflectJsonApi.fromJsonOrNull<DeepNaviMap>(
                    data["map"] as? SimpleJsonObject
                        ?: return@searchMapByMapIdRes callback("json error: no map item in data, while updating map info searched by id", null, null),
                    DeepNaviMap::class.java
                ) ?: return@searchMapByMapIdRes callback("jsonObj parse error occurred while updating map info searched by id", null, null)
                callback("search map successfully", mapInfo, parsePoints((data["points"] as? SimpleJsonArray)?.iterator(), mapInfo))
            }
        }
    }
}
