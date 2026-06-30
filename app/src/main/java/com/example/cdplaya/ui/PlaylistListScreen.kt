package com.example.cdplaya.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.Playlist

@Composable
fun PlaylistListScreen(
    playlists: List<Playlist>,
    onCreatePlaylistClick: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onRenamePlaylistClick: (Playlist, String) -> Unit,
    onDeletePlaylistClick: (Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    var playlistPendingRename by remember {
        mutableStateOf<Playlist?>(null)
    }

    var playlistPendingDelete by remember {
        mutableStateOf<Playlist?>(null)
    }

    Column(
        modifier = modifier
    ) {
        Button(
            onClick = onCreatePlaylistClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PlaylistAdd,
                contentDescription = null
            )

            Text(
                text = "Create Playlist",
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (playlists.isEmpty()) {
            Text(
                text = "No playlists yet.",
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn {
                items(
                    items = playlists,
                    key = { playlist -> playlist.playlistId }
                ) { playlist ->
                    var isMenuExpanded by remember {
                        mutableStateOf(false)
                    }

                    ListItem(
                        headlineContent = {
                            Text(
                                text = playlist.name,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "${playlist.songCount} song(s)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    isMenuExpanded = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "Playlist actions"
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
                                        Text(text = "Rename")
                                    },
                                    onClick = {
                                        isMenuExpanded = false
                                        playlistPendingRename = playlist
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(text = "Delete")
                                    },
                                    onClick = {
                                        isMenuExpanded = false
                                        playlistPendingDelete = playlist
                                    }
                                )
                            }
                        },
                        modifier = Modifier.clickable {
                            onPlaylistClick(playlist)
                        }
                    )
                }
            }
        }
    }

    val playlistToRename = playlistPendingRename

    if (playlistToRename != null) {
        PlaylistNameDialog(
            title = "Rename Playlist",
            confirmButtonText = "Rename",
            initialName = playlistToRename.name,
            originalName = playlistToRename.name,
            existingPlaylistNames = playlists.map { playlist ->
                playlist.name
            },
            onDismiss = {
                playlistPendingRename = null
            },
            onConfirmClick = { newName ->
                onRenamePlaylistClick(playlistToRename, newName)
                playlistPendingRename = null
            }
        )
    }

    val playlistToDelete = playlistPendingDelete

    if (playlistToDelete != null) {
        DeletePlaylistDialog(
            playlist = playlistToDelete,
            onDismiss = {
                playlistPendingDelete = null
            },
            onConfirmDeleteClick = { playlist ->
                onDeletePlaylistClick(playlist)
                playlistPendingDelete = null
            }
        )
    }
}