package com.example.cdplaya

import android.net.Uri
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cdplaya.data.Song
import com.example.cdplaya.data.local.AppDatabase
import com.example.cdplaya.data.local.DatabaseProvider
import com.example.cdplaya.data.toCachedSongEntity
import com.example.cdplaya.data.toSong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibrarySourceMetadataTest {
    @Test
    fun songCacheRoundTripPreservesSourceMetadataAndTimestampUnits() {
        val song = Song(
            id = 41L,
            title = "Track",
            artist = "Artist",
            album = "Album",
            trackNumber = 3,
            duration = 234_567L,
            uri = Uri.parse("content://media/external/audio/media/41"),
            filePath = "/storage/emulated/0/Music/Track.flac",
            folderPath = "/storage/emulated/0/Music",
            albumArtUri = Uri.parse("content://art/41"),
            albumArtist = "Album Artist",
            volumeName = "external_primary",
            displayName = "Track.flac",
            relativePath = "Music/",
            fileSizeBytes = 12_345_678L,
            dateAddedEpochSeconds = 1_700_000_123L,
            dateModifiedEpochSeconds = 1_700_000_456L
        )

        assertEquals(song, song.toCachedSongEntity(cachedAt = 999L).toSong())
    }

    @Test
    fun migrationFiveToSixPreservesCachedRowsWithSafeDefaults() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "metadata-migration-${System.nanoTime()}.db"
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseName)
            .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE `database_marker` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`id`))
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE `favorite_songs` (`songKey` TEXT NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, `duration` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`songKey`))
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE `playlists` (`playlistId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE `playlist_songs` (`playlistSongId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `playlistId` INTEGER NOT NULL, `songKey` TEXT NOT NULL, `position` INTEGER NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, `duration` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`playlistId`) ON UPDATE NO ACTION ON DELETE CASCADE)
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX `index_playlist_songs_playlistId` ON `playlist_songs` (`playlistId`)")
                    db.execSQL("CREATE INDEX `index_playlist_songs_songKey` ON `playlist_songs` (`songKey`)")
                    db.execSQL(
                        """
                        CREATE TABLE `song_play_stats` (`songKey` TEXT NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, `duration` INTEGER NOT NULL, `playCount` INTEGER NOT NULL, `firstPlayedAt` INTEGER NOT NULL, `lastPlayedAt` INTEGER NOT NULL, PRIMARY KEY(`songKey`))
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX `index_song_play_stats_lastPlayedAt` ON `song_play_stats` (`lastPlayedAt`)")
                    db.execSQL("CREATE INDEX `index_song_play_stats_playCount` ON `song_play_stats` (`playCount`)")
                    db.execSQL(
                        """
                        CREATE TABLE `cached_songs` (`mediaStoreId` INTEGER NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, `trackNumber` INTEGER NOT NULL, `duration` INTEGER NOT NULL, `uriString` TEXT NOT NULL, `filePath` TEXT NOT NULL, `folderPath` TEXT NOT NULL, `albumArtUriString` TEXT, `albumArtist` TEXT NOT NULL, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`mediaStoreId`))
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX `index_cached_songs_folderPath` ON `cached_songs` (`folderPath`)")
                    db.execSQL("CREATE INDEX `index_cached_songs_title` ON `cached_songs` (`title`)")
                    db.execSQL("INSERT INTO `cached_songs` VALUES (7, 'Title', 'Artist', 'Album', 1, 120000, 'content://song/7', '/music/7.mp3', '/music', NULL, '', 123)")
                }

                override fun onUpgrade(
                    db: androidx.sqlite.db.SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) = Unit
            })
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        helper.writableDatabase.close()
        helper.close()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(DatabaseProvider.MIGRATION_5_6)
            .build()
        try {
            val cursor = database.openHelper.writableDatabase.query(
                "SELECT title, volumeName, displayName, fileSizeBytes, dateAddedEpochSeconds, dateModifiedEpochSeconds FROM cached_songs WHERE mediaStoreId = 7"
            )
            cursor.use {
                assertTrue(it.moveToFirst())
                assertEquals("Title", it.getString(0))
                assertEquals("", it.getString(1))
                assertEquals("", it.getString(2))
                assertEquals(0L, it.getLong(3))
                assertEquals(0L, it.getLong(4))
                assertEquals(0L, it.getLong(5))
            }
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }
}
