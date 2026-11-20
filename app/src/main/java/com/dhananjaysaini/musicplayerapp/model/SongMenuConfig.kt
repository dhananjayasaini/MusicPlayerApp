package com.dhananjaysaini.musicplayerapp.model

data class SongMenuConfig(
    val play: Boolean = true,
    val playNext: Boolean = true,
    val addToQueue: Boolean = true,
    val delete: Boolean = true,
    val edit: Boolean = true,
    val addToFav: Boolean = false,
    val removeFromFav: Boolean = false,
    val addToPlaylist: Boolean = true,
    val removeFromPlaylist: Boolean = false,
    val setAsRingtone: Boolean = true,
    val goToAlbum: Boolean = true,
    val hideSong: Boolean = true,
    val share: Boolean = true
)