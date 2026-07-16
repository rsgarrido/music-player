package com.example.cdplaya.data.backup

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AppBackupJson {
    const val CURRENT_SCHEMA_VERSION = 1

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

        require(backup.schemaVersion == CURRENT_SCHEMA_VERSION) {
            "Unsupported CDPlaya backup schema version ${backup.schemaVersion}; " +
                "expected $CURRENT_SCHEMA_VERSION."
        }

        return backup
    }
}
