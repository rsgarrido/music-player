package io.github.rsgarrido.sazanami.ui.player.modern

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.rsgarrido.sazanami.ui.player.PlayerLyricsTransitionState
import io.github.rsgarrido.sazanami.ui.player.PlayerMorphState
import io.github.rsgarrido.sazanami.ui.player.PlayerPresentation

/** Shared with the visible morph metadata so an artist tap never blocks existing vertical drags. */
@Composable
internal fun rememberModernPlayerVerticalDragModifier(
    playerMorphState: PlayerMorphState,
    lyricsTransitionState: PlayerLyricsTransitionState,
    containerHeightPx: Float,
    defaultMorphDragRangePx: Float?
): Modifier {
    var isMorphDrag by remember { mutableStateOf(false) }
    val verticalDragState = rememberDraggableState { deltaY ->
        if (playerMorphState.progress < 1f && lyricsTransitionState.progress == 0f) {
            isMorphDrag = true
            playerMorphState.dragBy(deltaY)
        } else if (deltaY < 0f || lyricsTransitionState.progress > 0f) {
            if (lyricsTransitionState.progress == 0f) {
                lyricsTransitionState.beginOpeningDrag()
            }
            lyricsTransitionState.dragOpeningBy(deltaY, containerHeightPx)
            playerMorphState.updateProgressFromDrag(1f)
        } else {
            isMorphDrag = true
            playerMorphState.dragBy(deltaY)
        }
    }
    return Modifier.draggable(
        state = verticalDragState,
        orientation = Orientation.Vertical,
        onDragStarted = {
            isMorphDrag = playerMorphState.progress < 1f
            if (defaultMorphDragRangePx != null) {
                playerMorphState.beginDragWithRange(defaultMorphDragRangePx)
            } else {
                playerMorphState.beginDrag(containerHeightPx)
            }
        },
        enabled = !lyricsTransitionState.lyricsInteractive,
        onDragStopped = { velocityY ->
            if (!isMorphDrag && (
                lyricsTransitionState.progress > 0f ||
                        playerMorphState.progress >= 1f &&
                        velocityY <= PlayerLyricsTransitionState.OPEN_VELOCITY_PX_PER_SECOND
                )
            ) {
                playerMorphState.snapTo(PlayerPresentation.Expanded)
                lyricsTransitionState.settleOpening(velocityY)
            } else {
                playerMorphState.endDrag(velocityY)
            }
            isMorphDrag = false
        }
    )
}
