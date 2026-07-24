package com.example.cdplaya.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsScreenTest {
    @Test
    fun copiedSummaryContainsUsefulStateWithoutFilePaths() {
        val summary = formatDiagnosticsSummary(
            DiagnosticsSnapshot(
                appVersionName = "1.0",
                appVersionCode = 1,
                librarySongCount = 42,
                selectedFolderCount = 2,
                playerTheme = "Retro Rack",
                replayGainMode = "Track",
                isPlaybackConnected = true,
                currentSongTitle = "Example",
                currentSongArtist = "Artist",
                isPlaying = true,
                currentPositionMs = 12_000,
                durationMs = 180_000,
                queueCount = 3,
                upcomingCount = 9,
                previousCount = 2,
                forwardCount = 1,
                waveformFileCount = 5,
                waveformTotalBytes = 4096,
                unresolvedFavoriteCount = 1,
                unresolvedPlaylistRowCount = 2,
                unresolvedListeningHistoryCount = 3
            )
        )

        assertTrue(summary.contains("Library songs: 42"))
        assertTrue(summary.contains("Current media: Present"))
        assertFalse(summary.contains("Example"))
        assertFalse(summary.contains("Artist"))
        assertTrue(summary.contains("Audio source: Unknown"))
        assertTrue(summary.contains("Offload preference: Disabled"))
        assertTrue(summary.contains("Equalizer: Bypassed"))
        assertTrue(summary.contains("Equalizer processor: Unconfigured"))
        assertTrue(
            summary.contains("User offload preference allowed")
        )
        assertTrue(summary.contains("Source information describes"))
        assertTrue(summary.contains("Waveform cache: 5 files, 4096 bytes"))
        assertTrue(summary.contains("Unresolved favorites: 1"))
        assertTrue(summary.contains("Unresolved playlist rows: 2"))
        assertTrue(summary.contains("Unresolved history rows: 3"))
        assertFalse(summary.contains("/music/"))
        assertFalse(summary.contains("filePath"))
        assertFalse(summary.contains("bit-perfect", ignoreCase = true))
        assertFalse(summary.contains("hardware output", ignoreCase = true))
        assertFalse(summary.contains("limiter", ignoreCase = true))
        assertFalse(summary.contains("true-peak", ignoreCase = true))
        assertFalse(summary.contains("Bluetooth address", ignoreCase = true))
    }
}
