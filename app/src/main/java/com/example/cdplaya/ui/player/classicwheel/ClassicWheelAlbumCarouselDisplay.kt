package com.example.cdplaya.ui.player.classicwheel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import kotlin.math.abs

data class ClassicWheelAlbumCarouselItem(
    val title: String,
    val artist: String,
    val albumArtUri: Any?
)

@Composable
fun ClassicWheelAlbumCarouselDisplay(
    items: List<ClassicWheelAlbumCarouselItem>,
    selectedIndex: Int,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFF7F7F2))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No albums found",
                color = Color.Black,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Check your library",
                color = Color.DarkGray,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        return
    }

    val safeSelectedIndex = selectedIndex.coerceIn(0, items.lastIndex)
    val selectedItem = items[safeSelectedIndex]
    val carouselPositions = buildClassicWheelCarouselPositions(
        itemCount = items.size,
        selectedIndex = safeSelectedIndex
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F2))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            carouselPositions.forEach { position ->
                val distanceFromCenter = abs(position.relativeOffset)
                val item = items[position.itemIndex]

                val coverSize = when (distanceFromCenter) {
                    0 -> 132.dp
                    1 -> 112.dp
                    else -> 94.dp
                }

                val scale = when (distanceFromCenter) {
                    0 -> 1f
                    1 -> 0.86f
                    else -> 0.72f
                }

                val alpha = when (distanceFromCenter) {
                    0 -> 1f
                    1 -> 0.9f
                    else -> 0.55f
                }

                val rotation = when {
                    position.relativeOffset < 0 -> 46f
                    position.relativeOffset > 0 -> -46f
                    else -> 0f
                }

                Surface(
                    modifier = Modifier
                        .offset(x = (position.relativeOffset * 56).dp)
                        .size(coverSize)
                        .zIndex(10f - distanceFromCenter)
                        .graphicsLayer {
                            rotationY = rotation
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        },
                    shape = RoundedCornerShape(5.dp),
                    color = Color.Black,
                    shadowElevation = if (distanceFromCenter == 0) {
                        8.dp
                    } else {
                        3.dp
                    }
                ) {
                    AsyncImage(
                        model = item.albumArtUri,
                        contentDescription = "Album art for ${item.title}",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(3.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .aspectRatio(1f),
                        contentScale = ContentScale.Crop,
                        error = painterResource(android.R.drawable.ic_media_play),
                        placeholder = painterResource(android.R.drawable.ic_media_play)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = selectedItem.title,
            color = Color.Black,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Text(
            text = selectedItem.artist,
            color = Color.DarkGray,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Center to open",
            color = Color.DarkGray,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private data class ClassicWheelCarouselPosition(
    val itemIndex: Int,
    val relativeOffset: Int
)

private fun buildClassicWheelCarouselPositions(
    itemCount: Int,
    selectedIndex: Int
): List<ClassicWheelCarouselPosition> {
    if (itemCount <= 0) {
        return emptyList()
    }

    val relativeOffsets = listOf(0, -1, 1, -2, 2)

    return relativeOffsets
        .map { relativeOffset ->
            val rawIndex = selectedIndex + relativeOffset
            val wrappedIndex = ((rawIndex % itemCount) + itemCount) % itemCount

            ClassicWheelCarouselPosition(
                itemIndex = wrappedIndex,
                relativeOffset = relativeOffset
            )
        }
        .distinctBy { position ->
            position.itemIndex
        }
}