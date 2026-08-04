package com.example.cdplaya

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cdplaya.data.backup.BackupSongRating
import com.example.cdplaya.data.backup.BackupSongRatings
import com.example.cdplaya.data.backup.ListeningHistoryBackupRepository
import com.example.cdplaya.data.backup.SongRatingBackupValidator
import com.example.cdplaya.data.local.AppDatabase
import com.example.cdplaya.data.local.ListeningTrackIdentityEntity
import com.example.cdplaya.data.local.LocalTrackBindingEntity
import com.example.cdplaya.data.local.SongRatingEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SongRatingBackupRoundTripTest {
    private lateinit var source: AppDatabase
    private lateinit var target: AppDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        source = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        target = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        source.close()
        target.close()
    }

    @Test
    fun canonicalExportAndRestoreRemapBoundUnboundAndDuplicateLookingRatings() = runBlocking {
        val first = source.listeningTrackIdentityDao().insert(identity(createdAt = 10L))
        val second = source.listeningTrackIdentityDao().insert(identity(createdAt = 20L))
        source.localTrackBindingDao().insert(binding(first))
        source.songRatingDao().upsert(SongRatingEntity(first, 1, 100L, 100L))
        source.songRatingDao().upsert(SongRatingEntity(second, 5, 200L, 250L))
        val exported = ListeningHistoryBackupRepository(source).exportWithRatings()
        assertEquals(listOf(first, second), exported.ratings.entries.map { it.trackIdentityBackupId })

        target.listeningTrackIdentityDao().insert(identity(createdAt = 999L))
        val targetRepository = ListeningHistoryBackupRepository(target)
        target.withTransaction {
            val idMap = targetRepository.restoreValidatedWithinTransaction(exported.history)
            targetRepository.restoreRatingsValidatedWithinTransaction(exported.ratings, idMap)
        }

        val restoredIdentities = target.listeningTrackIdentityDao().getAll().associateBy { it.createdAt }
        val restoredFirstId = restoredIdentities.getValue(10L).id
        val restoredSecondId = restoredIdentities.getValue(20L).id
        assertEquals(1, target.songRatingDao().getByTrackIdentityId(restoredFirstId)?.rating)
        assertEquals(5, target.songRatingDao().getByTrackIdentityId(restoredSecondId)?.rating)
        assertEquals(100L, target.songRatingDao().getByTrackIdentityId(restoredFirstId)?.ratedAt)
        assertEquals(250L, target.songRatingDao().getByTrackIdentityId(restoredSecondId)?.updatedAt)
        assertEquals(1, target.localTrackBindingDao().getForTrackIdentity(restoredFirstId).size)
        assertEquals(0, target.localTrackBindingDao().getForTrackIdentity(restoredSecondId).size)
    }

    @Test
    fun invalidRatingIsRejectedBeforeMutationAndLeavesCurrentStateUntouched() = runBlocking {
        val currentId = target.listeningTrackIdentityDao().insert(identity(createdAt = 77L))
        target.songRatingDao().upsert(SongRatingEntity(currentId, 4, 10L, 10L))
        val incomingIdentity = source.listeningTrackIdentityDao().insert(identity(createdAt = 88L))
        val exported = ListeningHistoryBackupRepository(source).exportWithRatings()
        val invalid = BackupSongRatings(
            entries = listOf(BackupSongRating(incomingIdentity, 0, 1L, 1L))
        )

        assertThrows(IllegalArgumentException::class.java) {
            SongRatingBackupValidator.validate(invalid, exported.history)
        }
        assertEquals(listOf(77L), target.listeningTrackIdentityDao().getAll().map { it.createdAt })
        assertEquals(4, target.songRatingDao().getByTrackIdentityId(currentId)?.rating)
        assertEquals(1L, target.songRatingDao().count())
    }

    private fun identity(createdAt: Long) = ListeningTrackIdentityEntity(
        titleSnapshot = "同じ曲",
        artistSnapshot = "Artist",
        albumSnapshot = "Album",
        albumArtistSnapshot = null,
        durationMsSnapshot = 60_000L,
        normalizedTitle = "同じ曲",
        normalizedArtist = "artist",
        normalizedAlbum = "album",
        metadataKey = "portable:same",
        metadataKeyVersion = 1,
        createdAt = createdAt,
        updatedAt = createdAt
    )

    private fun binding(identityId: Long) = LocalTrackBindingEntity(
        trackIdentityId = identityId,
        referenceKey = "local:$identityId",
        mediaStoreId = identityId,
        volumeName = "external",
        contentUri = "content://media/$identityId",
        relativePath = "Music/",
        displayName = "同じ曲-$identityId.flac",
        absolutePath = null,
        fileSizeBytes = 1L,
        dateModifiedEpochSeconds = 1L,
        durationMsSnapshot = 60_000L,
        legacyStableKey = null,
        portableKey = "portable:same",
        portableKeyVersion = 1,
        firstSeenAt = 1L,
        lastSeenAt = 1L,
        missingSince = null
    )
}
