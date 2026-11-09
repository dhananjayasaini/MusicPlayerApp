package com.dhananjaysaini.musicplayerapp.utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.media.audiofx.Visualizer
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorInt
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

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

    @ColorInt
    var playedColor: Int = Color.parseColor("#FFD700") // Gold / Bright yellow default
        set(value) {
            field = value
            playedPaint.color = value
            thumbPaint.color = value
            postInvalidateOnAnimation()
        }

    @ColorInt
    var unplayedColor: Int = Color.parseColor("#55FFFFFF") // Semi-transparent white
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

    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = playedColor
        setShadowLayer(8f, 0f, 0f, Color.argb(120, 0, 0, 0))
    }

    private val barCount = 42
    private val baseHeights = FloatArray(barCount)
    private val liveWaveFactors = FloatArray(barCount) { 1.0f }

    private var visualizer: Visualizer? = null
    private var isDragging = false
    private var pulseAnimator: ValueAnimator? = null
    private var pulsePhase = 0f

    init {
        // Pre-generate a natural musical waveform curve
        val random = Random(42)
        for (i in 0 until barCount) {
            val normalized = i.toFloat() / barCount
            val curve = (sin(normalized * Math.PI) * 0.7f + 0.3f).toFloat()
            val noise = (random.nextFloat() * 0.4f + 0.6f)
            baseHeights[i] = (curve * noise).coerceIn(0.18f, 1.0f)
        }

        startPulseAnimation()
    }

    private fun startPulseAnimation() {
        pulseAnimator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
            duration = 1600
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                pulsePhase = animator.animatedValue as Float
                for (i in 0 until barCount) {
                    val wave = sin(pulsePhase + i * 0.35f) * 0.15f + 1f
                    liveWaveFactors[i] = wave.toFloat()
                }
                postInvalidateOnAnimation()
            }
            start()
        }
    }

    /**
     * Link with audio session for real-time frequency visualizer if permission granted
     */
    fun attachVisualizer(audioSessionId: Int) {
        releaseVisualizer()
        if (audioSessionId <= 0) return
        try {
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[0]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        vis: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        if (waveform == null) return
                        val step = (waveform.size / barCount).coerceAtLeast(1)
                        for (i in 0 until barCount) {
                            val byteIndex = (i * step).coerceIn(0, waveform.size - 1)
                            val energy = abs(waveform[byteIndex].toInt()) / 128f
                            liveWaveFactors[i] = (energy * 0.7f + 0.6f).coerceIn(0.4f, 1.6f)
                        }
                        postInvalidateOnAnimation()
                    }

                    override fun onFftDataCapture(
                        vis: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {}
                }, Visualizer.getMaxCaptureRate() / 2, true, false)
                enabled = true
            }
        } catch (_: Exception) {
            // Visualizer permission or device limit fallback: keeps pulsing animation
        }
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
        pulseAnimator?.cancel()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                parent.requestDisallowInterceptTouchEvent(true)
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
                    parent.requestDisallowInterceptTouchEvent(false)
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
        onSeekListener?.onProgressChanged(this, newProgress, true)
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width - paddingLeft - paddingRight
        val h = height - paddingTop - paddingBottom
        if (w <= 0 || h <= 0) return

        val centerY = paddingTop + h / 2f
        val step = w.toFloat() / barCount
        val barStroke = (step * 0.65f).coerceIn(4f, 18f)

        playedPaint.strokeWidth = barStroke
        unplayedPaint.strokeWidth = barStroke

        val progressRatio = if (max > 0) progress.toFloat() / max else 0f
        val progressX = paddingLeft + (progressRatio * w)

        for (i in 0 until barCount) {
            val barX = paddingLeft + (i + 0.5f) * step
            val dynamicHeight = (h * 0.85f * baseHeights[i] * liveWaveFactors[i])
                .coerceIn(12f, h.toFloat())
            val halfHeight = dynamicHeight / 2f

            val paint = if (barX <= progressX) playedPaint else unplayedPaint
            canvas.drawLine(barX, centerY - halfHeight, barX, centerY + halfHeight, paint)
        }

        // Draw sleek thumb indicator at current seek point
        canvas.drawCircle(progressX, centerY, barStroke * 0.9f, thumbPaint)
    }
}
