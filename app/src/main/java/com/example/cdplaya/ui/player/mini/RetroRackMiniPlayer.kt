package com.example.cdplaya.ui.player.mini

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cdplaya.ui.player.theme.PlayerThemeTokens
import com.example.cdplaya.ui.player.theme.darken
import com.example.cdplaya.ui.player.theme.lighten
import java.util.Locale

@Composable
fun RetroRackMiniPlayer(
    state: MiniPlayerState,
    callbacks: MiniPlayerCallbacks,
    tokens: PlayerThemeTokens,
    modifier: Modifier = Modifier
) {
    val panelColor = tokens.shellColor
    val displayColor = tokens.displayBackgroundColor
    val meterColor = tokens.accentColor

    MiniPlayerScaffold(
        state = state,
        callbacks = callbacks,
        modifier = modifier,
        containerColor = panelColor.darken(0.35f),
        borderColor = panelColor.lighten(0.32f),
        shape = RoundedCornerShape(8.dp)
    ) { displayedState ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            MiniPlayerArtwork(
                song = displayedState.currentSong,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.width(7.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(displayColor, RoundedCornerShape(3.dp))
                    .border(1.dp, panelColor.lighten(0.18f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 7.dp, vertical = 4.dp)
            ) {
                Text(
                    text = displayedState.currentSong.miniTitle.uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.labelMedium,
                    color = meterColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = displayedState.currentSong.miniArtist.uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.labelSmall,
                    color = meterColor.copy(alpha = 0.62f),
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(
                            normalizedMiniPlayerProgress(
                                displayedState.currentPosition,
                                displayedState.duration
                            )
                        )
                        .height(2.dp)
                        .background(meterColor)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            MiniPlayerPlayPauseButton(
                isPlaying = displayedState.isPlaying,
                onClick = callbacks.onPlayPauseClick,
                iconTint = tokens.displayTextColor,
                decoration = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(panelColor, RoundedCornerShape(3.dp))
                            .border(1.dp, panelColor.lighten(0.36f), RoundedCornerShape(3.dp))
                    )
                }
            )
        }
    }
}
