package com.dhananjaysaini.musicplayerapp.model

import java.io.File

data class RecordingItem(
    val file: File,
    val name: String,
    val durationMs: Long,
    val dateMs: Long,
    val sizeBytes: Long
)
