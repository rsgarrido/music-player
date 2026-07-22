package com.example.cdplaya.data.backup

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AppBackupJson {
    const val CURRENT_SCHEMA_VERSION = 2
    private const val OLDEST_SUPPORTED_SCHEMA_VERSION = 1

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodeBackup(backup: AppBackup): String = json.encodeToString(backup)

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

        return if (backup.schemaVersion == CURRENT_SCHEMA_VERSION) {
            backup
        } else {
            migrateV1ToV2(backup)
        }
    }

    private fun migrateV1ToV2(backup: AppBackup): AppBackup {
        return backup.copy(
            schemaVersion = CURRENT_SCHEMA_VERSION,
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
}
