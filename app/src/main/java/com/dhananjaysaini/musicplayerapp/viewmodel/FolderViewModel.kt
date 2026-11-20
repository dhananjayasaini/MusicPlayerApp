package com.dhananjaysaini.musicplayerapp.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.dhananjaysaini.musicplayerapp.model.SongFolder
import com.dhananjaysaini.musicplayerapp.utils.HiddenSongManager
import com.dhananjaysaini.musicplayerapp.utils.getMusicFolders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FolderViewModel(application: Application)
    : AndroidViewModel(application) {

    val folders = MutableLiveData<List<SongFolder>>()

    fun loadFolders(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = getMusicFolders(context).filter { !HiddenSongManager.isFolderHidden(it.path) }
            folders.postValue(list)
        }
    }
}
