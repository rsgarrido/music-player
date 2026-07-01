package com.example.cdplaya.ui

import android.app.Activity
import android.media.MediaScannerConnection
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.cdplaya.data.EditableSongTags
import com.example.cdplaya.data.Song
import com.example.cdplaya.data.TagEditorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TagEditorActions(
    val saveTags: (Song, EditableSongTags) -> Unit
)

private data class PendingTagSave(
    val song: Song,
    val editedTags: EditableSongTags
)

@Composable
fun rememberTagEditorActions(
    snackbarHostState: SnackbarHostState,
    tagEditorRepository: TagEditorRepository,
    onTagsSaved: (Song, EditableSongTags) -> Unit,
    onCloseEditor: () -> Unit
): TagEditorActions {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var pendingTagSave by remember {
        mutableStateOf<PendingTagSave?>(null)
    }

    fun showMessage(message: String) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
                withDismissAction = true
            )
        }
    }

    fun scanEditedSongFile(
        song: Song,
        editedTags: EditableSongTags,
        successMessage: String
    ) {
        MediaScannerConnection.scanFile(
            context.applicationContext,
            arrayOf(song.filePath),
            null
        ) { _, _ ->
            coroutineScope.launch {
                delay(500)
                onTagsSaved(song, editedTags)
                onCloseEditor()

                snackbarHostState.showSnackbar(
                    message = successMessage,
                    duration = SnackbarDuration.Short,
                    withDismissAction = true
                )
            }
        }
    }

    fun writeTagsAfterPermission(
        song: Song,
        editedTags: EditableSongTags
    ) {
        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) {
                tagEditorRepository.writeTags(
                    song = song,
                    editedTags = editedTags
                )
            }

            if (result.wasSuccessful) {
                scanEditedSongFile(
                    song = song,
                    editedTags = editedTags,
                    successMessage = result.message
                )
            } else {
                showMessage(result.message)
            }
        }
    }

    val writeRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val pendingSave = pendingTagSave
        pendingTagSave = null

        if (pendingSave == null) {
            return@rememberLauncherForActivityResult
        }

        if (result.resultCode == Activity.RESULT_OK) {
            writeTagsAfterPermission(
                song = pendingSave.song,
                editedTags = pendingSave.editedTags
            )
        } else {
            showMessage("Write permission was denied.")
        }
    }

    return TagEditorActions(
        saveTags = { song, editedTags ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    pendingTagSave = PendingTagSave(
                        song = song,
                        editedTags = editedTags
                    )

                    val pendingIntent = MediaStore.createWriteRequest(
                        context.contentResolver,
                        listOf(song.uri)
                    )

                    writeRequestLauncher.launch(
                        IntentSenderRequest.Builder(
                            pendingIntent.intentSender
                        ).build()
                    )
                } catch (exception: Exception) {
                    pendingTagSave = null
                    showMessage(
                        exception.message ?: "Could not request write permission."
                    )
                }
            } else {
                writeTagsAfterPermission(
                    song = song,
                    editedTags = editedTags
                )
            }
        }
    )
}