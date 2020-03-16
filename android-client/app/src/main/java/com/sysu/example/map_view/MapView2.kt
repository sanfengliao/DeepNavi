@file:Suppress("unused", "LeakingThis")

package com.sysu.example.map_view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import com.sysu.example.R
import com.sysu.example.utils.dp2px
import kotlin.math.roundToInt
import kotlin.math.sqrt

open class MapView2 : AppCompatImageView, ViewTreeObserver.OnGlobalLayoutListener, ScaleGestureDetector.OnScaleGestureListener {
    constructor(context: Context?) : this(context, null, 0)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        scaleType = ScaleType.MATRIX
        mScaleGestureDetector = ScaleGestureDetector(context, this)
        mGestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (!isAutoScaling) {  // 如果不在自动缩放
                    isAutoScaling = true
                    val x = e.x  // 双击触点x坐标
                    val y = e.y  // 双击触点y坐标
                    val scale = getDrawableScale()
                    if (scale < scaleMid) {  // 当前缩放比例小于一级缩放比例
                        postDelayed(AutoScaleTask(scaleMid, x, y), 10)  // 一级放大
                    } else if (scale >= scaleMid && scale < scaleMax) { // 当前缩放比例在一级缩放和二级缩放比例之间
                        postDelayed(AutoScaleTask(scaleMax, x, y), 10)  // 二级放大
                    } else if (scale == scaleMax) {  // 当前缩放比例等于二级缩放比例
                        postDelayed(AutoScaleTask(scaleAdaptive, x, y), 10)  // 缩小至自适应view比例
                    } else {
                        isAutoScaling = false
                    }
                }
                return super.onDoubleTap(e)
            }
        })
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        mScaleGestureDetector.onTouchEvent(event)
        mGestureDetector.onTouchEvent(event)
        // 不在自动缩放中才可以拖动图片（这个判断可有可无，根据需求来）
        if (!isAutoScaling) {
            // 绑定touch事件，处理移动图片逻辑
            moveByTouchEvent(event)
        }
        return true
    }

    // scale
    open var scaleMin = 0.5f  // 最小缩小比例值系数
    open var scaleAdaptive = 1f  // 自适应ViewGroup(或屏幕)缩放比例值
    open var scaleMid = 2f  // 中间放大比例值系数，双击一次的放大值
    open var scaleMax = 4f  // 最大放大比例值系数，双击两次的放大值
    open var mScaleMatrix = Matrix()  // 缩放矩阵
    open var mScaleGestureDetector: ScaleGestureDetector

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    }

    override fun setImageBitmap(bm: Bitmap?) {
        isPicLoaded = false
        super.setImageBitmap(bm)
    }

    override fun setImageDrawable(drawable: Drawable?) {
        isPicLoaded = false
        super.setImageDrawable(drawable)
    }

    override fun setImageResource(resId: Int) {
        isPicLoaded = false
        super.setImageResource(resId)
    }

    override fun setImageURI(uri: Uri?) {
        isPicLoaded = false
        super.setImageURI(uri)
    }

    open var isPicLoaded = false
    override fun onGlobalLayout() {
        if (!isPicLoaded) {
            val d = drawable ?: return
            isPicLoaded = true

            val w = width
            val h = height
            // 获取图片固有的宽高（不是指本身属性:分辨率，因为android系统在加载显示图片前可能对其压缩）
            val w2 = d.intrinsicWidth
            val h2 = d.intrinsicHeight

            val first = h.toFloat() / h2
            val second = w.toFloat() / w2
            val scaleAdaptive2: Float

            // 对比图片宽高和当前View的宽高，针对性的缩放
            scaleAdaptive = if (w2 >= w && h2 <= h) {  // 如果图片固宽大于View宽,固高小于View高，
                scaleAdaptive2 = second
                first
            } else if (w2 <= w && h2 >= h) {  // 固宽小于View宽,固高大于View高，针对宽度放大
                scaleAdaptive2 = first
                second
            } else if (w2 >= w && h2 >= h || w2 <= w && h2 <= h) {  // 固宽和固高都大于或都小于View的宽高，
                scaleAdaptive2 = second.coerceAtMost(first)
                second.coerceAtLeast(first)
            } else {
                scaleAdaptive2 = 1f
                1f
            }

            // 先将图片移动到View中心位置
            mScaleMatrix.reset()
            mScaleMatrix.postTranslate((w - w2).toFloat() / 2, (h - h2).toFloat() / 2)
            // 再对图片从View的中心点缩放
            scaleAdaptive = scaleAdaptive2
            mScaleMatrix.postScale(scaleAdaptive, scaleAdaptive, w.toFloat() / 2, h.toFloat() / 2)
            // 执行偏移和缩放
            imageMatrix = mScaleMatrix
            onChangedListener?.onChanged(getMatrixRect())

            // 根据当前图片的缩放情况，重新调整图片的最大最小缩放值
            scaleMax *= scaleAdaptive
            scaleMid *= scaleAdaptive
            scaleMin *= scaleAdaptive
            if (scaleMin > scaleAdaptive2) {
                scaleMin = scaleAdaptive2
            }
        }
    }

    override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean = true  // 返回为true，则缩放手势事件往下进行，否则到此为止，即不会执行onScale和onScaleEnd方法

    override fun onScaleEnd(detector: ScaleGestureDetector?) {
        drawable ?: return
        detector ?: return
        // if (getDrawableScale() < scaleAdaptive) {
        //     postDelayed(AutoScaleTask(scaleAdaptive, width / 2f, height * 1f), 2)
        // }
        if (getDrawableScale() < scaleMin) {
            postDelayed(AutoScaleTask(scaleMin, width / 2f, height * 1f), 2)
        }
    }

    /**
     * 自动缩放任务
     */
    open inner class AutoScaleTask(
        open val targetScale: Float,  // 目标缩放值
        open val x: Float,  // 缩放焦点的x坐标
        open val y: Float  // 缩放焦点的y坐标
    ) : Runnable {
        open var tmpScale = if (getDrawableScale() < targetScale) {  // 当前缩放值小于目标缩放值，目标是放大图片
            // 设定缩放梯度为放大梯度
            TMP_AMPLIFY
        } else {  // 当前缩放值小于(等于可以忽略)目标缩放值，目标是缩小图片
            // 设定缩放梯度为缩小梯度
            TMP_SHRINK
        }  // 缩小梯度

        override fun run() {
            // 设定缩放参数
            mScaleMatrix.postScale(tmpScale, tmpScale, x, y)
            // 检查即将缩放后造成的留空隙和图片不居中的问题，及时调整缩放参数
            checkBorderAndCenter()
            imageMatrix = mScaleMatrix
            onChangedListener?.onChanged(getMatrixRect())
            // 当前缩放值
            val scale = getDrawableScale()
            // 如果tmpScale>1即放大任务状态，且当前缩放值还是小于目标缩放值或
            // tmpScale<1即缩小任务状态，且当前缩放值还是大于目标缩放值就继续执行缩放任务
            if (tmpScale > 1 && scale < targetScale || scale > targetScale && tmpScale < 1) {
                postDelayed(this, 2)
            } else {  // 缩放的略微过头了,需要强制设定为目标缩放值
                tmpScale = targetScale / scale
                mScaleMatrix.postScale(tmpScale, tmpScale, x, y)
                checkBorderAndCenter()
                imageMatrix = mScaleMatrix
                onChangedListener?.onChanged(getMatrixRect())
                isAutoScaling = false
            }
        }
    }

    override fun onScale(detector: ScaleGestureDetector?): Boolean {
        drawable ?: return true
        detector ?: return true
        var scaleFactor = detector.scaleFactor  // 缩放因子(即将缩放的值)
        val scale = getDrawableScale()  // 当前图片已缩放的值（如果onScale第一次被调用，scale就是自适应后的缩放值：SCALE_ADAPTIVE）
        // 当前缩放值在最大放大值以内且手势检测缩放因子为缩小手势(小于1)，或当前缩放值在最小缩小值以内且缩放因子为放大手势，允许缩放
        if (scale <= scaleMax && scaleFactor < 1 || scale >= scaleMin && scaleFactor > 1) {
            // 进一步考虑即将缩小后的缩放比例(scale*scaleFactor)低于规定SCALE_MIN-SCALE_MAX范围的最小值SCALE_MIN
            if (scale * scaleFactor < scaleMin && scaleFactor < 1) {
                // 强制锁定缩小后缩放比例为SCALE_MIN（scale*scaleFactor=SCALE_MIN）
                scaleFactor = scaleMin / scale
            }
            // 进一步考虑即将放大后的缩放比例(scale*scaleFactor)高于规定SCALE_MIN-SCALE_MAX范围的最大值SCALE_MAX
            if (scale * scaleFactor > scaleMax && scaleFactor > 1) {
                // 强制锁定放大后缩放比例为SCALE_MAX（scale*scaleFactor=SCALE_MAX）
                scaleFactor = scaleMax / scale
            }
            // 设定缩放值和缩放位置，这里缩放位置便是手势焦点的位置
            mScaleMatrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
            // 检查即将缩放后造成的留空隙和图片不居中的问题，及时调整缩放参数
            checkBorderAndCenter()
            // 执行缩放
            imageMatrix = mScaleMatrix
            onChangedListener?.onChanged(getMatrixRect())
        }
        return true
    }

    /**
     * 处理缩放和移动后图片边界与屏幕有间隙或者不居中的问题
     */
    open fun checkBorderAndCenter() {
        val rect = getMatrixRect()
        val width = width
        val height = height
        var deltaX = 0f  // X轴方向偏移量
        var deltaY = 0f  // Y轴方向偏移量
        // 图片宽度大于等于View宽
        if (rect.width() >= width) {
            // 图片左边坐标大于0，即左边有空隙
            if (rect.left > 0) {
                // 向左移动rect.left个单位到View最左边,rect.left=0
                deltaX = -rect.left
            }
            // 图片右边坐标小于width，即右边有空隙
            if (rect.right < width) {
                // 向右移动width - rect.left个单位到View最右边,rect.right=width
                deltaX = width - rect.right
            }
        }
        // 图片高度大于等于View高，同理
        if (rect.height() >= height) {
            // 图片上面坐标大于0，即上面有空隙
            if (rect.top > 0) {
                // 向上移动rect.top个单位到View最上边,rect.top=0
                deltaY = -rect.top
            }
            // 图片下面坐标小于height，即下面有空隙
            if (rect.bottom < height) {
                // 向下移动height - rect.bottom个单位到View最下边,rect.bottom=height
                deltaY = height - rect.bottom
            }
        }
        // 图片宽度小于View宽
        if (rect.width() < width) {
            // 计算需要移动到X方向View中心的距离
            deltaX = width / 2 - rect.right + rect.width() / 2
        }
        // 图片高度小于View高度
        if (rect.height() < height) {
            // 计算需要移动到Y方向View中心的距离
            deltaY = height / 2 - rect.bottom + rect.height() / 2
        }
        mScaleMatrix.postTranslate(deltaX, deltaY)
    }

    /**
     * 根据当前图片矩阵变换成的四个角的坐标，即left,top,right,bottom
     *
     * @return
     */
    open fun getMatrixRect(): RectF {
        val rect = RectF()
        val drawable = drawable
        if (drawable != null) {
            rect.set(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        }
        mScaleMatrix.mapRect(rect)
        return rect
    }

    /**
     * 获取当前已经缩放的比例
     * 因为x方向和y方向比例相同，所以只返回x方向的缩放比例即可
     */
    open fun getDrawableScale(): Float {
        val values = FloatArray(9)
        mScaleMatrix.getValues(values)
        return values[Matrix.MSCALE_X]
    }

    // gesture

    open var isAutoScaling = false  // 是否处于自动缩放中,用于是否响应双击手势的flag
    open var mGestureDetector: GestureDetector  // 手势探测器

    // 上一次触点中心坐标
    open var mLastX = 0f
    open var mLastY = 0f

    // 上一次拖动图片的触点数（手指数）
    open var mLastPointCount = 0

    /**
     * 通过Touch事件移动图片
     *
     * @param event
     */
    open fun moveByTouchEvent(event: MotionEvent?) {
        event ?: return
        when (event.action) {
            MotionEvent.ACTION_MOVE -> { // 手势移动
                val rect = getMatrixRect()
                // 图片宽高小于等于View宽高，即图片可以完全显示于屏幕中，那就没必要拖动了
                if (rect.width() <= width && rect.height() <= height) {
                    return
                }
                // 计算多个触点的中心坐标
                var x = 0f
                var y = 0f
                val pointerCount = event.pointerCount  // 获取触点数（手指数）
                0.until(pointerCount).forEach { i ->
                    x += event.getX(i)
                    y += event.getY(i)
                }
                // 得到最终的中心坐标
                x /= pointerCount
                y /= pointerCount
                // 如果触点数（手指数）发生变化，需要重置上一次中心坐标和数量的参考值
                if (mLastPointCount != pointerCount) {
                    mLastX = x
                    mLastY = y
                    mLastPointCount = pointerCount
                }
                var deltaX = x.toDouble() - mLastX  // X方向的位移
                var deltaY = y.toDouble() - mLastY  // Y方向的位移
                // 如果可以拖拽
                if (isCanDrag(deltaX, deltaY)) {
                    // 图片宽小于等于view宽，则X方向不需要移动
                    if (rect.width() <= width) {
                        deltaX = 0.0
                    }
                    // 图片高小于等于view高，则Y方向不需要移动
                    if (rect.height() <= height) {
                        deltaY = 0.0
                    }
                    //完成缩放
                    mScaleMatrix.postTranslate(deltaX.toFloat(), deltaY.toFloat())
                    checkBorderAndCenter()
                    imageMatrix = mScaleMatrix
                    onChangedListener?.onChanged(getMatrixRect())
                }
                // 交换中心坐标值，作为下次移动事件的参考值
                mLastX = x
                mLastY = y
            }
            MotionEvent.ACTION_CANCEL,  // 取消
            MotionEvent.ACTION_UP ->  // 释放
                mLastPointCount = 0  // 触点数置零，便于下次判断是否重置mLastX和mLastY
        }
    }

    /**
     * 是否可以移动图片
     *
     * @param deltaX
     * @param deltaY
     */
    open fun isCanDrag(deltaX: Double, deltaY: Double): Boolean =
        sqrt(deltaX * deltaX + deltaY * deltaY) >= ViewConfiguration.get(context).scaledTouchSlop

    // listener
    var onChangedListener: OnMapStateChangedListener? = null   // 地图状态变化监听对象

    /**
     * 监听地图自适应屏幕缩放、手势移动以及缩放的状态变化接口
     */
    interface OnMapStateChangedListener {
        fun onChanged(rectF: RectF?)
    }

    companion object {
        const val TMP_AMPLIFY = 1.05f  // 放大梯度
        const val TMP_SHRINK = 0.96f  // 缩小梯度
    }
}

open class Marker {
    open var name: String? = null
        get() {
            return if (field == null) {
                index.toString()
            } else {
                field
            }
        }
    open var data: Any? = null
    open var index: Int = -1

    open var scaleX = 0f
    open var scaleY = 0f
    open var iconView: View? = null
    open var imgSrcId = 0

    constructor()
    constructor(scaleX: Float, scaleY: Float, imgSrcId: Int = R.drawable.point, name: String? = null) {
        this.scaleX = scaleX
        this.scaleY = scaleY
        this.imgSrcId = imgSrcId
        this.name = name
    }
}

open class LineEdge {
    open var index: Int = -1
    open var startPointIndex: Int = -1
    open var endPointIndex: Int = -1

    constructor()
    constructor(spi: Int, epi: Int) {
        this.startPointIndex = spi
        this.endPointIndex = epi
    }
}

open class MapContainer : ViewGroup, MapView2.OnMapStateChangedListener {
    constructor(context: Context) : this(context, null, 0, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        setWillNotDraw(false)

        attrs ?: return
        val a = context.obtainStyledAttributes(attrs, R.styleable.MapContainer)
        markerWidth = a.getDimensionPixelOffset(R.styleable.MapContainer_marker_width, markerWidth)
        markerHeight = a.getDimensionPixelOffset(R.styleable.MapContainer_marker_height, markerHeight)
        markerAnimDuration = a.getInteger(R.styleable.MapContainer_marker_anim_duration, markerAnimDuration)
        markerAnimResId = a.getResourceId(R.styleable.MapContainer_marker_anim, markerAnimResId)
        a.recycle()

        mMapView = MapView2(context)
        addView(mMapView)
        mMapView.onChangedListener = this
        mMapView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        mEdgeView = EdgeView(context)
        addView(mEdgeView)
        mEdgeView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        mEdgeView.background = context.getDrawable(android.R.color.transparent)

        posView = View(context)
        addView(posView)
        posView.layoutParams = LayoutParams(dp2px(40f), dp2px(40f))
        posView.background = context.getDrawable(R.drawable.current_pos)
        posView.visibility = View.GONE
    }

    open var markerWidth: Int = 30
    open var markerHeight: Int = 60
    open var markerAnimResId: Int = 0
    open var markerAnimDuration: Int = 2000

    open lateinit var mMapView: MapView2

    // marker / point

    open var mMarkers: MutableList<Marker>? = null  // marker集合
        set(value) {
            field = value
            /* 移除上次传入的所有marker(即移除已显示的markers) */
            var i = 0
            while (i < childCount) {
                val child = getChildAt(i) ?: break
                val tag: Any? = child.getTag(R.id.is_marker)
                if (tag is Boolean && tag) {
                    removeView(child)
                } else {
                    i++
                }
            }
            // 移除所有边
            mEdges = null
            // 初始化marker
            initMarkers()
            onChanged(mMapView.getMatrixRect())
        }

    open fun initMarkers() {
        val markers = mMarkers ?: return
        val params = LayoutParams(markerWidth, markerHeight)
        markers.forEachIndexed { index, marker ->
            initMarker(marker, index, params)
        }
    }

    protected open fun initMarker(marker: Marker, index: Int, params: LayoutParams) {
        marker.index = index
        val view = if (marker.iconView == null) {
            val imageView = ImageView(context)
            marker.iconView = imageView
            imageView.setImageResource(marker.imgSrcId)
            imageView
        } else {
            marker.iconView!!.setBackgroundResource(marker.imgSrcId)
            marker.iconView!!
        }
        addView(view)
        view.setTag(R.id.is_marker, true)
        view.setTag(R.id.position_tag, index)
        view.layoutParams = params
        view.setOnClickListener(listener)
    }

    open fun addMarker(m: Marker) {
        if (mMarkers == null) {
            mMarkers = ArrayList()
        }
        initMarker(m, mMarkers!!.size, LayoutParams(markerWidth, markerHeight))
        mMarkers!!.add(m)
        onChanged(mMapView.getMatrixRect())
    }

    open fun removeMarker(m: Marker) {
        val iterator = mMarkers?.iterator() ?: return
        var removed = false
        while (iterator.hasNext()) {
            val marker = iterator.next()
            if (removed) {
                marker.index--
                marker.iconView?.setTag(R.id.position_tag, marker.index)
            } else if (marker == m) {
                iterator.remove()
                removeView(marker.iconView)
                removed = true
            }
        }
        onChanged(mMapView.getMatrixRect())
    }

    open var onMarkerClickListener: OnMarkerClickListener? = null
    open val listener = OnClickListener {
        val pos = it.getTag(R.id.position_tag) as? Int ?: return@OnClickListener
        onMarkerClickListener?.onMarkerClick(it, pos)
    }

    /**
     * maker被点击监听接口,便于回调给业务类处理事件
     */
    interface OnMarkerClickListener {
        fun onMarkerClick(view: View?, position: Int)
    }

    // edge

    open var mEdges: MutableList<LineEdge>? = null
        set(value) {
            if (mMarkers.isNullOrEmpty()) {
                return
            }
            if (value != null) {
                val iterator = value.iterator()
                var index = 0
                val maxIndex = mMarkers!!.size
                while (iterator.hasNext()) {
                    val lineEdge = iterator.next()
                    if (lineEdge.startPointIndex < 0 || lineEdge.startPointIndex >= maxIndex
                        || lineEdge.endPointIndex < 0 || lineEdge.endPointIndex >= maxIndex
                    ) {
                        iterator.remove()
                    }
                    index++
                }
            }
            field = value
            onChanged(mMapView.getMatrixRect())
        }
    open lateinit var mEdgeView: EdgeView

    open fun updateEdgeView() {
        mEdgeView.mEdges = mEdges
        mEdgeView.mMarkers = mMarkers
        mEdgeView.rectF = mMapView.getMatrixRect()
        mEdgeView.isAnimFinished = isAnimFinished
        mEdgeView.invalidate()
    }

    open fun addEdge(edge: LineEdge) {
        val maxIndex = mMarkers?.size ?: return
        if (edge.startPointIndex < 0 || edge.startPointIndex >= maxIndex || edge.endPointIndex < 0 || edge.endPointIndex >= maxIndex) {
            return
        }
        if (mEdges == null) {
            mEdges = ArrayList()
        }
        edge.index = mEdges!!.size
        mEdges!!.add(edge)
        onChanged(mMapView.getMatrixRect())
    }

    open fun removeEdge(edge: LineEdge) {
        val iterator = mEdges?.iterator() ?: return
        var removed = false
        while (iterator.hasNext()) {
            val e = iterator.next()
            if (removed) {
                e.index--
            } else if (e == edge) {
                iterator.remove()
                removed = true
            }
        }
        onChanged(mMapView.getMatrixRect())
    }

    // pos

    open lateinit var posView: View

    // x和y是百分比
    open fun pos(x: Float, y: Float) {
        if (x <= 0f || y <= 0f) {
            posView.visibility = View.INVISIBLE
        } else {
            if (posView.visibility == View.GONE || posView.visibility == View.INVISIBLE) {
                posView.visibility = View.VISIBLE
            }
            posView.tag = x to y
        }
        onChanged(mMapView.getMatrixRect())
    }
    open fun rotate(angle: Float) {
        if (posView.isVisible) {
            posView.rotation = angle
        }
    }

    // layout

    open var isAnimStarted = false
    open var isAnimFinished = false
    override fun onChanged(rectF: RectF?) {
        mMarkers ?: return
        rectF ?: return
        if (isAnimFinished) {
            updateEdgeView()
        }
        val pWidth = rectF.width()  // 地图宽度
        val pHeight = rectF.height()  // 地图高度
        val pLeft = rectF.left  // 地图左边x坐标
        val pTop = rectF.top  // 地图顶部y坐标
        // val drawableScale = mMapView.getDrawableScale()
        mMarkers!!.forEach {
            val left = (pLeft + pWidth * it.scaleX - markerWidth/* * drawableScale*/ / 2).roundToInt()
            val top = (pTop + pHeight * it.scaleY - markerHeight/* * drawableScale*/).roundToInt()
            val right = (pLeft + pWidth * it.scaleX + markerWidth/* * drawableScale*/ / 2).roundToInt()
            val bottom = (pTop + pHeight * it.scaleY).roundToInt()
            if (!isAnimStarted) {
                it.iconView?.startAnimation(
                    when (markerAnimResId) {
                        0 -> {
                            val temp = TranslateAnimation(0f, 0f, top * -1f, 0f)
                            temp.duration = markerAnimDuration.toLong()
                            temp
                        }
                        else -> AnimationUtils.loadAnimation(context, markerAnimResId)
                    }
                )
            }
            it.iconView?.layout(left, top, right, bottom)
        }
        if (posView.isVisible) {
            val data = posView.tag as Pair<Float, Float>
            val rightX = pLeft + pWidth * data.first
            val rightY = pTop + pHeight * data.second
            val halfWidth = posView.width / 2
            val halfHeight = posView.height / 2
            posView.layout(
                (rightX - halfWidth).roundToInt(),
                (rightY * scaleY - halfHeight).roundToInt(),
                (rightX + halfWidth).roundToInt(),
                (rightY * scaleY + halfHeight).roundToInt()
            )
        }
        postDelayed({
            isAnimFinished = true
            onChanged(mMapView.getMatrixRect())
        }, markerAnimDuration.toLong() + 200)
        isAnimStarted = true
        isAnimFinished = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        (0 until childCount).forEach { measureChild(getChildAt(it), widthMeasureSpec, heightMeasureSpec) }
    }

    /**
     * 这个Flag标记是为了不让ViewGroup不断地绘制子View，
     * 导致不断地重置， 因为之后MapView的缩放，
     * 移动以及markerView的移动等所涉及的重绘都是由逻辑代码控制好了
     */
    protected open var isFirstLayout = true

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (changed && isFirstLayout) {
            (0 until childCount).forEach {
                val child = getChildAt(it)
                child.layout(0, 0, child.measuredWidth, child.measuredHeight)
            }
            // isFirstLayout = false
        }
    }
}

open class EdgeView : View {
    constructor(context: Context) : this(context, null, 0, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        setWillNotDraw(false)

        edgePaint.color = context.resources.getColor(android.R.color.holo_red_light)
        edgePaint.strokeWidth = 10f
        edgePaint.strokeJoin = Paint.Join.ROUND
        edgePaint.strokeCap = Paint.Cap.ROUND

        layoutParams = ViewGroup.LayoutParams(-1, -1)
        background = context.getDrawable(android.R.color.transparent)
    }

    open var mMarkers: MutableList<Marker>? = null
    open var mEdges: MutableList<LineEdge>? = null
    open var rectF: RectF? = null
    var isAnimFinished = false
    var edgePaint: Paint = Paint()

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (!isAnimFinished) {
            return
        }
        canvas ?: return
        val markers = mMarkers ?: return
        val rectF = rectF ?: return
        val pWidth = rectF.width()  // 地图宽度
        val pHeight = rectF.height()  // 地图高度
        val pLeft = rectF.left  // 地图左边x坐标
        val pTop = rectF.top  // 地图顶部y坐标
        mEdges?.forEach {
            val startMarker = markers.getOrNull(it.startPointIndex) ?: return@forEach
            val endMarker = markers.getOrNull(it.endPointIndex) ?: return@forEach
            val startX = pLeft + pWidth * startMarker.scaleX
            val startY = pTop + pHeight * startMarker.scaleY
            val endX = pLeft + pWidth * endMarker.scaleX
            val endY = pTop + pHeight * endMarker.scaleY
            canvas.drawLine(startX, startY, endX, endY, edgePaint)
        }
    }
}

// 一般查看地图（或大图）时，双击，如果当前缩放比例小于一级放大（scale<SCALE_MID）比例就自动放大到一级放大（SCALE_MID）,
// 如果比例大于等于一级放大（SCALE_MID）比例且小于二级放大（SCALE_MAX）比例就自动放大到二级放大（SCALE_MAX），
// 如果等于二级放大（SCALE_MID）比例就缩小到自适应View大小（SCALE_ADAPTIVE），在自动缩放过程中不再响应双击事件，直到自动缩放结束，这就意味着需要一个flag进行锁定

// [Android自定义导览地图组件(一)](https://blog.csdn.net/ausboyue/article/details/77435821?depth_1-utm_source=distribute.pc_relevant.none-task&utm_source=distribute.pc_relevant.none-task)
