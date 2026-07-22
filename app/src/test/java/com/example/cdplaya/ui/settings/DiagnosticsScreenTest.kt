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
                waveformTotalBytes = 4096
            )
        )

        assertTrue(summary.contains("Library songs: 42"))
        assertTrue(summary.contains("Current song: Example"))
        assertTrue(summary.contains("Waveform cache: 5 files, 4096 bytes"))
        assertFalse(summary.contains("/music/"))
        assertFalse(summary.contains("filePath"))
    }
}
