package com.dhananjaysaini.musicplayerapp.repository

import androidx.lifecycle.LiveData
import com.dhananjaysaini.musicplayerapp.dao.PlaylistDao
import com.dhananjaysaini.musicplayerapp.dao.PlaylistSongDao
import com.dhananjaysaini.musicplayerapp.model.PlaylistEntity
import com.dhananjaysaini.musicplayerapp.model.PlaylistSongEntity


class PlaylistRepository(
    private val playlistDao: PlaylistDao,
    private val playlistSongDao: PlaylistSongDao

) {
    val playlists: LiveData<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

//    val playlistsWithCount = playlistDao.getPlaylistsWithCount()

    suspend fun createPlaylist(name: String) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun deletePlaylist(id: Int) {
        playlistDao.deletePlaylist(id)
    }

    suspend fun renamePlaylist(id: Int, newName: String) {
        playlistDao.renamePlaylist(id, newName)
    }

    fun getPlaylistSongIds(playlistId: Int): LiveData<List<String>> {
        return playlistSongDao.getSongIds(playlistId)
    }

    suspend fun getSongIdsList(playlistId: Int): List<String> {
        return playlistSongDao.getSongIdsList(playlistId)
    }

    fun getPlaylistById(playlistId: Int): LiveData<PlaylistEntity> {
        return playlistDao.getPlaylistById(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Int, songId: String) {
        playlistSongDao.addSong(PlaylistSongEntity(playlistId, songId))
    }
    suspend fun removeSongToPlaylist(playlistId: Int, songId: String) {
        playlistSongDao.removeSong(playlistId, songId)
    }
}

