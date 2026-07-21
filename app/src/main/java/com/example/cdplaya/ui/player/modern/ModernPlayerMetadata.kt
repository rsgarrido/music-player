package com.example.cdplaya.ui.player.modern

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
    AnimatedContent(
        targetState = currentSong,
        transitionSpec = {
            fadeIn(
                animationSpec = tween(ModernPlayerDefaults.SongTransitionDurationMillis)
            ).togetherWith(
                fadeOut(animationSpec = tween(140))
            )
        },
        contentKey = { song -> song.id },
        modifier = Modifier.fillMaxWidth(),
        label = "modernPlayerMetadata"
    ) { displayedSong ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = displayedSong.title.ifBlank { "Unknown Title" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = style.contentColor,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = displayedSong.artist.ifBlank { "Unknown Artist" },
                style = MaterialTheme.typography.titleMedium,
                color = style.secondaryContentColor,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = displayedSong.album.ifBlank { "Unknown Album" },
                style = MaterialTheme.typography.bodyMedium,
                color = style.tertiaryContentColor,
                maxLines = 1
            )
        }
    }
}
