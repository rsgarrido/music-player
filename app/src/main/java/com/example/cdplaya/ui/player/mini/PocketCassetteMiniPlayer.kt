package com.example.cdplaya.ui.player.mini

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cdplaya.ui.player.theme.PlayerThemeTokens
import com.example.cdplaya.ui.player.theme.darken
import com.example.cdplaya.ui.player.theme.lighten
import java.util.Locale

@Composable
fun PocketCassetteMiniPlayer(
    state: MiniPlayerState,
    callbacks: MiniPlayerCallbacks,
    tokens: PlayerThemeTokens,
    modifier: Modifier = Modifier
) {
    val panel = tokens.accentColor
    val ink = tokens.displayTextColor

    MiniPlayerScaffold(
        state = state,
        callbacks = callbacks,
        modifier = modifier,
        containerColor = tokens.shellColor,
        borderColor = panel.lighten(0.18f),
        shape = RoundedCornerShape(12.dp)
    ) { displayedState ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            CassetteWindow(
                state = displayedState,
                tokens = tokens
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayedState.currentSong.miniTitle.uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.labelMedium,
                    color = ink,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = displayedState.currentSong.miniArtist.uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            MiniPlayerPlayPauseButton(
                isPlaying = displayedState.isPlaying,
                onClick = callbacks.onPlayPauseClick,
                iconTint = ink,
                decoration = {
                    Box(
                        modifier = Modifier
                            .size(width = 34.dp, height = 28.dp)
                            .background(
                                tokens.secondaryAccentColor ?: panel.darken(0.25f),
                                RoundedCornerShape(5.dp)
                            )
                            .border(
                                1.dp,
                                panel.lighten(0.28f),
                                RoundedCornerShape(5.dp)
                            )
                    )
                }
            )
        }
    }
}

@Composable
private fun CassetteWindow(
    state: MiniPlayerState,
    tokens: PlayerThemeTokens
) {
    val progress = normalizedMiniPlayerProgress(state.currentPosition, state.duration)
    Row(
        modifier = Modifier
            .width(92.dp)
            .height(44.dp)
            .background(tokens.displayBackgroundColor, RoundedCornerShape(6.dp))
            .border(1.dp, tokens.accentColor.darken(0.35f), RoundedCornerShape(6.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiniPlayerArtwork(
            song = state.currentSong,
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Canvas(modifier = Modifier.weight(1f).height(30.dp)) {
            val reelRadius = size.minDimension * 0.27f
            val left = Offset(size.width * 0.27f, size.height * 0.46f)
            val right = Offset(size.width * 0.73f, size.height * 0.46f)
            drawLine(
                color = tokens.displayTextColor.copy(alpha = 0.48f),
                start = left,
                end = right,
                strokeWidth = 2.dp.toPx()
            )
            listOf(left, right).forEach { center ->
                drawCircle(
                    color = tokens.displayTextColor.copy(alpha = 0.72f),
                    radius = reelRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = tokens.accentColor,
                    radius = reelRadius * 0.34f,
                    center = center
                )
            }
            drawLine(
                color = tokens.secondaryAccentColor ?: Color.White,
                start = Offset(0f, size.height - 2.dp.toPx()),
                end = Offset(size.width * progress, size.height - 2.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
