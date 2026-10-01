package io.github.rsgarrido.sazanami.ui.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.data.Playlist
import io.github.rsgarrido.sazanami.data.PlaylistFolder

@Composable
fun PlaylistFolderNameDialog(
    title: String,
    confirmButtonText: String,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onConfirmClick: (String) -> Unit,
    initialName: String = "",
    originalName: String? = null
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val trimmedName = name.trim()
    val duplicate = existingNames.any {
        it.equals(trimmedName, ignoreCase = true) &&
            !it.equals(originalName, ignoreCase = true)
    }
    val error = when {
        trimmedName.isBlank() -> stringResource(R.string.playlist_folder_name_empty)
        duplicate -> stringResource(R.string.playlist_folder_name_duplicate)
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.playlist_folder_name_label)) },
                singleLine = true,
                isError = error != null,
                supportingText = { error?.let { Text(it) } }
            )
        },
        confirmButton = {
            Button(
                enabled = error == null,
                onClick = { onConfirmClick(trimmedName) }
            ) { Text(confirmButtonText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) } }
    )
}

@Composable
fun DeletePlaylistFolderDialog(
    folder: PlaylistFolder,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlist_folder_delete_title)) },
        text = {
            Text(
                pluralStringResource(
                    R.plurals.playlist_folder_delete_explanation,
                    folder.playlistCount,
                    folder.name,
                    folder.playlistCount
                )
            )
        },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(R.string.playlist_folder_delete_action)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) } }
    )
}

@Composable
fun MovePlaylistToFolderDialog(
    playlist: Playlist,
    folders: List<PlaylistFolder>,
    onDismiss: () -> Unit,
    onFolderSelected: (Long?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlist_folder_move_title, playlist.name)) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                item(key = PLAYLIST_ROOT_FOLDER_LAZY_LIST_KEY) {
                    FolderDestinationRow(
                        name = stringResource(R.string.playlist_folder_root),
                        selected = playlist.folderId == null,
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        onClick = { onFolderSelected(null) }
                    )
                }
                items(
                    items = folders,
                    key = { folder -> playlistFolderLazyListKey(folder.folderId) }
                ) { folder ->
                    FolderDestinationRow(
                        name = folder.name,
                        selected = playlist.folderId == folder.folderId,
                        icon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                        onClick = { onFolderSelected(folder.folderId) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) } }
    )
}

@Composable
private fun FolderDestinationRow(
    name: String,
    selected: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(name) },
        leadingContent = icon,
        trailingContent = {
            if (selected) Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.playlist_folder_current_location))
        },
        modifier = Modifier.clickable(enabled = !selected, onClick = onClick)
    )
}
