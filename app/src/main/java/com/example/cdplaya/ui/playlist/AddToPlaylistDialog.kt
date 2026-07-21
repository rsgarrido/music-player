package com.example.cdplaya.ui.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import com.example.cdplaya.R
import com.example.cdplaya.data.Playlist

@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (Playlist) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Add to Playlist")
        },
        text = {
            if (playlists.isEmpty()) {
                Text(text = "Create a playlist first.")
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp)
                ) {
                    items(
                        items = playlists,
                        key = { playlist -> playlist.playlistId }
                    ) { playlist ->
                        ListItem(
                            headlineContent = {
                                Text(text = playlist.name)
                            },
                            supportingContent = {
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.song_count,
                                        playlist.songCount,
                                        playlist.songCount
                                    )
                                )
                            },
                            modifier = Modifier.clickable {
                                onPlaylistSelected(playlist)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}
