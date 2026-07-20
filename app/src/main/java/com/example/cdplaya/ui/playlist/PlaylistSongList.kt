package com.example.cdplaya.ui.playlist

import android.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.cdplaya.data.PlaylistSong
import com.example.cdplaya.data.Song
import com.example.cdplaya.data.favoriteKey

@Composable
fun PlaylistSongList(
    playlistSongs: List<Song>,
    playlistSongRows: List<PlaylistSong>,
    currentSongId: Long?,
    recentlyAddedSongIds: Set<Long>,
    favoriteSongKeys: Set<String>,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayNextClick: (Song) -> Unit,
    onAddToQueueClick: (Song) -> Unit,
    onToggleFavoriteClick: (Song) -> Unit,
    onRemovePlaylistSongClick: (PlaylistSong) -> Unit,
    onMovePlaylistSongUpClick: (PlaylistSong) -> Unit,
    onMovePlaylistSongDownClick: (PlaylistSong) -> Unit,
    onEditSongTagsClick: (Song) -> Unit,
    bottomContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = bottomContentPadding)
    ) {
        itemsIndexed(
            items = playlistSongs,
            key = { index, song ->
                playlistSongRows.getOrNull(index)?.playlistSongId ?: "${song.id}-$index"
            }
        ) { index, song ->
            val playlistSong = playlistSongRows.getOrNull(index)
            val isCurrentSong = song.id == currentSongId
            val wasRecentlyAdded = song.id in recentlyAddedSongIds
            val isFavorite = song.favoriteKey() in favoriteSongKeys
            var isMenuExpanded by remember { mutableStateOf(false) }
            val canMoveUp = playlistSong != null && index > 0
            val canMoveDown = playlistSong != null && index < playlistSongRows.lastIndex

            ListItem(
                leadingContent = {
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
                    Text(text = song.artist.ifBlank { "Unknown Artist" })
                },
                trailingContent = {
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
                            text = {
                                Text(text = "Play next")
                            },
                            onClick = {
                                isMenuExpanded = false
                                onPlayNextClick(song)
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(text = "Add to queue")
                            },
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
                            text = {
                                Text(text = "Move up")
                            },
                            enabled = canMoveUp,
                            onClick = {
                                isMenuExpanded = false

                                playlistSong?.let { row ->
                                    onMovePlaylistSongUpClick(row)
                                }
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(text = "Move down")
                            },
                            enabled = canMoveDown,
                            onClick = {
                                isMenuExpanded = false

                                playlistSong?.let { row ->
                                    onMovePlaylistSongDownClick(row)
                                }
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(text = "Edit tags")
                            },
                            onClick = {
                                isMenuExpanded = false
                                onEditSongTagsClick(song)
                            }
                        )

                        if (playlistSong != null) {
                            DropdownMenuItem(
                                text = {
                                    Text(text = "Remove from playlist")
                                },
                                onClick = {
                                    isMenuExpanded = false
                                    onRemovePlaylistSongClick(playlistSong)
                                }
                            )
                        }
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = if (isCurrentSong) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                modifier = Modifier.clickable {
                    onSongClick(song, playlistSongs)
                }
            )
        }
    }
}
