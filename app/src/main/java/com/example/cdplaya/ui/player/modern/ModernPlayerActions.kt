package com.example.cdplaya.ui.player.modern

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.cdplaya.data.Song

@Composable
internal fun ModernPlayerHeader(
    onCollapseClick: () -> Unit,
    style: ModernPlayerStyle
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCollapseClick) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Collapse player",
                tint = style.contentColor
            )
        }

        Text(
            text = "Now Playing",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = style.contentColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
internal fun ModernPlayerFavoriteAction(
    currentSong: Song,
    isCurrentSongFavorite: Boolean,
    onToggleFavoriteClick: (Song) -> Unit,
    style: ModernPlayerStyle
) {
    IconButton(
        onClick = {
            onToggleFavoriteClick(currentSong)
        }
    ) {
        Icon(
            imageVector = if (isCurrentSongFavorite) {
                Icons.Filled.Favorite
            } else {
                Icons.Filled.FavoriteBorder
            },
            contentDescription = if (isCurrentSongFavorite) {
                "Remove from favorites"
            } else {
                "Add to favorites"
            },
            tint = if (isCurrentSongFavorite) {
                style.accentColor
            } else {
                style.contentColor
            }
        )
    }
}

@Composable
internal fun ModernPlayerActions(
    onOpenUpNextClick: () -> Unit,
    style: ModernPlayerStyle
) {
    Button(
        onClick = onOpenUpNextClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = style.secondaryActionBackgroundColor,
            contentColor = style.contentColor
        )
    ) {
        Text(text = "Up Next")
    }
}
