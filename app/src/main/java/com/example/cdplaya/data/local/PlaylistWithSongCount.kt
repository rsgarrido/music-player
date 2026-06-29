package com.example.cdplaya.data.local

data class PlaylistWithSongCount(
    val playlistId: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val songCount: Int
)