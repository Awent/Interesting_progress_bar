package com.lee.bar

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import java.lang.ref.WeakReference

/**
 * 一个矩阵进度条
 *
 * [setStartAngle]
 * [setColor]
 * [setProBackground]
 * [setProgressListener]
 *
 */
class ProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val debug = false
    private val tag = "ProgressView"

    companion object {
        private const val handler_msg_time = 1
    }

    // 矩形颜色
    @ColorInt
    private var color = Color.parseColor("#1A397C") // 随时设置随时变化

    // 矩形进度
    private var currentValue = 0f // 随时设置随时变化

    // 起始角度
    private var startAngle = -90f // 默认是正北方向 -90度(如果需要在左上角或者右上角，需要根据边长计算角度，然后设置)
    private val updateTime = 25 //更新UI周期,毫秒
    private var currentLocation = 0 // 当前位置
    private var duration = 60 // 倒计时总时长 单位 s 秒

    private var paint: Paint? = null// 画笔
    private var xFerMode: PorterDuffXfermode? = null// 画笔模式
    private var rectF: RectF? = null// 方框的位置
    private var rectF2: RectF? = null// 覆盖方框 擦除方框的位置
    private var listener: ProgressListener? = null// 进度变化监听器
    private var currentSecond = 0// 当前秒
    private var totalCount = 0
    private var everyAngle = 0f
    private var handler: PreviewHandler? = null
    private var timerThread: TimerThread? = null
    private var destRect: Rect? = null
    private var bgBitmap: Bitmap? = null

    init {
        paint = Paint()
        xFerMode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
//        xFerMode = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
//        xFerMode = new PorterDuffXfermode(PorterDuff.Mode.DST_IN);
//        xFerMode = new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT);
//        xFerMode = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
        rectF = RectF()
        rectF2 = RectF()
        destRect = Rect()
        bgBitmap = BitmapFactory.decodeResource(resources, R.mipmap.progress_load_bg)
        handler = PreviewHandler(this)
        timerThread = TimerThread().apply {
            start()
            onPause()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val left = paddingLeft
        val top = paddingTop
        val right = measuredWidth - paddingRight
        val bottom = measuredHeight - paddingBottom
        destRect?.set(left, top, right - left, bottom - top)
        rectF?.set(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        rectF2?.set(-200f, -200f, measuredWidth + 200.toFloat(), measuredHeight + 200.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 绘制第二层
        val sc = rectF?.let {
            canvas.saveLayer(it.left, it.top, it.right, it.bottom, null)
        }
        // 绘制矩形背景图
        bgBitmap?.apply {
            destRect?.let {
                canvas.drawBitmap(this, null, it, null)
            }
        }
        // 设置遮挡属性
        paint?.let {
            it.xfermode = xFerMode
            it.style = Paint.Style.FILL
            it.color = color
        }
        if (currentLocation >= totalCount) {
            // 最后一点点了。绘制扇形
            rectF2?.apply {
                paint?.let {
                    canvas.drawArc(this, startAngle, 0f, true, it)
                }
            }
        } else {
            // 绘制扇形
            rectF2?.apply {
                paint?.let {
                    canvas.drawArc(this, startAngle, currentValue, true, it)
                }
            }
        }
        // 将第二层反馈给画布
        if (sc != null) {
            canvas.restoreToCount(sc)
        }
    }

    /**
     * 实时更新当前进度时间
     *
     * @param time
     */
    fun updateTime(time: Int) {
        if (time == 0) {
            listener?.progress(duration, duration.toFloat())
        }
        if (debug) {
            Log.i(
                tag,
                "currentLocation = $currentLocation,totalCount = $totalCount,currentSecond = $currentSecond"
            )
        }
        if (currentLocation >= totalCount) {
            currentLocation = 0
            return
        }
        val offsetCount = (time * 1000) / updateTime
        currentLocation = totalCount - offsetCount
        timerThread?.apply {
            if (isStop) {
                onStart()
            }
        }
    }

    fun reSet() {
        currentLocation = 0
    }

    /**
     * 设置进度条底层颜色
     *
     * @param color 颜色色值
     */
    fun setColor(@ColorRes color: Int): ProgressView {
        this.color = ContextCompat.getColor(context, color)
        return this
    }

    /**
     * 设置开始角度 默认为-90 正北方向(顺时针)
     *
     * @param startAngle 开始角度
     */
    fun setStartAngle(startAngle: Int): ProgressView {
        this.startAngle = startAngle.toFloat()
        return this
    }


    /**
     * 获取当前秒数
     *
     * @return
     */
    fun getCurrentSecond(): Int {
        return currentSecond
    }

    /**
     * 获取总共秒数
     *
     * @return
     */
    fun getTotalSecond(): Int {
        return duration
    }

    /**
     * 进度变化监听
     */
    fun setProgressListener(listener: ProgressListener?): ProgressView {
        this.listener = listener
        return this
    }

    /**
     * 设置进度条背景
     */
    fun setProBackground(@DrawableRes proRes: Int): ProgressView {
        bgBitmap = BitmapFactory.decodeResource(resources, proRes)
        return this
    }

    fun start() {
        start(60)
    }

    /**
     * 设置秒数，开始倒计时(不可重复设置)
     *
     * @param time 倒计时秒数
     */
    fun start(time: Int) {
        duration = time
        if (duration <= 0) {
            return
        }
        // 计算一共需要更新多少次
        totalCount = (duration * 1000) / updateTime
        if (totalCount <= 0) {
            return
        }
        listener?.start()
        // 每次更新多少度
        everyAngle = (360 / totalCount.toFloat())
        timerThread?.apply {
            takeIf {
                !it.isAlive
            }?.let {
                start()
            }
            onStart()
        }
    }

    private inner class TimerThread : Thread() {
        private val control = Object() //只是任意的实例化一个对象而已,用于控制线程暂停和开启的一个锁

        //用来停止线程
        private var endThread = false
        var isStop = false
            private set

        fun onStop() {
            setSuspend(true)
        }

        fun onPause() {
            setSuspend(true)
        }

        fun onStart() {
            endThread = false
            setSuspend(false)
        }

        fun onDestroy() {
            endThread = true
            interrupt()
        }

        private fun setSuspend(isStop: Boolean) {
            if (!isStop) {
                synchronized(control) { control.notifyAll() }
            }
            this.isStop = isStop
        }

        override fun run() {
            super.run()
            while (!endThread) {
                try {
                    sleep(updateTime.toLong())
                    if (!isStop) {
                        currentLocation++
                        if (debug) {
                            Log.e(tag, "currentLocation = $currentLocation")
                        }
                        handler?.sendEmptyMessage(handler_msg_time)
                    } else {
                        synchronized(control) { control.wait() }
                    }
                } catch (e: InterruptedException) {
                    if (endThread) {
                        break
                    }
                    e.printStackTrace()
                }
            }
        }
    }

    private class PreviewHandler constructor(instance: ProgressView) : Handler() {
        private val reference: WeakReference<ProgressView> = WeakReference<ProgressView>(instance)
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val instance: ProgressView? = reference.get() ?: return
            when (msg.what) {
                handler_msg_time -> instance?.update()
            }
        }

    }

    private var currentTimeMillis = 0

    private fun update() {
        invalidate()
        if (currentLocation >= totalCount) {
            currentValue = 0f
            currentTimeMillis = 0
            timerThread?.onStop()
            listener?.over()
        } else {
//            currentValue = 360 - ((float) currentLocation * everyAngle);
            currentValue = (currentLocation.toFloat() * everyAngle)
            // 当前秒
            currentTimeMillis = currentLocation * updateTime / 1000
            if (currentSecond != currentTimeMillis) {
                currentSecond = currentTimeMillis
                listener?.progress(duration, currentTimeMillis.toFloat())
            }
        }
    }

    /**
     * 停止更新（不会回调over）
     */
    fun stop() {
        currentValue = 0f
        currentSecond = 0
        currentLocation = 0
        currentTimeMillis = 0
        timerThread?.onStop()
    }

    /**
     * 暂停更新（不会回调over）
     */
    fun pause() {
        timerThread?.onPause()
    }

    fun destroy() {
        handler?.removeCallbacksAndMessages(null)
        handler = null
        timerThread?.onDestroy()
        timerThread = null
        listener = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
        destroy()
    }

    interface ProgressListener {
        /**
         * 倒计时结束
         */
        fun over()

        /**
         * 开始
         */
        fun start()

        /**
         * 进度回调
         *
         * @param total    总共秒数
         * @param progress 当前秒数
         */
        fun progress(total: Int, progress: Float)
    }
}