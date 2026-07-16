package com.example.cdplaya.ui.player.modern

import android.R
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cdplaya.data.Song

@Composable
internal fun ModernPlayerArtwork(
    currentSong: Song,
    artworkSize: Dp,
    style: ModernPlayerStyle
) {
    Card(
        modifier = Modifier.size(artworkSize),
        shape = style.artworkShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 18.dp),
        colors = CardDefaults.cardColors(
            containerColor = style.artworkContainerColor
        )
    ) {
        AsyncImage(
            model = currentSong.albumArtUri,
            contentDescription = "Album art for ${currentSong.title}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            error = painterResource(R.drawable.ic_media_play),
            placeholder = painterResource(R.drawable.ic_media_play)
        )
    }
}
