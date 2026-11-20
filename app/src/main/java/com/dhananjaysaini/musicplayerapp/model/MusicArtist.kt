package com.dhananjaysaini.musicplayerapp.model

data class MusicArtist(
    val name: String,
    val songCount: Int,
    val artUri: String?,
    val artistName: String? = null,
    val albumCount: Int = 1
)
