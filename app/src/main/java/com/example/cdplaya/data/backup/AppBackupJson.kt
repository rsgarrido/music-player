package com.example.cdplaya.data.backup

import com.example.cdplaya.player.equalizer.GraphicEqualizerPresets
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AppBackupJson {
    const val CURRENT_SCHEMA_VERSION = 4
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

        var migrated = backup
        if (migrated.schemaVersion == 1) {
            migrated = migrateV1ToV2(migrated)
        }
        if (migrated.schemaVersion == 2) {
            migrated = migrateV2ToV3(migrated)
        }
        if (migrated.schemaVersion == 3) {
            migrated = migrateV3ToV4(migrated)
        }
        validateEqualizerBackup(migrated.preferences.equalizer)
        return migrated
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
            schemaVersion = 3,
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

    private fun migrateV3ToV4(backup: AppBackup): AppBackup {
        return backup.copy(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            preferences = backup.preferences.copy(
                equalizer = BackupEqualizerPreferences()
            )
        )
    }

    private fun validateEqualizerBackup(
        equalizer: BackupEqualizerPreferences
    ) {
        require(equalizer.bandGainsDb.size == 10) {
            "Backup equalizer must contain exactly 10 band gains."
        }
        require(
            equalizer.preampDb.isFinite() &&
                equalizer.preampDb in -15.0..6.0
        ) {
            "Backup equalizer preamp is invalid."
        }
        require(
            equalizer.bandGainsDb.all { gain ->
                gain.isFinite() && gain in -12.0..12.0
            }
        ) {
            "Backup equalizer band gain is invalid."
        }
        equalizer.userPresets.forEach { preset ->
            require(preset.bandGainsDb.size == 10) {
                "Backup equalizer preset must contain exactly 10 band gains."
            }
            require(
                preset.id.isNotBlank() &&
                    preset.name.isNotBlank() &&
                    preset.name.length <= 40 &&
                    preset.preampDb.isFinite() &&
                    preset.preampDb in -15.0..6.0 &&
                    preset.bandGainsDb.all { gain ->
                        gain.isFinite() && gain in -12.0..12.0
                    }
            ) {
                "Backup equalizer preset is invalid."
            }
        }
        require(
            equalizer.userPresets
                .map { preset -> preset.id }
                .distinct().size ==
                equalizer.userPresets.size
        ) {
            "Backup equalizer preset IDs must be unique."
        }
        val presetNames = equalizer.userPresets.map { preset ->
            preset.name.trim().lowercase()
        }
        require(presetNames.distinct().size == presetNames.size) {
            "Backup equalizer preset names must be unique."
        }
        require(
            presetNames.none { name ->
                name in GraphicEqualizerPresets
                    .builtInNamesLowercase
            }
        ) {
            "Backup equalizer preset name conflicts with a built-in."
        }
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
