@file:Suppress("unused")

package com.sysu.example.utils

import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DrawableRes
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

fun View.setPaddingLeft(p: Int) = this.setPadding(p, this.paddingTop, this.paddingRight, this.paddingBottom)
fun View.setPaddingRight(p: Int) = this.setPadding(this.paddingLeft, this.paddingTop, p, this.paddingBottom)
fun View.setPaddingTop(p: Int) = this.setPadding(this.paddingLeft, p, this.paddingRight, this.paddingBottom)
fun View.setPaddingBottom(p: Int) = this.setPadding(this.paddingLeft, this.paddingTop, this.paddingRight, p)
fun View.setPaddingHorizontal(p: Int) = this.setPadding(p, this.paddingTop, p, this.paddingBottom)
fun View.setPaddingVertical(p: Int) = this.setPadding(this.paddingLeft, p, this.paddingRight, p)

fun View.setMargin(m: Int) {
    val params = this.layoutParams
    if (params is ViewGroup.MarginLayoutParams) {
        params.setMargins(m, m, m, m)
        this.layoutParams = params
    }
}

fun View.setMarginLeft(m: Int) {
    val params = this.layoutParams
    if (params is ViewGroup.MarginLayoutParams) {
        params.leftMargin = m
        this.layoutParams = params
    }
}

fun View.setMarginRight(m: Int) {
    val params = this.layoutParams
    if (params is ViewGroup.MarginLayoutParams) {
        params.rightMargin = m
        this.layoutParams = params
    }
}

fun View.setMarginTop(m: Int) {
    val params = this.layoutParams
    if (params is ViewGroup.MarginLayoutParams) {
        params.topMargin = m
        this.layoutParams = params
    }
}

fun View.setMarginBottom(m: Int) {
    val params = this.layoutParams
    if (params is ViewGroup.MarginLayoutParams) {
        params.bottomMargin = m
        this.layoutParams = params
    }
}

fun View.setMarginHorizontal(m: Int) {
    val params = this.layoutParams
    if (params is ViewGroup.MarginLayoutParams) {
        params.leftMargin = m
        params.rightMargin = m
        this.layoutParams = params
    }
}

fun View.setMarginVertical(m: Int) {
    val params = this.layoutParams
    if (params is ViewGroup.MarginLayoutParams) {
        params.topMargin = m
        params.bottomMargin = m
        this.layoutParams = params
    }
}

fun TextView.setDrawableLeft(@DrawableRes d: Int) = setDrawableLeft(ContextApi.appContext.resources.getDrawable(d))
fun TextView.setDrawableRight(@DrawableRes d: Int) = setDrawableRight(ContextApi.appContext.resources.getDrawable(d))
fun TextView.setDrawableTop(@DrawableRes d: Int) = setDrawableTop(ContextApi.appContext.resources.getDrawable(d))
fun TextView.setDrawableBottom(@DrawableRes d: Int) = setDrawableBottom(ContextApi.appContext.resources.getDrawable(d))

fun TextView.setDrawableLeft(d: Drawable) = setDrawableByIndex(d, 0)
fun TextView.setDrawableRight(d: Drawable) = setDrawableByIndex(d, 2)
fun TextView.setDrawableTop(d: Drawable) = setDrawableByIndex(d, 1)
fun TextView.setDrawableBottom(d: Drawable) = setDrawableByIndex(d, 3)

fun TextView.setDrawableByIndex(d: Drawable, index: Int) {
    val drawables = compoundDrawables
    drawables[index] = d
    setCompoundDrawables(drawables[0], drawables[1], drawables[2], drawables[3])  // left, top, right, bottom
}

fun TextView.setDrawableHorizontal(@DrawableRes d: Int) = setDrawableHorizontal(ContextApi.appContext.resources.getDrawable(d))
fun TextView.setDrawableHorizontal(d: Drawable) {
    val drawables = compoundDrawables
    setCompoundDrawables(d, drawables[1], d, drawables[3])
}

fun TextView.setDrawableVertical(@DrawableRes d: Int) = setDrawableVertical(ContextApi.appContext.resources.getDrawable(d))
fun TextView.setDrawableVertical(d: Drawable) {
    val drawables = compoundDrawables
    setCompoundDrawables(drawables[0], d, drawables[2], d)
}

val str2IdMap = ConcurrentHashMap<String, Int>()
val id2StrMap = ConcurrentHashMap<Int, String>()

const val EMPTY_VIEW_STR_ID = "EMPTY_VIEW_STR_ID"

val sNextGeneratedId = AtomicInteger(1)

fun generateViewId(): Int {
    while (true) {
        val result: Int = sNextGeneratedId.get()
        // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
        var newValue = result + 1
        if (newValue > 0x00FFFFFF) newValue = 1 // Roll over to 1, not 0.
        if (sNextGeneratedId.compareAndSet(result, newValue)) {
            return result
        }
    }
}

var View.strId: String
    set(value) {
        if (value == EMPTY_VIEW_STR_ID) {
            val id = this.id
            if (id2StrMap.containsKey(id)) {
                str2IdMap.remove(id2StrMap[id])
                id2StrMap.remove(id)
            }
        } else if (!str2IdMap.containsKey(value)) {
            val id = generateViewId()
            str2IdMap[value] = id
            id2StrMap[id] = value
            this.id = id
        }
    }
    get() {
        val id = this.id
        if (id == View.NO_ID || !id2StrMap.containsKey(id)) {
            return EMPTY_VIEW_STR_ID
        }
        return id2StrMap[id]!!
    }

fun <T : TextView> T.setOnCompoundDrawableClickListener(index: Int = 2, action: Int = MotionEvent.ACTION_UP, callback: (t: T, d: Drawable) -> Boolean) =
    setOnTouchListener touch@{ v, event ->
        if (event.action != action) {
            return@touch false
        }
        val drawable = compoundDrawables[index] ?: return@touch false
        if (event.x > width - paddingRight - drawable.intrinsicWidth) {
            return@touch callback(this, drawable)
        }
        false
    }
