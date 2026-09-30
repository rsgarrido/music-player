package io.github.rsgarrido.sazanami.ui.settings

import io.github.rsgarrido.sazanami.data.backup.BackupExportResult
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupExportActionsTest {
    @Test
    fun backupFilename_usesProvidedLocalDateAndPackageExtension() {
        assertEquals(
            "sazanami-backup-2026-07-15.sazanami",
            backupFilename(LocalDate.of(2026, 7, 15))
        )
    }

    @Test
    fun backupExportPresentation_preservesAllCountsForLocalizedFormatting() {
        assertEquals(
            BackupPresentationCounts(24, 3, 120, 86, 0, 4),
            BackupExportResult(
                favoriteCount = 24,
                playlistCount = 3,
                playlistSongCount = 120,
                listeningHistoryCount = 86,
                visualAssetCount = 4
            ).presentationCounts()
        )
    }
}
