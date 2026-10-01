package io.github.rsgarrido.sazanami.ui.playlist

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.data.Playlist

@Composable
fun DeletePlaylistDialog(
    playlist: Playlist,
    onDismiss: () -> Unit,
    onConfirmDeleteClick: (Playlist) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.playlist_delete_title))
        },
        text = {
            Text(
                text = stringResource(R.string.playlist_delete_explanation, playlist.name)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmDeleteClick(playlist)
                }
            ) {
                Text(text = stringResource(R.string.playlist_delete_action))
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss
            ) {
                Text(text = stringResource(R.string.settings_cancel))
            }
        }
    )
}
