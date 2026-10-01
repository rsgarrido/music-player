package io.github.rsgarrido.sazanami.ui.settings

import io.github.rsgarrido.sazanami.data.backup.BackupRestoreResult
import io.github.rsgarrido.sazanami.data.backup.BackupRestoreSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupRestoreActionsTest {
    @Test
    fun restoreSummaryPreservesAllCountsForLocalizedPluralPresentation() {
        assertEquals(
            BackupPresentationCounts(24, 3, 120, 86, 2, 4),
            BackupRestoreSummary(
                favoriteCount = 24,
                playlistCount = 3,
                playlistSongCount = 120,
                listeningHistoryCount = 86,
                selectedFolderCount = 2,
                visualAssetCount = 4
            ).presentationCounts()
        )
    }

    @Test
    fun restoreResultPreservesAllCountsForLocalizedSuccessMessage() {
        assertEquals(
            BackupPresentationCounts(1, 1, 1, 1, 1, 1),
            BackupRestoreResult(
                favoriteCount = 1,
                playlistCount = 1,
                playlistSongCount = 1,
                listeningHistoryCount = 1,
                selectedFolderCount = 1,
                visualAssetCount = 1
            ).presentationCounts()
        )
    }
}
