package com.example.cdplaya.ui.player.modern

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.Song

@Composable
internal fun ModernPlayerMetadata(
    currentSong: Song,
    style: ModernPlayerStyle
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = currentSong.title.ifBlank { "Unknown Title" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = style.contentColor,
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = currentSong.artist.ifBlank { "Unknown Artist" },
            style = MaterialTheme.typography.titleMedium,
            color = style.secondaryContentColor,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = currentSong.album.ifBlank { "Unknown Album" },
            style = MaterialTheme.typography.bodyMedium,
            color = style.tertiaryContentColor,
            maxLines = 1
        )
    }
}
