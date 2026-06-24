package com.example.cdplaya.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_songs")
data class FavoriteSongEntity(
    @PrimaryKey val songKey: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val createdAt: Long
)