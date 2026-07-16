package com.example.cdplaya.ui.player.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.RepeatMode
import com.example.cdplaya.ui.player.playerSwipeGestures

@Composable
fun ModernExpandedPlayer(
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
    modifier: Modifier = Modifier,
    style: ModernPlayerStyle = ModernPlayerDefaults.style(),
    albumArtSize: Dp = ModernPlayerDefaults.MaximumArtworkSize,
    lyricsContent: @Composable () -> Unit = {}
) {
    if (currentSong == null) {
        return
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(style.backgroundColor)
            .playerSwipeGestures(
                onSwipeDown = onCollapseClick,
                onSwipeLeft = onNextClick,
                onSwipeRight = onPreviousClick
            )
    ) {
        val foregroundAlbumArtSize = minOf(
            albumArtSize,
            maxWidth - 48.dp,
            maxHeight * 0.34f
        )

        ModernPlayerBackground(
            currentSong = currentSong,
            style = style
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(
                    horizontal = ModernPlayerDefaults.ContentHorizontalPadding,
                    vertical = ModernPlayerDefaults.ContentVerticalPadding
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ModernPlayerHeader(
                onCollapseClick = onCollapseClick,
                style = style
            )

            ModernPlayerFavoriteAction(
                currentSong = currentSong,
                isCurrentSongFavorite = isCurrentSongFavorite,
                onToggleFavoriteClick = onToggleFavoriteClick,
                style = style
            )

            Spacer(modifier = Modifier.height(14.dp))

            ModernPlayerArtwork(
                currentSong = currentSong,
                artworkSize = foregroundAlbumArtSize,
                style = style
            )

            Spacer(modifier = Modifier.height(24.dp))

            ModernPlayerMetadata(
                currentSong = currentSong,
                style = style
            )

            lyricsContent()

            Spacer(modifier = Modifier.weight(1f))

            ModernPlayerSeekBar(
                currentPosition = currentPosition,
                duration = duration,
                onSeekChange = onSeekChange,
                style = style
            )

            Spacer(modifier = Modifier.height(18.dp))

            ModernPlayerControls(
                isPlaying = isPlaying,
                isShuffleEnabled = isShuffleEnabled,
                repeatMode = repeatMode,
                onPlayPauseClick = onPlayPauseClick,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onShuffleClick = onShuffleClick,
                onRepeatClick = onRepeatClick,
                style = style
            )

            Spacer(modifier = Modifier.height(20.dp))

            ModernPlayerActions(
                onOpenUpNextClick = onOpenUpNextClick,
                style = style
            )
        }
    }
}
