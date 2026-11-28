package com.dhananjaysaini.musicplayerapp.utils

import kotlin.math.abs

object ArtistAvatarHelper {

    private val INITIAL_COLORS = intArrayOf(
        0xFF00F5D4.toInt(), // teal / mint
        0xFFFF9E00.toInt(), // orange
        0xFF00E5FF.toInt(), // neon cyan
        0xFFFF4081.toInt(), // neon pink
        0xFFFFB703.toInt(), // amber / gold
        0xFFB026FF.toInt(), // purple
        0xFF06D6A0.toInt(), // emerald green
        0xFF48CAE4.toInt(), // sky blue
        0xFFFF6B6B.toInt()  // coral
    )

    fun getColorForName(name: String): Int {
        val initial = getInitial(name) ?: ""
        // Hash combination gives a distinct color based on letter & name
        val hash = abs(initial.hashCode() * 31 + name.hashCode())
        return INITIAL_COLORS[hash % INITIAL_COLORS.size]
    }

    fun getInitial(name: String): String? {
        val clean = name.trim()
        if (clean.isEmpty() || clean.startsWith("<") || clean.equals("unknown", ignoreCase = true) || clean.equals("unknown artist", ignoreCase = true)) {
            return null
        }
        val firstChar = clean.firstOrNull { it.isLetterOrDigit() } ?: return null
        return firstChar.uppercase()
    }
}
