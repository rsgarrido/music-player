package io.github.rsgarrido.sazanami.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.data.membershipKey
import io.github.rsgarrido.sazanami.ui.MusicOverlayState

/** Prepare outside PlayerMorphHost so its content carries only the More dispatcher. */
@Composable
internal fun rememberNowPlayingMoreActionDispatcher(
    currentSong: Song?,
    librarySongs: List<Song>,
    favoriteMembershipKeys: Set<String>,
    canPresent: Boolean,
    overlayState: MusicOverlayState,
    onToggleFavorite: (Song) -> Unit,
    onOpenAlbum: (Song) -> Unit,
    onOpenLyrics: () -> Unit,
    onFavoriteFeedback: (NowPlayingFavoriteFeedback) -> Unit,
    onOpenArtist: (Song) -> Unit,
    onTrackInfoClick: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onRateSong: (Song) -> Unit,
    onOpenSleepTimer: () -> Unit
): (NowPlayingAction, Song) -> Unit {
    val latestSong = rememberUpdatedState(currentSong)
    val latestLibrary = rememberUpdatedState(librarySongs)
    val latestFavorites = rememberUpdatedState(favoriteMembershipKeys)
    val latestCanPresent = rememberUpdatedState(canPresent)
    val latestToggleFavorite = rememberUpdatedState(onToggleFavorite)
    val latestOpenAlbum = rememberUpdatedState(onOpenAlbum)
    val latestOpenLyrics = rememberUpdatedState(onOpenLyrics)
    val latestFavoriteFeedback = rememberUpdatedState(onFavoriteFeedback)
    val latestOpenArtist = rememberUpdatedState(onOpenArtist)
    val latestTrackInfo = rememberUpdatedState(onTrackInfoClick)
    val latestAddToPlaylist = rememberUpdatedState(onAddToPlaylist)
    val latestRateSong = rememberUpdatedState(onRateSong)
    val latestOpenSleepTimer = rememberUpdatedState(onOpenSleepTimer)

    return remember(overlayState) {
        { action: NowPlayingAction, target: Song ->
            val song = latestSong.value
            if (isCurrentNowPlayingTarget(
                    overlayState.currentNowPlayingMoreTarget(song), target
                ) && latestCanPresent.value
            ) {
                performNowPlayingAction(
                    action = action,
                    target = target,
                    currentSong = song,
                    librarySongs = latestLibrary.value,
                    onDismiss = overlayState::dismissNowPlayingMore,
                    onToggleFavorite = latestToggleFavorite.value,
                    onOpenAlbum = latestOpenAlbum.value,
                    onOpenLyrics = latestOpenLyrics.value,
                    isFavorite = target.membershipKey() in latestFavorites.value,
                    onFavoriteFeedback = latestFavoriteFeedback.value,
                    onOpenArtist = latestOpenArtist.value,
                    onTrackInfoClick = latestTrackInfo.value,
                    onAddToPlaylist = latestAddToPlaylist.value,
                    onRateSong = latestRateSong.value,
                    onOpenSleepTimer = latestOpenSleepTimer.value
                )
            } else {
                overlayState.dismissNowPlayingMore()
            }
            Unit
        }
    }
}
