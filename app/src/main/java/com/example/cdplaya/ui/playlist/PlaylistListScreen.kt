package com.example.cdplaya.ui.playlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.example.cdplaya.R
import com.example.cdplaya.data.Playlist
import com.example.cdplaya.ui.library.LibraryItemAction
import com.example.cdplaya.ui.library.LibraryItemActionSheet
import com.example.cdplaya.ui.library.LibraryItemActionSheetTarget
import com.example.cdplaya.ui.library.libraryItemActions

@Composable
fun PlaylistListScreen(
    playlists: List<Playlist>,
    onCreatePlaylistClick: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onRenamePlaylistClick: (Playlist, String) -> Unit,
    onDeletePlaylistClick: (Playlist) -> Unit,
    onExportPlaylistClick: (Playlist) -> Unit,
    onImportPlaylistClick: () -> Unit,
    bottomContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    var playlistPendingRename by remember {
        mutableStateOf<Playlist?>(null)
    }

    var playlistPendingDelete by remember {
        mutableStateOf<Playlist?>(null)
    }

    var actionSheetTarget by remember {
        mutableStateOf<LibraryItemActionSheetTarget?>(null)
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
                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                contentDescription = null
            )

            Text(
                text = "Create Playlist",
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        OutlinedButton(
            onClick = onImportPlaylistClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp
                )
        ) {
            Text(text = "Import M3U")
        }

        if (playlists.isEmpty()) {
            Text(
                text = "No playlists yet.",
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = bottomContentPadding)
            ) {
                items(
                    items = playlists,
                    key = { playlist -> playlist.playlistId }
                ) { playlist ->
                    val songCountText = pluralStringResource(
                        R.plurals.song_count,
                        playlist.songCount,
                        playlist.songCount
                    )

                    ListItem(
                        headlineContent = {
                            Text(
                                text = playlist.name,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        supportingContent = {
                            Text(
                                text = songCountText,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        modifier = Modifier.libraryItemActions(
                            clickLabel = "Open ${playlist.name}",
                            onClick = {
                                onPlaylistClick(playlist)
                            },
                            onShowActions = {
                                actionSheetTarget = LibraryItemActionSheetTarget(
                                    title = playlist.name,
                                    subtitle = songCountText,
                                    artworkUri = null,
                                    artworkDescription = "Playlist ${playlist.name}",
                                    actions = listOf(
                                        LibraryItemAction(
                                            label = "Rename",
                                            icon = Icons.Filled.Edit,
                                            onClick = {
                                                playlistPendingRename = playlist
                                            }
                                        ),
                                        LibraryItemAction(
                                            label = "Export as M3U8",
                                            icon = Icons.Filled.Share,
                                            onClick = {
                                                onExportPlaylistClick(playlist)
                                            }
                                        ),
                                        LibraryItemAction(
                                            label = "Delete",
                                            icon = Icons.Filled.Delete,
                                            isDestructive = true,
                                            onClick = {
                                                playlistPendingDelete = playlist
                                            }
                                        )
                                    )
                                )
                            }
                        )
                    )
                }
            }
        }
    }

    actionSheetTarget?.let { target ->
        LibraryItemActionSheet(
            target = target,
            onDismissRequest = {
                actionSheetTarget = null
            }
        )
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
