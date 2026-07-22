package com.example.cdplaya.ui.player.mini

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.data.Song
import com.example.cdplaya.ui.player.theme.PlayerThemeTokens
import com.example.cdplaya.ui.player.theme.defaultTokens

data class MiniPlayerState(
    val currentSong: Song,
    val isPlaying: Boolean,
    val currentPosition: Int,
    val duration: Int,
    val albumArtSize: Dp
)

data class MiniPlayerCallbacks(
    val onPlayPauseClick: () -> Unit,
    val onPreviousClick: () -> Unit,
    val onNextClick: () -> Unit,
    val onExpandClick: () -> Unit
)

internal enum class MiniPlayerVariant {
    MODERN,
    CLASSIC_WHEEL,
    POCKET_CASSETTE,
    POCKET_FLIP,
    RETRO_RACK
}

internal fun miniPlayerVariantFor(playerTheme: PlayerTheme): MiniPlayerVariant =
    when (playerTheme) {
        PlayerTheme.DEFAULT -> MiniPlayerVariant.MODERN
        PlayerTheme.CLASSIC_WHEEL -> MiniPlayerVariant.CLASSIC_WHEEL
        PlayerTheme.POCKET_CASSETTE -> MiniPlayerVariant.POCKET_CASSETTE
        PlayerTheme.POCKET_FLIP -> MiniPlayerVariant.POCKET_FLIP
        PlayerTheme.RETRO_RACK -> MiniPlayerVariant.RETRO_RACK
    }

@Composable
fun MiniPlayerHost(
    selectedPlayerTheme: PlayerTheme,
    tokens: PlayerThemeTokens?,
    state: MiniPlayerState,
    callbacks: MiniPlayerCallbacks,
    modifier: Modifier = Modifier
) {
    val resolvedTokens = tokens ?: selectedPlayerTheme.defaultTokens()

    when (miniPlayerVariantFor(selectedPlayerTheme)) {
        MiniPlayerVariant.MODERN -> ModernMiniPlayer(
            state = state,
            callbacks = callbacks,
            modifier = modifier
        )

        MiniPlayerVariant.CLASSIC_WHEEL -> ClassicWheelMiniPlayer(
            state = state,
            callbacks = callbacks,
            tokens = resolvedTokens,
            modifier = modifier
        )

        MiniPlayerVariant.POCKET_CASSETTE -> PocketCassetteMiniPlayer(
            state = state,
            callbacks = callbacks,
            tokens = resolvedTokens,
            modifier = modifier
        )

        MiniPlayerVariant.POCKET_FLIP -> PocketFlipMiniPlayer(
            state = state,
            callbacks = callbacks,
            tokens = resolvedTokens,
            modifier = modifier
        )

        MiniPlayerVariant.RETRO_RACK -> RetroRackMiniPlayer(
            state = state,
            callbacks = callbacks,
            tokens = resolvedTokens,
            modifier = modifier
        )
    }
}
