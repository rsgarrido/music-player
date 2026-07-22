package com.example.cdplaya.ui.player.mini

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.data.Song

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
    MODERN
}

internal fun miniPlayerVariantFor(playerTheme: PlayerTheme): MiniPlayerVariant =
    when (playerTheme) {
        PlayerTheme.DEFAULT,
        PlayerTheme.CLASSIC_WHEEL,
        PlayerTheme.POCKET_CASSETTE,
        PlayerTheme.POCKET_FLIP,
        PlayerTheme.RETRO_RACK -> MiniPlayerVariant.MODERN
    }

@Composable
fun MiniPlayerHost(
    selectedPlayerTheme: PlayerTheme,
    state: MiniPlayerState,
    callbacks: MiniPlayerCallbacks,
    modifier: Modifier = Modifier
) {
    when (miniPlayerVariantFor(selectedPlayerTheme)) {
        MiniPlayerVariant.MODERN -> ModernMiniPlayer(
            state = state,
            callbacks = callbacks,
            modifier = modifier
        )
    }
}
