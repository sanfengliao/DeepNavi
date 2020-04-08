package com.liang.example.map.net

import com.liang.example.json.JsonStyle
import com.liang.example.json.SimpleJsonObject
import com.liang.example.json.SimpleJsonParser
import com.liang.example.json.SimpleJsonString
import com.liang.example.net.doPostAsync

// 增加edge
// TODO: 删除edge

object EdgeApi {
    fun addEdge(url: String, mapId: String, id1: String, id2: String, callback: (msg: String, flag: Boolean) -> Unit) =
        doPostAsync(url, null, "mapId=$mapId&pointAId=$id1&pointBId=$id2".toByteArray()) addEdgeRes@{
            val content = it?.content ?: return@addEdgeRes callback("no response for adding edge", false)
            val jsonObj = SimpleJsonParser.fromJson(String(content), JsonStyle.STANDARD) as? SimpleJsonObject
                ?: return@addEdgeRes callback("jsonObj parse error occurred while adding edge", false)
            if ("msg" in jsonObj) {
                callback((jsonObj["msg"]!! as SimpleJsonString).value2(), false)
            } else {
                callback("Add edge successfully", true)
            }
        }
}
