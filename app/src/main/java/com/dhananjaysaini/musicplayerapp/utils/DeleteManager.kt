package com.dhananjaysaini.musicplayerapp.utils

import android.app.Activity
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.model.Music

object DeleteManager {
    const val DELETE_REQUEST_CODE = 1001

    fun deleteSong(activity: Activity, song: Music) {

        val uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            song.id.toLong()
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                val intentSender = MediaStore.createDeleteRequest(
                    activity.contentResolver,
                    listOf(uri)
                ).intentSender

                activity.startIntentSenderForResult(
                    intentSender,
                    DELETE_REQUEST_CODE,
                    null,
                    0,
                    0,
                    0
                )

            } else {

                activity.contentResolver.delete(uri, null, null)

//                Toast.makeText(activity, "Song deleted", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {

            Toast.makeText(activity, activity.getString(R.string.toast_unable_to_delete), Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteSongs(activity: Activity, songs: List<Music>) {
        if (songs.isEmpty()) return
        val uris = songs.mapNotNull {
            try {
                ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    it.id.toLong()
                )
            } catch (_: Exception) {
                null
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intentSender = MediaStore.createDeleteRequest(
                    activity.contentResolver,
                    uris
                ).intentSender

                activity.startIntentSenderForResult(
                    intentSender,
                    DELETE_REQUEST_CODE,
                    null,
                    0,
                    0,
                    0
                )
            } else {
                for (uri in uris) {
                    activity.contentResolver.delete(uri, null, null)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(activity, activity.getString(R.string.toast_unable_to_delete), Toast.LENGTH_SHORT).show()
        }
    }
}