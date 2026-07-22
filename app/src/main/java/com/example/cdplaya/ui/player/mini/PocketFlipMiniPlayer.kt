package com.example.cdplaya.ui.player.mini

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cdplaya.ui.player.theme.PlayerThemeTokens
import com.example.cdplaya.ui.player.theme.darken
import com.example.cdplaya.ui.player.theme.lighten

@Composable
fun PocketFlipMiniPlayer(
    state: MiniPlayerState,
    callbacks: MiniPlayerCallbacks,
    tokens: PlayerThemeTokens,
    modifier: Modifier = Modifier
) {
    val displayText = tokens.displayTextColor
    val buttonColor = tokens.secondaryAccentColor ?: tokens.shellColor.darken(0.3f)

    MiniPlayerScaffold(
        state = state,
        callbacks = callbacks,
        modifier = modifier,
        containerColor = tokens.shellColor,
        borderColor = tokens.shellColor.lighten(0.2f),
        shape = RoundedCornerShape(10.dp)
    ) { displayedState ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .background(tokens.displayBackgroundColor, RoundedCornerShape(4.dp))
                    .border(2.dp, tokens.displayBackgroundColor.darken(0.75f), RoundedCornerShape(4.dp))
                    .padding(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniPlayerArtwork(
                    song = displayedState.currentSong,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(7.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayedState.currentSong.miniTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = displayText,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = displayedState.currentSong.miniArtist,
                        style = MaterialTheme.typography.labelSmall,
                        color = displayText.copy(alpha = 0.68f),
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    SegmentedProgress(
                        progress = normalizedMiniPlayerProgress(
                            displayedState.currentPosition,
                            displayedState.duration
                        ),
                        activeColor = tokens.accentColor,
                        inactiveColor = displayText.copy(alpha = 0.18f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            MiniPlayerPlayPauseButton(
                isPlaying = displayedState.isPlaying,
                onClick = callbacks.onPlayPauseClick,
                iconTint = displayText,
                decoration = {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(buttonColor, RoundedCornerShape(6.dp))
                            .border(2.dp, buttonColor.lighten(0.22f), RoundedCornerShape(6.dp))
                    )
                }
            )
        }
    }
}

@Composable
private fun SegmentedProgress(
    progress: Float,
    activeColor: androidx.compose.ui.graphics.Color,
    inactiveColor: androidx.compose.ui.graphics.Color
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
    ) {
        val segmentCount = 10
        val gap = 2.dp.toPx()
        val segmentWidth = (size.width - gap * (segmentCount - 1)) / segmentCount
        val activeSegments = (progress * segmentCount).toInt()
        repeat(segmentCount) { index ->
            val left = index * (segmentWidth + gap)
            drawRect(
                color = if (index < activeSegments) activeColor else inactiveColor,
                topLeft = Offset(left, 0f),
                size = androidx.compose.ui.geometry.Size(segmentWidth, size.height)
            )
        }
    }
}
