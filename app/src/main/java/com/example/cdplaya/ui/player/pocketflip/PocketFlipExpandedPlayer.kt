package com.example.cdplaya.ui.player.pocketflip

import androidx.compose.runtime.Composable
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.RepeatMode
import com.example.cdplaya.ui.player.PlayerCard

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
    PlayerCard(
        currentSong = currentSong,
        isPlaying = isPlaying,
        isExpanded = true,
        isShuffleEnabled = isShuffleEnabled,
        repeatMode = repeatMode,
        currentPosition = currentPosition,
        duration = duration,
        onPlayPauseClick = onPlayPauseClick,
        onPreviousClick = onPreviousClick,
        onNextClick = onNextClick,
        onSeekChange = onSeekChange,
        onShuffleClick = onShuffleClick,
        onRepeatClick = onRepeatClick,
        onExpandClick = {},
        onCollapseClick = onCollapseClick,
        onOpenUpNextClick = onOpenUpNextClick,
        isCurrentSongFavorite = isCurrentSongFavorite,
        onToggleFavoriteClick = onToggleFavoriteClick
    )
}
