package io.github.rsgarrido.sazanami

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.rsgarrido.sazanami.data.FavoritesRepository
import io.github.rsgarrido.sazanami.data.ListeningHistoryRepository
import io.github.rsgarrido.sazanami.data.PlaybackQueueEntryDraft
import io.github.rsgarrido.sazanami.data.PlaybackQueueRepository
import io.github.rsgarrido.sazanami.data.PlaylistsRepository
import io.github.rsgarrido.sazanami.data.backup.AppBackup
import io.github.rsgarrido.sazanami.data.backup.BackupListeningHistorySummary
import io.github.rsgarrido.sazanami.data.backup.BackupListeningHistoryV2
import io.github.rsgarrido.sazanami.data.backup.BackupListeningTrackIdentity
import io.github.rsgarrido.sazanami.data.backup.BackupRepository
import io.github.rsgarrido.sazanami.data.local.AppDatabase
import io.github.rsgarrido.sazanami.data.local.ListeningTrackIdentityEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRestoreQueueTest {
    private lateinit var database: AppDatabase
    private lateinit var queues: PlaybackQueueRepository
    private lateinit var backups: BackupRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().context
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        queues = PlaybackQueueRepository(database)
        backups = BackupRepository(
            context = context,
            favoritesRepository = FavoritesRepository(database.favoriteSongDao()),
            playlistsRepository = PlaylistsRepository(database.playlistDao()),
            listeningHistoryRepository = ListeningHistoryRepository(database.songPlayStatsDao()),
            appDatabase = database
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun restoreReplacesHistoryAndClearsActiveAndNamedQueues() = runBlocking {
        seedQueues()

        backups.restoreBackup(replacementBackup())

        assertTrue(queues.listQueues().isEmpty())
        assertNull(queues.getActiveQueueId())
        assertEquals(listOf("Restored"), database.listeningTrackIdentityDao().getAll().map { it.titleSnapshot })
    }

    @Test
    fun failedHistoryWriteRollsBackQueueCleanup() = runBlocking {
        seedQueues()
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_backup_identity BEFORE INSERT ON listening_track_identities " +
                "BEGIN SELECT RAISE(ABORT, 'forced restore failure'); END"
        )

        assertTrue(runCatching { backups.restoreBackup(replacementBackup()) }.isFailure)

        assertEquals(setOf("active", "named"), queues.listQueues().map { it.queueId }.toSet())
        assertEquals("active", queues.getActiveQueueId())
        assertEquals(1, queues.loadQueue("active")?.entries?.size)
        assertEquals(1, queues.loadQueue("named")?.entries?.size)
        assertEquals(listOf("Before"), database.listeningTrackIdentityDao().getAll().map { it.titleSnapshot })
    }

    private suspend fun seedQueues() {
        val identityId = database.listeningTrackIdentityDao().insert(
            ListeningTrackIdentityEntity(
                titleSnapshot = "Before", artistSnapshot = "Artist", albumSnapshot = "Album",
                albumArtistSnapshot = null, durationMsSnapshot = 1_000,
                normalizedTitle = "before", normalizedArtist = "artist", normalizedAlbum = "album",
                metadataKey = null, metadataKeyVersion = 1, createdAt = 1, updatedAt = 1
            )
        )
        queues.createQueue(
            queueId = "active", displayName = "Current",
            entries = listOf(PlaybackQueueEntryDraft(identityId, baseOrder = 0, playbackOrder = 0))
        )
        queues.createQueue(
            queueId = "named", displayName = "Saved",
            entries = listOf(PlaybackQueueEntryDraft(identityId, baseOrder = 0, playbackOrder = 0))
        )
        queues.setActiveQueue("active")
    }

    private fun replacementBackup() = AppBackup(
        createdAt = 2,
        canonicalListeningHistory = BackupListeningHistoryV2(
            identities = listOf(
                BackupListeningTrackIdentity(
                    backupIdentityId = 1, titleSnapshot = "Restored", artistSnapshot = "Artist",
                    albumSnapshot = "Album", albumArtistSnapshot = null, durationMsSnapshot = 1_000,
                    normalizedTitle = "restored", normalizedArtist = "artist", normalizedAlbum = "album",
                    metadataKey = null, metadataKeyVersion = 1, createdAt = 2, updatedAt = 2
                )
            ),
            summary = BackupListeningHistorySummary(identityCount = 1)
        )
    )
}
