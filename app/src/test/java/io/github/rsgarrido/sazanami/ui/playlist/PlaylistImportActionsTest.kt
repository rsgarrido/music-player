package io.github.rsgarrido.sazanami.ui.playlist

import android.content.res.Resources
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.data.playlistfile.PlaylistImportResult
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class PlaylistImportActionsTest {
    @Test
    fun importResultMessage_reportsImportedAndUnmatchedCounts() {
        val resources = mock(Resources::class.java)
        `when`(resources.getQuantityString(R.plurals.playlist_import_many_songs_with_unmatched, 3, 12, "Road Trip", 3))
            .thenReturn("localized")
        assertEquals(
            "localized",
            importResultMessage(
                resources,
                PlaylistImportResult(
                    playlistName = "Road Trip",
                    importedSongCount = 12,
                    unmatchedEntryCount = 3
                )
            )
        )
        verify(resources).getQuantityString(R.plurals.playlist_import_many_songs_with_unmatched, 3, 12, "Road Trip", 3)
    }

    @Test
    fun importResultMessage_reportsNoMatches() {
        val resources = mock(Resources::class.java)
        `when`(resources.getString(R.string.playlist_import_no_matches)).thenReturn("localized")
        assertEquals(
            "localized",
            importResultMessage(
                resources,
                PlaylistImportResult(
                    playlistName = null,
                    importedSongCount = 0,
                    unmatchedEntryCount = 4
                )
            )
        )
        verify(resources).getString(R.string.playlist_import_no_matches)
    }
}
