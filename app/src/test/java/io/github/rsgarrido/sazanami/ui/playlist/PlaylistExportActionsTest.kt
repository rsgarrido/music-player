package io.github.rsgarrido.sazanami.ui.playlist

import android.content.res.Resources
import io.github.rsgarrido.sazanami.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class PlaylistExportActionsTest {
    @Test
    fun sanitizedM3uFilename_replacesInvalidCharactersAndAddsExtension() {
        assertEquals(
            "Road_Trip_2026.m3u8",
            sanitizedM3uFilename(" Road/Trip:2026 ")
        )
    }

    @Test
    fun sanitizedM3uFilename_doesNotDuplicateExtension() {
        assertEquals(
            "Favorites.m3u8",
            sanitizedM3uFilename("Favorites.M3U8")
        )
    }

    @Test
    fun exportSuccessMessage_includesSkippedSongCountWhenNeeded() {
        val resources = mock(Resources::class.java)
        `when`(resources.getQuantityString(R.plurals.playlist_export_many_songs_with_skipped, 2, 12, 2))
            .thenReturn("localized")
        assertEquals(
            "localized",
            exportSuccessMessage(
                resources = resources,
                exportedSongCount = 12,
                unavailableSongCount = 2
            )
        )
        verify(resources).getQuantityString(R.plurals.playlist_export_many_songs_with_skipped, 2, 12, 2)
    }

    @Test
    fun exportSuccessMessage_usesSingularSongLabels() {
        val resources = mock(Resources::class.java)
        `when`(resources.getQuantityString(R.plurals.playlist_export_one_song_with_skipped, 1, 1, 1))
            .thenReturn("localized")
        assertEquals(
            "localized",
            exportSuccessMessage(
                resources = resources,
                exportedSongCount = 1,
                unavailableSongCount = 1
            )
        )
        verify(resources).getQuantityString(R.plurals.playlist_export_one_song_with_skipped, 1, 1, 1)
    }
}
