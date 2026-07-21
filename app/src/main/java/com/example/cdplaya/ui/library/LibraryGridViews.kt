package com.example.cdplaya.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cdplaya.data.Song
import com.example.cdplaya.data.favoriteKey
import com.example.cdplaya.ui.AppShellIcons
import com.example.cdplaya.ui.AppShellTypography

@Composable
fun SongGrid(
    songs: List<Song>,
    currentSongId: Long?,
    recentlyAddedSongIds: Set<Long>,
    favoriteSongKeys: Set<String>,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayNextClick: (Song) -> Unit,
    onAddToQueueClick: (Song) -> Unit,
    onToggleFavoriteClick: (Song) -> Unit,
    onAddToPlaylistClick: (Song) -> Unit,
    onEditSongTagsClick: (Song) -> Unit,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 148.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = libraryGridPadding(bottomContentPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(
            items = songs,
            key = { song -> song.id }
        ) { song ->
            val isCurrentSong = song.id == currentSongId
            LibraryGridCard(
                artworkUri = song.albumArtUri,
                artworkDescription = "Album art for ${song.title}",
                title = song.title.ifBlank { "Unknown Title" },
                subtitle = song.artist.ifBlank { "Unknown Artist" },
                selected = isCurrentSong,
                onClick = { onSongClick(song, songs) },
                actions = {
                    SongActionsMenu(
                        song = song,
                        wasRecentlyAdded = song.id in recentlyAddedSongIds,
                        isFavorite = song.favoriteKey() in favoriteSongKeys,
                        onPlayNextClick = onPlayNextClick,
                        onAddToQueueClick = onAddToQueueClick,
                        onToggleFavoriteClick = onToggleFavoriteClick,
                        onAddToPlaylistClick = onAddToPlaylistClick,
                        onEditSongTagsClick = onEditSongTagsClick
                    )
                }
            )
        }
    }
}

@Composable
fun AlbumGridScreen(
    songs: List<Song>,
    sortOption: LibrarySortOption,
    onAlbumClick: (String) -> Unit,
    onAlbumPlayClick: (String, List<Song>) -> Unit,
    onAlbumShuffleClick: (String, List<Song>) -> Unit,
    onAlbumPlayNextClick: (String, List<Song>) -> Unit,
    onAlbumAddToQueueClick: (String, List<Song>) -> Unit,
    onAlbumAddToPlaylistClick: (String, List<Song>) -> Unit,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier
) {
    val albums = sortedLibraryAlbumGroups(songs, sortOption)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 148.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = libraryGridPadding(bottomContentPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(
            items = albums,
            key = { album -> album.key }
        ) { album ->
            LibraryGridCard(
                artworkUri = album.songs.firstOrNull()?.albumArtUri,
                artworkDescription = "Album art for ${album.title}",
                title = album.title,
                subtitle = album.artistText,
                onClick = { onAlbumClick(album.key) },
                actions = {
                    AlbumActionsMenu(
                        albumTitle = album.title,
                        albumSongs = album.songs,
                        onPlayClick = onAlbumPlayClick,
                        onShuffleClick = onAlbumShuffleClick,
                        onPlayNextClick = onAlbumPlayNextClick,
                        onAddToQueueClick = onAlbumAddToQueueClick,
                        onAddToPlaylistClick = onAlbumAddToPlaylistClick
                    )
                }
            )
        }
    }
}

@Composable
fun ArtistGridScreen(
    songs: List<Song>,
    sortOption: LibrarySortOption,
    onArtistClick: (String) -> Unit,
    onArtistPlayClick: (String, List<Song>) -> Unit,
    onArtistShuffleClick: (String, List<Song>) -> Unit,
    onArtistPlayNextClick: (String, List<Song>) -> Unit,
    onArtistAddToQueueClick: (String, List<Song>) -> Unit,
    onArtistAddToPlaylistClick: (String, List<Song>) -> Unit,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier
) {
    val artists = sortedLibraryArtistGroups(songs, sortOption)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 148.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = libraryGridPadding(bottomContentPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(
            items = artists,
            key = { artist -> artist.name }
        ) { artist ->
            LibraryGridCard(
                artworkUri = artist.songs.firstOrNull()?.albumArtUri,
                artworkDescription = "Artwork for ${artist.name}",
                title = artist.name,
                subtitle = null,
                onClick = { onArtistClick(artist.name) },
                actions = {
                    ArtistActionsMenu(
                        artistName = artist.name,
                        artistSongs = artist.songs,
                        onPlayClick = onArtistPlayClick,
                        onShuffleClick = onArtistShuffleClick,
                        onPlayNextClick = onArtistPlayNextClick,
                        onAddToQueueClick = onArtistAddToQueueClick,
                        onAddToPlaylistClick = onArtistAddToPlaylistClick
                    )
                }
            )
        }
    }
}

@Composable
private fun LibraryGridCard(
    artworkUri: Any?,
    artworkDescription: String,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    actions: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            1.dp,
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
            }
        )
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
                Icon(
                    imageVector = AppShellIcons.AlbumStack,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                )
                AsyncImage(
                    model = artworkUri,
                    contentDescription = artworkDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        actions()
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = AppShellTypography.SongTitle,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = AppShellTypography.SongSubtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun libraryGridPadding(bottomContentPadding: Dp): PaddingValues {
    return PaddingValues(
        start = 12.dp,
        top = 8.dp,
        end = 12.dp,
        bottom = bottomContentPadding
    )
}
