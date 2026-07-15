package com.example.cdplaya.ui.player.pocketflip

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.cdplaya.data.Song

@Composable
internal fun PocketFlipDisplayHalf(
    currentSong: Song?,
    isPlaying: Boolean,
    currentPosition: Int,
    duration: Int,
    onSeekChange: (Int) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val bezelRadius = if (compact) 18.dp else 22.dp
    val screenRadius = if (compact) 6.dp else 8.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bezelRadius))
            .pocketFlipBezelFinish(bezelRadius)
            .padding(if (compact) 10.dp else 14.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
    ) {
        PocketFlipDisplayHeader(isPlaying = isPlaying, compact = compact)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(screenRadius))
                .background(PocketFlipColors.display)
                .pocketFlipLcdFrameFinish(screenRadius)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (compact) 9.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PocketFlipArtwork(
                        song = currentSong,
                        compact = compact,
                        modifier = Modifier.size(if (compact) 104.dp else 116.dp)
                    )

                    PocketFlipMetadata(
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        compact = compact,
                        modifier = Modifier.weight(1f)
                    )
                }

                PocketFlipLcdMeter(
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    compact = compact,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            PocketFlipLcdOverlay(modifier = Modifier.fillMaxSize())
        }

        PocketFlipSeekBar(
            currentPosition = currentPosition,
            duration = duration,
            onSeekChange = onSeekChange,
            compact = compact
        )
    }
}

@Composable
private fun PocketFlipDisplayHeader(
    isPlaying: Boolean,
    compact: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PocketFlipBezelScrew()
        Spacer(modifier = Modifier.width(if (compact) 6.dp else 8.dp))
        Box(
            modifier = Modifier
                .size(if (compact) 7.dp else 8.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    if (isPlaying) PocketFlipColors.statusOn else PocketFlipColors.statusIdle
                )
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "PWR",
            color = PocketFlipColors.bezelTextMuted,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 8.sp,
            letterSpacing = 0.6.sp
        )
        Spacer(modifier = Modifier.width(if (compact) 7.dp else 10.dp))
        Text(
            text = "POCKET FLIP // AUDIO",
            color = PocketFlipColors.bezelText,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 9.sp else 10.sp,
            letterSpacing = 0.7.sp,
            maxLines = 1
        )
        Spacer(modifier = Modifier.weight(1f))
        PocketFlipBattery(compact = compact)
        Spacer(modifier = Modifier.width(if (compact) 6.dp else 8.dp))
        PocketFlipBezelScrew(reverseSlot = true)
    }
}

@Composable
private fun PocketFlipBattery(compact: Boolean) {
    Canvas(
        modifier = Modifier.size(
            width = if (compact) 29.dp else 33.dp,
            height = if (compact) 12.dp else 14.dp
        )
    ) {
        val capWidth = 2.dp.toPx()
        val bodyWidth = size.width - capWidth - 1.dp.toPx()
        val corner = CornerRadius(2.dp.toPx())
        drawRoundRect(
            color = PocketFlipColors.bezelTextMuted,
            topLeft = Offset.Zero,
            size = Size(bodyWidth, size.height),
            cornerRadius = corner,
            style = Stroke(width = 1.dp.toPx())
        )
        drawRoundRect(
            color = PocketFlipColors.bezelTextMuted,
            topLeft = Offset(bodyWidth + 1.dp.toPx(), size.height * 0.3f),
            size = Size(capWidth, size.height * 0.4f),
            cornerRadius = CornerRadius(1.dp.toPx())
        )
        val gap = 2.dp.toPx()
        val segmentWidth = (bodyWidth - gap * 4f) / 3f
        repeat(3) { index ->
            drawRect(
                color = PocketFlipColors.screenAccent,
                topLeft = Offset(
                    x = gap + index * (segmentWidth + gap),
                    y = gap
                ),
                size = Size(segmentWidth, size.height - gap * 2f)
            )
        }
    }
}

@Composable
private fun PocketFlipBezelScrew(reverseSlot: Boolean = false) {
    Canvas(modifier = Modifier.size(9.dp)) {
        drawCircle(color = Color(0xFF08080A), radius = size.minDimension / 2f)
        drawCircle(color = Color(0xFF303036), radius = size.minDimension * 0.35f)
        val slot = size.minDimension * 0.19f
        drawLine(
            color = Color(0xFF0C0C0E),
            start = if (reverseSlot) {
                center + Offset(-slot, slot)
            } else {
                center + Offset(-slot, -slot)
            },
            end = if (reverseSlot) {
                center + Offset(slot, -slot)
            } else {
                center + Offset(slot, slot)
            },
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
private fun PocketFlipArtwork(
    song: Song?,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (compact) 3.dp else 4.dp))
            .background(PocketFlipColors.artworkWell)
            .pocketFlipArtworkFrameFinish(),
        contentAlignment = Alignment.Center
    ) {
        if (song?.albumArtUri != null) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = "Current album artwork",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (compact) 4.dp else 5.dp)
            )
            PocketFlipArtworkLcdTreatment(
                modifier = Modifier
                    .matchParentSize()
                    .padding(if (compact) 4.dp else 5.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Album,
                contentDescription = null,
                tint = PocketFlipColors.screenTextMuted,
                modifier = Modifier.size(if (compact) 42.dp else 54.dp)
            )
        }
    }
}

@Composable
private fun PocketFlipMetadata(
    currentSong: Song?,
    isPlaying: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        PocketFlipLcdStatusRow(
            currentSong = currentSong,
            isPlaying = isPlaying,
            compact = compact
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = currentSong?.title ?: "No track loaded",
                color = PocketFlipColors.screenText,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 13.sp else 14.sp,
                lineHeight = if (compact) 16.sp else 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(if (compact) 2.dp else 3.dp))
            Text(
                text = currentSong?.artist?.ifBlank { "Unknown artist" } ?: "",
                color = PocketFlipColors.screenText,
                fontFamily = FontFamily.Monospace,
                fontSize = if (compact) 9.sp else 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = currentSong?.album?.ifBlank { "Unknown album" } ?: "",
                color = PocketFlipColors.screenTextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = if (compact) 8.sp else 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

    }
}

@Composable
private fun PocketFlipSeekBar(
    currentPosition: Int,
    duration: Int,
    onSeekChange: (Int) -> Unit,
    compact: Boolean
) {
    val safeDuration = duration.coerceAtLeast(1)
    val safePosition = currentPosition.coerceIn(0, safeDuration)
    val progress = safePosition.toFloat() / safeDuration.toFloat()

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 38.dp else 42.dp)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = safePosition.toFloat(),
                        range = 0f..safeDuration.toFloat()
                    )
                    setProgress { target ->
                        onSeekChange(target.coerceIn(0f, safeDuration.toFloat()).toInt())
                        true
                    }
                }
                .pointerInput(safeDuration) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        fun seekTo(x: Float) {
                            val fraction = (x / size.width.toFloat()).coerceIn(0f, 1f)
                            onSeekChange((fraction * safeDuration).toInt())
                        }

                        seekTo(down.position.x)
                        var pressed = true
                        while (pressed) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            pressed = change.pressed
                            if (pressed) {
                                seekTo(change.position.x)
                                change.consume()
                            }
                        }
                    }
                }
        ) {
            val housingHeight = if (compact) 15.dp.toPx() else 17.dp.toPx()
            val housingTop = (size.height - housingHeight) / 2f
            val inset = 4.dp.toPx()
            val segmentGap = 2.dp.toPx()
            val segmentCount = 18
            val usableWidth = size.width - inset * 2f
            val segmentWidth = (usableWidth - segmentGap * (segmentCount - 1)) / segmentCount

            drawRoundRect(
                color = PocketFlipColors.seekHousing,
                topLeft = Offset(0f, housingTop),
                size = Size(size.width, housingHeight),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
            repeat(segmentCount) { index ->
                val segmentProgress = (index + 1).toFloat() / segmentCount
                drawRect(
                    color = if (segmentProgress <= progress) {
                        PocketFlipColors.screenAccent
                    } else {
                        PocketFlipColors.seekInactive
                    },
                    topLeft = Offset(
                        x = inset + index * (segmentWidth + segmentGap),
                        y = housingTop + 4.dp.toPx()
                    ),
                    size = Size(segmentWidth, housingHeight - 8.dp.toPx())
                )
            }

            val thumbWidth = 7.dp.toPx()
            val thumbHeight = housingHeight + 8.dp.toPx()
            val thumbX = (progress * size.width - thumbWidth / 2f)
                .coerceIn(0f, size.width - thumbWidth)
            drawRoundRect(
                color = PocketFlipColors.seekThumb,
                topLeft = Offset(thumbX, (size.height - thumbHeight) / 2f),
                size = Size(thumbWidth, thumbHeight),
                cornerRadius = CornerRadius(1.dp.toPx())
            )
            drawLine(
                color = PocketFlipColors.seekThumbHighlight,
                start = Offset(thumbX + 1.dp.toPx(), (size.height - thumbHeight) / 2f),
                end = Offset(thumbX + 1.dp.toPx(), (size.height + thumbHeight) / 2f),
                strokeWidth = 1.dp.toPx()
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatPocketFlipTime(safePosition),
                color = PocketFlipColors.bezelText,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 9.sp else 10.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "TRACK POSITION",
                color = PocketFlipColors.bezelTextMuted,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                letterSpacing = 0.6.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatPocketFlipTime(duration),
                color = PocketFlipColors.bezelText,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 9.sp else 10.sp
            )
        }
    }
}

private fun formatPocketFlipTime(milliseconds: Int): String {
    val totalSeconds = (milliseconds / 1_000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
