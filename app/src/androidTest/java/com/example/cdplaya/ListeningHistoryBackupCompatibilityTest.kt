package com.example.cdplaya

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cdplaya.data.ListeningHistoryRepository
import com.example.cdplaya.data.FavoritesRepository
import com.example.cdplaya.data.PlaylistsRepository
import com.example.cdplaya.data.backup.AppBackupJson
import com.example.cdplaya.data.backup.ListeningHistoryBackupRepository
import com.example.cdplaya.data.local.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListeningHistoryBackupCompatibilityTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun version6_restoresAggregatesAsSeparateCanonicalBaselinesAndCompatibilityOnly() = runBlocking {
        val backup = AppBackupJson.decodeBackup(V6_FIXTURE)
        val favoritesRepository = FavoritesRepository(database.favoriteSongDao())
        val playlistsRepository = PlaylistsRepository(database.playlistDao())
        favoritesRepository.restoreFavoritesFromBackup(backup.favorites)
        playlistsRepository.restorePlaylistsFromBackup(backup.playlists)
        ListeningHistoryRepository(database.songPlayStatsDao())
            .restoreListeningHistoryFromBackup(backup.listeningHistory)
        ListeningHistoryBackupRepository(database).restore(backup.canonicalListeningHistory!!)

        val canonical = ListeningHistoryBackupRepository(database).export()
        assertEquals(2, canonical.identities.size)
        assertEquals(2, canonical.bindings.size)
        assertEquals(listOf(3, 4), canonical.baselines.map { it.historicalPlayCount })
        assertEquals(listOf(10L, 20L), canonical.baselines.map { it.firstKnownPlayedAt })
        assertEquals(listOf(30L, 40L), canonical.baselines.map { it.lastKnownPlayedAt })
        assertTrue(canonical.events.isEmpty())
        assertNotEquals(canonical.identities[0].backupIdentityId, canonical.identities[1].backupIdentityId)
        assertEquals(2, database.songPlayStatsDao().getRecentlyPlayed().size)
        assertEquals(1, favoritesRepository.getFavoritesForBackup().size)
        assertEquals("Saved", playlistsRepository.getPlaylists().single().name)
        assertEquals(1, playlistsRepository.getPlaylistsForBackup().single().songs.size)
    }

    private companion object {
        val V6_FIXTURE = """
            {
              "schemaVersion": 6,
              "createdAt": 999,
              "favorites": [{"songKey":"favorite","title":"Favorite","artist":"Artist","album":"Album","duration":1000,"createdAt":9}],
              "playlists": [{"name":"Saved","createdAt":1,"updatedAt":2,"songs":[{"songKey":"playlist","position":0,"title":"Playlist","artist":"Artist","album":"Album","duration":1000,"addedAt":3}]}],
              "listeningHistory": [
                {"songKey":"one","title":"Same","artist":"Artist","album":"Album","duration":1000,"playCount":3,"firstPlayedAt":10,"lastPlayedAt":30},
                {"songKey":"two","title":"Same","artist":"Artist","album":"Album","duration":1000,"playCount":4,"firstPlayedAt":20,"lastPlayedAt":40}
              ]
            }
        """.trimIndent()
    }
}
