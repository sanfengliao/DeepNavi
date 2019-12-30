package com.sysu.example.utils

fun Class<*>.isExtendExclusive(cls: Class<*>): Boolean {
    val parentCls = superclass ?: return false
    if (parentCls == cls) {
        return true
    }
    return parentCls.isExtendExclusive(cls)
}

fun Class<*>.isExtendInclusive(cls: Class<*>): Boolean {
    if (this == cls) {
        return true
    }
    return isExtendExclusive(cls)
}

fun Class<*>.isImplementExclusive(cls: Class<*>): Boolean {
    val parentInterfaces = interfaces
    if (parentInterfaces.isEmpty()) {
        return false
    }
    if (cls in parentInterfaces) {
        return true
    }
    parentInterfaces.forEach {
        if (it.isImplementExclusive(cls)) {
            return true
        }
    }
    return false
}

fun Class<*>.isImplementInclusive(cls: Class<*>): Boolean {
    if (this == cls) {
        return true
    }
    return isImplementExclusive(cls)
}
