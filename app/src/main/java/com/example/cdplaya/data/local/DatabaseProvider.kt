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
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8
                )
                .build()
                .also { database ->
                    instance = database
                }
        }
    }

    val MIGRATION_1_2 = object : Migration(1, 2) {
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

    val MIGRATION_2_3 = object : Migration(2, 3) {
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

    val MIGRATION_3_4 = object : Migration(3, 4) {
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

    val MIGRATION_4_5 = object : Migration(4, 5) {
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

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `cached_songs` ADD COLUMN `volumeName` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `cached_songs` ADD COLUMN `displayName` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `cached_songs` ADD COLUMN `relativePath` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `cached_songs` ADD COLUMN `fileSizeBytes` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `cached_songs` ADD COLUMN `dateAddedEpochSeconds` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `cached_songs` ADD COLUMN `dateModifiedEpochSeconds` INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE `favorite_songs_new` (
                    `referenceKey` TEXT NOT NULL,
                    `songKey` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `artist` TEXT NOT NULL,
                    `album` TEXT NOT NULL,
                    `duration` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `mediaStoreId` INTEGER,
                    `volumeName` TEXT NOT NULL,
                    `contentUri` TEXT NOT NULL,
                    `relativePath` TEXT NOT NULL,
                    `displayName` TEXT NOT NULL,
                    `fileSizeBytes` INTEGER NOT NULL,
                    `dateModifiedEpochSeconds` INTEGER NOT NULL,
                    `albumArtist` TEXT NOT NULL,
                    `portableKey` TEXT NOT NULL,
                    `portableKeyVersion` INTEGER NOT NULL,
                    PRIMARY KEY(`referenceKey`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `favorite_songs_new`
                    (`referenceKey`, `songKey`, `title`, `artist`, `album`, `duration`, `createdAt`,
                     `mediaStoreId`, `volumeName`, `contentUri`, `relativePath`, `displayName`,
                     `fileSizeBytes`, `dateModifiedEpochSeconds`, `albumArtist`, `portableKey`, `portableKeyVersion`)
                SELECT 'legacy:' || `songKey`, `songKey`, `title`, `artist`, `album`, `duration`, `createdAt`,
                       NULL, '', '', '', '', 0, 0, '', '', 1
                FROM `favorite_songs`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `favorite_songs`")
            db.execSQL("ALTER TABLE `favorite_songs_new` RENAME TO `favorite_songs`")
            db.execSQL("CREATE INDEX `index_favorite_songs_songKey` ON `favorite_songs` (`songKey`)")

            addReferenceColumns(db, "playlist_songs")

            db.execSQL(
                """
                CREATE TABLE `song_play_stats_new` (
                    `referenceKey` TEXT NOT NULL,
                    `songKey` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `artist` TEXT NOT NULL,
                    `album` TEXT NOT NULL,
                    `duration` INTEGER NOT NULL,
                    `playCount` INTEGER NOT NULL,
                    `firstPlayedAt` INTEGER NOT NULL,
                    `lastPlayedAt` INTEGER NOT NULL,
                    `mediaStoreId` INTEGER,
                    `volumeName` TEXT NOT NULL,
                    `contentUri` TEXT NOT NULL,
                    `relativePath` TEXT NOT NULL,
                    `displayName` TEXT NOT NULL,
                    `fileSizeBytes` INTEGER NOT NULL,
                    `dateModifiedEpochSeconds` INTEGER NOT NULL,
                    `albumArtist` TEXT NOT NULL,
                    `portableKey` TEXT NOT NULL,
                    `portableKeyVersion` INTEGER NOT NULL,
                    PRIMARY KEY(`referenceKey`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `song_play_stats_new`
                    (`referenceKey`, `songKey`, `title`, `artist`, `album`, `duration`, `playCount`,
                     `firstPlayedAt`, `lastPlayedAt`, `mediaStoreId`, `volumeName`, `contentUri`,
                     `relativePath`, `displayName`, `fileSizeBytes`, `dateModifiedEpochSeconds`,
                     `albumArtist`, `portableKey`, `portableKeyVersion`)
                SELECT 'legacy:' || `songKey`, `songKey`, `title`, `artist`, `album`, `duration`, `playCount`,
                       `firstPlayedAt`, `lastPlayedAt`, NULL, '', '', '', '', 0, 0, '', '', 1
                FROM `song_play_stats`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `song_play_stats`")
            db.execSQL("ALTER TABLE `song_play_stats_new` RENAME TO `song_play_stats`")
            db.execSQL("CREATE INDEX `index_song_play_stats_songKey` ON `song_play_stats` (`songKey`)")
            db.execSQL("CREATE INDEX `index_song_play_stats_lastPlayedAt` ON `song_play_stats` (`lastPlayedAt`)")
            db.execSQL("CREATE INDEX `index_song_play_stats_playCount` ON `song_play_stats` (`playCount`)")
        }

        private fun addReferenceColumns(db: SupportSQLiteDatabase, tableName: String) {
            db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `mediaStoreId` INTEGER")
            db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `volumeName` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `contentUri` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `relativePath` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `displayName` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `fileSizeBytes` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `dateModifiedEpochSeconds` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `albumArtist` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `portableKey` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `portableKeyVersion` INTEGER NOT NULL DEFAULT 1")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `cached_songs` ADD COLUMN `artworkEnrichmentVersion` INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    private const val DATABASE_NAME = "cdplaya_database"
}
