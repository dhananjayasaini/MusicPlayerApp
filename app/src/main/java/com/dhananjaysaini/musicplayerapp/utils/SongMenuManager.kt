package com.dhananjaysaini.musicplayerapp.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel


object SongMenuManager {

    fun handlePlay(context: Context, song: Music) {
        val intent = Intent(context, MusicService::class.java).apply {
            action = Constants.ACTION_PLAY_BOTTOM_SHEET
            putExtra("song", song)
        }
        ContextCompat.startForegroundService(context, intent)
    }


    fun handlePlayNext(context: Context, song: Music) {

        val intent = Intent(context, MusicService::class.java).apply {
            action = Constants.ACTION_PLAY_NEXT
            putExtra(Constants.EXTRA_SONG, song)
        }
        context.startService(intent)
    }

    fun handleAddToQueue(context: Context, song: Music) {

        val intent = Intent(context, MusicService::class.java).apply {
            action = Constants.ACTION_ADD_TO_QUEUE
            putExtra(Constants.EXTRA_SONG, song)
        }

        context.startService(intent)
    }

    fun handleAddToPlaylist(fragmentManager: FragmentManager, song: Music) {
        PlaylistPickerBottomSheet(song).show(fragmentManager, "PlaylistPicker")
    }

    fun handleAddToFavourite(fragmentManager: FragmentManager, song: Music) {
        FavouriteManager.addFavourite(song)
    }

    fun handleRemoveFromFavourite(context: Context, song: Music) {
        FavouriteManager.removeFavourite(song)
    }

    fun handleRemoveFromPlaylist(context: Context, song: Music) {
        PlaylistManager.removeSongFromPlaylist("", song)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun handleDelete(activity: Activity, song: Music) {
        DeleteManager.deleteSong(activity, song)
    }

    fun handleShare(context: Context, song: Music) {

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "audio/*"
        intent.putExtra(Intent.EXTRA_STREAM, song)

        context.startActivity(
            Intent.createChooser(intent, "Share Song")
        )
    }
}