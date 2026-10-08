package io.github.rsgarrido.sazanami.ui.player.modern

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.data.membershipKey
import io.github.rsgarrido.sazanami.ui.player.PlayerLyricsTransitionState
import io.github.rsgarrido.sazanami.ui.player.PlayerMorphState
import io.github.rsgarrido.sazanami.ui.player.canOpenArtworkAtExpandedEndpoint

internal fun modernArtworkClickCallback(
    onViewArtwork: ((Song) -> Unit)?,
    displayedSong: Song,
    currentSong: Song,
    player: PlayerMorphState,
    lyrics: PlayerLyricsTransitionState,
    carousel: ModernArtworkCarouselState,
    hasVisibleOwner: Boolean
): ((Song) -> Unit)? {
    fun eligible() = hasVisibleOwner && displayedSong.albumArtUri != null &&
            displayedSong.membershipKey() == currentSong.membershipKey() && carousel.isIdle &&
            canOpenArtworkAtExpandedEndpoint(player, lyrics)
    if (onViewArtwork == null || !eligible()) return null
    return { target -> if (eligible() && target.membershipKey() == displayedSong.membershipKey()) onViewArtwork(target) }
}

/** Both visible morph artwork and the standalone endpoint use the existing carousel behavior. */
@Composable
internal fun rememberModernArtworkHorizontalDragModifier(
    carousel: ModernArtworkCarouselState,
    sourceSongId: Long,
    enabled: Boolean
): Modifier = Modifier.draggable(
    state = rememberDraggableState(carousel::dragBy),
    orientation = Orientation.Horizontal,
    enabled = enabled,
    onDragStarted = { carousel.startDrag() },
    onDragStopped = { velocity -> carousel.settle(velocity, sourceSongId) }
)
