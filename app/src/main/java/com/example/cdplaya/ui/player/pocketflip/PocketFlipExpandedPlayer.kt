package com.example.cdplaya.ui.player.pocketflip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.RepeatMode

@Composable
fun PocketFlipExpandedPlayer(
    currentSong: Song?,
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
    onToggleFavoriteClick: (Song) -> Unit
) {
    val configuration = LocalConfiguration.current
    val compact = configuration.screenHeightDp < 700 || configuration.screenWidthDp < 360

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
