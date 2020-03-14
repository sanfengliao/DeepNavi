package com.sysu.example

import com.sysu.example.App.Companion.BASE_URL

object KeyUrls {
    // 新建一个地图
    const val UPLOAD_MAP = "$BASE_URL/map"
    // 添加点
    const val ADD_POINT = "$BASE_URL/point"
    // 添加关键点
    const val ADD_KEY_POINT = "$BASE_URL/loc"
    // 添加边
    const val ADD_EDGE = "$BASE_URL/edge"
    // 获取地图信息
    const val LOAD_MAP = UPLOAD_MAP
    // 获取所有点
    const val GET_POINTS = "$BASE_URL/point"
    // 根据起点搜索
    const val SEARCH_BY_START_POINT = "$BASE_URL/loc/search"
    // 根据终点和mapId搜索
    const val SEARCH_BY_END_POINT = SEARCH_BY_START_POINT
    // 根据起点和终点获取路径
    const val GET_PATH = "$BASE_URL/map/navi"
    // 训练deep-navi模型
    const val TRAIN_MAP = "$BASE_URL/map/train"
}
