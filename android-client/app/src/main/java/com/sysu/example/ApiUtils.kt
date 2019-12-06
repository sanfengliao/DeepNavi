package com.sysu.example

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.lang.reflect.Type

object JsonApi {
    val gson = Gson()

    fun <T : Any> fromJsonNonNull(data: String, type: Type): T = gson.fromJson<T>(data, type)

    fun <T : Any> fromJsonNullable(data: String, type: Type): T? = try {
        gson.fromJson<T>(data, type)
    } catch (e: JsonSyntaxException) {
        null
    }

    fun toJson(data: Any?): String = gson.toJson(data)
}
