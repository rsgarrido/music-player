package com.example.cdplaya.ui.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.RepeatMode
import com.example.cdplaya.ui.player.mini.MiniPlayerCallbacks
import com.example.cdplaya.ui.player.mini.MiniPlayerHost
import com.example.cdplaya.ui.player.mini.MiniPlayerState
import com.example.cdplaya.ui.player.modern.ModernExpandedPlayer
import com.example.cdplaya.ui.player.theme.PlayerThemeTokens

@Composable
fun PlayerCard(
    currentSong: Song?,
    isPlaying: Boolean,
    isExpanded: Boolean,
    currentPosition: Int,
    duration: Int,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    selectedPlayerTheme: PlayerTheme,
    selectedPlayerThemeTokens: PlayerThemeTokens,
    modifier: Modifier = Modifier,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekChange: (Int) -> Unit,
    onExpandClick: () -> Unit,
    onCollapseClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onOpenUpNextClick: () -> Unit = {},
    isCurrentSongFavorite: Boolean = false,
    onToggleFavoriteClick: (Song) -> Unit = {},
) {
    if (currentSong == null) {
        return
    }

    val albumArtSize by animateDpAsState(
        targetValue = if (isExpanded) 292.dp else 52.dp,
        animationSpec = tween(durationMillis = 300),
        label = "albumArtSize"
    )

    if (isExpanded) {
        ModernExpandedPlayer(
            currentSong = currentSong,
            isPlaying = isPlaying,
            isShuffleEnabled = isShuffleEnabled,
            repeatMode = repeatMode,
            currentPosition = currentPosition,
            duration = duration,
            isCurrentSongFavorite = isCurrentSongFavorite,
            onPlayPauseClick = onPlayPauseClick,
            onPreviousClick = onPreviousClick,
            onNextClick = onNextClick,
            onSeekChange = onSeekChange,
            onShuffleClick = onShuffleClick,
            onRepeatClick = onRepeatClick,
            onCollapseClick = onCollapseClick,
            onOpenUpNextClick = onOpenUpNextClick,
            onToggleFavoriteClick = onToggleFavoriteClick,
            albumArtSize = albumArtSize,
            modifier = modifier
        )
    } else {
        MiniPlayerHost(
            selectedPlayerTheme = selectedPlayerTheme,
            tokens = selectedPlayerThemeTokens,
            state = MiniPlayerState(
                currentSong = currentSong,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                albumArtSize = albumArtSize
            ),
            callbacks = MiniPlayerCallbacks(
                onPlayPauseClick = onPlayPauseClick,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onExpandClick = onExpandClick
            ),
            modifier = modifier
        )
    }
}
