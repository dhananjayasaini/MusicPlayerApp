package com.dhananjaysaini.musicplayerapp.repository

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.utils.EditSongManager

object GenreRepository {

    data class GenreItem(
        val id: Long,
        val name: String,
        val songCount: Int,
        val artUri: String?,
        val songIds: List<String>
    )

    fun fetchGenres(context: Context, allSongs: List<Music>): List<GenreItem> {
        val songMap = allSongs.associateBy { it.id }
        val genreToSongIds = LinkedHashMap<String, MutableList<String>>()

        // 1. Query MediaStore.Audio.Genres table
        try {
            val genreUri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Genres._ID,
                MediaStore.Audio.Genres.NAME
            )
            context.contentResolver.query(
                genreUri, projection, null, null,
                "${MediaStore.Audio.Genres.NAME} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
                while (cursor.moveToNext()) {
                    val genreId = cursor.getLong(idCol)
                    val rawName = cursor.getString(nameCol)?.trim().orEmpty()
                    if (rawName.isBlank()) continue

                    val membersUri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
                    val memberProjection = arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID)
                    try {
                        context.contentResolver.query(
                            membersUri, memberProjection, null, null, null
                        )?.use { mCursor ->
                            val audioIdCol = mCursor.getColumnIndex(MediaStore.Audio.Genres.Members.AUDIO_ID)
                            if (audioIdCol != -1) {
                                while (mCursor.moveToNext()) {
                                    val audioId = mCursor.getLong(audioIdCol).toString()
                                    if (songMap.containsKey(audioId)) {
                                        genreToSongIds.getOrPut(rawName) { mutableListOf() }.add(audioId)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("GenreRepository", "Members query failed for $rawName: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("GenreRepository", "Genres query failed: ${e.message}")
        }

        // 2. Map custom/edited genre
        for (song in allSongs) {
            val edited = EditSongManager.getEditedInfo(context, song.id)
            val customGenre = edited?.genre ?: song.genre
            if (!customGenre.isNullOrBlank()) {
                val list = genreToSongIds.getOrPut(customGenre) { mutableListOf() }
                if (!list.contains(song.id)) {
                    list.add(song.id)
                }
            }
        }

        // 3. Any songs not assigned to a genre
        val categorizedIds = genreToSongIds.values.flatten().toSet()
        val uncategorized = allSongs.filter { !categorizedIds.contains(it.id) }
        if (uncategorized.isNotEmpty()) {
            val unknownGenre = context.getString(R.string.unknown_genre)
            genreToSongIds.getOrPut(unknownGenre) { mutableListOf() }
                .addAll(uncategorized.map { it.id })
        }

        val result = ArrayList<GenreItem>()
        var fakeId = 1L
        for ((name, songIds) in genreToSongIds) {
            val distinctIds = songIds.distinct()
            if (distinctIds.isEmpty()) continue
            val firstSong = distinctIds.firstNotNullOfOrNull { songMap[it] }
            result.add(
                GenreItem(
                    id = fakeId++,
                    name = name,
                    songCount = distinctIds.size,
                    artUri = firstSong?.artUri,
                    songIds = distinctIds
                )
            )
        }

        return result.sortedBy { it.name.lowercase() }
    }

    fun getSongsForGenre(context: Context, genreName: String, allSongs: List<Music>): List<Music> {
        val genres = fetchGenres(context, allSongs)
        val genre = genres.firstOrNull { it.name.equals(genreName, ignoreCase = true) } ?: return emptyList()
        val idSet = genre.songIds.toSet()
        return allSongs.filter { idSet.contains(it.id) }
    }
}
