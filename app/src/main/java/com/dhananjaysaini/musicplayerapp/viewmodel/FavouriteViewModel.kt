package com.dhananjaysaini.musicplayerapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.dhananjaysaini.musicplayerapp.database.MusicDatabase
import com.dhananjaysaini.musicplayerapp.model.FavouriteEntity

import kotlinx.coroutines.launch

class FavouriteViewModel(application: Application) : AndroidViewModel(application) {

//  private val repo = FavouriteRepository(MusicDatabase.getDatabase(application).FavouriteDao())

    private val dao = MusicDatabase.getDatabase(application).FavouriteDao()

//  val favouriteIds = MutableLiveData<List<String>>()
    val favouriteIds: LiveData<List<String>> = dao.getFavouriteIds()

//    fun loadFavourites() {
//        viewModelScope.launch {
//            favouriteIds.postValue(repo.getFavouriteIds())
//        }
//    }

    fun toggle(songId: String) {
        viewModelScope.launch {
            if (dao.isFavourite(songId)) {
                dao.removeFavourite(FavouriteEntity(songId))
            } else {
                dao.addFavourite(FavouriteEntity(songId))
            }
        }
    }

    fun setFavourite(songId: String, isFav: Boolean) {
        viewModelScope.launch {
            if (isFav) {
                dao.addFavourite(FavouriteEntity(songId))
            } else {
                dao.removeFavourite(FavouriteEntity(songId))
            }
        }
    }

//    suspend fun isFavourite(songId: String): Boolean {
//        return repo.isFavourite(songId)
//    }

}


