package com.example.cdplaya.ui.navigation

import com.example.cdplaya.ui.library.LibraryTab
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackLaunchContextTest {
    @Test
    fun capturePrefersAlbumDetailOverSearch() {
        val context = capturePlaybackLaunchContext(
            mainDestination = MainDestination.LIBRARY,
            selectedLibraryTab = LibraryTab.ALBUMS,
            selectedAlbumFolderPath = "/music/album",
            selectedArtistName = null,
            selectedPlaylistId = null,
            searchQuery = "track"
        )

        assertEquals(
            PlaybackLaunchContext.AlbumDetail("/music/album"),
            context
        )
    }

    @Test
    fun capturePreservesTopLevelSearchQuery() {
        val context = capturePlaybackLaunchContext(
            mainDestination = MainDestination.LIBRARY,
            selectedLibraryTab = LibraryTab.SONGS,
            selectedAlbumFolderPath = null,
            selectedArtistName = null,
            selectedPlaylistId = null,
            searchQuery = "needle"
        )

        assertEquals(PlaybackLaunchContext.Search("needle"), context)
    }

    @Test
    fun missingDetailsFallBackToTheirParentSections() {
        val albumContext = PlaybackLaunchContext.AlbumDetail("missing")
            .withValidDetails(emptySet(), emptySet(), emptySet())
        val artistContext = PlaybackLaunchContext.ArtistDetail("missing")
            .withValidDetails(emptySet(), emptySet(), emptySet())
        val playlistContext = PlaybackLaunchContext.PlaylistDetail(42L)
            .withValidDetails(emptySet(), emptySet(), emptySet())

        assertEquals(
            PlaybackLaunchContext.LibrarySection(LibraryTab.ALBUMS),
            albumContext
        )
        assertEquals(
            PlaybackLaunchContext.LibrarySection(LibraryTab.ARTISTS),
            artistContext
        )
        assertEquals(
            PlaybackLaunchContext.LibrarySection(LibraryTab.PLAYLISTS),
            playlistContext
        )
    }
}
