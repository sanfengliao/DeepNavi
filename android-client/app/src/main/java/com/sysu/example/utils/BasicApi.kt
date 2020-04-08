package com.sysu.example.utils

fun Boolean.toInt() = if (this) 1 else 0

inline fun <reified T> Array<T>.add(data: T): Array<T> {
    val result = this.toMutableList()
    result.add(data)
    return result.toTypedArray()
}

inline fun <reified T> Array<T>.remove(data: T): Array<T> {
    val result = this.toMutableList()
    result.remove(data)
    return result.toTypedArray()
}

fun DoubleArray.toFloatList(): List<Float> = this.map { it.toFloat() }
