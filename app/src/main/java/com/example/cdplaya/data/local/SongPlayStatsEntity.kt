package com.example.cdplaya.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "song_play_stats",
    indices = [
        Index(value = ["lastPlayedAt"]),
        Index(value = ["playCount"])
    ]
)
data class SongPlayStatsEntity(
    @PrimaryKey val songKey: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val playCount: Int,
    val firstPlayedAt: Long,
    val lastPlayedAt: Long
)