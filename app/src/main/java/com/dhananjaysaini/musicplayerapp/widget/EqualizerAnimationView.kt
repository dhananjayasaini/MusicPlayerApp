package com.dhananjaysaini.musicplayerapp.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.sin

/**
 * Animated audio equalizer / visualizer view with 4 bouncing rounded bars.
 * Used to indicate the currently playing audio track.
 */
class EqualizerAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barCount = 4
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFAA1D") // Vibrant Golden Yellow Accent
        style = Paint.Style.FILL
    }

    private val barRect = RectF()
    // Bar height fractions (0.0 to 1.0)
    private val barHeights = floatArrayOf(0.30f, 0.55f, 0.40f, 0.25f)
    private var animator: ValueAnimator? = null
    private var isPlaying = false

    init {
        setupAnimator()
    }

    private fun setupAnimator() {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener { va ->
                val progress = va.animatedFraction.toDouble()
                // Continuous, phase-shifted sine oscillations for natural equalizer spectrum
                val h1 = 0.22f + 0.65f * (0.5f + 0.5f * sin(2.0 * PI * progress).toFloat())
                val h2 = 0.30f + 0.65f * (0.5f + 0.5f * sin(3.0 * PI * progress + 1.5).toFloat())
                val h3 = 0.20f + 0.75f * (0.5f + 0.5f * sin(2.5 * PI * progress + 3.0).toFloat())
                val h4 = 0.28f + 0.62f * (0.5f + 0.5f * sin(3.5 * PI * progress + 0.8).toFloat())

                barHeights[0] = h1.coerceIn(0.18f, 1.0f)
                barHeights[1] = h2.coerceIn(0.18f, 1.0f)
                barHeights[2] = h3.coerceIn(0.18f, 1.0f)
                barHeights[3] = h4.coerceIn(0.18f, 1.0f)

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
    }

    fun pauseAnimation() {
        isPlaying = false
        animator?.cancel()
        // Resting equalizer position
        barHeights[0] = 0.30f
        barHeights[1] = 0.55f
        barHeights[2] = 0.40f
        barHeights[3] = 0.25f
        invalidate()
    }

    fun stopAnimation() {
        isPlaying = false
        animator?.cancel()
        barHeights[0] = 0.18f
        barHeights[1] = 0.18f
        barHeights[2] = 0.18f
        barHeights[3] = 0.18f
        invalidate()
    }

    fun setBarColor(color: Int) {
        barPaint.color = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val density = resources.displayMetrics.density
        val gap = 2.0f * density
        val totalGaps = (barCount - 1) * gap
        val desiredBarWidth = ((w - totalGaps) / barCount).coerceAtLeast(3f)
        val cornerRadius = desiredBarWidth / 2f

        for (i in 0 until barCount) {
            val left = i * (desiredBarWidth + gap)
            val right = left + desiredBarWidth
            val barH = (h * barHeights[i]).coerceAtLeast(desiredBarWidth)
            val top = h - barH
            val bottom = h

            barRect.set(left, top, right, bottom)
            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barPaint)
        }
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
