package io.github.rsgarrido.sazanami.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.ui.navigation.MainDestination
import io.github.rsgarrido.sazanami.ui.player.openCurrentNowPlayingArtist

/** Stable, narrow callback; latest inputs and detail cleanup stay outside PlayerMorphHost. */
@Composable
internal fun rememberNowPlayingArtistNavigation(
    currentSong: Song?,
    librarySongs: List<Song>,
    onNavigate: (String) -> Unit
): (Song) -> Unit {
    val latestSong = rememberUpdatedState(currentSong)
    val latestLibrary = rememberUpdatedState(librarySongs)
    val latestNavigate = rememberUpdatedState(onNavigate)
    return remember {
        { target: Song ->
            openCurrentNowPlayingArtist(target, latestSong.value, latestLibrary.value, latestNavigate.value)
            Unit
        }
    }
}

internal fun navigateToNowPlayingArtist(
    name: String,
    navigationState: MusicNavigationState,
    resetLyrics: () -> Unit,
    collapsePlayer: () -> Unit,
    clearLibrarySelection: () -> Unit,
    clearPlaylistSelection: () -> Unit
) {
    resetLyrics()
    collapsePlayer()
    clearLibrarySelection()
    navigationState.clearAlbum()
    navigationState.openArtist(name, DetailEntryOrigin.LIBRARY)
    navigationState.selectedGenreKey.value = null
    clearPlaylistSelection()
    navigationState.searchQuery.value = ""
    navigationState.mainDestination.value = MainDestination.LIBRARY
}
