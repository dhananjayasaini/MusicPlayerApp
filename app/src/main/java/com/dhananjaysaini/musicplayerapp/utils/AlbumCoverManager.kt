package com.dhananjaysaini.musicplayerapp.utils

import android.content.Context

object AlbumCoverManager {

    private const val PREF_NAME = "custom_album_covers"

    fun getCustomCover(context: Context, albumName: String): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(albumName, null)
    }

    fun setCustomCover(context: Context, albumName: String, uriString: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(albumName, uriString).apply()
    }
}
