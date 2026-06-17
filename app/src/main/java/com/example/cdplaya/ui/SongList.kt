package com.example.cdplaya.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cdplaya.data.Song

@Composable
fun SongList(
    songs: List<Song>,
    currentSongId: Long?,
    recentlyAddedSongIds: Set<Long>,
    onSongClick: (Song, List<Song>) -> Unit,
    onAddToQueueClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
    showAlbumName: Boolean = false,
    showTrackNumbers: Boolean = false
) {
    LazyColumn(
        modifier = modifier
    ) {
        items(
            items = songs,
            key = { song -> song.id }
        ) { song ->
            val isCurrentSong = song.id == currentSongId
            val wasRecentlyAdded = song.id in recentlyAddedSongIds

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
                            error = painterResource(android.R.drawable.ic_media_play),
                            placeholder = painterResource(android.R.drawable.ic_media_play)
                        )
                    }
                },
                headlineContent = {
                    Text(
                        text = song.title,
                        fontWeight = if (isCurrentSong) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )
                },
                supportingContent = {
                    Text(text = song.artist)
                },
                trailingContent = {
                    IconButton(
                        onClick = {
                            onAddToQueueClick(song)
                        }
                    ) {
                        Icon(
                            imageVector = if (wasRecentlyAdded) {
                                Icons.Filled.Check
                            } else {
                                Icons.Filled.PlaylistAdd
                            },
                            contentDescription = if (wasRecentlyAdded) {
                                "${song.title} added to queue"
                            } else {
                                "Add ${song.title} to queue"
                            },
                            tint = if (wasRecentlyAdded) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
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
                    onSongClick(song, songs)
                }
            )
        }
    }
}