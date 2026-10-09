package io.github.rsgarrido.sazanami.ui

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.rsgarrido.sazanami.data.BatchMetadataEditorState
import io.github.rsgarrido.sazanami.data.BatchMetadataOperationState
import io.github.rsgarrido.sazanami.data.EditableSongTags
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.ui.tageditor.BatchMetadataActions
import io.github.rsgarrido.sazanami.ui.tageditor.BatchMetadataEditorContext
import io.github.rsgarrido.sazanami.ui.tageditor.BatchMetadataEditorScreen
import io.github.rsgarrido.sazanami.ui.tageditor.BatchMetadataExecutionScreen
import io.github.rsgarrido.sazanami.ui.tageditor.BatchSongSelectionScreen
import io.github.rsgarrido.sazanami.ui.tageditor.DiscardTagChangesDialog
import io.github.rsgarrido.sazanami.ui.tageditor.TagEditorActions
import io.github.rsgarrido.sazanami.ui.tageditor.TagEditorScreen

/** Editor presentation only. Targets, workflow actions and picker registration stay screen-owned. */
@Composable
internal fun MusicMetadataPresentation(
    songForTagEdit: Song?,
    batchEditorState: BatchMetadataEditorState?,
    batchExecutionState: BatchMetadataOperationState?,
    batchEditorContext: BatchMetadataEditorContext,
    currentSongId: Long?,
    isTagSaveInProgress: Boolean,
    selectedArtworkUri: Uri?,
    tagEditorActions: TagEditorActions,
    batchMetadataActions: BatchMetadataActions,
    onReadEditableSongTags: (Song) -> EditableSongTags,
    onGetUnsupportedTagEditingMessage: (Song) -> String?,
    onRequestCloseTagEditor: () -> Unit,
    onChooseTagArtwork: () -> Unit,
    onUnsavedTagChangesChanged: (Boolean) -> Unit,
    onBatchEditorStateChanged: (BatchMetadataEditorState) -> Unit,
    onChooseBatchArtwork: () -> Unit,
    onCloseBatchEditor: () -> Unit,
    onCloseBatchResults: () -> Unit
) {
    if (batchExecutionState != null) {
        BatchMetadataExecutionScreen(
            state = batchExecutionState,
            onCancel = batchMetadataActions.cancel,
            onRetryFailed = batchMetadataActions.retryFailed,
            onContinueUnprocessed = batchMetadataActions.continueUnprocessed,
            onRetryRefresh = batchMetadataActions.retryRefresh,
            onDone = onCloseBatchResults,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        )
    } else if (batchEditorState != null) {
        BatchMetadataEditorScreen(
            state = batchEditorState,
            context = batchEditorContext,
            onStateChanged = onBatchEditorStateChanged,
            onChooseArtwork = onChooseBatchArtwork,
            onApply = batchMetadataActions.apply,
            onBack = onCloseBatchEditor,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        )
    } else if (songForTagEdit != null) {
        val initialEditableTags = remember(songForTagEdit.id, songForTagEdit.filePath) {
            onReadEditableSongTags(songForTagEdit)
        }
        val unsupportedTagEditingMessage = remember(songForTagEdit.id, songForTagEdit.filePath) {
            onGetUnsupportedTagEditingMessage(songForTagEdit)
        }
        TagEditorScreen(
            song = songForTagEdit,
            initialTags = initialEditableTags,
            isSaving = isTagSaveInProgress,
            unsupportedMessage = unsupportedTagEditingMessage,
            isCurrentSong = currentSongId == songForTagEdit.id,
            selectedArtworkUri = selectedArtworkUri,
            onChangeArtworkClick = onChooseTagArtwork,
            onBackClick = onRequestCloseTagEditor,
            onSaveClick = { editedTags ->
                tagEditorActions.saveTags(songForTagEdit, editedTags, selectedArtworkUri)
            },
            onUnsavedChangesChanged = onUnsavedTagChangesChanged,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        )
    }
}

/** Invoke separately at the existing dialog position, after bottom chrome and before player overlays. */
@Composable
internal fun MusicMetadataDialogs(
    isBatchSongSelectionVisible: Boolean,
    songs: List<Song>,
    isBatchPreparationInProgress: Boolean,
    isDiscardTagChangesDialogVisible: Boolean,
    onDismissBatchSelection: () -> Unit,
    onContinueBatchSelection: (List<Song>) -> Unit,
    onDismissTagDiscard: () -> Unit,
    onConfirmTagDiscard: () -> Unit
) {
    if (isBatchSongSelectionVisible) {
        BatchSongSelectionScreen(
            songs = songs,
            isPreparing = isBatchPreparationInProgress,
            onDismiss = onDismissBatchSelection,
            onContinue = onContinueBatchSelection
        )
    }
    if (isDiscardTagChangesDialogVisible) {
        DiscardTagChangesDialog(
            onDismiss = onDismissTagDiscard,
            onConfirmDiscardClick = onConfirmTagDiscard
        )
    }
}
