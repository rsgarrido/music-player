package com.example.cdplaya.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.RepeatMode
import com.example.cdplaya.ui.player.classicwheel.ClassicWheelExpandedPlayer
import com.example.cdplaya.ui.player.modern.ModernExpandedPlayer
import com.example.cdplaya.ui.player.modern.ModernArtworkTransitionStyle
import com.example.cdplaya.ui.player.modern.ModernSeekbarStyle
import com.example.cdplaya.ui.player.modern.selectNearbyWaveformSongs
import com.example.cdplaya.ui.player.pocketcassette.PocketCassetteExpandedPlayer
import com.example.cdplaya.ui.player.pocketflip.PocketFlipExpandedPlayer
import com.example.cdplaya.ui.player.retrorack.RetroRackExpandedPlayer
import com.example.cdplaya.ui.player.theme.PlayerThemeTokens

@Composable
fun ExpandedPlayerThemeHost(
    selectedPlayerTheme: PlayerTheme,
    tokens: PlayerThemeTokens,
    currentSong: Song?,
    previousPreviewSong: Song?,
    nextPreviewSong: Song?,
    modernArtworkTransitionStyle: ModernArtworkTransitionStyle,
    modernSeekbarStyle: ModernSeekbarStyle,
    isVisualizerWorkAllowed: Boolean,
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
    onOpenSleepTimerClick: () -> Unit,
    onOpenMoreClick: () -> Unit,
    onToggleFavoriteClick: (Song) -> Unit,
    songs: List<Song>,
    upcomingSongs: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit
) {
    val shouldLoadWaveform = shouldLoadExpandedPlayerWaveform(
        selectedPlayerTheme = selectedPlayerTheme,
        modernSeekbarStyle = modernSeekbarStyle
    )
    val shouldPrefetchWaveforms = selectedPlayerTheme == PlayerTheme.DEFAULT &&
            modernSeekbarStyle.usesWaveformData
    val nearbyWaveformSongs = remember(
        shouldPrefetchWaveforms,
        currentSong?.id,
        currentSong?.filePath,
        nextPreviewSong?.id,
        nextPreviewSong?.filePath,
        previousPreviewSong?.id,
        previousPreviewSong?.filePath
    ) {
        if (shouldPrefetchWaveforms && currentSong != null) {
            selectNearbyWaveformSongs(
                currentSong = currentSong,
                nextSong = nextPreviewSong,
                previousSong = previousPreviewSong
            )
        } else {
            emptyList()
        }
    }
    val waveformData = rememberExpandedPlayerWaveformData(
        currentSong = currentSong,
        shouldLoad = shouldLoadWaveform,
        prefetchSongs = nearbyWaveformSongs
    )

    when (selectedPlayerTheme) {
        PlayerTheme.DEFAULT -> {
            ModernExpandedPlayer(
                currentSong = currentSong,
                previousPreviewSong = previousPreviewSong,
                nextPreviewSong = nextPreviewSong,
                artworkTransitionStyle = modernArtworkTransitionStyle,
                seekbarStyle = modernSeekbarStyle,
                waveformData = waveformData,
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
                onSongClick = onSongClick,
                tokens = tokens
            )
        }

        PlayerTheme.RETRO_RACK -> {
            RetroRackExpandedPlayer(
                currentSong = currentSong,
                waveformData = waveformData,
                isVisualizerWorkAllowed = isVisualizerWorkAllowed,
                isPlaying = isPlaying,
                isShuffleEnabled = isShuffleEnabled,
                repeatMode = repeatMode,
                currentPosition = currentPosition,
                duration = duration,
                isCurrentSongFavorite = isCurrentSongFavorite,
                upcomingSongs = upcomingSongs,
                onPlayPauseClick = onPlayPauseClick,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onSeekChange = onSeekChange,
                onShuffleClick = onShuffleClick,
                onRepeatClick = onRepeatClick,
                onCollapseClick = onCollapseClick,
                onOpenUpNextClick = onOpenUpNextClick,
                onToggleFavoriteClick = onToggleFavoriteClick,
                onSongClick = onSongClick,
                tokens = tokens
            )
        }

        PlayerTheme.POCKET_FLIP -> {
            PocketFlipExpandedPlayer(
                currentSong = currentSong,
                waveformData = waveformData,
                isVisualizerWorkAllowed = isVisualizerWorkAllowed,
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
                tokens = tokens
            )
        }

        PlayerTheme.POCKET_CASSETTE -> {
            PocketCassetteExpandedPlayer(
                currentSong = currentSong,
                isVisualizerWorkAllowed = isVisualizerWorkAllowed,
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
                tokens = tokens
            )
        }

    }
}
