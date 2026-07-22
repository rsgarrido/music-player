package com.example.cdplaya.ui.player.pocketflip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.RepeatMode
import com.example.cdplaya.player.waveform.WaveformData
import com.example.cdplaya.ui.player.theme.PlayerThemeTokens

@Composable
fun PocketFlipExpandedPlayer(
    currentSong: Song?,
    waveformData: WaveformData? = null,
    isVisualizerWorkAllowed: Boolean = true,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    currentPosition: Int,
    duration: Int,
    isCurrentSongFavorite: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekChange: (Int) -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onCollapseClick: () -> Unit,
    onOpenUpNextClick: () -> Unit,
    onToggleFavoriteClick: (Song) -> Unit,
    tokens: PlayerThemeTokens = PocketFlipDefaultTokens
) {
    val palette = remember(tokens) { PocketFlipPalette.from(tokens) }
    val configuration = LocalConfiguration.current
    val compact = configuration.screenHeightDp < 700 || configuration.screenWidthDp < 360

    CompositionLocalProvider(LocalPocketFlipPalette provides palette) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .pocketFlipShellFinish()
            .padding(
                horizontal = if (compact) 10.dp else 16.dp,
                vertical = if (compact) 10.dp else 16.dp
            ),
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
    ) {
        PocketFlipDisplayHalf(
            currentSong = currentSong,
            waveformData = waveformData,
            isVisualizerWorkAllowed = isVisualizerWorkAllowed,
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            duration = duration,
            onSeekChange = onSeekChange,
            compact = compact,
            modifier = Modifier.weight(if (compact) 0.54f else 0.57f)
        )

        PocketFlipHinge(compact = compact)

        PocketFlipControlHalf(
            currentSong = currentSong,
            isPlaying = isPlaying,
            isShuffleEnabled = isShuffleEnabled,
            repeatMode = repeatMode,
            isCurrentSongFavorite = isCurrentSongFavorite,
            onPlayPauseClick = onPlayPauseClick,
            onPreviousClick = onPreviousClick,
            onNextClick = onNextClick,
            onShuffleClick = onShuffleClick,
            onRepeatClick = onRepeatClick,
            onOpenUpNextClick = onOpenUpNextClick,
            onCollapseClick = onCollapseClick,
            onToggleFavoriteClick = onToggleFavoriteClick,
            compact = compact,
            modifier = Modifier.weight(if (compact) 0.46f else 0.43f)
        )
    }
    }
}
