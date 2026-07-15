package com.example.cdplaya.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5
                )
                .build()
                .also { database ->
                    instance = database
                }
        }
    }

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `favorite_songs` (
                    `songKey` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `artist` TEXT NOT NULL,
                    `album` TEXT NOT NULL,
                    `duration` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`songKey`)
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `playlists` (
                    `playlistId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `playlist_songs` (
                    `playlistSongId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `playlistId` INTEGER NOT NULL,
                    `songKey` TEXT NOT NULL,
                    `position` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `artist` TEXT NOT NULL,
                    `album` TEXT NOT NULL,
                    `duration` INTEGER NOT NULL,
                    `addedAt` INTEGER NOT NULL,
                    FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`playlistId`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_songs_playlistId` ON `playlist_songs` (`playlistId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_songs_songKey` ON `playlist_songs` (`songKey`)")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
            CREATE TABLE IF NOT EXISTS `song_play_stats` (
                `songKey` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `artist` TEXT NOT NULL,
                `album` TEXT NOT NULL,
                `duration` INTEGER NOT NULL,
                `playCount` INTEGER NOT NULL,
                `firstPlayedAt` INTEGER NOT NULL,
                `lastPlayedAt` INTEGER NOT NULL,
                PRIMARY KEY(`songKey`)
            )
            """.trimIndent()
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_song_play_stats_lastPlayedAt` ON `song_play_stats` (`lastPlayedAt`)"
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_song_play_stats_playCount` ON `song_play_stats` (`playCount`)"
            )
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
            CREATE TABLE IF NOT EXISTS `cached_songs` (
                `mediaStoreId` INTEGER NOT NULL,
                `title` TEXT NOT NULL,
                `artist` TEXT NOT NULL,
                `album` TEXT NOT NULL,
                `trackNumber` INTEGER NOT NULL,
                `duration` INTEGER NOT NULL,
                `uriString` TEXT NOT NULL,
                `filePath` TEXT NOT NULL,
                `folderPath` TEXT NOT NULL,
                `albumArtUriString` TEXT,
                `albumArtist` TEXT NOT NULL,
                `cachedAt` INTEGER NOT NULL,
                PRIMARY KEY(`mediaStoreId`)
            )
            """.trimIndent()
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_cached_songs_folderPath` ON `cached_songs` (`folderPath`)"
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_cached_songs_title` ON `cached_songs` (`title`)"
            )
        }
    }

    private const val DATABASE_NAME = "cdplaya_database"
}