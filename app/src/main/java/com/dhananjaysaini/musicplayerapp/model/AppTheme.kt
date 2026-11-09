package com.dhananjaysaini.musicplayerapp.model

enum class ThemeType {
    GRADIENT,
    IMAGE,
    CUSTOM
}

data class AppTheme(
    val id: String,
    val name: String,
    val type: ThemeType,
    val category: String = "Color",
    val drawableRes: Int = 0,
    val swatchRes: Int = 0,
    val startColor: Int = 0,
    val centerColor: Int = 0,
    val endColor: Int = 0,
    val isPro: Boolean = false,
    val isDark: Boolean = true,
    val customImageUri: String? = null
)
