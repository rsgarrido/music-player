package com.example.cdplaya.data

import com.example.cdplaya.data.backup.BackupFavoriteSong
import com.example.cdplaya.data.backup.BackupSongReference
import com.example.cdplaya.data.backup.restoredReferenceKey
import com.example.cdplaya.data.backup.toBackupSongReference
import com.example.cdplaya.data.backup.toSongReference
import com.example.cdplaya.data.local.FavoriteSongDao
import com.example.cdplaya.data.local.FavoriteSongEntity

class FavoritesRepository(
    private val favoriteSongDao: FavoriteSongDao
) {
    suspend fun getFavoriteSongKeys(): Set<String> {
        return favoriteSongDao.getAllFavorites().mapTo(mutableSetOf()) { it.referenceKey }
    }

    suspend fun reconcileSongReferences(songs: Collection<Song>): SongReferenceReconciliation {
        val memberships = mutableSetOf<String>()
        var unresolved = 0
        var ambiguous = 0
        var backfilled = 0

        favoriteSongDao.getAllFavorites().forEach { favorite ->
            when (val result = SongReferenceResolver.resolve(favorite.toSongReference(), songs)) {
                is SongReferenceResolution.Resolved -> {
                    val membershipKey = result.song.membershipKey()
                    memberships += membershipKey
                    val updated = favorite.withSongReference(result.song)
                    if (updated != favorite) {
                        persistReconciledFavorite(favorite, updated)
                        backfilled += 1
                    }
                }

                is SongReferenceResolution.Ambiguous -> ambiguous += 1
                SongReferenceResolution.NotFound -> unresolved += 1
            }
        }
        return SongReferenceReconciliation(
            resolvedMembershipKeys = memberships,
            unresolvedCount = unresolved,
            ambiguousCount = ambiguous,
            backfilledCount = backfilled
        )
    }

    suspend fun getFavoritesForBackup(): List<BackupFavoriteSong> {
        return favoriteSongDao.getAllFavorites().map { favorite ->
            BackupFavoriteSong(
                songKey = favorite.songKey,
                title = favorite.title,
                artist = favorite.artist,
                album = favorite.album,
                duration = favorite.duration,
                createdAt = favorite.createdAt,
                reference = favorite.toSongReference().toBackupSongReference()
            )
        }
    }

    suspend fun restoreFavoritesFromBackup(favorites: List<BackupFavoriteSong>) {
        favoriteSongDao.deleteAllFavorites()
        favoriteSongDao.insertFavorites(favorites.map { favorite -> favorite.toLegacyEntity() })
    }

    suspend fun addFavorite(song: Song) {
        val reference = song.toSongReference()
        favoriteSongDao.insertFavorite(
            FavoriteSongEntity(
                referenceKey = song.membershipKey(),
                songKey = reference.legacyStableKey,
                title = reference.title,
                artist = reference.artist,
                album = reference.album,
                duration = reference.duration,
                createdAt = System.currentTimeMillis(),
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
        )
    }

    suspend fun removeFavorite(song: Song) {
        favoriteSongDao.deleteFavoriteByReferenceKey(song.membershipKey())
        favoriteSongDao.deleteFavoriteByKey(song.stableKey())
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
        favoriteSongDao.getAllFavorites().forEach { favorite ->
            if (SongReferenceResolver.resolve(favorite.toSongReference(), listOf(originalSong))
                is SongReferenceResolution.Resolved
            ) {
                persistReconciledFavorite(favorite, favorite.withSongReference(updatedSong))
            }
        }
    }

    private suspend fun persistReconciledFavorite(
        old: FavoriteSongEntity,
        updated: FavoriteSongEntity
    ) {
        if (old.referenceKey != updated.referenceKey &&
            favoriteSongDao.countFavoriteByReferenceKey(updated.referenceKey) > 0
        ) {
            favoriteSongDao.deleteFavoriteByReferenceKey(old.referenceKey)
            return
        }
        favoriteSongDao.insertFavorite(updated)
        if (old.referenceKey != updated.referenceKey) {
            favoriteSongDao.deleteFavoriteByReferenceKey(old.referenceKey)
        }
    }
}

private fun BackupFavoriteSong.toLegacyEntity(): FavoriteSongEntity {
    val backupReference = reference ?: BackupSongReference(
        duration = duration,
        title = title,
        artist = artist,
        album = album,
        legacyStableKey = songKey,
        portableKey = portableMetadataKey(title, artist, album, duration).orEmpty()
    )
    val restoredReference = backupReference.toSongReference()
    return FavoriteSongEntity(
    referenceKey = backupReference.restoredReferenceKey(),
    songKey = restoredReference.legacyStableKey.ifBlank { songKey },
    title = restoredReference.title.ifBlank { title },
    artist = restoredReference.artist.ifBlank { artist },
    album = restoredReference.album.ifBlank { album },
    duration = restoredReference.duration.takeIf { it > 0L } ?: duration,
    createdAt = createdAt,
    mediaStoreId = null,
    volumeName = restoredReference.volumeName,
    contentUri = restoredReference.contentUri,
    relativePath = restoredReference.relativePath,
    displayName = restoredReference.displayName,
    fileSizeBytes = restoredReference.fileSizeBytes,
    dateModifiedEpochSeconds = 0L,
    albumArtist = restoredReference.albumArtist,
    portableKey = restoredReference.portableKey,
    portableKeyVersion = restoredReference.portableKeyVersion
    )
}
