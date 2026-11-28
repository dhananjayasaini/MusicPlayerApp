package com.dhananjaysaini.musicplayerapp.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.sin

/**
 * Animated border view drawn directly around the album art URI thumbnail.
 * Features a rotating golden/amber sweep gradient with smooth 60fps rotation
 * and subtle breathing effect during playback.
 */
class ArtBorderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val borderRect = RectF()
    private val shaderMatrix = Matrix()
    private var sweepGradient: SweepGradient? = null

    private var rotationAngle = 0f
    private var pulseFraction = 0f
    private var animator: ValueAnimator? = null
    private var isPlaying = false

    private val gradientColors = intArrayOf(
        Color.parseColor("#FFAA1D"), // Brand Golden
        Color.parseColor("#FF6D00"), // Deep Golden Orange
        Color.parseColor("#FFE57F"), // Luminous Golden Accent
        Color.parseColor("#FF9100"), // Radiant Amber
        Color.parseColor("#FFAA1D")  // Brand Golden
    )
    private val gradientPositions = floatArrayOf(0.0f, 0.28f, 0.58f, 0.82f, 1.0f)

    init {
        setupAnimator()
    }

    private fun setupAnimator() {
        animator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 1600L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener { va ->
                rotationAngle = va.animatedValue as Float
                val progress = va.animatedFraction.toDouble()
                pulseFraction = (0.5 + 0.5 * sin(2.0 * PI * progress)).toFloat()
                invalidate()
            }
        }
    }

    fun startAnimation() {
        isPlaying = true
        if (visibility != VISIBLE) return
        if (animator == null) {
            setupAnimator()
        }
        if (animator?.isRunning != true) {
            animator?.start()
        }
        invalidate()
    }

    fun pauseAnimation() {
        isPlaying = false
        animator?.cancel()
        invalidate()
    }

    fun stopAnimation() {
        isPlaying = false
        animator?.cancel()
        rotationAngle = 0f
        pulseFraction = 0f
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            sweepGradient = SweepGradient(
                w / 2f,
                h / 2f,
                gradientColors,
                gradientPositions
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val density = resources.displayMetrics.density
        // Base stroke ~2.5dp with subtle breathing pulse when playing
        val strokeWidth = if (isPlaying) {
            (2.4f + 0.4f * pulseFraction) * density
        } else {
            2.4f * density
        }

        borderPaint.strokeWidth = strokeWidth
        val halfStroke = strokeWidth / 2f
        borderRect.set(halfStroke, halfStroke, w - halfStroke, h - halfStroke)

        // Corner radius matches the square curved corners of the 50dp ShapeableImageView (~5dp)
        val cornerRadius = (w * 0.10f).coerceAtLeast(5f * density)

        if (isPlaying && sweepGradient != null) {
            shaderMatrix.setRotate(rotationAngle, w / 2f, h / 2f)
            sweepGradient?.setLocalMatrix(shaderMatrix)
            borderPaint.shader = sweepGradient
        } else {
            // Static golden border when paused
            borderPaint.shader = null
            borderPaint.color = Color.parseColor("#FFAA1D")
        }

        canvas.drawRoundRect(borderRect, cornerRadius, cornerRadius, borderPaint)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            if (isPlaying && animator?.isRunning != true) {
                animator?.start()
            }
        } else {
            animator?.cancel()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isPlaying && visibility == VISIBLE && animator?.isRunning != true) {
            animator?.start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
