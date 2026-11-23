package com.dhananjaysaini.musicplayerapp.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dhananjaysaini.musicplayerapp.model.PlaylistEntity
import com.dhananjaysaini.musicplayerapp.model.PlaylistWithCount

@Dao
interface PlaylistDao {

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): LiveData<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    suspend fun getAllPlaylistsList(): List<PlaylistEntity>

    @Query("DELETE FROM playlists WHERE songId = :playlistId")
    suspend fun deletePlaylist(playlistId: Int)

    @Query("UPDATE playlists SET name = :newName WHERE songId = :playlistId")
    suspend fun renamePlaylist(playlistId: Int, newName: String)

    @Query("SELECT * FROM playlists WHERE songId = :playlistId")
    fun getPlaylistById(playlistId: Int): LiveData<PlaylistEntity>

//    @Query("""SELECT p.songId, p.name, COUNT(ps.songId) AS songCount FROM playlists p
//        LEFT JOIN playlist_songs ps ON p.songId = ps.playlistId GROUP BY p.songId ORDER BY p.createdAt DESC """)
//    fun getPlaylistsWithCount(): LiveData<List<PlaylistWithCount>>

}
