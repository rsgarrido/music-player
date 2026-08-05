package com.example.cdplaya.data.local

import android.content.Context
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.cdplaya.data.identityNormalized

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
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10
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

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createListeningHistoryTables(db)
            migrateLegacyListeningBaselines(db, migratedAt = System.currentTimeMillis())
        }

        private fun createListeningHistoryTables(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `listening_track_identities` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `titleSnapshot` TEXT NOT NULL,
                    `artistSnapshot` TEXT NOT NULL,
                    `albumSnapshot` TEXT NOT NULL,
                    `albumArtistSnapshot` TEXT,
                    `durationMsSnapshot` INTEGER,
                    `normalizedTitle` TEXT NOT NULL,
                    `normalizedArtist` TEXT NOT NULL,
                    `normalizedAlbum` TEXT NOT NULL,
                    `metadataKey` TEXT,
                    `metadataKeyVersion` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_listening_track_identities_normalizedArtist_normalizedTitle_durationMsSnapshot` ON `listening_track_identities` (`normalizedArtist`, `normalizedTitle`, `durationMsSnapshot`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_listening_track_identities_normalizedAlbum` ON `listening_track_identities` (`normalizedAlbum`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_listening_track_identities_metadataKey` ON `listening_track_identities` (`metadataKey`)"
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `local_track_bindings` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `trackIdentityId` INTEGER NOT NULL,
                    `referenceKey` TEXT NOT NULL,
                    `mediaStoreId` INTEGER,
                    `volumeName` TEXT,
                    `contentUri` TEXT,
                    `relativePath` TEXT,
                    `displayName` TEXT,
                    `absolutePath` TEXT,
                    `fileSizeBytes` INTEGER,
                    `dateModifiedEpochSeconds` INTEGER,
                    `durationMsSnapshot` INTEGER,
                    `legacyStableKey` TEXT,
                    `portableKey` TEXT,
                    `portableKeyVersion` INTEGER,
                    `firstSeenAt` INTEGER NOT NULL,
                    `lastSeenAt` INTEGER NOT NULL,
                    `missingSince` INTEGER,
                    FOREIGN KEY(`trackIdentityId`) REFERENCES `listening_track_identities`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_local_track_bindings_trackIdentityId` ON `local_track_bindings` (`trackIdentityId`)"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_local_track_bindings_referenceKey` ON `local_track_bindings` (`referenceKey`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_local_track_bindings_volumeName_mediaStoreId` ON `local_track_bindings` (`volumeName`, `mediaStoreId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_local_track_bindings_portableKey` ON `local_track_bindings` (`portableKey`)"
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `listening_events` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `eventUuid` TEXT NOT NULL,
                    `source` TEXT NOT NULL,
                    `trackIdentityId` INTEGER NOT NULL,
                    `localTrackBindingId` INTEGER,
                    `playbackSessionId` TEXT,
                    `startedAt` INTEGER NOT NULL,
                    `endedAt` INTEGER NOT NULL,
                    `listenedMs` INTEGER NOT NULL,
                    `trackDurationMs` INTEGER,
                    `qualifiedAsPlay` INTEGER NOT NULL,
                    `qualificationReason` TEXT NOT NULL,
                    `qualificationRuleVersion` INTEGER NOT NULL,
                    `endReason` TEXT NOT NULL,
                    `sourceEventKey` TEXT,
                    `importBatchId` INTEGER,
                    `createdAt` INTEGER NOT NULL,
                    FOREIGN KEY(`trackIdentityId`) REFERENCES `listening_track_identities`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                    FOREIGN KEY(`localTrackBindingId`) REFERENCES `local_track_bindings`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_listening_events_eventUuid` ON `listening_events` (`eventUuid`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_listening_events_playbackSessionId` ON `listening_events` (`playbackSessionId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_listening_events_source_sourceEventKey` ON `listening_events` (`source`, `sourceEventKey`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_listening_events_trackIdentityId_startedAt` ON `listening_events` (`trackIdentityId`, `startedAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_listening_events_localTrackBindingId` ON `listening_events` (`localTrackBindingId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_listening_events_qualifiedAsPlay_startedAt` ON `listening_events` (`qualifiedAsPlay`, `startedAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_listening_events_source_startedAt` ON `listening_events` (`source`, `startedAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_listening_events_importBatchId` ON `listening_events` (`importBatchId`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `legacy_listening_baselines` (
                    `trackIdentityId` INTEGER NOT NULL,
                    `historicalPlayCount` INTEGER NOT NULL,
                    `firstKnownPlayedAt` INTEGER NOT NULL,
                    `lastKnownPlayedAt` INTEGER NOT NULL,
                    `legacyReferenceKey` TEXT NOT NULL,
                    `migratedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`trackIdentityId`),
                    FOREIGN KEY(`trackIdentityId`) REFERENCES `listening_track_identities`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_legacy_listening_baselines_legacyReferenceKey` ON `legacy_listening_baselines` (`legacyReferenceKey`)"
            )
        }

        private fun migrateLegacyListeningBaselines(
            db: SupportSQLiteDatabase,
            migratedAt: Long
        ) {
            db.query("SELECT * FROM `song_play_stats` ORDER BY `referenceKey`").use { cursor ->
                while (cursor.moveToNext()) {
                    val title = cursor.requiredString("title")
                    val artist = cursor.requiredString("artist")
                    val album = cursor.requiredString("album")
                    val duration = cursor.requiredLong("duration")
                    val portableKey = cursor.requiredString("portableKey").ifBlank { null }
                    val identityId = db.insert(
                        "listening_track_identities",
                        SQLiteDatabase.CONFLICT_ABORT,
                        ContentValues().apply {
                            put("titleSnapshot", title)
                            put("artistSnapshot", artist)
                            put("albumSnapshot", album)
                            put("albumArtistSnapshot", cursor.requiredString("albumArtist"))
                            put("durationMsSnapshot", duration)
                            put("normalizedTitle", title.identityNormalized())
                            put("normalizedArtist", artist.identityNormalized())
                            put("normalizedAlbum", album.identityNormalized())
                            if (portableKey == null) putNull("metadataKey") else put("metadataKey", portableKey)
                            put("metadataKeyVersion", cursor.requiredInt("portableKeyVersion"))
                            put("createdAt", migratedAt)
                            put("updatedAt", migratedAt)
                        }
                    )

                    db.insert(
                        "local_track_bindings",
                        SQLiteDatabase.CONFLICT_ABORT,
                        ContentValues().apply {
                            put("trackIdentityId", identityId)
                            put("referenceKey", cursor.requiredString("referenceKey"))
                            cursor.putNullableLong(this, "mediaStoreId")
                            putOptionalString("volumeName", cursor.requiredString("volumeName"))
                            putOptionalString("contentUri", cursor.requiredString("contentUri"))
                            putOptionalString("relativePath", cursor.requiredString("relativePath"))
                            putOptionalString("displayName", cursor.requiredString("displayName"))
                            putNull("absolutePath")
                            put("fileSizeBytes", cursor.requiredLong("fileSizeBytes"))
                            put("dateModifiedEpochSeconds", cursor.requiredLong("dateModifiedEpochSeconds"))
                            put("durationMsSnapshot", duration)
                            putOptionalString("legacyStableKey", cursor.requiredString("songKey"))
                            if (portableKey == null) putNull("portableKey") else put("portableKey", portableKey)
                            put("portableKeyVersion", cursor.requiredInt("portableKeyVersion"))
                            put("firstSeenAt", migratedAt)
                            put("lastSeenAt", migratedAt)
                            putNull("missingSince")
                        }
                    )

                    db.insert(
                        "legacy_listening_baselines",
                        SQLiteDatabase.CONFLICT_ABORT,
                        ContentValues().apply {
                            put("trackIdentityId", identityId)
                            put("historicalPlayCount", cursor.requiredInt("playCount"))
                            put("firstKnownPlayedAt", cursor.requiredLong("firstPlayedAt"))
                            put("lastKnownPlayedAt", cursor.requiredLong("lastPlayedAt"))
                            put("legacyReferenceKey", cursor.requiredString("referenceKey"))
                            put("migratedAt", migratedAt)
                        }
                    )
                }
            }
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `song_ratings` (
                    `trackIdentityId` INTEGER NOT NULL,
                    `rating` INTEGER NOT NULL,
                    `ratedAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`trackIdentityId`),
                    FOREIGN KEY(`trackIdentityId`) REFERENCES `listening_track_identities`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_song_ratings_rating` ON `song_ratings` (`rating`)"
            )
        }
    }

    private fun Cursor.requiredString(columnName: String): String =
        getString(getColumnIndexOrThrow(columnName))

    private fun Cursor.requiredLong(columnName: String): Long =
        getLong(getColumnIndexOrThrow(columnName))

    private fun Cursor.requiredInt(columnName: String): Int =
        getInt(getColumnIndexOrThrow(columnName))

    private fun Cursor.putNullableLong(values: ContentValues, columnName: String) {
        val index = getColumnIndexOrThrow(columnName)
        if (isNull(index)) values.putNull(columnName) else values.put(columnName, getLong(index))
    }

    private fun ContentValues.putOptionalString(columnName: String, value: String) {
        if (value.isBlank()) putNull(columnName) else put(columnName, value)
    }

    private const val DATABASE_NAME = "cdplaya_database"
}
