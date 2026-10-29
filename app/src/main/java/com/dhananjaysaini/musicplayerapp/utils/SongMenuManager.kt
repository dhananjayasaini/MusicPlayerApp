package com.dhananjaysaini.musicplayerapp.utils

import android.app.Activity
import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentManager
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.database.MusicDatabase
import com.dhananjaysaini.musicplayerapp.model.FavouriteEntity
import com.dhananjaysaini.musicplayerapp.model.Music
import java.io.File
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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

    fun handleAddToFavourite(context: Context, song: Music) {
        FavouriteManager.addFavourite(song)
        CoroutineScope(Dispatchers.IO).launch {
            val dao = MusicDatabase.getDatabase(context.applicationContext).FavouriteDao()
            dao.addFavourite(FavouriteEntity(song.id))
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Added to favourite", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun handleAddToFavourite(fragmentManager: FragmentManager, song: Music) {
        val context = fragmentManager.fragments.firstOrNull()?.context
        if (context != null) {
            handleAddToFavourite(context, song)
        } else {
            FavouriteManager.addFavourite(song)
        }
    }

    fun handleRemoveFromFavourite(context: Context, song: Music) {
        FavouriteManager.removeFavourite(song)
        CoroutineScope(Dispatchers.IO).launch {
            val dao = MusicDatabase.getDatabase(context.applicationContext).FavouriteDao()
            dao.removeFavourite(FavouriteEntity(song.id))
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Removed from favourite", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun handleRemoveFromPlaylist(context: Context, song: Music) {
        PlaylistManager.removeSongFromPlaylist("", song)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun handleDelete(activity: Activity, song: Music) {
        DeleteManager.deleteSong(activity, song)
    }

    fun handleShare(context: Context, song: Music) {
        try {
            val uri: Uri = try {
                val songIdLong = song.id.toLongOrNull()
                if (songIdLong != null) {
                    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songIdLong)
                } else {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        File(song.path)
                    )
                }
            } catch (e: Exception) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    File(song.path)
                )
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, song.title)
                putExtra(Intent.EXTRA_TEXT, "${song.title} - ${song.artist}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Audio").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share audio", Toast.LENGTH_SHORT).show()
        }
    }
}