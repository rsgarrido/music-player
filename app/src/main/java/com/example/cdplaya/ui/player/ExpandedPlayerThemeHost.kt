package com.example.cdplaya.ui.player

import androidx.compose.runtime.Composable
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.RepeatMode
import com.example.cdplaya.ui.player.classicwheel.ClassicWheelExpandedPlayer
import com.example.cdplaya.ui.player.retrorack.RetroRackExpandedPlayer

@Composable
fun ExpandedPlayerThemeHost(
    selectedPlayerTheme: PlayerTheme,
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
    onToggleFavoriteClick: (Song) -> Unit,
    songs: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit
) {
    when (selectedPlayerTheme) {
        PlayerTheme.DEFAULT -> {
            DefaultExpandedPlayer(
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
                onToggleFavoriteClick = onToggleFavoriteClick
            )
        }

        PlayerTheme.CLASSIC_WHEEL -> {
            ClassicWheelExpandedPlayer(
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
                songs = songs,
                onSongClick = onSongClick
            )
        }

        PlayerTheme.RETRO_RACK -> {
            RetroRackExpandedPlayer(
                currentSong = currentSong,
                onCollapseClick = onCollapseClick
            )
        }
    }
}

@Composable
private fun DefaultExpandedPlayer(
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
