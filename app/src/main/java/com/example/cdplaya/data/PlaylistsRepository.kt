package com.example.cdplaya.data

import com.example.cdplaya.data.backup.BackupPlaylist
import com.example.cdplaya.data.backup.BackupPlaylistSong
import com.example.cdplaya.data.local.PlaylistDao
import com.example.cdplaya.data.local.PlaylistEntity
import com.example.cdplaya.data.local.PlaylistSongEntity
import java.util.Locale

class PlaylistsRepository(
    private val playlistDao: PlaylistDao
) {
    suspend fun getPlaylists(): List<Playlist> {
        return playlistDao.getPlaylistsWithSongCount().map { playlist ->
            Playlist(
                playlistId = playlist.playlistId,
                name = playlist.name,
                songCount = playlist.songCount
            )
        }
    }

    suspend fun getPlaylistsForBackup(): List<BackupPlaylist> {
        val songsByPlaylistId = playlistDao.getAllPlaylistSongEntities()
            .groupBy { playlistSong -> playlistSong.playlistId }

        return playlistDao.getAllPlaylistEntities().map { playlist ->
            BackupPlaylist(
                name = playlist.name,
                createdAt = playlist.createdAt,
                updatedAt = playlist.updatedAt,
                songs = songsByPlaylistId[playlist.playlistId]
                    .orEmpty()
                    .map { playlistSong ->
                        BackupPlaylistSong(
                            songKey = playlistSong.songKey,
                            position = playlistSong.position,
                            title = playlistSong.title,
                            artist = playlistSong.artist,
                            album = playlistSong.album,
                            duration = playlistSong.duration,
                            addedAt = playlistSong.addedAt
                        )
                    }
            )
        }
    }

    suspend fun restorePlaylistsFromBackup(playlists: List<BackupPlaylist>) {
        playlistDao.deleteAllPlaylistSongs()
        playlistDao.deleteAllPlaylists()

        val restoredNames = mutableListOf<String>()

        playlists.forEach { playlist ->
            val uniqueName = uniquePlaylistName(
                preferredName = playlist.name,
                existingNames = restoredNames
            )
            restoredNames += uniqueName

            val newPlaylistId = playlistDao.insertPlaylist(
                PlaylistEntity(
                    name = uniqueName,
                    createdAt = playlist.createdAt,
                    updatedAt = playlist.updatedAt
                )
            )

            if (playlist.songs.isNotEmpty()) {
                playlistDao.insertPlaylistSongs(
                    playlist.songs.map { playlistSong ->
                        PlaylistSongEntity(
                            playlistId = newPlaylistId,
                            songKey = playlistSong.songKey,
                            position = playlistSong.position,
                            title = playlistSong.title,
                            artist = playlistSong.artist,
                            album = playlistSong.album,
                            duration = playlistSong.duration,
                            addedAt = playlistSong.addedAt,
                            mediaStoreId = null,
                            volumeName = "",
                            contentUri = "",
                            relativePath = "",
                            displayName = "",
                            fileSizeBytes = 0L,
                            dateModifiedEpochSeconds = 0L,
                            albumArtist = "",
                            portableKey = portableMetadataKey(
                                playlistSong.title,
                                playlistSong.artist,
                                playlistSong.album,
                                playlistSong.duration
                            ).orEmpty(),
                            portableKeyVersion = SongIdentity.PORTABLE_KEY_VERSION
                        )
                    }
                )
            }
        }
    }

    suspend fun getPlaylistName(playlistId: Long): String {
        return playlistDao.getPlaylistById(playlistId)?.name ?: "Playlist"
    }

    suspend fun getPlaylistSongs(playlistId: Long): List<PlaylistSong> {
        return playlistDao.getPlaylistSongs(playlistId).map { playlistSong ->
            PlaylistSong(
                playlistSongId = playlistSong.playlistSongId,
                playlistId = playlistSong.playlistId,
                songKey = playlistSong.songKey,
                position = playlistSong.position,
                title = playlistSong.title,
                artist = playlistSong.artist,
                album = playlistSong.album,
                duration = playlistSong.duration,
                reference = playlistSong.toSongReference()
            )
        }
    }

    suspend fun createPlaylist(name: String): Boolean {
        return createPlaylistReturningId(name) != null
    }

    suspend fun createPlaylistReturningId(name: String): Long? {
        val trimmedName = name.trim()

        if (trimmedName.isBlank()) {
            return null
        }

        val playlistNameAlreadyExists =
            playlistDao.countPlaylistsWithName(trimmedName) > 0

        if (playlistNameAlreadyExists) {
            return null
        }

        val now = System.currentTimeMillis()

        return playlistDao.insertPlaylist(
            PlaylistEntity(
                name = trimmedName,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun createPlaylistWithUniqueName(
        preferredName: String,
        songs: List<Song>
    ): Playlist {
        require(songs.isNotEmpty()) {
            "Cannot create an imported playlist without songs."
        }

        val uniqueName = uniquePlaylistName(
            preferredName = preferredName,
            existingNames = getPlaylists().map { playlist ->
                playlist.name
            }
        )
        val playlistId = checkNotNull(createPlaylistReturningId(uniqueName)) {
            "Unable to create imported playlist."
        }

        try {
            addSongsToPlaylist(
                playlistId = playlistId,
                songs = songs
            )
        } catch (exception: Exception) {
            playlistDao.deletePlaylist(playlistId)
            throw exception
        }

        return Playlist(
            playlistId = playlistId,
            name = uniqueName,
            songCount = songs.size
        )
    }

    suspend fun renamePlaylist(
        playlistId: Long,
        newName: String
    ): Boolean {
        val trimmedName = newName.trim()

        if (trimmedName.isBlank()) {
            return false
        }

        val playlistNameAlreadyExists =
            playlistDao.countOtherPlaylistsWithName(
                playlistId = playlistId,
                name = trimmedName
            ) > 0

        if (playlistNameAlreadyExists) {
            return false
        }

        playlistDao.renamePlaylist(
            playlistId = playlistId,
            name = trimmedName,
            updatedAt = System.currentTimeMillis()
        )

        return true
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(
        playlistId: Long,
        song: Song
    ) {
        addSongsToPlaylist(
            playlistId = playlistId,
            songs = listOf(song)
        )
    }

    suspend fun addSongsToPlaylist(
        playlistId: Long,
        songs: List<Song>
    ): Int {
        if (songs.isEmpty()) {
            return 0
        }

        val now = System.currentTimeMillis()
        val firstPosition = playlistDao.getLastPositionForPlaylist(playlistId) + 1

        val playlistSongEntities = songs.mapIndexed { index, song ->
            playlistSongEntity(
                playlistId = playlistId,
                position = firstPosition + index,
                song = song,
                addedAt = now
            )
        }

        playlistDao.insertPlaylistSongs(playlistSongEntities)

        playlistDao.updatePlaylistTimestamp(
            playlistId = playlistId,
            updatedAt = now
        )

        return songs.size
    }

    suspend fun removePlaylistSong(
        playlistId: Long,
        playlistSongId: Long
    ) {
        playlistDao.deletePlaylistSong(playlistSongId)
        playlistDao.updatePlaylistTimestamp(
            playlistId = playlistId,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun movePlaylistSongUp(
        playlistId: Long,
        playlistSongId: Long
    ) {
        val playlistSongs = playlistDao.getPlaylistSongs(playlistId)
        val currentIndex = playlistSongs.indexOfFirst { playlistSong ->
            playlistSong.playlistSongId == playlistSongId
        }

        if (currentIndex <= 0) {
            return
        }

        val currentPlaylistSong = playlistSongs[currentIndex]
        val previousPlaylistSong = playlistSongs[currentIndex - 1]

        swapPlaylistSongPositions(
            playlistId = playlistId,
            firstPlaylistSong = currentPlaylistSong,
            secondPlaylistSong = previousPlaylistSong
        )
    }

    suspend fun movePlaylistSongDown(
        playlistId: Long,
        playlistSongId: Long
    ) {
        val playlistSongs = playlistDao.getPlaylistSongs(playlistId)
        val currentIndex = playlistSongs.indexOfFirst { playlistSong ->
            playlistSong.playlistSongId == playlistSongId
        }

        if (currentIndex == -1 || currentIndex >= playlistSongs.lastIndex) {
            return
        }

        val currentPlaylistSong = playlistSongs[currentIndex]
        val nextPlaylistSong = playlistSongs[currentIndex + 1]

        swapPlaylistSongPositions(
            playlistId = playlistId,
            firstPlaylistSong = currentPlaylistSong,
            secondPlaylistSong = nextPlaylistSong
        )
    }

    private suspend fun swapPlaylistSongPositions(
        playlistId: Long,
        firstPlaylistSong: PlaylistSongEntity,
        secondPlaylistSong: PlaylistSongEntity
    ) {
        val firstPosition = firstPlaylistSong.position
        val secondPosition = secondPlaylistSong.position

        playlistDao.updatePlaylistSongPosition(
            playlistSongId = firstPlaylistSong.playlistSongId,
            position = secondPosition
        )

        playlistDao.updatePlaylistSongPosition(
            playlistSongId = secondPlaylistSong.playlistSongId,
            position = firstPosition
        )

        playlistDao.updatePlaylistTimestamp(
            playlistId = playlistId,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun updateSongReferencesAfterTagEdit(
        originalSong: Song,
        editedTags: EditableSongTags
    ) {
        val updatedSong = originalSong.copy(
            title = editedTags.title.trim(),
            artist = editedTags.artist.trim(),
            album = editedTags.album.trim()
        )
        playlistDao.getAllPlaylistSongEntities().forEach { playlistSong ->
            if (SongReferenceResolver.resolve(playlistSong.toSongReference(), listOf(originalSong))
                is SongReferenceResolution.Resolved
            ) {
                playlistDao.updatePlaylistSong(playlistSong.withSongReference(updatedSong))
            }
        }
    }

    suspend fun reconcileSongReferences(songs: Collection<Song>): SongReferenceReconciliation {
        var unresolved = 0
        var ambiguous = 0
        var backfilled = 0
        playlistDao.getAllPlaylistSongEntities().forEach { playlistSong ->
            when (val result = SongReferenceResolver.resolve(playlistSong.toSongReference(), songs)) {
                is SongReferenceResolution.Resolved -> {
                    val updated = playlistSong.withSongReference(result.song)
                    if (updated != playlistSong) {
                        playlistDao.updatePlaylistSong(updated)
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
}

private fun playlistSongEntity(
    playlistId: Long,
    position: Int,
    song: Song,
    addedAt: Long
): PlaylistSongEntity {
    val reference = song.toSongReference()
    return PlaylistSongEntity(
        playlistId = playlistId,
        songKey = reference.legacyStableKey,
        position = position,
        title = reference.title,
        artist = reference.artist,
        album = reference.album,
        duration = reference.duration,
        addedAt = addedAt,
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

internal fun uniquePlaylistName(
    preferredName: String,
    existingNames: Collection<String>
): String {
    val baseName = preferredName.trim().ifBlank { "Imported Playlist" }
    val lowercaseExistingNames = existingNames.mapTo(mutableSetOf()) { name ->
        name.trim().lowercase(Locale.ROOT)
    }

    if (baseName.lowercase(Locale.ROOT) !in lowercaseExistingNames) {
        return baseName
    }

    var suffix = 2

    while (true) {
        val candidate = "$baseName ($suffix)"

        if (candidate.lowercase(Locale.ROOT) !in lowercaseExistingNames) {
            return candidate
        }

        suffix += 1
    }
}
