package com.example.cdplaya.ui.library

import android.R
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
import com.example.cdplaya.ui.sortSongsForArtistDetail

private data class ArtistGroup(
    val name: String,
    val songs: List<Song>
)

@Composable
fun ArtistListScreen(
    songs: List<Song>,
    onArtistClick: (String) -> Unit,
    onArtistPlayClick: (String, List<Song>) -> Unit,
    onArtistShuffleClick: (String, List<Song>) -> Unit,
    onArtistPlayNextClick: (String, List<Song>) -> Unit,
    onArtistAddToQueueClick: (String, List<Song>) -> Unit,
    onArtistAddToPlaylistClick: (String, List<Song>) -> Unit,
    modifier: Modifier = Modifier,
    sortOption: LibrarySortOption = LibrarySortOption.NAME
) {
    val artistGroups = songs
        .groupBy { song -> song.artist.ifBlank { "Unknown Artist" } }
        .map { entry ->
            ArtistGroup(
                name = entry.key,
                songs = sortSongsForArtistDetail(entry.value)
            )
        }

    val artists = when (sortOption) {
        LibrarySortOption.SONG_COUNT -> {
            artistGroups.sortedWith(
                compareByDescending<ArtistGroup> { artist ->
                    artist.songs.size
                }.thenBy { artist ->
                    artist.name.lowercase()
                }
            )
        }

        else -> {
            artistGroups.sortedBy { artist ->
                artist.name.lowercase()
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = artists,
            key = { artist -> artist.name }
        ) { artist ->
            val firstSong = artist.songs.firstOrNull()

            ListItem(
                leadingContent = {
                    AsyncImage(
                        model = firstSong?.albumArtUri,
                        contentDescription = "Artwork for ${artist.name}",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_media_play),
                        placeholder = painterResource(R.drawable.ic_media_play)
                    )
                },
                headlineContent = {
                    Text(text = artist.name)
                },
                supportingContent = {
                    Text(text = "${artist.songs.size} song(s)")
                },
                trailingContent = {
                    ArtistActionsMenu(
                        artistName = artist.name,
                        artistSongs = artist.songs,
                        onPlayClick = onArtistPlayClick,
                        onShuffleClick = onArtistShuffleClick,
                        onPlayNextClick = onArtistPlayNextClick,
                        onAddToQueueClick = onArtistAddToQueueClick,
                        onAddToPlaylistClick = onArtistAddToPlaylistClick
                    )
                },
                modifier = Modifier.clickable {
                    onArtistClick(artist.name)
                }
            )
        }
    }
}

@Composable
private fun ArtistActionsMenu(
    artistName: String,
    artistSongs: List<Song>,
    onPlayClick: (String, List<Song>) -> Unit,
    onShuffleClick: (String, List<Song>) -> Unit,
    onPlayNextClick: (String, List<Song>) -> Unit,
    onAddToQueueClick: (String, List<Song>) -> Unit,
    onAddToPlaylistClick: (String, List<Song>) -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    IconButton(
        onClick = {
            isMenuExpanded = true
        }
    ) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = "Artist actions"
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
                onPlayClick(artistName, artistSongs)
            }
        )

        DropdownMenuItem(
            text = { Text(text = "Shuffle") },
            onClick = {
                isMenuExpanded = false
                onShuffleClick(artistName, artistSongs)
            }
        )

        DropdownMenuItem(
            text = { Text(text = "Play next") },
            onClick = {
                isMenuExpanded = false
                onPlayNextClick(artistName, artistSongs)
            }
        )

        DropdownMenuItem(
            text = { Text(text = "Add to queue") },
            onClick = {
                isMenuExpanded = false
                onAddToQueueClick(artistName, artistSongs)
            }
        )

        DropdownMenuItem(
            text = {
                Text(text = "Add to playlist")
            },
            onClick = {
                isMenuExpanded = false
                onAddToPlaylistClick(artistName, artistSongs)
            }
        )
    }
}