package com.example.cdplaya.data

data class ListeningHistoryEntry(
    val songKey: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val playCount: Int,
    val firstPlayedAt: Long,
    val lastPlayedAt: Long,
    val reference: SongReference = SongReference(
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        legacyStableKey = songKey
    )
)
