package com.example.cdplaya.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.example.cdplaya.player.audio.AudioOffloadPreference
import org.junit.Assert.fail
import org.junit.Test

class AppBackupJsonTest {
    @Test
    fun encodeBackup_includesCurrentSchemaVersion() {
        val encoded = AppBackupJson.encodeBackup(emptyBackup())

        assertTrue(encoded.contains("\"schemaVersion\": 4"))
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
        assertEquals(
            AudioOffloadPreference.DISABLED,
            AudioOffloadPreference.fromStorageValue(decoded.preferences.audioOffloadPreference)
        )
    }

    @Test
    fun decodeBackup_migratesV1PreferencesAndReferencesToV4() {
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

        assertEquals(4, decoded.schemaVersion)
        assertEquals("slide", decoded.preferences.modernArtworkTransitionStyle)
        assertEquals("classic_bar", decoded.preferences.modernSeekbarStyle)
        assertEquals(emptyMap<String, BackupPlayerThemeTokenOverrides>(), decoded.preferences.playerThemeTokenOverrides)
        assertEquals("list", decoded.preferences.songsViewMode)
        assertEquals(2, decoded.preferences.songsGridColumnCount)
        assertEquals("classic_wheel", decoded.preferences.selectedPlayerThemeId)
        assertEquals(
            BackupEqualizerPreferences(),
            decoded.preferences.equalizer
        )
    }

    @Test
    fun v4Backup_roundTripsAllDurablePreferenceAndReferenceFields() {
        val preferences = BackupPreferences(
            selectedLibraryFolders = listOf("Music"),
            selectedPlayerThemeId = "retro_rack",
            replayGainMode = "TRACK",
            audioOffloadPreference = "AUTOMATIC",
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
        val reference = BackupSongReference(
            relativePath = "Music/Album/",
            displayName = "track.flac",
            fileSizeBytes = 42L,
            duration = 1_000L,
            title = "Track",
            artist = "Artist",
            album = "Album",
            legacyStableKey = "legacy",
            portableKey = "portable:v1:key"
        )
        val backup = emptyBackup().copy(
            preferences = preferences,
            favorites = listOf(
                BackupFavoriteSong("legacy", "Track", "Artist", "Album", 1_000L, 3L, reference)
            )
        )

        val decoded = AppBackupJson.decodeBackup(AppBackupJson.encodeBackup(backup))

        assertEquals(preferences, decoded.preferences)
        assertEquals(reference, decoded.favorites.single().reference)
    }

    @Test
    fun v2FixtureStillRestoresAsLegacyReference() {
        val decoded = AppBackupJson.decodeBackup(
            """
            {
              "schemaVersion": 2,
              "createdAt": 123,
              "favorites": [{
                "songKey": "old-key",
                "title": "Track",
                "artist": "Artist",
                "album": "Album",
                "duration": 1000,
                "createdAt": 9
              }]
            }
            """.trimIndent()
        )

        assertEquals(4, decoded.schemaVersion)
        assertEquals("old-key", decoded.favorites.single().reference?.legacyStableKey)
    }

    @Test
    fun decodeBackup_rejectsUnsupportedSchemaVersion() {
        val exception = expectIllegalArgumentException {
            AppBackupJson.decodeBackup(
                AppBackupJson.encodeBackup(emptyBackup().copy(schemaVersion = 5))
            )
        }

        assertTrue(exception.message.orEmpty().contains("Unsupported CDPlaya backup schema version 5"))
    }

    @Test
    fun emptyBackup_roundTrips() {
        val backup = emptyBackup()

        assertEquals(backup, AppBackupJson.decodeBackup(AppBackupJson.encodeBackup(backup)))
    }

    @Test
    fun v4EqualizerAndUserPresetsRoundTripWithoutRuntimeState() {
        val equalizer = BackupEqualizerPreferences(
            enabled = true,
            preampDb = -2.5,
            automaticHeadroomEnabled = false,
            bandGainsDb = listOf(
                4.0, 3.5, 2.5, 1.0, 0.0,
                -0.5, -1.0, -1.5, -2.0, -2.5
            ),
            userPresets = listOf(
                BackupEqualizerPreset(
                    id = "stable-id",
                    name = "Road",
                    preampDb = -1.0,
                    automaticHeadroomEnabled = true,
                    bandGainsDb = List(10) { index ->
                        index / 10.0
                    }
                )
            )
        )
        val encoded = AppBackupJson.encodeBackup(
            emptyBackup().copy(
                preferences = BackupPreferences(
                    equalizer = equalizer
                )
            )
        )
        val decoded = AppBackupJson.decodeBackup(encoded)

        assertEquals(
            equalizer,
            decoded.preferences.equalizer
        )
        assertTrue(!encoded.contains("comparisonBypassed"))
        assertTrue(!encoded.contains("runtimeState"))
        assertTrue(!encoded.contains("Bass Lift"))
    }

    @Test
    fun malformedEqualizerBandCountsAndReservedNamesAreRejected() {
        val malformed = emptyBackup().copy(
            preferences = BackupPreferences(
                equalizer = BackupEqualizerPreferences(
                    bandGainsDb = List(9) { 0.0 }
                )
            )
        )
        expectIllegalArgumentException {
            AppBackupJson.decodeBackup(
                AppBackupJson.encodeBackup(malformed)
            )
        }

        val reserved = emptyBackup().copy(
            preferences = BackupPreferences(
                equalizer = BackupEqualizerPreferences(
                    userPresets = listOf(
                        BackupEqualizerPreset(
                            id = "id",
                            name = "bass lift",
                            preampDb = 0.0,
                            automaticHeadroomEnabled = true,
                            bandGainsDb = List(10) { 0.0 }
                        )
                    )
                )
            )
        )
        expectIllegalArgumentException {
            AppBackupJson.decodeBackup(
                AppBackupJson.encodeBackup(reserved)
            )
        }
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

    @Test
    fun portableSongReferenceOmitsAbsoluteAndDeviceLocalPaths() {
        val reference = com.example.cdplaya.data.SongReference(
            mediaStoreId = 44L,
            contentUri = "content://media/external/audio/44",
            relativePath = "/storage/emulated/0/Music",
            displayName = "track.flac",
            title = "Track",
            artist = "Artist",
            album = "Album",
            duration = 1_000L,
            portableKey = "portable:v1:key"
        ).toBackupSongReference()
        val encoded = AppBackupJson.encodeBackup(
            emptyBackup().copy(
                preferences = BackupPreferences(selectedLibraryFolders = listOf("/private/music")),
                favorites = listOf(
                    BackupFavoriteSong("legacy", "Track", "Artist", "Album", 1_000L, 1L, reference)
                )
            )
        )

        assertEquals("", reference.relativePath)
        assertTrue(!encoded.contains("/storage/"))
        assertTrue(!encoded.contains("/private/"))
        assertTrue(encoded.contains("private/music"))
        assertTrue(!encoded.contains("content://"))
        assertTrue(!encoded.contains("mediaStoreId"))
    }

    @Test
    fun folderSelectionsBecomePortableRelativeTokens() {
        assertEquals("Music/Rock", "/storage/emulated/0/Music/Rock".toPortableFolderSelection())
        assertEquals("Music/Rock", "/storage/ABCD-1234/Music/Rock".toPortableFolderSelection())
        assertEquals("Music/Rock", "/sdcard/Music/Rock".toPortableFolderSelection())
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
