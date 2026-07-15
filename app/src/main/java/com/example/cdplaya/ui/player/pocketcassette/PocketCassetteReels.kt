package com.example.cdplaya.ui.player.pocketcassette

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.cdplaya.data.Song
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
internal fun PocketCassetteWindow(
    currentSong: Song?,
    isPlaying: Boolean,
    currentPosition: Int,
    duration: Int,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val reelRotation = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                reelRotation.animateTo(
                    targetValue = reelRotation.value + 360f,
                    animationSpec = tween(durationMillis = 4_800, easing = LinearEasing)
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pocketCassetteBluePanelFinish(10.dp)
            .padding(if (compact) 10.dp else 14.dp)
    ) {
        PocketCassetteScrew(modifier = Modifier.align(Alignment.TopStart))
        PocketCassetteScrew(modifier = Modifier.align(Alignment.TopEnd))
        PocketCassetteScrew(modifier = Modifier.align(Alignment.BottomStart))
        PocketCassetteScrew(modifier = Modifier.align(Alignment.BottomEnd))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (compact) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Text(
                    text = "TAPE BAY // SIDE A",
                    color = Color.White.copy(alpha = 0.82f),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 8.sp else 9.sp,
                    letterSpacing = 0.7.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            if (isPlaying) PocketCassetteColors.orange else Color(0xFF623C34),
                            RoundedCornerShape(50)
                        )
                )
                androidx.compose.material3.Text(
                    text = if (isPlaying) "  MOTION" else "  HOLD",
                    color = Color.White.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(7.dp))
                    .background(PocketCassetteColors.window)
            ) {
                AsyncImage(
                    model = currentSong?.albumArtUri,
                    contentDescription = "Album artwork cassette label",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.56f))
                )

                PocketCassetteMechanism(
                    rotation = reelRotation.value,
                    compact = compact,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (compact) 66.dp else 76.dp)
                )

                PocketCassetteTrackLabel(
                    currentSong = currentSong,
                    currentPosition = currentPosition,
                    duration = duration,
                    compact = compact,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRoundRect(
                        color = PocketCassetteColors.windowEdge,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.18f),
                        start = Offset(9.dp.toPx(), 3.dp.toPx()),
                        end = Offset(size.width - 9.dp.toPx(), 3.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }
    }
}

@Composable
private fun PocketCassetteMechanism(
    rotation: Float,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val leftCenter = Offset(size.width * 0.29f, size.height * 0.44f)
        val rightCenter = Offset(size.width * 0.71f, size.height * 0.44f)
        val radius = min(size.width * 0.155f, size.height * if (compact) 0.27f else 0.25f)
            .coerceAtLeast(22.dp.toPx())
        val rollerY = (size.height * 0.88f).coerceAtLeast(leftCenter.y + radius * 0.95f)
        val rollerRadius = radius * 0.13f

        drawLine(
            color = PocketCassetteColors.tape,
            start = Offset(leftCenter.x - radius * 0.86f, leftCenter.y + radius * 0.55f),
            end = Offset(leftCenter.x - radius * 0.32f, rollerY),
            strokeWidth = 3.dp.toPx()
        )
        drawLine(
            color = PocketCassetteColors.tape,
            start = Offset(leftCenter.x - radius * 0.32f, rollerY),
            end = Offset(rightCenter.x + radius * 0.32f, rollerY),
            strokeWidth = 3.dp.toPx()
        )
        drawLine(
            color = PocketCassetteColors.tape,
            start = Offset(rightCenter.x + radius * 0.32f, rollerY),
            end = Offset(rightCenter.x + radius * 0.86f, rightCenter.y + radius * 0.55f),
            strokeWidth = 3.dp.toPx()
        )

        listOf(
            Offset(leftCenter.x - radius * 0.28f, rollerY),
            Offset(rightCenter.x + radius * 0.28f, rollerY)
        ).forEach { rollerCenter ->
            drawCircle(
                color = PocketCassetteColors.reelHub,
                radius = rollerRadius,
                center = rollerCenter
            )
            drawCircle(
                color = PocketCassetteColors.reel,
                radius = rollerRadius,
                center = rollerCenter,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        drawReel(center = leftCenter, radius = radius, rotation = rotation)
        drawReel(center = rightCenter, radius = radius, rotation = rotation)

        val headWidth = radius * 0.52f
        drawRect(
            color = PocketCassetteColors.reelHub,
            topLeft = Offset(size.width / 2f - headWidth / 2f, rollerY - radius * 0.08f),
            size = Size(headWidth, radius * 0.18f)
        )
        drawLine(
            color = PocketCassetteColors.orange.copy(alpha = 0.72f),
            start = Offset(size.width / 2f - headWidth * 0.35f, rollerY + radius * 0.02f),
            end = Offset(size.width / 2f + headWidth * 0.35f, rollerY + radius * 0.02f),
            strokeWidth = 1.dp.toPx()
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawReel(
    center: Offset,
    radius: Float,
    rotation: Float
) {
    drawCircle(
        color = Color.Black.copy(alpha = 0.58f),
        radius = radius * 1.07f,
        center = center
    )
    drawCircle(
        color = PocketCassetteColors.reel.copy(alpha = 0.24f),
        radius = radius,
        center = center
    )
    drawCircle(
        color = PocketCassetteColors.reel,
        radius = radius,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
    drawCircle(
        color = PocketCassetteColors.reelHub,
        radius = radius * 0.31f,
        center = center
    )

    rotate(degrees = rotation, pivot = center) {
        repeat(6) { index ->
            val angle = Math.toRadians((index * 60).toDouble())
            val startRadius = radius * 0.37f
            val endRadius = radius * 0.78f
            drawLine(
                color = PocketCassetteColors.reel,
                start = Offset(
                    x = center.x + cos(angle).toFloat() * startRadius,
                    y = center.y + sin(angle).toFloat() * startRadius
                ),
                end = Offset(
                    x = center.x + cos(angle).toFloat() * endRadius,
                    y = center.y + sin(angle).toFloat() * endRadius
                ),
                strokeWidth = radius * 0.11f
            )
        }
    }
    drawCircle(
        color = PocketCassetteColors.window,
        radius = radius * 0.13f,
        center = center
    )
}

@Composable
private fun PocketCassetteTrackLabel(
    currentSong: Song?,
    currentPosition: Int,
    duration: Int,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xE0192328))
            .padding(
                horizontal = if (compact) 8.dp else 11.dp,
                vertical = if (compact) 5.dp else 7.dp
            ),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        androidx.compose.material3.Text(
            text = currentSong?.title ?: "No track loaded",
            color = PocketCassetteColors.windowText,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 11.sp else 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Text(
                text = currentSong?.artist?.ifBlank { "Unknown artist" }.orEmpty(),
                color = PocketCassetteColors.windowTextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = if (compact) 8.sp else 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            androidx.compose.material3.Text(
                text = "${formatPocketCassetteTime(currentPosition)} / ${formatPocketCassetteTime(duration)}",
                color = PocketCassetteColors.windowText,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 8.sp else 9.sp
            )
        }
        androidx.compose.material3.Text(
            text = currentSong?.album?.ifBlank { "Unknown album" }.orEmpty(),
            color = PocketCassetteColors.windowTextMuted.copy(alpha = 0.8f),
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

internal fun formatPocketCassetteTime(milliseconds: Int): String {
    val totalSeconds = (milliseconds / 1_000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
