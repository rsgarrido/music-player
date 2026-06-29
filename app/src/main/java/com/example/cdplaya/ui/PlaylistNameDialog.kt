package com.example.cdplaya.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun PlaylistNameDialog(
    onDismiss: () -> Unit,
    onCreateClick: (String) -> Unit
) {
    var playlistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Create Playlist")
        },
        text = {
            OutlinedTextField(
                value = playlistName,
                onValueChange = { value ->
                    playlistName = value
                },
                label = {
                    Text(text = "Playlist name")
                },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreateClick(playlistName)
                },
                enabled = playlistName.isNotBlank()
            ) {
                Text(text = "Create")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss
            ) {
                Text(text = "Cancel")
            }
        }
    )
}