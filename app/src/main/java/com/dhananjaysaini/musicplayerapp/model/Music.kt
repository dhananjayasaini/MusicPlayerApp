package com.dhananjaysaini.musicplayerapp.model

import android.annotation.SuppressLint
import java.io.Serializable
import java.util.concurrent.TimeUnit

data class Music(
    val id: String,
    val title: String,
    val album: String,
    val artist: String,
    val duration: Long,
    val path: String,
    val artUri: String,
    val date: Long,
    val genre: String? = null
) : Serializable

@SuppressLint("DefaultLocale")
fun formatDuration (duration: Long):String {

    val hours = TimeUnit.MILLISECONDS.toHours(duration)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }

}
