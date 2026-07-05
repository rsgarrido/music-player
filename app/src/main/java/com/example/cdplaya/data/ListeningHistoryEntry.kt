package com.example.cdplaya.data

data class ListeningHistoryEntry(
    val songKey: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val playCount: Int,
    val firstPlayedAt: Long,
    val lastPlayedAt: Long
)