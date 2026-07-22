package com.example.cdplaya.data

import com.example.cdplaya.data.backup.BackupListeningHistoryEntry
import com.example.cdplaya.data.local.SongPlayStatsDao
import com.example.cdplaya.data.local.SongPlayStatsEntity
import kotlin.math.max
import kotlin.math.min

class ListeningHistoryRepository(
    private val songPlayStatsDao: SongPlayStatsDao
) {
    suspend fun getRecentlyPlayed(): List<ListeningHistoryEntry> =
        songPlayStatsDao.getRecentlyPlayed().map { it.toListeningHistoryEntry() }

    suspend fun getMostPlayed(): List<ListeningHistoryEntry> =
        songPlayStatsDao.getMostPlayed().map { it.toListeningHistoryEntry() }

    suspend fun getListeningHistoryForBackup(): List<BackupListeningHistoryEntry> {
        return songPlayStatsDao.getRecentlyPlayed().map { stats ->
            BackupListeningHistoryEntry(
                songKey = stats.songKey,
                title = stats.title,
                artist = stats.artist,
                album = stats.album,
                duration = stats.duration,
                playCount = stats.playCount,
                firstPlayedAt = stats.firstPlayedAt,
                lastPlayedAt = stats.lastPlayedAt
            )
        }
    }

    suspend fun restoreListeningHistoryFromBackup(
        listeningHistory: List<BackupListeningHistoryEntry>
    ) {
        songPlayStatsDao.deleteAllStats()
        songPlayStatsDao.insertOrReplaceStats(listeningHistory.map { it.toLegacyEntity() })
    }

    suspend fun recordSongPlay(song: Song) {
        val referenceKey = song.membershipKey()
        val now = System.currentTimeMillis()
        val existingStats = songPlayStatsDao.getStatsByReferenceKey(referenceKey)
        val updatedStats = if (existingStats == null) {
            newStats(song, referenceKey, now)
        } else {
            existingStats.withSongReference(song).copy(
                playCount = existingStats.playCount + 1,
                lastPlayedAt = now
            )
        }
        songPlayStatsDao.insertOrReplaceStats(updatedStats)
    }

    suspend fun updateSongReferenceAfterTagEdit(
        originalSong: Song,
        editedTags: EditableSongTags
    ) {
        val updatedSong = originalSong.copy(
            title = editedTags.title.trim(),
            artist = editedTags.artist.trim(),
            album = editedTags.album.trim()
        )
        songPlayStatsDao.getRecentlyPlayed().forEach { stats ->
            if (SongReferenceResolver.resolve(stats.toSongReference(), listOf(originalSong))
                is SongReferenceResolution.Resolved
            ) {
                persistReconciledStats(stats, stats.withSongReference(updatedSong))
            }
        }
    }

    suspend fun reconcileSongReferences(songs: Collection<Song>): SongReferenceReconciliation {
        var unresolved = 0
        var ambiguous = 0
        var backfilled = 0
        songPlayStatsDao.getRecentlyPlayed().forEach { stats ->
            when (val result = SongReferenceResolver.resolve(stats.toSongReference(), songs)) {
                is SongReferenceResolution.Resolved -> {
                    val updated = stats.withSongReference(result.song)
                    if (updated != stats) {
                        persistReconciledStats(stats, updated)
                        backfilled += 1
                    }
                }

                is SongReferenceResolution.Ambiguous -> ambiguous += 1
                SongReferenceResolution.NotFound -> unresolved += 1
            }
        }
        return SongReferenceReconciliation(
            unresolvedCount = unresolved,
            ambiguousCount = ambiguous,
            backfilledCount = backfilled
        )
    }

    private suspend fun persistReconciledStats(
        old: SongPlayStatsEntity,
        updated: SongPlayStatsEntity
    ) {
        val existingTarget = if (old.referenceKey == updated.referenceKey) null else {
            songPlayStatsDao.getStatsByReferenceKey(updated.referenceKey)
        }
        val finalStats = if (existingTarget == null) updated else {
            updated.copy(
                playCount = existingTarget.playCount + old.playCount,
                firstPlayedAt = min(existingTarget.firstPlayedAt, old.firstPlayedAt),
                lastPlayedAt = max(existingTarget.lastPlayedAt, old.lastPlayedAt)
            )
        }
        songPlayStatsDao.insertOrReplaceStats(finalStats)
        if (old.referenceKey != finalStats.referenceKey) {
            songPlayStatsDao.deleteStatsByReferenceKey(old.referenceKey)
        }
    }

    private fun SongPlayStatsEntity.toListeningHistoryEntry() = ListeningHistoryEntry(
        songKey = songKey,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        playCount = playCount,
        firstPlayedAt = firstPlayedAt,
        lastPlayedAt = lastPlayedAt,
        reference = toSongReference()
    )
}

private fun newStats(song: Song, referenceKey: String, now: Long): SongPlayStatsEntity {
    val reference = song.toSongReference()
    return SongPlayStatsEntity(
        referenceKey = referenceKey,
        songKey = reference.legacyStableKey,
        title = reference.title,
        artist = reference.artist,
        album = reference.album,
        duration = reference.duration,
        playCount = 1,
        firstPlayedAt = now,
        lastPlayedAt = now,
        mediaStoreId = reference.mediaStoreId,
        volumeName = reference.volumeName,
        contentUri = reference.contentUri,
        relativePath = reference.relativePath,
        displayName = reference.displayName,
        fileSizeBytes = reference.fileSizeBytes,
        dateModifiedEpochSeconds = reference.dateModifiedEpochSeconds,
        albumArtist = reference.albumArtist,
        portableKey = reference.portableKey,
        portableKeyVersion = reference.portableKeyVersion
    )
}

private fun BackupListeningHistoryEntry.toLegacyEntity() = SongPlayStatsEntity(
    referenceKey = "legacy:$songKey",
    songKey = songKey,
    title = title,
    artist = artist,
    album = album,
    duration = duration,
    playCount = playCount,
    firstPlayedAt = firstPlayedAt,
    lastPlayedAt = lastPlayedAt,
    mediaStoreId = null,
    volumeName = "",
    contentUri = "",
    relativePath = "",
    displayName = "",
    fileSizeBytes = 0L,
    dateModifiedEpochSeconds = 0L,
    albumArtist = "",
    portableKey = portableMetadataKey(title, artist, album, duration).orEmpty(),
    portableKeyVersion = SongIdentity.PORTABLE_KEY_VERSION
)
