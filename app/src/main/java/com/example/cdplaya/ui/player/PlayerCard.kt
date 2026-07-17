package com.example.cdplaya.ui.player

import android.R
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.RepeatMode
import com.example.cdplaya.ui.player.modern.ModernExpandedPlayer
import kotlin.math.abs

@Composable
fun PlayerCard(
    currentSong: Song?,
    isPlaying: Boolean,
    isExpanded: Boolean,
    currentPosition: Int,
    duration: Int,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
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
        MiniPlayerCard(
            currentSong = currentSong,
            isPlaying = isPlaying,
            albumArtSize = albumArtSize,
            onPlayPauseClick = onPlayPauseClick,
            onPreviousClick = onPreviousClick,
            onNextClick = onNextClick,
            onExpandClick = onExpandClick,
            modifier = modifier
        )
    }
}

@Composable
private fun MiniPlayerCard(
    currentSong: Song,
    isPlaying: Boolean,
    albumArtSize: Dp,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isNextTransition by remember { mutableStateOf(true) }

    Surface(
        onClick = onExpandClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .playerSwipeGestures(
                onSwipeLeft = {
                    isNextTransition = true
                    onNextClick()
                },
                onSwipeRight = {
                    isNextTransition = false
                    onPreviousClick()
                }
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = currentSong,
                transitionSpec = {
                    val enterDirection = if (isNextTransition) 1 else -1
                    val exitDirection = -enterDirection

                    (slideInHorizontally(tween(190)) { width ->
                        enterDirection * width / 3
                    } + fadeIn(tween(160))).togetherWith(
                        slideOutHorizontally(tween(170)) { width ->
                            exitDirection * width / 3
                        } + fadeOut(tween(140))
                    )
                },
                contentKey = { song -> song.id },
                modifier = Modifier.weight(1f),
                label = "miniPlayerSong"
            ) { displayedSong ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = displayedSong.albumArtUri,
                        contentDescription = "Album art for ${displayedSong.title}",
                        modifier = Modifier
                            .size(albumArtSize)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_media_play),
                        placeholder = painterResource(R.drawable.ic_media_play)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = displayedSong.title.ifBlank { "Unknown Title" },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )

                        Text(
                            text = displayedSong.artist.ifBlank { "Unknown Artist" },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                }
            }

            IconButton(onClick = onPlayPauseClick) {
                Icon(
                    imageVector = if (isPlaying) {
                        Icons.Filled.Pause
                    } else {
                        Icons.Filled.PlayArrow
                    },
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }
        }
    }
}

fun Modifier.playerSwipeGestures(
    onSwipeUp: (() -> Unit)? = null,
    onSwipeDown: (() -> Unit)? = null,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null
): Modifier {
    return this.pointerInput(Unit) {
        var totalDragX = 0f
        var totalDragY = 0f

        detectDragGestures(
            onDragStart = {
                totalDragX = 0f
                totalDragY = 0f
            },
            onDrag = { change, dragAmount ->
                change.consume()
                totalDragX += dragAmount.x
                totalDragY += dragAmount.y
            },
            onDragEnd = {
                val swipeThreshold = 120f

                if (abs(totalDragX) > abs(totalDragY)) {
                    if (totalDragX > swipeThreshold) {
                        onSwipeRight?.invoke()
                    } else if (totalDragX < -swipeThreshold) {
                        onSwipeLeft?.invoke()
                    }
                } else {
                    if (totalDragY > swipeThreshold) {
                        onSwipeDown?.invoke()
                    } else if (totalDragY < -swipeThreshold) {
                        onSwipeUp?.invoke()
                    }
                }
            }
        )
    }
}
