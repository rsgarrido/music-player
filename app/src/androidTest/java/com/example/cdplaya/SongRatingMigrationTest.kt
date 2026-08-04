package com.example.cdplaya

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cdplaya.data.local.AppDatabase
import com.example.cdplaya.data.local.DatabaseProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SongRatingMigrationTest {
    @Test
    fun migrationNineToTenPreservesExistingDataAndCreatesConstrainedRatingTable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "song-rating-9-10-${System.nanoTime()}.db"
        val throughNine = listOf(
            DatabaseProvider.MIGRATION_1_2,
            DatabaseProvider.MIGRATION_2_3,
            DatabaseProvider.MIGRATION_3_4,
            DatabaseProvider.MIGRATION_4_5,
            DatabaseProvider.MIGRATION_5_6,
            DatabaseProvider.MIGRATION_6_7,
            DatabaseProvider.MIGRATION_7_8,
            DatabaseProvider.MIGRATION_8_9
        )
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseName)
            .callback(object : SupportSQLiteOpenHelper.Callback(9) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE `database_marker` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`id`))"
                    )
                    throughNine.forEach { it.migrate(db) }
                    db.execSQL("INSERT INTO database_marker VALUES (1, 'preserved')")
                    db.execSQL(
                        """
                        INSERT INTO listening_track_identities VALUES
                        (1, 'Title', 'Artist', 'Album', NULL, 1000, 'title', 'artist', 'album', NULL, 1, 10, 10)
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        FrameworkSQLiteOpenHelperFactory().create(configuration).use { it.writableDatabase }

        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(DatabaseProvider.MIGRATION_9_10)
            .build()
        try {
            val sqlite = database.openHelper.writableDatabase
            assertEquals(1L, sqlite.longQuery("SELECT COUNT(*) FROM database_marker WHERE name = 'preserved'"))
            assertEquals(1L, sqlite.longQuery("SELECT COUNT(*) FROM listening_track_identities"))
            assertEquals(0L, sqlite.longQuery("SELECT COUNT(*) FROM song_ratings"))
            assertEquals(
                1L,
                sqlite.longQuery(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' AND name = 'index_song_ratings_rating'"
                )
            )
            sqlite.execSQL("INSERT INTO song_ratings VALUES (1, 5, 20, 20)")
            sqlite.execSQL("DELETE FROM listening_track_identities WHERE id = 1")
            assertEquals(0L, sqlite.longQuery("SELECT COUNT(*) FROM song_ratings"))
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }

    private fun SupportSQLiteDatabase.longQuery(sql: String): Long = query(sql).use { cursor ->
        assertTrue(cursor.moveToFirst())
        cursor.getLong(0)
    }
}
