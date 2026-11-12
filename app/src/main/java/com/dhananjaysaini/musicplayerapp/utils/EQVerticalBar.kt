package com.dhananjaysaini.musicplayerapp.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.roundToInt


class EQVerticalBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var maxValue = 100
    private var currentValue = 50

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        strokeWidth = 14f
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF9F00")
        strokeWidth = 14f
        strokeCap = Paint.Cap.ROUND
    }

    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF9F00")
    }

    var onProgressChanged: ((Int) -> Unit)? = null

    fun setMax(max: Int) {
        maxValue = max.coerceAtLeast(1)
        currentValue = currentValue.coerceIn(0, maxValue)
        invalidate()
    }

    fun setProgress(value: Int) {
        currentValue = value.coerceIn(0, maxValue)
        invalidate()
    }

    fun getProgress(): Int = currentValue

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val top = paddingTop + 20f
        val bottom = height - paddingBottom - 20f

        // background line
        canvas.drawLine(centerX, top, centerX, bottom, bgPaint)

        val safeMax = maxValue.coerceAtLeast(1)
        val progressY = bottom - ((currentValue.toFloat() / safeMax) * (bottom - top))

        // progress line
        canvas.drawLine(centerX, progressY, centerX, bottom, progressPaint)

        // thumb circle
        canvas.drawCircle(centerX, progressY, 18f, thumbPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false

        parent?.requestDisallowInterceptTouchEvent(true)

        when (event.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                val top = paddingTop + 20f
                val bottom = height - paddingBottom - 20f
                if (bottom <= top) return true

                val y = event.y.coerceIn(top, bottom)

                val scale = 1f - ((y - top) / (bottom - top))
                val value = (scale * maxValue).roundToInt()

                setProgress(value)
                onProgressChanged?.invoke(currentValue)

                return true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return false
    }

}
