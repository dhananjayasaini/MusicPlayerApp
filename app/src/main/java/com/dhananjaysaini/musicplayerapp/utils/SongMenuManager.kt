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
import com.dhananjaysaini.musicplayerapp.R
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
                Toast.makeText(context, context.getString(R.string.added_to_favourite_msg), Toast.LENGTH_SHORT).show()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    MusicService.musicService?.updateNotification(MusicService.mediaPlayer?.isPlaying == true)
                }
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
                Toast.makeText(context, context.getString(R.string.removed_from_favourite_msg), Toast.LENGTH_SHORT).show()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    MusicService.musicService?.updateNotification(MusicService.mediaPlayer?.isPlaying == true)
                }
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

            val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_audio_title)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.unable_to_share_audio), Toast.LENGTH_SHORT).show()
        }
    }

    fun handleSetAsRingtone(activity: Activity, song: Music) {
        RingtoneHelper.showSetRingtoneDialog(activity, song)
    }

    fun handleGoToAlbum(context: Context, song: Music) {
        val album = song.album?.trim()
        if (album.isNullOrEmpty() || album.equals("unknown", ignoreCase = true) || album.equals("<unknown>", ignoreCase = true)) {
            Toast.makeText(context, context.getString(R.string.no_album_found), Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(context, com.dhananjaysaini.musicplayerapp.activities.AlbumSongsActivity::class.java).apply {
            putExtra("albumName", album)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun handleHideSong(activity: Activity, song: Music, onSongHidden: (() -> Unit)? = null) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.hide_song_dialog_title)
            .setMessage(activity.getString(R.string.hide_song_dialog_msg, song.title))
            .setPositiveButton(R.string.action_hide_song) { _, _ ->
                HiddenSongManager.hideSong(activity, song.id)
                MusicService.allSongs = MusicService.allSongs.filter { it.id != song.id }
                MusicService.removeSongFromPlaylistPreservingPosition(song.id)
                if (MusicService.song?.id == song.id) {
                    if (MusicService.playlist.isNotEmpty()) {
                        val nextIndex = MusicService.position.coerceAtMost(MusicService.playlist.size - 1)
                        MusicService.musicService?.playAt(nextIndex, true)
                    } else {
                        val pauseIntent = Intent(activity, MusicService::class.java).apply {
                            action = Constants.ACTION_PAUSE
                        }
                        activity.startService(pauseIntent)
                    }
                }
                Toast.makeText(activity, activity.getString(R.string.toast_song_hidden), Toast.LENGTH_SHORT).show()
                onSongHidden?.invoke()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }
}