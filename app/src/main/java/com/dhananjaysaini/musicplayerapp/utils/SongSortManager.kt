package com.dhananjaysaini.musicplayerapp.utils

import android.content.Context
import com.dhananjaysaini.musicplayerapp.database.MusicDatabase
import com.dhananjaysaini.musicplayerapp.model.Music
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object SongSortManager {

    enum class SortType {
        SONG_NAME,
        ARTIST_NAME,
        ALBUM_NAME,
        FOLDER_NAME,
        ADDED_TIME,
        PLAY_COUNT,
        YEAR,
        DURATION,
        SIZE,
        RANDOM
    }

    enum class SortOrder {
        ASCENDING,
        DESCENDING
    }

    private const val PREFS_NAME = "song_sort_preferences"
    private const val KEY_SORT_TYPE = "sort_type"
    private const val KEY_SORT_ORDER = "sort_order"

    private var playCountCache: Map<String, Int>? = null

    fun getSortType(context: Context): SortType {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = sp.getString(KEY_SORT_TYPE, SortType.SONG_NAME.name)
        return try {
            SortType.valueOf(name ?: SortType.SONG_NAME.name)
        } catch (_: Exception) {
            SortType.SONG_NAME
        }
    }

    fun getSortOrder(context: Context): SortOrder {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = sp.getString(KEY_SORT_ORDER, SortOrder.ASCENDING.name)
        return try {
            SortOrder.valueOf(name ?: SortOrder.ASCENDING.name)
        } catch (_: Exception) {
            SortOrder.ASCENDING
        }
    }

    fun saveSort(context: Context, type: SortType, order: SortOrder) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit()
            .putString(KEY_SORT_TYPE, type.name)
            .putString(KEY_SORT_ORDER, order.name)
            .apply()
    }

    suspend fun refreshPlayCountCache(context: Context) = withContext(Dispatchers.IO) {
        try {
            val list = MusicDatabase.getDatabase(context.applicationContext).playHistoryDao().getAllHistorySync()
            playCountCache = list.associate { it.songId to it.playCount }
        } catch (_: Exception) {}
    }

    fun sortSongs(songs: List<Music>, context: Context): List<Music> {
        val type = getSortType(context)
        val order = getSortOrder(context)
        return sortSongs(songs, type, order)
    }

    fun sortSongs(
        songs: List<Music>,
        type: SortType,
        order: SortOrder
    ): List<Music> {
        val sorted = when (type) {
            SortType.SONG_NAME -> songs.sortedBy { it.title.lowercase() }
            SortType.ARTIST_NAME -> songs.sortedBy { it.artist.lowercase() }
            SortType.ALBUM_NAME -> songs.sortedBy { it.album.lowercase() }
            SortType.FOLDER_NAME -> songs.sortedBy {
                try {
                    File(it.path).parentFile?.name?.lowercase() ?: ""
                } catch (_: Exception) {
                    ""
                }
            }
            SortType.ADDED_TIME -> songs.sortedBy { it.date }
            SortType.PLAY_COUNT -> {
                val counts = playCountCache ?: emptyMap()
                songs.sortedBy { counts[it.id] ?: 0 }
            }
            SortType.YEAR -> songs.sortedBy { it.date }
            SortType.DURATION -> songs.sortedBy { it.duration }
            SortType.SIZE -> songs.sortedBy {
                try {
                    File(it.path).length()
                } catch (_: Exception) {
                    0L
                }
            }
            SortType.RANDOM -> songs.shuffled()
        }

        return if (type == SortType.RANDOM) {
            sorted
        } else if (order == SortOrder.DESCENDING) {
            sorted.reversed()
        } else {
            sorted
        }
    }
}
