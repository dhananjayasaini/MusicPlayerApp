package com.dhananjaysaini.musicplayerapp.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object EditSongManager {

    private const val PREF_NAME = "edited_songs_metadata"
    private const val KEY_EDITED_MAP = "edited_map"

    data class EditedSongInfo(
        val title: String,
        val artist: String,
        val album: String,
        val genre: String? = null
    )

    private val editedMap = HashMap<String, EditedSongInfo>()
    private var isInitialized = false
    private val gson = Gson()

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_EDITED_MAP, null)
        if (!json.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<HashMap<String, EditedSongInfo>>() {}.type
                val map: HashMap<String, EditedSongInfo> = gson.fromJson(json, type)
                editedMap.putAll(map)
            } catch (_: Exception) {}
        }
        isInitialized = true
    }

    fun getEditedInfo(context: Context, songId: String): EditedSongInfo? {
        init(context)
        return editedMap[songId]
    }

    fun getEditedInfo(songId: String): EditedSongInfo? {
        return editedMap[songId]
    }

    fun saveEdit(
        context: Context,
        song: Music,
        newTitle: String,
        newArtist: String,
        newAlbum: String,
        mainViewModel: MainViewModel?,
        newGenre: String? = null
    ) {
        init(context)

        val cleanTitle = newTitle.trim()
        val cleanArtist = newArtist.trim().ifEmpty { "Unknown" }
        val cleanAlbum = newAlbum.trim().ifEmpty { "Unknown" }
        val cleanGenre = newGenre?.trim()?.ifEmpty { null } ?: (editedMap[song.id]?.genre ?: song.genre)

        // 1. Persist to SharedPreferences
        val info = EditedSongInfo(cleanTitle, cleanArtist, cleanAlbum, cleanGenre)
        editedMap[song.id] = info
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val json = gson.toJson(editedMap)
            prefs.edit().putString(KEY_EDITED_MAP, json).apply()
        } catch (_: Exception) {}

        // 2. Create updated Music object
        val updatedSong = song.copy(title = cleanTitle, artist = cleanArtist, album = cleanAlbum, genre = cleanGenre)

        // 3. Update MusicService in-memory state
        MusicService.allSongs = MusicService.allSongs.map {
            if (it.id == song.id) updatedSong else it
        }
        MusicService.playlist = ArrayList(MusicService.playlist.map {
            if (it.id == song.id) updatedSong else it
        })
        if (MusicService.song?.id == song.id) {
            MusicService.song = updatedSong
            MusicService.currentSongLiveData.postValue(updatedSong)
        }

        // 4. Update MainViewModel LiveData so UI in AllSongsFragment and other screens updates instantly
        mainViewModel?.let { vm ->
            val currentList = vm.musicListLiveData.value ?: ArrayList(MusicService.allSongs)
            val updatedList = ArrayList(currentList.map {
                if (it.id == song.id) updatedSong else it
            })
            vm.musicListLiveData.postValue(updatedList)
        }

        // 5. Best-effort background update to MediaStore (works on Android 9 and some devices)
        tryUpdateMediaStore(context, song.id, cleanTitle, cleanArtist, cleanAlbum)

        // 6. User feedback toast
        Toast.makeText(context, context.getString(R.string.toast_song_updated), Toast.LENGTH_SHORT).show()
    }

    private fun tryUpdateMediaStore(context: Context, songId: String, title: String, artist: String, album: String) {
        val songIdLong = songId.toLongOrNull() ?: return
        try {
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songIdLong)
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.TITLE, title)
                put(MediaStore.Audio.Media.ARTIST, artist)
                put(MediaStore.Audio.Media.ALBUM, album)
            }
            context.contentResolver.update(uri, values, null, null)
        } catch (_: Exception) {}
    }
}
