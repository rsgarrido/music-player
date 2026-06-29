package com.example.cdplaya.data

data class PlaylistSong(
    val playlistSongId: Long,
    val playlistId: Long,
    val songKey: String,
    val position: Int,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long
)