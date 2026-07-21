package com.example.cdplaya.ui.player.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cdplaya.player.RepeatMode

@Composable
internal fun ModernPlayerControls(
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    style: ModernPlayerStyle
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        ModernPlayerModeIconButton(
            onClick = onShuffleClick,
            style = style
        ) {
            Icon(
                imageVector = Icons.Filled.Shuffle,
                contentDescription = if (isShuffleEnabled) "Shuffle on" else "Shuffle off",
                tint = if (isShuffleEnabled) {
                    style.accentColor
                } else {
                    style.inactiveControlColor
                }
            )
        }

        IconButton(onClick = onPreviousClick) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous song",
                tint = style.contentColor,
                modifier = Modifier.size(38.dp)
            )
        }

        ModernPlayerPlayPauseButton(
            isPlaying = isPlaying,
            onClick = onPlayPauseClick,
            style = style
        )

        IconButton(onClick = onNextClick) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next song",
                tint = style.contentColor,
                modifier = Modifier.size(38.dp)
            )
        }

        ModernPlayerModeIconButton(
            onClick = onRepeatClick,
            style = style
        ) {
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
                    style.inactiveControlColor
                } else {
                    style.accentColor
                }
            )
        }
    }
}

@Composable
private fun ModernPlayerPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    style: ModernPlayerStyle,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(82.dp)
            .shadow(
                elevation = 14.dp,
                shape = style.primaryControlShape,
                ambientColor = Color.Black.copy(alpha = 0.24f),
                spotColor = Color.Black.copy(alpha = 0.32f)
            )
            .clip(style.primaryControlShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        style.primaryControlSurfaceTopColor,
                        style.primaryControlSurfaceBottomColor
                    )
                )
            )
            .border(
                width = 1.dp,
                color = style.primaryControlSurfaceBorderColor,
                shape = style.primaryControlShape
            )
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = if (isPlaying) {
                    Icons.Filled.Pause
                } else {
                    Icons.Filled.PlayArrow
                },
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = style.contentColor,
                modifier = Modifier.size(50.dp)
            )
        }
    }
}

@Composable
private fun ModernPlayerModeIconButton(
    onClick: () -> Unit,
    style: ModernPlayerStyle,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(style.modeControlShape),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            icon()
        }
    }
}
