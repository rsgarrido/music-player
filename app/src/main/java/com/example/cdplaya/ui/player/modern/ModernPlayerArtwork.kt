package com.example.cdplaya.ui.player.modern

import android.R
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
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
        ModernPlayerAlbumImage(
            currentSong = currentSong,
            contentDescription = "Album art for ${currentSong.title}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
internal fun ModernPlayerAlbumImage(
    currentSong: Song,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    transitionDurationMillis: Int = ModernPlayerDefaults.SongTransitionDurationMillis
) {
    val context = LocalContext.current
    val fallbackPainter = painterResource(R.drawable.ic_media_play)
    var retainedPainter by remember { mutableStateOf<Painter?>(null) }
    val request = remember(currentSong.id, currentSong.albumArtUri) {
        ImageRequest.Builder(context)
            .data(currentSong.albumArtUri)
            .crossfade(transitionDurationMillis)
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        transform = { state ->
            when (state) {
                is AsyncImagePainter.State.Loading -> state.copy(
                    painter = retainedPainter ?: fallbackPainter
                )

                is AsyncImagePainter.State.Error -> state.copy(
                    painter = fallbackPainter
                )

                else -> state
            }
        },
        onState = { state ->
            when (state) {
                is AsyncImagePainter.State.Success -> retainedPainter = state.painter
                is AsyncImagePainter.State.Error -> retainedPainter = fallbackPainter
                else -> Unit
            }
        },
        contentScale = contentScale
    )
}
