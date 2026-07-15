package com.example.cdplaya.ui.player.pocketflip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.RepeatMode

@Composable
internal fun PocketFlipControlHalf(
    currentSong: Song?,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    isCurrentSongFavorite: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onOpenUpNextClick: () -> Unit,
    onCollapseClick: () -> Unit,
    onToggleFavoriteClick: (Song) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PocketFlipDirectionPad(
                isShuffleEnabled = isShuffleEnabled,
                repeatMode = repeatMode,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onShuffleClick = onShuffleClick,
                onRepeatClick = onRepeatClick,
                compact = compact
            )

            PocketFlipActionButtons(
                currentSong = currentSong,
                isPlaying = isPlaying,
                isCurrentSongFavorite = isCurrentSongFavorite,
                onPlayPauseClick = onPlayPauseClick,
                onToggleFavoriteClick = onToggleFavoriteClick,
                compact = compact
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
        ) {
            PocketFlipUtilityButton(
                icon = Icons.Filled.List,
                label = "UP NEXT",
                contentDescription = "Open up next queue",
                onClick = onOpenUpNextClick,
                compact = compact,
                modifier = Modifier.weight(1f)
            )
            PocketFlipUtilityButton(
                icon = Icons.Filled.ExpandMore,
                label = "CLOSE",
                contentDescription = "Collapse player",
                onClick = onCollapseClick,
                compact = compact,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PocketFlipDirectionPad(
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    compact: Boolean
) {
    val padSize = if (compact) 132.dp else 148.dp
    val buttonSize = if (compact) 48.dp else 54.dp

    Box(
        modifier = Modifier.size(padSize),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 44.dp else 50.dp)
                .background(PocketFlipColors.buttonShadow, CircleShape)
        )
        PocketFlipPadButton(
            icon = Icons.Filled.Shuffle,
            contentDescription = if (isShuffleEnabled) "Disable shuffle" else "Enable shuffle",
            active = isShuffleEnabled,
            size = buttonSize,
            onClick = onShuffleClick,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        PocketFlipPadButton(
            icon = Icons.Filled.SkipPrevious,
            contentDescription = "Previous track",
            size = buttonSize,
            onClick = onPreviousClick,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        PocketFlipPadButton(
            icon = Icons.Filled.SkipNext,
            contentDescription = "Next track",
            size = buttonSize,
            onClick = onNextClick,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
        PocketFlipPadButton(
            icon = if (repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
            contentDescription = when (repeatMode) {
                RepeatMode.OFF -> "Enable repeat all"
                RepeatMode.ALL -> "Enable repeat one"
                RepeatMode.ONE -> "Disable repeat"
            },
            active = repeatMode != RepeatMode.OFF,
            size = buttonSize,
            onClick = onRepeatClick,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun PocketFlipActionButtons(
    currentSong: Song?,
    isPlaying: Boolean,
    isCurrentSongFavorite: Boolean,
    onPlayPauseClick: () -> Unit,
    onToggleFavoriteClick: (Song) -> Unit,
    compact: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
    ) {
        PocketFlipRoundAction(
            icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            label = if (isPlaying) "PAUSE" else "PLAY",
            contentDescription = if (isPlaying) "Pause" else "Play",
            size = if (compact) 68.dp else 78.dp,
            onClick = onPlayPauseClick
        )
        Column(
            modifier = Modifier.padding(top = if (compact) 34.dp else 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PocketFlipRoundAction(
                icon = if (isCurrentSongFavorite) {
                    Icons.Filled.Favorite
                } else {
                    Icons.Filled.FavoriteBorder
                },
                label = "FAV",
                contentDescription = if (isCurrentSongFavorite) {
                    "Remove from favorites"
                } else {
                    "Add to favorites"
                },
                size = if (compact) 54.dp else 60.dp,
                active = isCurrentSongFavorite,
                onClick = { currentSong?.let(onToggleFavoriteClick) }
            )
        }
    }
}

@Composable
private fun PocketFlipPadButton(
    icon: ImageVector,
    contentDescription: String,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = RoundedCornerShape(13.dp),
        color = if (active) PocketFlipColors.buttonActive else PocketFlipColors.button,
        contentColor = if (active) PocketFlipColors.buttonActiveIcon else PocketFlipColors.buttonIcon,
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(size * 0.48f)
            )
        }
    }
}

@Composable
private fun PocketFlipRoundAction(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    size: Dp,
    onClick: () -> Unit,
    active: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(size),
            shape = CircleShape,
            color = if (active) PocketFlipColors.actionActive else PocketFlipColors.action,
            contentColor = PocketFlipColors.actionIcon,
            shadowElevation = 5.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(size * 0.46f)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = PocketFlipColors.shellText,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 0.6.sp
        )
    }
}

@Composable
private fun PocketFlipUtilityButton(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(if (compact) 46.dp else 52.dp),
        shape = RoundedCornerShape(50),
        color = PocketFlipColors.utility,
        contentColor = PocketFlipColors.utilityIcon,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(if (compact) 20.dp else 22.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 10.sp else 11.sp,
                letterSpacing = 0.7.sp
            )
        }
    }
}
