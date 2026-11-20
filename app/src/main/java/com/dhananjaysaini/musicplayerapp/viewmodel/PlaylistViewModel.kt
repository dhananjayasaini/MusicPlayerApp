package com.dhananjaysaini.musicplayerapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.dhananjaysaini.musicplayerapp.database.MusicDatabase
import com.dhananjaysaini.musicplayerapp.model.PlaylistEntity
import com.dhananjaysaini.musicplayerapp.model.PlaylistSongEntity
import com.dhananjaysaini.musicplayerapp.repository.PlaylistRepository
import kotlinx.coroutines.launch


class PlaylistViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MusicDatabase.getDatabase(application)
    private val playlistRepo = PlaylistRepository(database.PlaylistDao(), database.PlaylistSongDao())
//    val playlistsWithCount = playlistRepo.playlistsWithCount

    // 🎵 All playlist folders
    val playlists: LiveData<List<PlaylistEntity>> = playlistRepo.playlists

    // 📂 PLAYLIST CRUD
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepo.createPlaylist(name)
        }
    }

    fun deletePlaylist(id: Int) {
        viewModelScope.launch {
            playlistRepo.deletePlaylist(id)
        }
    }

    fun renamePlaylist(id: Int, newName: String) {
        viewModelScope.launch {
            playlistRepo.renamePlaylist(id, newName)
        }
    }

    // 🎶 PLAYLIST SONGS
    fun getPlaylistSongIds(playlistId: Int): LiveData<List<String>> {
        return playlistRepo.getPlaylistSongIds(playlistId)
    }

    suspend fun getSongIdsList(playlistId: Int): List<String> {
        return playlistRepo.getSongIdsList(playlistId)
    }

    fun addSongToPlaylist(playlistId: Int, songId: String) {
        viewModelScope.launch {
            playlistRepo.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Int, songId: String) {
        viewModelScope.launch {
            playlistRepo.removeSongToPlaylist(playlistId, songId)
        }
    }

    fun getPlaylistById(playlistId: Int): LiveData<PlaylistEntity> {
        return playlistRepo.getPlaylistById(playlistId)
    }


}





