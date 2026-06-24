package com.example.cdplaya.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DatabaseMarkerEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase()