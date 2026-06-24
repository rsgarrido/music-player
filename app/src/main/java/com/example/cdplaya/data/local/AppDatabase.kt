package com.example.cdplaya.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DatabaseMarkerEntity::class,
        FavoriteSongEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteSongDao(): FavoriteSongDao
}