package com.dhananjaysaini.musicplayerapp.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import java.util.*

class AudioWaveformVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barCount = 42
    private val amplitudes = LinkedList<Float>()
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val colorStart = Color.parseColor("#00F5FF") // Neon Cyan
    private val colorEnd = Color.parseColor("#FF007F")   // Neon Pink

    init {
        // Initialize with minimal idle heights
        for (i in 0 until barCount) {
            amplitudes.add(0.08f)
        }
    }

    fun addAmplitude(amplitude: Int) {
        // Normalize 0..32767 to 0.05..1.0
        val norm = ((amplitude.toFloat() / 25000f) * 0.95f + 0.05f).coerceIn(0.05f, 1.0f)
        if (amplitudes.size >= barCount) {
            amplitudes.removeFirst()
        }
        amplitudes.add(norm)
        postInvalidateOnAnimation()
    }

    fun reset() {
        amplitudes.clear()
        for (i in 0 until barCount) {
            amplitudes.add(0.08f)
        }
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val centerY = h / 2f
        val maxHalfHeight = h * 0.44f
        val totalSpacing = w
        val barWidth = (totalSpacing / barCount) * 0.55f
        val gap = (totalSpacing / barCount) * 0.45f
        barPaint.strokeWidth = barWidth

        for (i in 0 until amplitudes.size) {
            val amp = amplitudes[i]
            val x = i * (barWidth + gap) + (barWidth / 2f)
            val halfHeight = (amp * maxHalfHeight).coerceAtLeast(3f)

            val fraction = i.toFloat() / (barCount - 1).coerceAtLeast(1)
            barPaint.color = ColorUtils.blendARGB(colorStart, colorEnd, fraction)

            canvas.drawLine(x, centerY - halfHeight, x, centerY + halfHeight, barPaint)
        }
    }
}
