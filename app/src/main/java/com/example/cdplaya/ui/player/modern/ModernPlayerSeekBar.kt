package com.example.cdplaya.ui.player.modern

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cdplaya.ui.formatDuration
import kotlin.math.max

@Composable
internal fun ModernPlayerSeekBar(
    currentPosition: Int,
    duration: Int,
    onSeekChange: (Int) -> Unit,
    seekbarStyle: ModernSeekbarStyle,
    waveformSeed: String,
    style: ModernPlayerStyle
) {
    val safeDuration = duration.coerceAtLeast(1)
    val safePosition = currentPosition.coerceIn(0, safeDuration)

    when (seekbarStyle) {
        ModernSeekbarStyle.CLASSIC_BAR -> ClassicSeekbar(
            safePosition = safePosition,
            safeDuration = safeDuration,
            onSeekChange = onSeekChange,
            style = style
        )

        ModernSeekbarStyle.SLIM_LINE -> VisualSeekbar(
            safePosition = safePosition,
            safeDuration = safeDuration,
            onSeekChange = onSeekChange,
            thumbSize = 8.dp,
            thumbColor = style.contentColor
        ) { sliderProgress ->
            RoundedTrack(
                progress = sliderProgress,
                height = 2.dp,
                activeColor = style.contentColor,
                inactiveColor = style.inactiveTrackColor
            )
        }

        ModernSeekbarStyle.THICK_CAPSULE -> VisualSeekbar(
            safePosition = safePosition,
            safeDuration = safeDuration,
            onSeekChange = onSeekChange,
            thumbSize = 20.dp,
            thumbColor = style.contentColor
        ) { sliderProgress ->
            RoundedTrack(
                progress = sliderProgress,
                height = 12.dp,
                activeColor = style.contentColor,
                inactiveColor = style.inactiveTrackColor
            )
        }

        ModernSeekbarStyle.SEGMENTED -> VisualSeekbar(
            safePosition = safePosition,
            safeDuration = safeDuration,
            onSeekChange = onSeekChange,
            thumbSize = 1.dp,
            thumbColor = Color.Transparent
        ) { sliderProgress ->
            SegmentedTrack(
                progress = sliderProgress,
                activeColor = style.contentColor,
                inactiveColor = style.inactiveTrackColor
            )
        }

        ModernSeekbarStyle.WAVEFORM_PREVIEW -> {
            val bars = remember(waveformSeed) {
                generateWaveformPreviewBars(waveformSeed)
            }
            VisualSeekbar(
                safePosition = safePosition,
                safeDuration = safeDuration,
                onSeekChange = onSeekChange,
                thumbSize = 1.dp,
                thumbColor = Color.Transparent
            ) { sliderProgress ->
                WaveformPreviewTrack(
                    progress = sliderProgress,
                    bars = bars,
                    activeColor = style.contentColor,
                    inactiveColor = style.inactiveTrackColor
                )
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formatDuration(safePosition),
            style = MaterialTheme.typography.bodySmall,
            color = style.timeColor
        )

        Text(
            text = formatDuration(duration),
            style = MaterialTheme.typography.bodySmall,
            color = style.timeColor
        )
    }
}

@Composable
private fun ClassicSeekbar(
    safePosition: Int,
    safeDuration: Int,
    onSeekChange: (Int) -> Unit,
    style: ModernPlayerStyle
) {
    Slider(
        value = safePosition.toFloat(),
        onValueChange = { newPosition ->
            onSeekChange(newPosition.toInt())
        },
        valueRange = 0f..safeDuration.toFloat(),
        colors = SliderDefaults.colors(
            thumbColor = style.contentColor,
            activeTrackColor = style.contentColor,
            inactiveTrackColor = style.inactiveTrackColor
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun VisualSeekbar(
    safePosition: Int,
    safeDuration: Int,
    onSeekChange: (Int) -> Unit,
    thumbSize: Dp,
    thumbColor: Color,
    track: @Composable (Float) -> Unit
) {
    Slider(
        value = safePosition.toFloat(),
        onValueChange = { newPosition ->
            onSeekChange(newPosition.toInt())
        },
        valueRange = 0f..safeDuration.toFloat(),
        thumb = {
            Box(
                modifier = Modifier
                    .size(thumbSize)
                    .background(thumbColor, CircleShape)
            )
        },
        track = { sliderState ->
            track((sliderState.value / safeDuration).coerceIn(0f, 1f))
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun RoundedTrack(
    progress: Float,
    height: Dp,
    activeColor: Color,
    inactiveColor: Color
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val radius = size.height / 2f
        drawRoundRect(
            color = inactiveColor,
            cornerRadius = CornerRadius(radius, radius)
        )
        drawRoundRect(
            color = activeColor,
            size = Size(size.width * progress, size.height),
            cornerRadius = CornerRadius(radius, radius)
        )
    }
}

@Composable
private fun SegmentedTrack(
    progress: Float,
    activeColor: Color,
    inactiveColor: Color,
    segmentCount: Int = 24
) {
    val fills = segmentedFillFractions(progress, segmentCount)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
    ) {
        val gap = 3.dp.toPx()
        val segmentWidth = max(1f, (size.width - gap * (segmentCount - 1)) / segmentCount)
        val radius = size.height / 2f

        fills.forEachIndexed { index, fill ->
            val left = index * (segmentWidth + gap)
            drawRoundRect(
                color = inactiveColor,
                topLeft = androidx.compose.ui.geometry.Offset(left, 0f),
                size = Size(segmentWidth, size.height),
                cornerRadius = CornerRadius(radius, radius)
            )
            if (fill > 0f) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = androidx.compose.ui.geometry.Offset(left, 0f),
                    size = Size(segmentWidth * fill, size.height),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
        }
    }
}

@Composable
private fun WaveformPreviewTrack(
    progress: Float,
    bars: List<Float>,
    activeColor: Color,
    inactiveColor: Color
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
    ) {
        if (bars.isEmpty()) return@Canvas

        val gap = 2.dp.toPx()
        val barWidth = max(1f, (size.width - gap * (bars.size - 1)) / bars.size)

        fun drawBars(color: Color) {
            bars.forEachIndexed { index, amplitude ->
                val barHeight = size.height * amplitude
                val left = index * (barWidth + gap)
                val top = (size.height - barHeight) / 2f
                val radius = barWidth / 2f
                drawRoundRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
        }

        drawBars(inactiveColor)
        clipRect(right = size.width * progress) {
            drawBars(activeColor)
        }
    }
}

internal fun segmentedFillFractions(
    progress: Float,
    segmentCount: Int
): List<Float> {
    if (segmentCount <= 0) return emptyList()

    val filledAmount = progress.coerceIn(0f, 1f) * segmentCount
    return List(segmentCount) { index ->
        (filledAmount - index).coerceIn(0f, 1f)
    }
}

internal fun generateWaveformPreviewBars(
    seed: String,
    barCount: Int = 48
): List<Float> {
    if (barCount <= 0) return emptyList()

    var state = seed.fold(0x811C9DC5u) { hash, character ->
        (hash xor character.code.toUInt()) * 0x01000193u
    }
    if (state == 0u) state = 0x6D2B79F5u

    return List(barCount) { index ->
        state = state xor (state shl 13)
        state = state xor (state shr 17)
        state = state xor (state shl 5)
        val randomUnit = (state and 0xFFFFu).toFloat() / 0xFFFFu.toFloat()
        val contour = 0.82f + ((index % 7) / 6f) * 0.18f
        (0.24f + randomUnit * 0.72f * contour).coerceIn(0.24f, 0.96f)
    }
}
