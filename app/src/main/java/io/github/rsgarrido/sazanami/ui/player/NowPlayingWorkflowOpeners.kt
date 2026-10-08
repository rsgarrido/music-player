package io.github.rsgarrido.sazanami.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.ui.ratings.LocalSongRatingUi

internal class NowPlayingWorkflowOpeners(
    val addToPlaylist: (Song) -> Unit,
    val rateSong: (Song) -> Unit
)

/** Reuse Library's openers; the player-content lambda receives only this stable pair of callbacks. */
@Composable
internal fun rememberNowPlayingWorkflowOpeners(
    currentSong: Song?,
    canPresent: Boolean,
    onAddToPlaylist: (Song) -> Unit
): NowPlayingWorkflowOpeners {
    val latestSong = rememberUpdatedState(currentSong)
    val latestCanPresent = rememberUpdatedState(canPresent)
    val latestPlaylist = rememberUpdatedState(onAddToPlaylist)
    val latestRating = rememberUpdatedState(LocalSongRatingUi.current.onOpen)
    return remember {
        NowPlayingWorkflowOpeners(
            addToPlaylist = { target ->
                if (latestCanPresent.value && isCurrentNowPlayingTarget(target, latestSong.value)) {
                    latestPlaylist.value(target)
                }
            },
            rateSong = { target ->
                if (latestCanPresent.value && isCurrentNowPlayingTarget(target, latestSong.value)) {
                    latestRating.value(target)
                }
            }
        )
    }
}
