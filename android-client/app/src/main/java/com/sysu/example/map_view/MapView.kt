@file:Suppress("unused", "LeakingThis")

package com.sysu.example.map_view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.sqrt

open class MarkObject {
    var mBitmap: Bitmap? = null
    var mapX = 0f
    var mapY = 0f
    var listener: MarkClickListener? = null

    constructor()

    constructor(mBitmap: Bitmap, mapX: Float, mapY: Float) {
        this.mBitmap = mBitmap
        this.mapX = mapX
        this.mapY = mapY
    }

    override fun toString(): String = "bitmap: $mBitmap, mapX: $mapX, mapY: $mapY, listener: $listener"

    interface MarkClickListener {
        fun onMarkClick(x: Float, y: Float)
    }
}

open class MapView : SurfaceView, SurfaceHolder.Callback {
    companion object {
        val TAG: String = MapView::class.java.simpleName
        const val DOUBLE_CLICK_TIME_SPACE: Long = 300
    }

    enum class Status {
        NONE, ZOOM, DRAG
    }

    open var mCurrentScaleMax = 0f
    open var mCurrentScale = 0f
    open var mCurrentScaleMin = 0f

    open var windowWidth = 0f
    open var windowHeight = 0f

    open var mBitmap: Bitmap? = null
    open var mPaint = Paint()

    open var mStartPoint = PointF()
    @Volatile
    open var mapCenter = PointF()  // mapCenter表示地图中心在屏幕上的坐标
    open var lastClickTime: Long = 0  // 记录上一次点击屏幕的时间，以判断双击事件
    open var mStatus = Status.NONE

    open var oldRate = 1f
    open var oldDist = 1f
    open var offsetX = 0f
    open var offsetY = 0f
    open var isShu = true

    open var drawerThread = DrawerThread(this)
    open var markList: MutableList<MarkObject> = ArrayList()

    constructor(context: Context?) : this(context, null, 0, 0)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        Log.d(TAG, "doInit")
        holder.addCallback(this)
        // 获取屏幕的宽和高
        windowWidth = resources.displayMetrics.widthPixels.toFloat()
        windowHeight = resources.displayMetrics.heightPixels.toFloat()
        mStartPoint = PointF()
        mapCenter = PointF()
        drawerThread.start()
    }

    open fun setBitmap(bitmap: Bitmap) {
        Log.d(TAG, "setBitmap")
        mBitmap?.recycle()
        mBitmap = bitmap
        // 设置最小缩放为铺满屏幕，最大缩放为最小缩放的4倍
        mCurrentScaleMin = (windowHeight / bitmap.height).coerceAtMost(windowWidth / bitmap.width)
        mCurrentScale = mCurrentScaleMin
        mCurrentScaleMax = mCurrentScaleMin * 4
        mapCenter.set(bitmap.width * mCurrentScale / 2, bitmap.height * mCurrentScale / 2)
        // 判断屏幕铺满的情况，isShu为true表示屏幕横向被铺满，为false表示屏幕纵向被铺满
        isShu = bitmap.height / windowHeight <= bitmap.width / windowWidth
        draw()
    }

    open operator fun plus(markObject: MarkObject) = addMark(markObject)
    open fun addMark(markObject: MarkObject, draw: Boolean = false): Boolean {
        Log.d(TAG, "addMark: $markObject")
        val result = markList.add(markObject)
        if (draw) {
            draw()
        }
        return result
    }

    open operator fun minus(markObject: MarkObject) = removeMark(markObject)
    open fun removeMark(markObject: MarkObject, draw: Boolean = false): Boolean {
        Log.d(TAG, "removeMark: $markObject")
        val result = markList.remove(markObject)
        if (draw) {
            draw()
        }
        return result
    }

    /**
     * 地图放大
     */
    open fun zoomIn() {
        if (mBitmap == null) {
            Log.d(TAG, "zoomIn failed, because mBitmap is null")
            return
        }
        if (mCurrentScale == mCurrentScaleMax) {
            Log.d(TAG, "zoomIn(mCurrentScale == mCurrentScaleMax == $mCurrentScale, mCurrentScaleMin: $mCurrentScaleMin)")
            return
        }
        Log.d(TAG, "zoomIn(mCurrentScale: $mCurrentScale, mCurrentScaleMin: $mCurrentScaleMin, mCurrentScaleMax: $mCurrentScaleMax)")
        mCurrentScale *= 1.5f
        if (mCurrentScale > mCurrentScaleMax) {
            mCurrentScale = mCurrentScaleMax
        }
        draw()
    }

    /**
     * 地图缩小
     */
    open fun zoomOut() {
        val mBitmap = this.mBitmap
        if (mBitmap == null) {
            Log.d(TAG, "zoomOut is failed because mBitmap is null")
            return
        }
        if (mCurrentScale == mCurrentScaleMin) {
            Log.d(TAG, "zoomOut(mCurrentScale == mCurrentScaleMin == $mCurrentScale, mCurrentScaleMax: $mCurrentScaleMax)")
            return
        }
        Log.d(TAG, "zoomOut(mCurrentScale: $mCurrentScale, mCurrentScaleMin: $mCurrentScaleMin, mCurrentScaleMax: $mCurrentScaleMax)")
        mCurrentScale /= 1.5f
        if (mCurrentScale < mCurrentScaleMin) {
            mCurrentScale = mCurrentScaleMin
        }

        val temp1 = mBitmap.width * mCurrentScale / 2
        val temp2 = mBitmap.height * mCurrentScale / 2
        if (isShu) {
            val temp3 = windowWidth - temp1
            when {
                mapCenter.x > temp1 -> mapCenter.x = temp1
                mapCenter.x < temp3 -> mapCenter.x = temp3
            }
            if (mapCenter.y > temp2) {
                mapCenter.y = temp2
            }
        } else {
            val temp3 = windowHeight - temp2
            when {
                mapCenter.y > temp2 -> mapCenter.y = temp2
                mapCenter.y < temp3 -> mapCenter.y = temp3
            }
            if (mapCenter.x > temp1) {
                mapCenter.x = temp1
            }
        }
        draw()
    }

    /**
     * 处理拖拽事件
     */
    open fun drag(event: MotionEvent) {
        val mBitmap = this.mBitmap
        if (mBitmap == null) {
            Log.d(TAG, "drag: mBitmap is null")
            return
        }
        Log.d(TAG, "drag -- currentPoint(${event.x}, ${event.y}), ")
        val currentPoint = PointF(event.x, event.y)
        offsetX = currentPoint.x - mStartPoint.x
        offsetY = currentPoint.y - mStartPoint.y
        // 以下是进行判断，防止出现图片拖拽离开屏幕
        if (offsetX > 0f && mapCenter.x + offsetX - mBitmap.width * mCurrentScale / 2 > 0
            || offsetX < 0 && mapCenter.x + offsetX + mBitmap.width * mCurrentScale / 2 < windowWidth
        ) {
            offsetX = 0f
        }
        if (offsetY > 0 && mapCenter.y + offsetY - mBitmap.height * mCurrentScale / 2 > 0
            || offsetY < 0 && mapCenter.y + offsetY + mBitmap.height * mCurrentScale / 2 < windowHeight
        ) {
            offsetY = 0f
        }
        mapCenter.x += offsetX
        mapCenter.y += offsetY
        draw()
        mStartPoint = currentPoint
    }

    /**
     * 计算两个触摸点的距离
     */
    protected open fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt(x * x + y * y)
    }

    /**
     * 处理多点触控缩放事件
     */
    open fun zoomAction(event: MotionEvent) {
        val mBitmap = this.mBitmap ?: return
        Log.d(TAG, "zoomAction: $event")
        val newDist = spacing(event)
        if (newDist > 10.0f) {
            mCurrentScale = oldRate * (newDist / oldDist)
            if (mCurrentScale < mCurrentScaleMin) {
                mCurrentScale = mCurrentScaleMin
            } else if (mCurrentScale > mCurrentScaleMax) {
                mCurrentScale = mCurrentScaleMax
            }
            if (isShu) {
                if (mapCenter.x - mBitmap.width * mCurrentScale / 2 > 0) {
                    mapCenter.x = mBitmap.width * mCurrentScale / 2
                } else if (mapCenter.x + mBitmap.width * mCurrentScale / 2 < windowWidth) {
                    mapCenter.x = windowWidth - mBitmap.width * mCurrentScale / 2
                }
                if (mapCenter.y - mBitmap.height * mCurrentScale / 2 > 0) {
                    mapCenter.y = mBitmap.height * mCurrentScale / 2
                }
            } else {
                if (mapCenter.y - mBitmap.height * mCurrentScale / 2 > 0) {
                    mapCenter.y = mBitmap.height * mCurrentScale / 2
                } else if (mapCenter.y + mBitmap.height * mCurrentScale / 2 < windowHeight) {
                    mapCenter.y = windowHeight - mBitmap.height * mCurrentScale / 2
                }
                if (mapCenter.x - mBitmap.width * mCurrentScale / 2 > 0) {
                    mapCenter.x = mBitmap.width * mCurrentScale / 2
                }
            }
        }
        draw()
    }

    /**
     * 处理点击标记的事件
     */
    open fun clickAction(event: MotionEvent) {
        val mBitmap = this.mBitmap ?: return
        Log.d(TAG, "clickAction: $event")
        val clickX = event.x
        val clickY = event.y
        for (markObject in markList) {
            val location = markObject.mBitmap ?: continue
            val objX = mapCenter.x - location.width / 2 - mBitmap.width * mCurrentScale / 2 + mBitmap.width * markObject.mapX * mCurrentScale
            val objY = mapCenter.y - location.height - mBitmap.height * mCurrentScale / 2 + mBitmap.height * markObject.mapY * mCurrentScale
            // 判断当前object是否包含触摸点，在这里为了得到更好的点击效果，我将标记的区域放大了
            if (objX - location.width < clickX && objX + location.width > clickX
                && objY + location.height > clickY && objY - location.height < clickY
            ) {
                markObject.listener?.onMarkClick(clickX, clickY)
                break
            }
        }
    }

    open fun draw() {
        Log.d(TAG, "draw")
        drawerThread.handler?.sendEmptyMessage(0)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.d(TAG, "onTouchEvent: $event")
        when (event.action.and(MotionEvent.ACTION_MASK)) {
            MotionEvent.ACTION_DOWN -> {
                if (event.pointerCount == 1) {
                    // 如果两次点击时间间隔小于一定值，则默认为双击事件
                    if (event.eventTime - lastClickTime < DOUBLE_CLICK_TIME_SPACE) {
                        zoomIn()
                    } else {
                        mStartPoint.set(event.x, event.y)
                        mStatus = Status.DRAG
                    }
                }
                lastClickTime = event.eventTime
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val distance = spacing(event)
                if (distance > 10f) {
                    mStatus = Status.ZOOM
                    oldDist = distance
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (mStatus == Status.DRAG) {
                    drag(event)
                } else if (mStatus == Status.ZOOM) {
                    zoomAction(event)
                }
            }
            MotionEvent.ACTION_UP -> {
                if (mStatus != Status.ZOOM) {
                    clickAction(event)
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                oldRate = mCurrentScale
                mStatus = Status.NONE
            }
        }
        return true
    }

    open fun onDestory() {
        Log.d(TAG, "onDestory")
        mBitmap?.recycle()
        markList.forEach { it.mBitmap?.recycle() }
        drawerThread.handler?.sendEmptyMessage(1)
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) = draw()
    override fun surfaceDestroyed(holder: SurfaceHolder?) = Unit
    override fun surfaceCreated(holder: SurfaceHolder?) = Unit

    open class DrawerThread(open val mapView: MapView) : Thread() {
        open var handler: Handler? = null

        override fun run() {
            Looper.prepare()
            synchronized(this) {
                handler = Handler(Handler.Callback { msg: Message ->
                    when (msg.what) {
                        1 -> Looper.myLooper()?.quit()
                        else -> {
                            val mBitmap = mapView.mBitmap ?: return@Callback true
                            val canvas: Canvas = mapView.holder.lockCanvas() ?: return@Callback true
                            val mapCenter = mapView.mapCenter
                            val mCurrentScale = mapView.mCurrentScale
                            canvas.drawColor(Color.GRAY)
                            val matrix = Matrix()
                            matrix.setScale(mCurrentScale, mCurrentScale, mBitmap.width / 2f, mBitmap.height / 2f)
                            matrix.postTranslate(mapCenter.x - mBitmap.width / 2, mapCenter.y - mBitmap.height / 2)
                            canvas.drawBitmap(mBitmap, matrix, mapView.mPaint)
                            matrix.setScale(1.0f, 1.0f)
                            mapView.markList.forEach {
                                val location = it.mBitmap ?: return@forEach
                                // 使用Matrix使得Bitmap的宽和高发生变化，在这里使用的mapX和mapY都是相对值
                                matrix.postTranslate(
                                    mapCenter.x - location.width / 2 - mBitmap.width * mCurrentScale / 2 + mBitmap.width * it.mapX * mCurrentScale,
                                    mapCenter.y - location.height - mBitmap.height * mCurrentScale / 2 + mBitmap.height * it.mapX * mCurrentScale
                                )
                                canvas.drawBitmap(location, matrix, mapView.mPaint)
                            }
                            mapView.holder.unlockCanvasAndPost(canvas)
                        }
                    }
                    true
                })
            }
            Looper.loop()
        }
    }
}
