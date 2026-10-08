package io.github.rsgarrido.sazanami.ui.player.modern

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.ui.player.PlayerLyricsTransitionState
import io.github.rsgarrido.sazanami.ui.player.PlayerMorphState

internal fun modernArtistClickCallback(
    onClick: (() -> Unit)?,
    playerMorphState: PlayerMorphState,
    lyricsTransitionState: PlayerLyricsTransitionState,
    carouselOffsetX: Float,
    hasVisibleContent: Boolean,
    isCurrentTrackDisplayed: Boolean
): (() -> Unit)? = onClick?.takeIf {
    canOpenModernMore(
        playerMorphState, lyricsTransitionState, carouselOffsetX,
        hasVisibleContent, isCurrentTrackDisplayed
    )
}

/** Match album text's appearance and button semantics without adding a dead clickable node. */
@Composable
internal fun Modifier.artistNavigationClick(onClick: (() -> Unit)?): Modifier =
    if (onClick == null) this else clickable(
        role = Role.Button,
        onClickLabel = stringResource(R.string.library_search_go_artist),
        onClick = onClick
    )
