package com.liang.example.map.net

import com.liang.example.json.JsonStyle
import com.liang.example.json.ReflectJsonApi
import com.liang.example.json.SimpleJsonArray
import com.liang.example.json.SimpleJsonObject
import com.liang.example.json.SimpleJsonParser
import com.liang.example.json.SimpleJsonString
import com.liang.example.map.bean.DeepNaviMap
import com.liang.example.map.bean.DeepNaviPoint
import com.liang.example.net.doGetAsync
import com.liang.example.net.doPostAsync

// 增加point
// TODO: 删除point
// TODO: 更改point
// 搜索point

object PointApi {
    fun addPoint(url: String, addPoint: DeepNaviPoint, mapInfo: DeepNaviMap, callback: (msg: String, pointId: String?) -> Unit): Boolean {
        doPostAsync(
            url, null, ReflectJsonApi.toJsonOrNull(addPoint)?.toByteArray() ?: return true
        ) addPointRes@{
            val content = it?.content ?: return@addPointRes callback("no response while add point: $it", null)
            val jsonObj = SimpleJsonParser.fromJson(String(content), JsonStyle.STANDARD) as? SimpleJsonObject
                ?: return@addPointRes callback("jsonObj parse error occurred while add point: $it", null)
            if ("msg" in jsonObj) {
                callback((jsonObj["msg"]!! as SimpleJsonString).value2(), null)
            } else {
                val pointId = ((jsonObj["data"] as? SimpleJsonObject)?.get("id") as? SimpleJsonString)?.value()
                    ?: return@addPointRes callback("no point id", null)
                callback("Add point successfully", pointId)
            }
        }
        return false
    }

    fun getPoints(url: String, mapInfo: DeepNaviMap, callback: (msg: String?, MutableList<DeepNaviPoint>?) -> Unit) =
        doGetAsync("$url?mapId=${mapInfo.id}") getAllPoints@{
            val jsonObj = SimpleJsonParser.fromJsonOrNull(
                String(it?.content ?: return@getAllPoints callback("no response for getting all points", null))
            ) as? SimpleJsonObject ?: return@getAllPoints callback("getting all points failed while parse json", null)
            val iterator = (jsonObj["data"] as? SimpleJsonArray)?.iterator()
                ?: return@getAllPoints callback("getting all points failed: ${(jsonObj["msg"] as? SimpleJsonString)?.value() ?: "unknown reason"}", null)
            val points = mutableListOf<DeepNaviPoint>()
            while (iterator.hasNext()) {
                val next = iterator.next() ?: continue
                val point = ReflectJsonApi.fromJsonOrNull<DeepNaviPoint>(next, DeepNaviPoint::class.java) ?: continue
                point.modelToWorld(mapInfo)
                points.add(point)
            }
            callback(null, points)
        }
}
