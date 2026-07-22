package com.example.cdplaya.data.backup

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AppBackupJson {
    const val CURRENT_SCHEMA_VERSION = 3
    private const val OLDEST_SUPPORTED_SCHEMA_VERSION = 1

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodeBackup(backup: AppBackup): String = json.encodeToString(backup.sanitizedForExport())

    fun decodeBackup(jsonText: String): AppBackup {
        val backup = try {
            json.decodeFromString<AppBackup>(jsonText)
        } catch (exception: SerializationException) {
            throw IllegalArgumentException("Invalid CDPlaya backup JSON.", exception)
        }

        require(backup.schemaVersion in OLDEST_SUPPORTED_SCHEMA_VERSION..CURRENT_SCHEMA_VERSION) {
            "Unsupported CDPlaya backup schema version ${backup.schemaVersion}; " +
                "supported versions are $OLDEST_SUPPORTED_SCHEMA_VERSION through " +
                "$CURRENT_SCHEMA_VERSION."
        }

        val v2 = if (backup.schemaVersion == 1) migrateV1ToV2(backup) else backup
        return if (v2.schemaVersion == 2) migrateV2ToV3(v2) else v2
    }

    private fun migrateV1ToV2(backup: AppBackup): AppBackup {
        return backup.copy(
            schemaVersion = 2,
            preferences = backup.preferences.copy(
                modernArtworkTransitionStyle = "slide",
                modernSeekbarStyle = "classic_bar",
                playerThemeTokenOverrides = emptyMap(),
                songsViewMode = "list",
                albumsViewMode = "list",
                artistsViewMode = "list",
                songsGridColumnCount = 2,
                albumsGridColumnCount = 2,
                artistsGridColumnCount = 2
            )
        )
    }

    private fun migrateV2ToV3(backup: AppBackup): AppBackup {
        return backup.copy(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            favorites = backup.favorites.map { favorite ->
                favorite.copy(reference = favorite.reference ?: favorite.legacyReference())
            },
            playlists = backup.playlists.map { playlist ->
                playlist.copy(
                    songs = playlist.songs.map { song ->
                        song.copy(reference = song.reference ?: song.legacyReference())
                    }
                )
            },
            listeningHistory = backup.listeningHistory.map { history ->
                history.copy(reference = history.reference ?: history.legacyReference())
            }
        )
    }
}

private fun AppBackup.sanitizedForExport(): AppBackup = copy(
    preferences = preferences.copy(
        selectedLibraryFolders = preferences.selectedLibraryFolders
            .map { it.toPortableFolderSelection() }
            .filter { it.isNotBlank() }
    ),
    favorites = favorites.map { favorite ->
        favorite.copy(reference = favorite.reference?.withoutAbsolutePath())
    },
    playlists = playlists.map { playlist ->
        playlist.copy(
            songs = playlist.songs.map { song ->
                song.copy(reference = song.reference?.withoutAbsolutePath())
            }
        )
    },
    listeningHistory = listeningHistory.map { history ->
        history.copy(reference = history.reference?.withoutAbsolutePath())
    }
)

private fun BackupSongReference.withoutAbsolutePath(): BackupSongReference = copy(
    relativePath = relativePath.takeUnless { it.isAbsolutePathLike() }.orEmpty()
)

private fun BackupFavoriteSong.legacyReference() = BackupSongReference(
    duration = duration,
    title = title,
    artist = artist,
    album = album,
    legacyStableKey = songKey
)

private fun BackupPlaylistSong.legacyReference() = BackupSongReference(
    duration = duration,
    title = title,
    artist = artist,
    album = album,
    legacyStableKey = songKey
)

private fun BackupListeningHistoryEntry.legacyReference() = BackupSongReference(
    duration = duration,
    title = title,
    artist = artist,
    album = album,
    legacyStableKey = songKey
)
