package io.github.rsgarrido.sazanami.ui.playlist

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.github.rsgarrido.sazanami.R

@Composable
fun PlaylistNameDialog(
    title: String = stringResource(R.string.playlist_create_title),
    confirmButtonText: String = stringResource(R.string.playlist_create_action),
    initialName: String = "",
    existingPlaylistNames: List<String> = emptyList(),
    originalName: String? = null,
    onDismiss: () -> Unit,
    onConfirmClick: (String) -> Unit
) {
    var playlistName by remember {
        mutableStateOf(initialName)
    }

    val trimmedName = playlistName.trim()

    val duplicateNameExists = existingPlaylistNames.any { existingName ->
        existingName.equals(trimmedName, ignoreCase = true) &&
                !existingName.equals(originalName, ignoreCase = true)
    }

    val errorMessage = when {
        trimmedName.isBlank() -> stringResource(R.string.playlist_name_empty)
        duplicateNameExists -> stringResource(R.string.playlist_name_duplicate)
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title)
        },
        text = {
            OutlinedTextField(
                value = playlistName,
                onValueChange = { value ->
                    playlistName = value
                },
                label = {
                    Text(text = stringResource(R.string.playlist_name_label))
                },
                singleLine = true,
                isError = errorMessage != null,
                supportingText = {
                    if (errorMessage != null) {
                        Text(text = errorMessage)
                    }
                }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmClick(trimmedName)
                },
                enabled = errorMessage == null
            ) {
                Text(text = confirmButtonText)
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
