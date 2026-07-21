package com.example.cdplaya.ui.library

import android.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.example.cdplaya.data.Song
import com.example.cdplaya.data.favoriteKey
import com.example.cdplaya.ui.getDisplayTrackNumber

@Composable
fun SongList(
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
    modifier: Modifier = Modifier,
    showAlbumName: Boolean = false,
    showTrackNumbers: Boolean = false,
    bottomContentPadding: Dp = 0.dp
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = bottomContentPadding)
    ) {
        items(
            items = songs,
            key = { song -> song.id }
        ) { song ->
            val isCurrentSong = song.id == currentSongId
            val wasRecentlyAdded = song.id in recentlyAddedSongIds
            val isFavorite = song.favoriteKey() in favoriteSongKeys

            ListItem(
                leadingContent = {
                    if (showTrackNumbers) {
                        Text(
                            text = getDisplayTrackNumber(song.trackNumber),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(56.dp)
                        )
                    } else {
                        AsyncImage(
                            model = song.albumArtUri,
                            contentDescription = "Album art for ${song.title}",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.drawable.ic_media_play),
                            placeholder = painterResource(R.drawable.ic_media_play)
                        )
                    }
                },
                headlineContent = {
                    Text(
                        text = song.title.ifBlank { "Unknown Title" },
                        fontWeight = if (isCurrentSong) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )
                },
                supportingContent = {
                    Text(
                        text = if (showAlbumName) {
                            song.album.ifBlank { "Unknown Album" }
                        } else {
                            song.artist.ifBlank { "Unknown Artist" }
                        }
                    )
                },
                trailingContent = {
                    SongActionsMenu(
                        song = song,
                        wasRecentlyAdded = wasRecentlyAdded,
                        isFavorite = isFavorite,
                        onPlayNextClick = onPlayNextClick,
                        onAddToQueueClick = onAddToQueueClick,
                        onToggleFavoriteClick = onToggleFavoriteClick,
                        onAddToPlaylistClick = onAddToPlaylistClick,
                        onEditSongTagsClick = onEditSongTagsClick
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = if (isCurrentSong) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                modifier = Modifier.clickable {
                    onSongClick(song, songs)
                }
            )
        }
    }
}

@Composable
internal fun SongActionsMenu(
    song: Song,
    wasRecentlyAdded: Boolean,
    isFavorite: Boolean,
    onPlayNextClick: (Song) -> Unit,
    onAddToQueueClick: (Song) -> Unit,
    onToggleFavoriteClick: (Song) -> Unit,
    onAddToPlaylistClick: (Song) -> Unit,
    onEditSongTagsClick: (Song) -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    IconButton(
        onClick = {
            isMenuExpanded = true
        }
    ) {
        Icon(
            imageVector = if (wasRecentlyAdded) {
                Icons.Filled.Check
            } else {
                Icons.Filled.MoreVert
            },
            contentDescription = "Song actions",
            tint = if (wasRecentlyAdded) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }

    DropdownMenu(
        expanded = isMenuExpanded,
        onDismissRequest = {
            isMenuExpanded = false
        }
    ) {
        DropdownMenuItem(
            text = { Text(text = "Play next") },
            onClick = {
                isMenuExpanded = false
                onPlayNextClick(song)
            }
        )
        DropdownMenuItem(
            text = { Text(text = "Add to queue") },
            onClick = {
                isMenuExpanded = false
                onAddToQueueClick(song)
            }
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = if (isFavorite) {
                        "Remove from favorites"
                    } else {
                        "Add to favorites"
                    }
                )
            },
            onClick = {
                isMenuExpanded = false
                onToggleFavoriteClick(song)
            }
        )
        DropdownMenuItem(
            text = { Text(text = "Add to playlist") },
            onClick = {
                isMenuExpanded = false
                onAddToPlaylistClick(song)
            }
        )
        DropdownMenuItem(
            text = { Text(text = "Edit tags") },
            onClick = {
                isMenuExpanded = false
                onEditSongTagsClick(song)
            }
        )
    }
}
