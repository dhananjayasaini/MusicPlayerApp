package com.dhananjaysaini.musicplayerapp.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.ColorUtils

object NotificationWaveformHelper {

    private const val BAR_COUNT = 38
    private val baseHeights = FloatArray(BAR_COUNT) { i ->
        val normalized = i.toFloat() / BAR_COUNT
        val curve = (Math.sin(normalized * Math.PI) * 0.65 + 0.35).toFloat()
        val noise = (Math.sin(i * 1.8) * 0.28 + 0.72).toFloat()
        (curve * noise).coerceIn(0.22f, 1.0f)
    }

    /**
     * Generates a sleek, vibrant neon soundwave bitmap representing playback progress.
     */
    fun generateWaveformBitmap(
        width: Int = 320,
        height: Int = 38,
        progressFraction: Float,
        playedColorStart: Int = Color.parseColor("#00F5FF"), // Neon Cyan
        playedColorEnd: Int = Color.parseColor("#FF007F"),   // Neon Pink
        unplayedColor: Int = Color.parseColor("#38224E")     // Dim Cyber Violet
    ): Bitmap {
        val safeWidth = width.coerceAtLeast(100)
        val safeHeight = height.coerceAtLeast(20)
        val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val totalSpacing = safeWidth.toFloat()
        val barWidth = (totalSpacing / BAR_COUNT) * 0.52f
        val gap = (totalSpacing / BAR_COUNT) * 0.48f
        val centerY = safeHeight / 2f
        val maxBarHalfHeight = safeHeight * 0.44f

        val playedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
            strokeWidth = barWidth
        }
        val unplayedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
            strokeWidth = barWidth
            color = unplayedColor
        }

        val activeBarIndex = (progressFraction.coerceIn(0f, 1f) * BAR_COUNT).toInt().coerceIn(0, BAR_COUNT)

        for (i in 0 until BAR_COUNT) {
            val x = i * (barWidth + gap) + (barWidth / 2f)
            val halfHeight = (baseHeights[i] * maxBarHalfHeight).coerceAtLeast(3f)
            val topY = centerY - halfHeight
            val bottomY = centerY + halfHeight

            if (i <= activeBarIndex) {
                val fraction = if (BAR_COUNT > 1) i.toFloat() / (BAR_COUNT - 1) else 0f
                playedPaint.color = ColorUtils.blendARGB(playedColorStart, playedColorEnd, fraction)
                canvas.drawLine(x, topY, x, bottomY, playedPaint)
            } else {
                canvas.drawLine(x, topY, x, bottomY, unplayedPaint)
            }
        }
        return bitmap
    }
}
