package com.example.cdplaya.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DatabaseMarkerEntity::class,
        FavoriteSongEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        SongPlayStatsEntity::class,
        CachedSongEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteSongDao(): FavoriteSongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun songPlayStatsDao(): SongPlayStatsDao
    abstract fun cachedSongDao(): CachedSongDao
}