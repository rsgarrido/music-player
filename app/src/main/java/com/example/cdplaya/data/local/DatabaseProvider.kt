package com.example.cdplaya.data.local

import android.content.Context
import androidx.room.Room

object DatabaseProvider {

    @Volatile
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .build()
                .also { database ->
                    instance = database
                }
        }
    }

    private const val DATABASE_NAME = "cdplaya_database"
}