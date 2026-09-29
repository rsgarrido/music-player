package io.github.rsgarrido.sazanami.ui.playlist

import android.content.res.Resources
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalResources
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.data.playlistfile.PlaylistImportResult
import kotlinx.coroutines.launch

data class PlaylistImportActions(
    val importPlaylist: () -> Unit
)

@Composable
fun rememberPlaylistImportActions(
    snackbarHostState: SnackbarHostState,
    onImport: (Uri, (Result<PlaylistImportResult>) -> Unit) -> Unit
): PlaylistImportActions {
    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()

    fun showMessage(message: String) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
                withDismissAction = true
            )
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImport(uri) { result ->
                result.fold(
                    onSuccess = { importResult ->
                        showMessage(importResultMessage(resources, importResult))
                    },
                    onFailure = {
                        showMessage(resources.getString(R.string.playlist_import_failed))
                    }
                )
            }
        }
    }

    return PlaylistImportActions(
        importPlaylist = {
            openDocumentLauncher.launch(
                arrayOf(
                    "*/*",
                    "audio/x-mpegurl",
                    "application/vnd.apple.mpegurl",
                    "text/plain",
                    "application/octet-stream"
                )
            )
        }
    )
}

internal fun importResultMessage(resources: Resources, result: PlaylistImportResult): String {
    val playlistName = result.playlistName

    if (result.importedSongCount == 0 || playlistName == null) {
        return resources.getString(R.string.playlist_import_no_matches)
    }

    if (result.unmatchedEntryCount == 0) {
        return resources.getQuantityString(
            R.plurals.playlist_import_success,
            result.importedSongCount,
            result.importedSongCount,
            playlistName
        )
    }
    val pluralRes = if (result.importedSongCount == 1)
        R.plurals.playlist_import_one_song_with_unmatched
    else R.plurals.playlist_import_many_songs_with_unmatched
    return resources.getQuantityString(
        pluralRes,
        result.unmatchedEntryCount,
        result.importedSongCount,
        playlistName,
        result.unmatchedEntryCount
    )
}
