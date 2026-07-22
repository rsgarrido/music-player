package com.example.cdplaya.ui.player.mini

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun ModernMiniPlayer(
    state: MiniPlayerState,
    callbacks: MiniPlayerCallbacks,
    modifier: Modifier = Modifier
) {
    MiniPlayerScaffold(
        state = state,
        callbacks = callbacks,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.76f),
        tonalElevation = 4.dp
    ) { displayedState ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            MiniPlayerArtwork(
                song = displayedState.currentSong,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(displayedState.albumArtSize)
                    .clip(RoundedCornerShape(10.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayedState.currentSong.miniTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    text = displayedState.currentSong.miniArtist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }

            MiniPlayerPlayPauseButton(
                isPlaying = displayedState.isPlaying,
                onClick = callbacks.onPlayPauseClick
            )
        }
    }
}
