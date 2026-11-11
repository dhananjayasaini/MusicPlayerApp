package com.dhananjaysaini.musicplayerapp.utils

import android.content.Context
import android.graphics.*
import android.media.audiofx.Visualizer
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import com.dhananjaysaini.musicplayerapp.model.formatDuration

class WaveformSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnWaveformSeekListener {
        fun onProgressChanged(waveformSeekBar: WaveformSeekBar, progress: Int, fromUser: Boolean)
        fun onStartTrackingTouch(waveformSeekBar: WaveformSeekBar)
        fun onStopTrackingTouch(waveformSeekBar: WaveformSeekBar)
    }

    var onSeekListener: OnWaveformSeekListener? = null

    var max: Int = 100
        set(value) {
            field = if (value > 0) value else 100
            postInvalidateOnAnimation()
        }

    private var currentProgress: Int = 0

    var progress: Int
        get() = currentProgress
        set(value) {
            if (!isDragging) {
                currentProgress = value.coerceIn(0, max)
                postInvalidateOnAnimation()
            }
        }

    var durationMs: Long = 0L
        set(value) {
            field = value.coerceAtLeast(0L)
            postInvalidateOnAnimation()
        }

    var positionMs: Long = 0L
        set(value) {
            if (!isDragging) {
                field = value.coerceIn(0L, durationMs.coerceAtLeast(1L))
                postInvalidateOnAnimation()
            }
        }

    @ColorInt
    var playedColor: Int = Color.WHITE
        set(value) {
            field = value
            playedPaint.color = value
            postInvalidateOnAnimation()
        }

    @ColorInt
    var unplayedColor: Int = Color.parseColor("#4DFFFFFF")
        set(value) {
            field = value
            unplayedPaint.color = value
            postInvalidateOnAnimation()
        }

    private val playedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = playedColor
    }

    private val unplayedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = unplayedColor
    }

    private val capsulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        setShadowLayer(8f, 0f, 2f, Color.argb(100, 0, 0, 0))
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B1C26")
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val capsuleRect = RectF()
    private var isDragging = false
    private var visualizer: Visualizer? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // Required for shadow on thumb
    }

    fun attachVisualizer(audioSessionId: Int) {
        releaseVisualizer()
        if (audioSessionId <= 0) return
        try {
            visualizer = Visualizer(audioSessionId).apply {
                enabled = false
            }
        } catch (_: Exception) {}
    }

    fun releaseVisualizer() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) {}
        visualizer = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        releaseVisualizer()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                parent?.requestDisallowInterceptTouchEvent(true)
                onSeekListener?.onStartTrackingTouch(this)
                updateProgressFromTouch(event.x)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    updateProgressFromTouch(event.x)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    updateProgressFromTouch(event.x)
                    isDragging = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    onSeekListener?.onStopTrackingTouch(this)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateProgressFromTouch(touchX: Float) {
        val availableWidth = (width - paddingLeft - paddingRight).toFloat()
        if (availableWidth <= 0) return
        val clampedX = (touchX - paddingLeft).coerceIn(0f, availableWidth)
        val ratio = clampedX / availableWidth
        val newProgress = (ratio * max).toInt().coerceIn(0, max)
        currentProgress = newProgress
        positionMs = (ratio * durationMs).toLong()
        onSeekListener?.onProgressChanged(this, newProgress, true)
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width - paddingLeft - paddingRight
        val h = height - paddingTop - paddingBottom
        if (w <= 0 || h <= 0) return

        val density = resources.displayMetrics.density
        val centerY = paddingTop + h / 2f
        val trackThickness = 4f * density

        playedPaint.strokeWidth = trackThickness
        unplayedPaint.strokeWidth = trackThickness

        val progressRatio = if (max > 0) progress.toFloat() / max else 0f
        val progressX = paddingLeft + (progressRatio * w)

        // 1. Draw Unplayed Track
        canvas.drawLine(
            paddingLeft.toFloat(),
            centerY,
            (width - paddingRight).toFloat(),
            centerY,
            unplayedPaint
        )

        // 2. Draw Played Track
        if (progressX > paddingLeft) {
            canvas.drawLine(
                paddingLeft.toFloat(),
                centerY,
                progressX,
                centerY,
                playedPaint
            )
        }

        // 3. Draw Timer In / Out Capsule Thumb (e.g. "2:33 / 3:53")
        val currentStr = formatDuration(positionMs)
        val totalStr = formatDuration(durationMs)
        val timeLabel = "$currentStr / $totalStr"

        textPaint.textSize = 11.5f * density
        val textWidth = textPaint.measureText(timeLabel)

        val capsuleHeight = 24f * density
        val capsuleWidth = (textWidth + 20f * density).coerceAtLeast(84f * density)
        val halfCapsuleW = capsuleWidth / 2f
        val halfCapsuleH = capsuleHeight / 2f

        val minCenterX = paddingLeft + halfCapsuleW
        val maxCenterX = width - paddingRight - halfCapsuleW
        val clampedCenterX = progressX.coerceIn(minCenterX, maxCenterX)

        capsuleRect.set(
            clampedCenterX - halfCapsuleW,
            centerY - halfCapsuleH,
            clampedCenterX + halfCapsuleW,
            centerY + halfCapsuleH
        )

        // Draw pill capsule background with rounded ends
        canvas.drawRoundRect(capsuleRect, halfCapsuleH, halfCapsuleH, capsulePaint)

        // Draw text inside capsule
        val fontMetrics = textPaint.fontMetrics
        val textBaselineY = centerY - (fontMetrics.descent + fontMetrics.ascent) / 2f
        canvas.drawText(timeLabel, clampedCenterX, textBaselineY, textPaint)
    }
}
