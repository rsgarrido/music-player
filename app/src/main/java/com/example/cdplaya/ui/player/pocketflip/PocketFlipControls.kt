package com.example.cdplaya.ui.player.pocketflip

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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

            PocketFlipActionCluster(
                currentSong = currentSong,
                isPlaying = isPlaying,
                isCurrentSongFavorite = isCurrentSongFavorite,
                onPlayPauseClick = onPlayPauseClick,
                onToggleFavoriteClick = onToggleFavoriteClick,
                compact = compact
            )
        }

        PocketFlipDeckDetails(compact = compact)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PocketFlipUtilitySwitch(
                label = "QUEUE",
                contentDescription = "Open up next queue",
                onClick = onOpenUpNextClick,
                compact = compact
            )
            Spacer(modifier = Modifier.width(if (compact) 12.dp else 18.dp))
            PocketFlipUtilitySwitch(
                label = "CLOSE",
                contentDescription = "Collapse player",
                onClick = onCollapseClick,
                compact = compact
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
    val padSize = if (compact) 136.dp else 150.dp
    val hitSize = if (compact) 52.dp else 58.dp

    Box(
        modifier = Modifier.size(padSize),
        contentAlignment = Alignment.Center
    ) {
        PocketFlipPadBody(modifier = Modifier.matchParentSize())

        PocketFlipPadHitTarget(
            icon = Icons.Filled.Shuffle,
            contentDescription = if (isShuffleEnabled) "Disable shuffle" else "Enable shuffle",
            active = isShuffleEnabled,
            size = hitSize,
            onClick = onShuffleClick,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        PocketFlipPadHitTarget(
            icon = Icons.Filled.SkipPrevious,
            contentDescription = "Previous track",
            size = hitSize,
            onClick = onPreviousClick,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        PocketFlipPadHitTarget(
            icon = Icons.Filled.SkipNext,
            contentDescription = "Next track",
            size = hitSize,
            onClick = onNextClick,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
        PocketFlipPadHitTarget(
            icon = if (repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
            contentDescription = when (repeatMode) {
                RepeatMode.OFF -> "Enable repeat all"
                RepeatMode.ALL -> "Enable repeat one"
                RepeatMode.ONE -> "Disable repeat"
            },
            active = repeatMode != RepeatMode.OFF,
            size = hitSize,
            onClick = onRepeatClick,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun PocketFlipPadBody(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val wellRadius = size.minDimension * 0.46f
        val thickness = size.minDimension * 0.31f
        val armInset = size.minDimension * 0.085f
        val corner = CornerRadius(size.minDimension * 0.07f)
        val horizontalTop = (size.height - thickness) / 2f
        val verticalLeft = (size.width - thickness) / 2f
        val bodyColor = PocketFlipColors.button

        drawCircle(
            color = PocketFlipColors.controlWell,
            radius = wellRadius,
            center = center
        )
        drawCircle(
            color = PocketFlipColors.controlGroove,
            radius = wellRadius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        drawRoundRect(
            color = PocketFlipColors.buttonShadow,
            topLeft = Offset(armInset, horizontalTop + 3.dp.toPx()),
            size = Size(size.width - armInset * 2f, thickness),
            cornerRadius = corner
        )
        drawRoundRect(
            color = PocketFlipColors.buttonShadow,
            topLeft = Offset(verticalLeft, armInset + 3.dp.toPx()),
            size = Size(thickness, size.height - armInset * 2f),
            cornerRadius = corner
        )
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(armInset, horizontalTop),
            size = Size(size.width - armInset * 2f, thickness),
            cornerRadius = corner
        )
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(verticalLeft, armInset),
            size = Size(thickness, size.height - armInset * 2f),
            cornerRadius = corner
        )
        drawCircle(
            color = PocketFlipColors.buttonCenter,
            radius = thickness * 0.33f,
            center = center
        )
        drawLine(
            color = PocketFlipColors.buttonHighlight,
            start = Offset(armInset + corner.x, horizontalTop + 1.dp.toPx()),
            end = Offset(size.width - armInset - corner.x, horizontalTop + 1.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
private fun PocketFlipPadHitTarget(
    icon: ImageVector,
    contentDescription: String,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .size(size)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
        ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            if (isPressed) {
                drawCircle(
                    color = PocketFlipColors.buttonPressed.copy(alpha = 0.58f),
                    radius = drawContext.size.minDimension * 0.31f,
                    center = center
                )
            }
            if (active) {
                drawCircle(
                    color = PocketFlipColors.modeGlow,
                    radius = drawContext.size.minDimension * 0.29f,
                    center = center
                )
                val lampCenter = Offset(center.x, drawContext.size.height * 0.78f)
                drawCircle(
                    color = PocketFlipColors.modeLamp.copy(alpha = 0.38f),
                    radius = 4.dp.toPx(),
                    center = lampCenter
                )
                drawCircle(
                    color = PocketFlipColors.modeLamp,
                    radius = 2.dp.toPx(),
                    center = lampCenter
                )
            }
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) {
                PocketFlipColors.buttonActiveIcon
            } else {
                PocketFlipColors.buttonIcon
            },
            modifier = Modifier.size(size * 0.39f)
        )
    }
}

@Composable
private fun PocketFlipActionCluster(
    currentSong: Song?,
    isPlaying: Boolean,
    isCurrentSongFavorite: Boolean,
    onPlayPauseClick: () -> Unit,
    onToggleFavoriteClick: (Song) -> Unit,
    compact: Boolean
) {
    Box(
        modifier = Modifier.size(
            width = if (compact) 126.dp else 140.dp,
            height = if (compact) 116.dp else 128.dp
        )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(
                    width = if (compact) 116.dp else 128.dp,
                    height = if (compact) 68.dp else 76.dp
                )
                .background(
                    PocketFlipColors.controlWell,
                    RoundedCornerShape(50)
                )
        )

        PocketFlipRoundAction(
            icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            label = if (isPlaying) "PAUSE" else "PLAY",
            contentDescription = if (isPlaying) "Pause" else "Play",
            faceSize = if (compact) 50.dp else 56.dp,
            onClick = onPlayPauseClick,
            modifier = Modifier.align(Alignment.TopStart)
        )
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
            faceSize = if (compact) 48.dp else 54.dp,
            active = isCurrentSongFavorite,
            onClick = { currentSong?.let(onToggleFavoriteClick) },
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Composable
private fun PocketFlipRoundAction(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    faceSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val faceColor = when {
        isPressed -> PocketFlipColors.actionPressed
        active -> PocketFlipColors.actionActive
        else -> PocketFlipColors.action
    }

    Column(
        modifier = modifier.width(faceSize + 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(faceSize + 12.dp)
                .semantics {
                    this.contentDescription = contentDescription
                    role = Role.Button
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .offset(y = if (isPressed) 2.dp else 0.dp)
                    .size(faceSize)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                faceColor.copy(red = (faceColor.red + 0.12f).coerceAtMost(1f)),
                                faceColor
                            )
                        ),
                        CircleShape
                    )
                    .pocketFlipRoundButtonFinish(isPressed = isPressed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PocketFlipColors.actionIcon,
                    modifier = Modifier.size(faceSize * 0.42f)
                )
            }
        }
        Text(
            text = label,
            color = PocketFlipColors.utilityLabel,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 8.sp,
            letterSpacing = 0.7.sp
        )
    }
}

@Composable
private fun PocketFlipDeckDetails(compact: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 24.dp else 28.dp)
            .padding(horizontal = if (compact) 4.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PocketFlipScrew()
        Spacer(modifier = Modifier.weight(1f))
        PocketFlipSpeakerGrille(compact = compact)
        Spacer(modifier = Modifier.weight(1f))
        PocketFlipScrew(reverseSlot = true)
    }
}

@Composable
private fun PocketFlipUtilitySwitch(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    compact: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Column(
        modifier = Modifier
            .width(if (compact) 78.dp else 88.dp)
            .height(if (compact) 48.dp else 52.dp)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = PocketFlipColors.engravedText,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 8.sp,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .offset(y = if (isPressed) 2.dp else 0.dp)
                .size(
                    width = if (compact) 48.dp else 54.dp,
                    height = if (compact) 17.dp else 19.dp
                )
                .background(
                    if (isPressed) {
                        PocketFlipColors.utilityPressed
                    } else {
                        PocketFlipColors.utility
                    },
                    RoundedCornerShape(50)
                )
                .pocketFlipUtilitySwitchFinish(isPressed = isPressed)
        )
    }
}

@Composable
private fun PocketFlipSpeakerGrille(compact: Boolean) {
    val width = if (compact) 58.dp else 68.dp
    val height = if (compact) 21.dp else 24.dp
    val holes = listOf(
        0.10f to 0.50f,
        0.22f to 0.28f,
        0.22f to 0.72f,
        0.36f to 0.50f,
        0.49f to 0.28f,
        0.49f to 0.72f,
        0.63f to 0.50f,
        0.77f to 0.28f,
        0.77f to 0.72f,
        0.90f to 0.50f
    )

    Canvas(modifier = Modifier.size(width = width, height = height)) {
        val radius = if (compact) 1.7.dp.toPx() else 2.dp.toPx()
        holes.forEach { (x, y) ->
            drawCircle(
                color = PocketFlipColors.speakerHighlight,
                radius = radius,
                center = Offset(size.width * x, size.height * y + 1.dp.toPx())
            )
            drawCircle(
                color = PocketFlipColors.speaker,
                radius = radius,
                center = Offset(size.width * x, size.height * y)
            )
        }
    }
}

@Composable
private fun PocketFlipScrew(reverseSlot: Boolean = false) {
    Canvas(modifier = Modifier.size(13.dp)) {
        val radius = size.minDimension / 2f
        drawCircle(
            color = PocketFlipColors.shellShadow,
            radius = radius,
            center = center + Offset(0f, 1.dp.toPx())
        )
        drawCircle(
            color = PocketFlipColors.screw,
            radius = radius * 0.82f,
            center = center
        )
        val slotOffset = radius * 0.32f
        drawLine(
            color = PocketFlipColors.screwSlot,
            start = if (reverseSlot) {
                center + Offset(-slotOffset, slotOffset)
            } else {
                center + Offset(-slotOffset, -slotOffset)
            },
            end = if (reverseSlot) {
                center + Offset(slotOffset, -slotOffset)
            } else {
                center + Offset(slotOffset, slotOffset)
            },
            strokeWidth = 1.dp.toPx()
        )
    }
}
