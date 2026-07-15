package com.example.cdplaya.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AppBackupJsonTest {
    @Test
    fun encodeBackup_includesCurrentSchemaVersion() {
        val encoded = AppBackupJson.encodeBackup(emptyBackup())

        assertTrue(encoded.contains("\"schemaVersion\": 1"))
    }

    @Test
    fun decodeBackup_decodesValidBackupAndIgnoresUnknownKeys() {
        val decoded = AppBackupJson.decodeBackup(
            """
            {
              "schemaVersion": 1,
              "createdAt": 123,
              "appName": "CDPlaya",
              "favorites": [],
              "playlists": [],
              "listeningHistory": [],
              "preferences": {
                "selectedLibraryFolders": ["/Music"],
                "selectedPlayerThemeId": "classic",
                "replayGainMode": "album",
                "futurePreference": true
              },
              "futureField": "ignored"
            }
            """.trimIndent()
        )

        assertEquals(123L, decoded.createdAt)
        assertEquals(listOf("/Music"), decoded.preferences.selectedLibraryFolders)
        assertEquals("classic", decoded.preferences.selectedPlayerThemeId)
        assertEquals("album", decoded.preferences.replayGainMode)
    }

    @Test
    fun decodeBackup_rejectsUnsupportedSchemaVersion() {
        val exception = expectIllegalArgumentException {
            AppBackupJson.decodeBackup(
                AppBackupJson.encodeBackup(emptyBackup().copy(schemaVersion = 2))
            )
        }

        assertTrue(exception.message.orEmpty().contains("Unsupported CDPlaya backup schema version 2"))
    }

    @Test
    fun emptyBackup_roundTrips() {
        val backup = emptyBackup()

        assertEquals(backup, AppBackupJson.decodeBackup(AppBackupJson.encodeBackup(backup)))
    }

    @Test
    fun decodeBackup_rejectsInvalidJsonWithClearMessage() {
        val exception = expectIllegalArgumentException {
            AppBackupJson.decodeBackup("not json")
        }

        assertEquals("Invalid CDPlaya backup JSON.", exception.message)
    }

    private fun emptyBackup() = AppBackup(createdAt = 123L)

    private fun expectIllegalArgumentException(block: () -> Unit): IllegalArgumentException {
        try {
            block()
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            return exception
        }

        throw AssertionError("Expected IllegalArgumentException")
    }
}
