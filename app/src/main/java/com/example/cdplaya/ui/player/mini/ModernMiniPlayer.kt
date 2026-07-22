package com.example.cdplaya.ui.player.mini

import android.R
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlin.math.abs

@Composable
fun ModernMiniPlayer(
    state: MiniPlayerState,
    callbacks: MiniPlayerCallbacks,
    modifier: Modifier = Modifier
) {
    var isNextTransition by remember { mutableStateOf(true) }

    Surface(
        onClick = callbacks.onExpandClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .miniPlayerSwipeGestures(
                onSwipeLeft = {
                    isNextTransition = true
                    callbacks.onNextClick()
                },
                onSwipeRight = {
                    isNextTransition = false
                    callbacks.onPreviousClick()
                }
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.76f)
        ),
        tonalElevation = 4.dp,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = state.currentSong,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = displayedSong.albumArtUri,
                        contentDescription = "Album art for ${displayedSong.title}",
                        modifier = Modifier
                            .size(state.albumArtSize)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_media_play),
                        placeholder = painterResource(R.drawable.ic_media_play)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
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

            IconButton(onClick = callbacks.onPlayPauseClick) {
                Icon(
                    imageVector = if (state.isPlaying) {
                        Icons.Filled.Pause
                    } else {
                        Icons.Filled.PlayArrow
                    },
                    contentDescription = if (state.isPlaying) "Pause" else "Play"
                )
            }
        }
    }
}

internal fun Modifier.miniPlayerSwipeGestures(
    onSwipeUp: (() -> Unit)? = null,
    onSwipeDown: (() -> Unit)? = null,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null
): Modifier = pointerInput(Unit) {
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
