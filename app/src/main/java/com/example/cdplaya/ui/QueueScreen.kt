package com.example.cdplaya.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cdplaya.data.Song

@Composable
fun QueueScreen(
    queuedSongs: List<Song>,
    onBackClick: () -> Unit,
    onRemoveFromQueueClick: (Int) -> Unit,
    onMoveQueueItemUpClick: (Int) -> Unit,
    onMoveQueueItemDownClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back to library"
                )
            }

            Text(
                text = "Queue",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (queuedSongs.isEmpty()) {
            Text(
                text = "Your queue is empty.",
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn {
                items(
                    count = queuedSongs.size,
                    key = { index -> "${queuedSongs[index].id}-$index" }
                ) { index ->
                    val song = queuedSongs[index]

                    ListItem(
                        leadingContent = {
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
                        },
                        headlineContent = {
                            Text(text = song.title)
                        },
                        supportingContent = {
                            Text(text = song.artist)
                        },
                        trailingContent = {
                            Row {
                                IconButton(
                                    onClick = {
                                        onMoveQueueItemUpClick(index)
                                    },
                                    enabled = index > 0
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowUp,
                                        contentDescription = "Move up"
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        onMoveQueueItemDownClick(index)
                                    },
                                    enabled = index < queuedSongs.lastIndex
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowDown,
                                        contentDescription = "Move down"
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        onRemoveFromQueueClick(index)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Remove from queue"
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}