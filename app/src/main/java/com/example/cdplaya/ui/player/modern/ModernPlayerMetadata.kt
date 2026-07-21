package com.example.cdplaya.ui.player.modern

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.audioquality.AudioQualityInfo
import com.example.cdplaya.player.audioquality.AudioQualityRepository

@Composable
internal fun ModernPlayerMetadataCarousel(
    carouselSongs: ModernCarouselSongs,
    carouselState: ModernArtworkCarouselState,
    audioQualityRepository: AudioQualityRepository,
    transitionStyle: ModernArtworkTransitionStyle,
    style: ModernPlayerStyle,
    modifier: Modifier = Modifier
) {
    var pageWidthPx by remember { mutableFloatStateOf(1f) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                if (size.width > 0) {
                    pageWidthPx = size.width.toFloat()
                }
            },
        contentAlignment = Alignment.TopStart
    ) {
        carouselSongs.items().forEach { item ->
            key(item.song.id) {
                ModernPlayerMetadataPage(
                    song = item.song,
                    audioQualityRepository = audioQualityRepository,
                    style = style,
                    modifier = Modifier.graphicsLayer {
                        val gestureOffset =
                            carouselState.offsetX / carouselState.artworkWidthPx
                        val transform = modernMetadataPageTransform(
                            style = transitionStyle,
                            gestureOffset = gestureOffset,
                            restingOffset = item.restingOffsetMultiplier,
                            isCurrent = item.isCurrent
                        )
                        translationX = transform.translationMultiplier * pageWidthPx
                        scaleX = transform.scale
                        scaleY = transform.scale
                        alpha = transform.alpha
                    }
                )
            }
        }
    }
}

@Composable
private fun ModernPlayerMetadataPage(
    song: Song,
    audioQualityRepository: AudioQualityRepository,
    style: ModernPlayerStyle,
    modifier: Modifier = Modifier
) {
    var audioQualityInfo by remember(song.id, song.filePath) {
        mutableStateOf<AudioQualityInfo?>(null)
    }

    LaunchedEffect(song.id, song.filePath) {
        audioQualityInfo = audioQualityRepository.getAudioQualityInfo(song)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ModernPlayerMetadata(
            currentSong = song,
            style = style
        )

        ModernPlayerAudioQualityBadge(
            audioQualityInfo = audioQualityInfo,
            style = style,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = 12.dp)
        )
    }
}

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
