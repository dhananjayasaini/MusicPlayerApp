package com.dhananjaysaini.musicplayerapp.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dhananjaysaini.musicplayerapp.model.PlaylistEntity
import com.dhananjaysaini.musicplayerapp.model.PlaylistSongEntity

@Dao
interface PlaylistSongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSong(entity: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId=:playlistId AND songId=:songId")
    suspend fun removeSong(playlistId: Int, songId: String)

    @Query("SELECT songId FROM playlist_songs WHERE playlistId=:playlistId")
    fun getSongIds(playlistId: Int): LiveData<List<String>>

    @Query("SELECT songId FROM playlist_songs WHERE playlistId=:playlistId")
    suspend fun getSongIdsList(playlistId: Int): List<String>

    @Query("SELECT * FROM playlists WHERE songId = :playlistId LIMIT 1")
    fun getPlaylistById(playlistId: Int): LiveData<PlaylistEntity>

}
