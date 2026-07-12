package com.example.cdplaya.data

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val trackNumber: Int,
    val duration: Long,
    val uri: Uri,
    val filePath: String,
    val folderPath: String,
    val albumArtUri: Uri?,
    val albumArtist: String = ""
)