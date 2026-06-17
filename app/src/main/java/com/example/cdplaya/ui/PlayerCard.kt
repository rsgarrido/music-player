package com.example.cdplaya.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekChange: (Int) -> Unit,
    onExpandClick: () -> Unit,
    onCollapseClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit
) {
    if (currentSong == null) {
        return
    }

    val albumArtSize by animateDpAsState(
        targetValue = if (isExpanded) 180.dp else 56.dp,
        animationSpec = tween(durationMillis = 300),
        label = "albumArtSize"
    )

    val cardCornerSize by animateDpAsState(
        targetValue = if (isExpanded) 24.dp else 16.dp,
        animationSpec = tween(durationMillis = 300),
        label = "cardCornerSize"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize(
                animationSpec = tween(durationMillis = 300)
            )
            .playerSwipeGestures(
                onSwipeDown = onExpandClick,
                onSwipeUp = onCollapseClick,
                onSwipeLeft = onNextClick,
                onSwipeRight = onPreviousClick
            ),
        shape = RoundedCornerShape(cardCornerSize),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        if (isExpanded) {
            ExpandedPlayerContent(
                currentSong = currentSong,
                isPlaying = isPlaying,
                albumArtSize = albumArtSize,
                currentPosition = currentPosition,
                duration = duration,
                isShuffleEnabled = isShuffleEnabled,
                repeatMode = repeatMode,
                onShuffleClick = onShuffleClick,
                onRepeatClick = onRepeatClick,
                onPlayPauseClick = onPlayPauseClick,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onSeekChange = onSeekChange,
                onCollapseClick = onCollapseClick
            )
        } else {
            MiniPlayerContent(
                currentSong = currentSong,
                isPlaying = isPlaying,
                albumArtSize = albumArtSize,
                onPlayPauseClick = onPlayPauseClick,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onExpandClick = onExpandClick
            )
        }
    }
}

@Composable
fun MiniPlayerContent(
    currentSong: Song,
    isPlaying: Boolean,
    albumArtSize: Dp,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onExpandClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = currentSong.albumArtUri,
            contentDescription = "Album art for ${currentSong.title}",
            modifier = Modifier
                .size(albumArtSize)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop,
            error = painterResource(android.R.drawable.ic_media_play),
            placeholder = painterResource(android.R.drawable.ic_media_play)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = currentSong.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )

            Text(
                text = currentSong.artist,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }

        IconButton(onClick = onPreviousClick) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous song"
            )
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

        IconButton(onClick = onNextClick) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next song"
            )
        }

        IconButton(onClick = onExpandClick) {
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = "Expand player"
            )
        }
    }
}

@Composable
fun ExpandedPlayerContent(
    currentSong: Song,
    isPlaying: Boolean,
    albumArtSize: Dp,
    currentPosition: Int,
    duration: Int,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekChange: (Int) -> Unit,
    onCollapseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = currentSong.albumArtUri,
            contentDescription = "Album art for ${currentSong.title}",
            modifier = Modifier
                .size(albumArtSize)
                .clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Crop,
            error = painterResource(android.R.drawable.ic_media_play),
            placeholder = painterResource(android.R.drawable.ic_media_play)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = currentSong.title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = currentSong.artist,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(12.dp))

        Slider(
            value = currentPosition.toFloat(),
            onValueChange = { newPosition ->
                onSeekChange(newPosition.toInt())
            },
            valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = formatDuration(currentPosition))
            Text(text = formatDuration(duration))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onShuffleClick) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = if (isShuffleEnabled) "Shuffle on" else "Shuffle off",
                    tint = if (isShuffleEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onPreviousClick) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous song"
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

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

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onNextClick) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next song"
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onRepeatClick) {
                Icon(
                    imageVector = if (repeatMode == RepeatMode.ONE) {
                        Icons.Filled.RepeatOne
                    } else {
                        Icons.Filled.Repeat
                    },
                    contentDescription = when (repeatMode) {
                        RepeatMode.OFF -> "Repeat off"
                        RepeatMode.ALL -> "Repeat all"
                        RepeatMode.ONE -> "Repeat one"
                    },
                    tint = if (repeatMode == RepeatMode.OFF) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onCollapseClick) {
                Icon(
                    imageVector = Icons.Filled.ExpandLess,
                    contentDescription = "Collapse player"
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