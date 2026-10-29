package com.dhananjaysaini.musicplayerapp.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.dhananjaysaini.musicplayerapp.repository.LoadMusicUseCase
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.repository.MusicRepository
import com.dhananjaysaini.musicplayerapp.service.MusicService
import kotlinx.coroutines.launch
import kotlin.math.log

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = MusicRepository(application)
    private val loadMusicUseCase = LoadMusicUseCase(repo)
    val musicListLiveData = MutableLiveData<ArrayList<Music>>()
    private val isLoading = MutableLiveData<Boolean>()
    val error = MutableLiveData<String>()

    init {
        if (MusicService.allSongs.isNotEmpty()) {
            musicListLiveData.value = ArrayList(MusicService.allSongs)
        }
    }

    fun loadMusic(context: Context, force: Boolean = false) {

        if (!force && !musicListLiveData.value.isNullOrEmpty()) return

        isLoading.postValue(true)

        viewModelScope.launch {
            try {
                val songs = loadMusicUseCase()
                musicListLiveData.postValue(songs)

                MusicService.playlist = ArrayList(songs)
                MusicService.allSongs = songs

            } catch (e: Exception) {
                error.postValue(e.message.toString())
            } finally {
                isLoading.postValue(false)
            }
        }
    }
}
