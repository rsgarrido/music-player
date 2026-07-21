package com.example.cdplaya.ui.player.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.RepeatMode
import com.example.cdplaya.player.audioquality.AudioQualityInfo
import com.example.cdplaya.player.audioquality.AudioQualityRepository
import com.example.cdplaya.ui.player.rememberExpandedPlayerDragState

@Composable
fun ModernExpandedPlayer(
    currentSong: Song?,
    previousPreviewSong: Song? = null,
    nextPreviewSong: Song? = null,
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

    val audioQualityRepository = remember { AudioQualityRepository() }
    var audioQualityInfo by remember { mutableStateOf<AudioQualityInfo?>(null) }

    LaunchedEffect(currentSong.id, currentSong.filePath) {
        audioQualityInfo = audioQualityRepository.getAudioQualityInfo(currentSong)
    }

    val dragState = rememberExpandedPlayerDragState(onCollapseClick)
    val verticalDragState = rememberDraggableState { deltaY ->
        dragState.dragBy(deltaY)
    }
    val dragProgress = dragState.progress

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Color.Black.copy(alpha = 0.24f * (1f - dragProgress))
            )
            .onSizeChanged { size ->
                dragState.updateContainerHeight(size.height)
            }
    ) {
        val foregroundAlbumArtSize = minOf(
            albumArtSize,
            maxWidth - 32.dp,
            maxHeight * 0.42f
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = dragState.offsetY
                    val contentScale = 1f - dragProgress * 0.04f
                    scaleX = contentScale
                    scaleY = contentScale
                    alpha = 1f - dragProgress * 0.1f
                    shape = RoundedCornerShape(28.dp * dragProgress)
                    clip = dragProgress > 0f
                }
                .background(style.backgroundColor)
                .draggable(
                    state = verticalDragState,
                    orientation = Orientation.Vertical,
                    onDragStarted = { dragState.startDrag() },
                    onDragStopped = { velocityY -> dragState.settle(velocityY) }
                )
        ) {
            ModernPlayerBackground(
                currentSong = currentSong,
                style = style
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 1f - dragProgress * 0.18f
                        translationY = dragProgress * 14.dp.toPx()
                    }
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(
                        horizontal = ModernPlayerDefaults.ContentHorizontalPadding,
                        vertical = ModernPlayerDefaults.ContentVerticalPadding
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ModernPlayerArtwork(
                    currentSong = currentSong,
                    previousPreviewSong = previousPreviewSong,
                    nextPreviewSong = nextPreviewSong,
                    artworkSize = foregroundAlbumArtSize,
                    style = style,
                    onPreviousClick = onPreviousClick,
                    onNextClick = onNextClick
                )

                Spacer(modifier = Modifier.height(24.dp))

                ModernPlayerMetadata(
                    currentSong = currentSong,
                    style = style
                )

                ModernPlayerAudioQualityBadge(
                    audioQualityInfo = audioQualityInfo,
                    style = style,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(top = 12.dp)
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
            }
        }
    }
}
