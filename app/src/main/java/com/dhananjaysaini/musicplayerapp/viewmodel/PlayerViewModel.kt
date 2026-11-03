package com.dhananjaysaini.musicplayerapp.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.model.PlayerUiState
import com.dhananjaysaini.musicplayerapp.repository.PlayerRepository
import com.dhananjaysaini.musicplayerapp.service.MusicService

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = PlayerRepository(application)
    private val handler = Handler(Looper.getMainLooper())

    val uiState = MutableLiveData<PlayerUiState>()

    private val songObserver: (Music?) -> Unit = {
        updateUiState()
    }

    init {
        startSeekBarUpdates()
        MusicService.currentSongLiveData.observeForever(songObserver)
    }

    fun playPause() {
        val isPlaying = MusicService.mediaPlayer?.isPlaying == true
        repo.sendCommand(if (isPlaying) Constants.ACTION_PAUSE else Constants.ACTION_PLAY)
    }

    fun nextSong() {
        repo.sendCommand(Constants.ACTION_NEXT)
    }

    fun previousSong() {
        repo.sendCommand(Constants.ACTION_PREVIOUS)
    }

    fun seekTo(ms: Int) {
        repo.sendCommand(Constants.ACTION_SEEK_TO) {
            putExtra("seekToMs", ms)
        }
    }

    fun toggleFavourite() {
        val currentSong = currentSong() ?: return
        repo.toggleFavourite(currentSong)
        updateUiState()
    }

    fun toggleRepeat() {

        val newRepeat = !repo.loadRepeatState()
        repo.saveRepeatState(newRepeat)
        updateUiState()
    }

    fun updateUiState() {
        val mp = MusicService.mediaPlayer
        val song = currentSong()

        if (song == null || mp == null) return

        uiState.postValue(
            PlayerUiState(
                currentSong = song,
                positionMs = mp.currentPosition ?: 0,
                durationMs = mp.duration ?: 0,
                isPlaying = mp.isPlaying ?: false,
                isFavourite = song.let { repo.isFavourite(it) } ?: false,
                isRepeat = repo.loadRepeatState()
            )
        )
    }

    private fun currentSong(): Music? {
        return MusicService.playlist.getOrNull(MusicService.position)
    }

    private fun startSeekBarUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                updateUiState()
                handler.postDelayed(this, 1000)
            }
        })
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
        MusicService.currentSongLiveData.removeObserver(songObserver)
    }
}
