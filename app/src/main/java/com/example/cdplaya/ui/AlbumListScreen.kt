package com.example.cdplaya.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cdplaya.data.Song

private data class AlbumGroup(
    val key: String,
    val title: String,
    val artistText: String,
    val songs: List<Song>
)

@Composable
fun AlbumListScreen(
    songs: List<Song>,
    onAlbumClick: (String) -> Unit,
    onAlbumPlayClick: (String, List<Song>) -> Unit,
    onAlbumShuffleClick: (String, List<Song>) -> Unit,
    onAlbumPlayNextClick: (String, List<Song>) -> Unit,
    onAlbumAddToQueueClick: (String, List<Song>) -> Unit,
    modifier: Modifier = Modifier,
    sortOption: LibrarySortOption = LibrarySortOption.TITLE
) {
    val albumGroups = songs
        .groupBy { song -> song.folderPath }
        .map { entry ->
            val albumSongs = sortSongsByAlbumOrder(entry.value)
            val firstSong = albumSongs.first()

            val artists = albumSongs
                .map { song -> song.artist }
                .distinct()
                .filter { artist -> artist.isNotBlank() }

            AlbumGroup(
                key = entry.key,
                title = firstSong.album.ifBlank { "Unknown Album" },
                artistText = if (artists.size == 1) {
                    artists.first()
                } else {
                    "Various Artists"
                },
                songs = albumSongs
            )
        }

    val albums = when (sortOption) {
        LibrarySortOption.ARTIST -> {
            albumGroups.sortedWith(
                compareBy<AlbumGroup> { album ->
                    album.artistText.lowercase()
                }.thenBy { album ->
                    album.title.lowercase()
                }
            )
        }

        LibrarySortOption.SONG_COUNT -> {
            albumGroups.sortedWith(
                compareByDescending<AlbumGroup> { album ->
                    album.songs.size
                }.thenBy { album ->
                    album.title.lowercase()
                }
            )
        }

        else -> {
            albumGroups.sortedBy { album ->
                album.title.lowercase()
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = albums,
            key = { album -> album.key }
        ) { album ->
            val firstSong = album.songs.firstOrNull()

            ListItem(
                leadingContent = {
                    AsyncImage(
                        model = firstSong?.albumArtUri,
                        contentDescription = "Album art for ${album.title}",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(android.R.drawable.ic_media_play),
                        placeholder = painterResource(android.R.drawable.ic_media_play)
                    )
                },
                headlineContent = {
                    Text(text = album.title)
                },
                supportingContent = {
                    Text(text = "${album.artistText} • ${album.songs.size} song(s)")
                },
                trailingContent = {
                    AlbumActionsMenu(
                        albumTitle = album.title,
                        albumSongs = album.songs,
                        onPlayClick = onAlbumPlayClick,
                        onShuffleClick = onAlbumShuffleClick,
                        onPlayNextClick = onAlbumPlayNextClick,
                        onAddToQueueClick = onAlbumAddToQueueClick
                    )
                },
                modifier = Modifier.clickable {
                    onAlbumClick(album.key)
                }
            )
        }
    }
}

@Composable
private fun AlbumActionsMenu(
    albumTitle: String,
    albumSongs: List<Song>,
    onPlayClick: (String, List<Song>) -> Unit,
    onShuffleClick: (String, List<Song>) -> Unit,
    onPlayNextClick: (String, List<Song>) -> Unit,
    onAddToQueueClick: (String, List<Song>) -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    IconButton(
        onClick = {
            isMenuExpanded = true
        }
    ) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = "Album actions"
        )
    }

    DropdownMenu(
        expanded = isMenuExpanded,
        onDismissRequest = {
            isMenuExpanded = false
        }
    ) {
        DropdownMenuItem(
            text = { Text(text = "Play") },
            onClick = {
                isMenuExpanded = false
                onPlayClick(albumTitle, albumSongs)
            }
        )

        DropdownMenuItem(
            text = { Text(text = "Shuffle") },
            onClick = {
                isMenuExpanded = false
                onShuffleClick(albumTitle, albumSongs)
            }
        )

        DropdownMenuItem(
            text = { Text(text = "Play next") },
            onClick = {
                isMenuExpanded = false
                onPlayNextClick(albumTitle, albumSongs)
            }
        )

        DropdownMenuItem(
            text = { Text(text = "Add to queue") },
            onClick = {
                isMenuExpanded = false
                onAddToQueueClick(albumTitle, albumSongs)
            }
        )
    }
}