package com.dhananjaysaini.musicplayerapp.model

import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import com.dhananjaysaini.musicplayerapp.widget.MusicAppWidgetProvider

data class WidgetStyleItem(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val sizeRes: Int,
    @LayoutRes val previewLayoutRes: Int,
    @LayoutRes val widgetLayoutRes: Int,
    val providerClass: Class<out MusicAppWidgetProvider>
)
