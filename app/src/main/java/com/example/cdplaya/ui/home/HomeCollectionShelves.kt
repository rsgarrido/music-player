package com.example.cdplaya.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cdplaya.data.Song
import com.example.cdplaya.ui.AppShellIcons
import com.example.cdplaya.ui.AppShellAccent
import com.example.cdplaya.ui.AppShellTypography

@Composable
fun HomeRecentlyPlayedShelf(
    songs: List<Song>,
    onSeeAllClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HomeCollectionSectionHeader(
            title = "Recently Played",
            onSeeAllClick = onSeeAllClick,
            modifier = Modifier.padding(start = 16.dp, end = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "recent-featured-${songs.first().id}") {
                HomeFeaturedSongCard(
                    song = songs.first(),
                    onClick = { onSongClick(songs.first()) }
                )
            }

            items(
                items = songs.drop(1),
                key = { song -> song.id }
            ) { song ->
                HomeCompactArtworkCard(
                    song = song,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
fun HomeFavoritesShelf(
    songs: List<Song>,
    onSeeAllClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HomeCollectionSectionHeader(
            title = "Favorites",
            onSeeAllClick = onSeeAllClick,
            modifier = Modifier.padding(start = 16.dp, end = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = songs,
                key = { song -> song.id }
            ) { song ->
                HomeFavoriteRowCard(
                    song = song,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
private fun HomeCollectionSectionHeader(
    title: String,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        HomeSectionHeader(text = title)

        TextButton(onClick = onSeeAllClick) {
            Text(
                text = "SEE ALL  ›",
                style = AppShellTypography.CompactAction,
                color = AppShellAccent
            )
        }
    }
}

@Composable
private fun HomeFeaturedSongCard(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)

    PressableHomeCard(
        onClick = onClick,
        modifier = modifier.width(228.dp),
        shape = shape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        pressedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(208.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            ArtworkPlaceholder(modifier = Modifier.fillMaxSize())

            AsyncImage(
                model = song.albumArtUri,
                contentDescription = "Artwork for ${song.title}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.42f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "LAST PLAYED",
                    style = AppShellTypography.Eyebrow,
                    color = AppShellAccent
                )
                Text(
                    text = song.title,
                    style = AppShellTypography.FeaturedSongTitle,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist.ifBlank { "Unknown artist" },
                    style = AppShellTypography.SongSubtitle,
                    color = Color.White.copy(alpha = 0.76f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HomeCompactArtworkCard(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PressableHomeCard(
        onClick = onClick,
        modifier = modifier.width(132.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(15.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                ArtworkPlaceholder(modifier = Modifier.fillMaxSize())
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = "Artwork for ${song.title}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            SongShelfMetadata(song = song)
        }
    }
}

@Composable
private fun HomeFavoriteRowCard(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PressableHomeCard(
        onClick = onClick,
        modifier = modifier.width(214.dp),
        shape = RoundedCornerShape(19.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                ArtworkPlaceholder(modifier = Modifier.fillMaxSize())
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = "Artwork for ${song.title}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            SongShelfMetadata(
                song = song,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SongShelfMetadata(
    song: Song,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = song.title,
            style = AppShellTypography.SongTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist.ifBlank { "Unknown artist" },
            style = AppShellTypography.SongSubtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ArtworkPlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = AppShellIcons.AlbumStack,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
        )
    }
}
