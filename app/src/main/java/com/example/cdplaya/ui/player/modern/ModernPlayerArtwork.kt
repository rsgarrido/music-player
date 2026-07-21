package com.example.cdplaya.ui.player.modern

import android.R
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
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
    previousPreviewSong: Song?,
    nextPreviewSong: Song?,
    artworkSize: Dp,
    style: ModernPlayerStyle,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    val carouselState = rememberModernArtworkCarouselState(
        onPrevious = onPreviousClick,
        onNext = onNextClick
    )
    val horizontalDragState = rememberDraggableState { deltaX ->
        carouselState.dragBy(deltaX)
    }
    val dragProgress = carouselState.dragProgress
    val currentArtworkScale = 1f - dragProgress * 0.035f
    val neighborArtworkScale = 0.94f + dragProgress * 0.04f
    val neighborArtworkAlpha = (dragProgress * 1.8f).coerceAtMost(0.9f)
    val carouselItems = buildList {
        previousPreviewSong
            ?.takeIf { song -> song.id != currentSong.id }
            ?.let { song ->
                add(ModernArtworkCarouselItem(song, restingOffsetMultiplier = -1f))
            }

        nextPreviewSong
            ?.takeIf { song ->
                song.id != currentSong.id && song.id != previousPreviewSong?.id
            }
            ?.let { song ->
                add(ModernArtworkCarouselItem(song, restingOffsetMultiplier = 1f))
            }

        add(
            ModernArtworkCarouselItem(
                song = currentSong,
                restingOffsetMultiplier = 0f,
                isCurrent = true
            )
        )
    }

    LaunchedEffect(currentSong.id) {
        carouselState.resetForSongChange()
    }

    Box(
        modifier = Modifier
            .size(artworkSize)
            .onSizeChanged { size ->
                carouselState.updateArtworkWidth(size.width)
            }
            .draggable(
                state = horizontalDragState,
                orientation = Orientation.Horizontal,
                onDragStarted = { carouselState.startDrag() },
                onDragStopped = { velocityX -> carouselState.settle(velocityX) }
            ),
        contentAlignment = Alignment.Center
    ) {
        carouselItems.forEach { item ->
            key(item.song.id) {
                ModernPlayerArtworkCard(
                    song = item.song,
                    artworkSize = artworkSize,
                    style = style,
                    contentDescription = if (item.isCurrent) {
                        "Album art for ${item.song.title}"
                    } else {
                        null
                    },
                    elevation = if (item.isCurrent) 18.dp else 10.dp,
                    modifier = Modifier.graphicsLayer {
                        translationX = carouselState.offsetX +
                                item.restingOffsetMultiplier * carouselState.artworkWidthPx
                        val artworkScale = if (item.isCurrent) {
                            currentArtworkScale
                        } else {
                            neighborArtworkScale
                        }
                        scaleX = artworkScale
                        scaleY = artworkScale
                        alpha = if (item.isCurrent) {
                            1f - dragProgress * 0.08f
                        } else {
                            neighborArtworkAlpha
                        }
                    }
                )
            }
        }
    }
}

private data class ModernArtworkCarouselItem(
    val song: Song,
    val restingOffsetMultiplier: Float,
    val isCurrent: Boolean = false
)

@Composable
private fun ModernPlayerArtworkCard(
    song: Song,
    artworkSize: Dp,
    style: ModernPlayerStyle,
    contentDescription: String?,
    elevation: Dp,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = Modifier
            .size(artworkSize)
            .then(modifier),
        shape = style.artworkShape,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(
            containerColor = style.artworkContainerColor
        )
    ) {
        ModernPlayerAlbumImage(
            currentSong = song,
            contentDescription = contentDescription,
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
