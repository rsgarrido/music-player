package com.example.cdplaya.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AppBackupJsonTest {
    @Test
    fun encodeBackup_includesCurrentSchemaVersion() {
        val encoded = AppBackupJson.encodeBackup(emptyBackup())

        assertTrue(encoded.contains("\"schemaVersion\": 2"))
    }

    @Test
    fun encodeBackup_includesAllBackupSections() {
        val encoded = AppBackupJson.encodeBackup(emptyBackup())

        listOf(
            "schemaVersion",
            "favorites",
            "playlists",
            "listeningHistory",
            "preferences"
        ).forEach { key ->
            assertTrue("Missing JSON key: $key", encoded.contains("\"$key\""))
        }
    }

    @Test
    fun decodeBackup_decodesValidBackupAndIgnoresUnknownKeys() {
        val decoded = AppBackupJson.decodeBackup(
            """
            {
              "schemaVersion": 2,
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
    fun decodeBackup_migratesV1PreferencesToV2Defaults() {
        val decoded = AppBackupJson.decodeBackup(
            """
            {
              "schemaVersion": 1,
              "createdAt": 123,
              "preferences": {
                "selectedLibraryFolders": ["/Music"],
                "selectedPlayerThemeId": "classic_wheel",
                "replayGainMode": "OFF"
              }
            }
            """.trimIndent()
        )

        assertEquals(2, decoded.schemaVersion)
        assertEquals("slide", decoded.preferences.modernArtworkTransitionStyle)
        assertEquals("classic_bar", decoded.preferences.modernSeekbarStyle)
        assertEquals(emptyMap<String, BackupPlayerThemeTokenOverrides>(), decoded.preferences.playerThemeTokenOverrides)
        assertEquals("list", decoded.preferences.songsViewMode)
        assertEquals(2, decoded.preferences.songsGridColumnCount)
        assertEquals("classic_wheel", decoded.preferences.selectedPlayerThemeId)
    }

    @Test
    fun v2Backup_roundTripsAllDurablePreferenceFields() {
        val preferences = BackupPreferences(
            selectedLibraryFolders = listOf("/Music"),
            selectedPlayerThemeId = "retro_rack",
            replayGainMode = "TRACK",
            modernArtworkTransitionStyle = "cover_flow",
            modernSeekbarStyle = "waveform_glow",
            playerThemeTokenOverrides = mapOf(
                "retro_rack" to BackupPlayerThemeTokenOverrides(
                    shellArgb = 0xFF010203L,
                    accentArgb = 0xFFAABBCCL,
                    secondaryAccentArgb = 0xFF102030L
                )
            ),
            songsViewMode = "grid",
            albumsViewMode = "list",
            artistsViewMode = "grid",
            songsGridColumnCount = 4,
            albumsGridColumnCount = 3,
            artistsGridColumnCount = 2
        )
        val backup = emptyBackup().copy(preferences = preferences)

        val decoded = AppBackupJson.decodeBackup(AppBackupJson.encodeBackup(backup))

        assertEquals(preferences, decoded.preferences)
    }

    @Test
    fun decodeBackup_rejectsUnsupportedSchemaVersion() {
        val exception = expectIllegalArgumentException {
            AppBackupJson.decodeBackup(
                AppBackupJson.encodeBackup(emptyBackup().copy(schemaVersion = 3))
            )
        }

        assertTrue(exception.message.orEmpty().contains("Unsupported CDPlaya backup schema version 3"))
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

    @Test
    fun encodedBackup_doesNotContainWaveformOrDerivedCacheData() {
        val encoded = AppBackupJson.encodeBackup(emptyBackup())

        assertTrue(!encoded.contains("waveform", ignoreCase = true))
        assertTrue(!encoded.contains("cache", ignoreCase = true))
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
