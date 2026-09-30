package io.github.rsgarrido.sazanami.ui.tageditor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.data.BatchArtworkValue
import io.github.rsgarrido.sazanami.data.BatchEditIntent
import io.github.rsgarrido.sazanami.data.BatchFieldState
import io.github.rsgarrido.sazanami.data.BatchInitialValue
import io.github.rsgarrido.sazanami.data.BatchMetadataEditorState
import io.github.rsgarrido.sazanami.data.BatchMetadataField
import io.github.rsgarrido.sazanami.data.BatchMetadataPlan
import io.github.rsgarrido.sazanami.data.BatchMetadataValue
import io.github.rsgarrido.sazanami.data.EditableMetadataField
import io.github.rsgarrido.sazanami.data.displayText
import io.github.rsgarrido.sazanami.data.isValidMetadataBpm

@Composable
fun BatchMetadataEditorScreen(
    state: BatchMetadataEditorState,
    context: BatchMetadataEditorContext = BatchMetadataEditorContext.SongSelection,
    onStateChanged: (BatchMetadataEditorState) -> Unit,
    onChooseArtwork: () -> Unit,
    onApply: (BatchMetadataPlan) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val plan = state.plan()
    var isApplyConfirmationVisible by remember { mutableStateOf(false) }
    var isDiscardConfirmationVisible by remember { mutableStateOf(false) }
    val requestBack = {
        if (plan.changeCount > 0) {
            isDiscardConfirmationVisible = true
        } else {
            onBack()
        }
    }
    BackHandler(onBack = requestBack)
    val hasInvalidBpm = state.fields.getValue(BatchMetadataField.BPM).intent
        .let { intent ->
            intent is BatchEditIntent.Set &&
                !intent.value.displayText().isValidMetadataBpm()
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = requestBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.metadata_back))
            }
            val albumContext = context as? BatchMetadataEditorContext.Album
            albumContext?.artworkUri?.let { artworkUri ->
                AsyncImage(
                    model = artworkUri,
                    contentDescription = stringResource(R.string.metadata_album_art_for, albumContext.title),
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(48.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(if (albumContext == null) R.string.metadata_edit_metadata
                        else R.string.metadata_edit_album_metadata),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    if (albumContext == null) {
                        pluralStringResource(R.plurals.metadata_tracks_planning,
                            state.selectedTrackCount, state.selectedTrackCount)
                    } else {
                        pluralStringResource(R.plurals.metadata_tracks_from_album,
                            state.selectedTrackCount, state.selectedTrackCount, albumContext.title)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.metadata_plan_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        if (context is BatchMetadataEditorContext.Album) {
            Text(stringResource(R.string.metadata_album_fields), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            BatchFieldEditors(
                fields = albumPrimaryFields,
                state = state,
                onStateChanged = onStateChanged
            )
            BatchArtworkEditor(
                state = state.artwork,
                supported = state.capabilities.supports(EditableMetadataField.ARTWORK),
                onChooseArtwork = onChooseArtwork,
                onClear = { onStateChanged(state.clearArtwork()) },
                onReset = { onStateChanged(state.resetArtwork()) }
            )
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.metadata_additional_fields), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.metadata_disc_bpm_preserved),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            BatchFieldEditors(
                fields = albumAdditionalFields,
                state = state,
                onStateChanged = onStateChanged
            )
        } else {
            BatchFieldEditors(
                fields = BatchMetadataField.entries,
                state = state,
                onStateChanged = onStateChanged
            )
            BatchArtworkEditor(
                state = state.artwork,
                supported = state.capabilities.supports(EditableMetadataField.ARTWORK),
                onChooseArtwork = onChooseArtwork,
                onClear = { onStateChanged(state.clearArtwork()) },
                onReset = { onStateChanged(state.resetArtwork()) }
            )
        }

        Spacer(Modifier.height(22.dp))
        Text(stringResource(R.string.metadata_planned_changes), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (plan.changeCount == 0) {
            Text(
                stringResource(R.string.metadata_no_changes),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    plan.fieldChanges.forEach { (field, change) ->
                        PlanChangeRow(
                            label = stringResource(field.labelRes),
                            oldValue = change.initial.describeInitial(),
                            newValue = change.intent.describeIntent()
                        )
                    }
                    plan.artworkChange?.let { change ->
                        PlanChangeRow(
                            label = stringResource(R.string.metadata_artwork),
                            oldValue = change.initial.describeArtworkInitial(),
                            newValue = change.intent.describeArtworkIntent()
                        )
                    }
                }
            }
        }

        if (hasInvalidBpm) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.metadata_bpm_batch_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { isApplyConfirmationVisible = true },
            enabled = plan.changeCount > 0 && !hasInvalidBpm,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.metadata_review_apply))
        }
        TextButton(onClick = requestBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (plan.changeCount == 0)
                R.string.metadata_cancel else R.string.metadata_discard_changes))
        }
    }

    if (isApplyConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { isApplyConfirmationVisible = false },
            title = { Text(stringResource(R.string.metadata_apply_confirm_title)) },
            text = {
                Text(
                    stringResource(R.string.metadata_apply_confirm_body,
                        pluralStringResource(R.plurals.metadata_explicit_changes,
                            plan.changeCount, plan.changeCount),
                        pluralStringResource(R.plurals.metadata_track_count,
                            plan.selectedTrackCount, plan.selectedTrackCount))
                )
            },
            confirmButton = {
                Button(onClick = {
                    isApplyConfirmationVisible = false
                    onApply(plan)
                }) { Text(stringResource(R.string.metadata_apply)) }
            },
            dismissButton = {
                TextButton(onClick = { isApplyConfirmationVisible = false }) {
                    Text(stringResource(R.string.metadata_keep_editing))
                }
            }
        )
    }

    if (isDiscardConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { isDiscardConfirmationVisible = false },
            title = { Text(stringResource(R.string.metadata_discard_confirm_title)) },
            text = { Text(stringResource(R.string.metadata_discard_confirm_body)) },
            confirmButton = {
                Button(onClick = {
                    isDiscardConfirmationVisible = false
                    onBack()
                }) { Text(stringResource(R.string.metadata_discard)) }
            },
            dismissButton = {
                TextButton(onClick = { isDiscardConfirmationVisible = false }) {
                    Text(stringResource(R.string.metadata_keep_editing))
                }
            }
        )
    }
}

@Composable
private fun BatchFieldEditors(
    fields: List<BatchMetadataField>,
    state: BatchMetadataEditorState,
    onStateChanged: (BatchMetadataEditorState) -> Unit
) {
    fields.forEach { field ->
        BatchFieldEditor(
            field = field,
            state = state.fields.getValue(field),
            supported = state.supports(field),
            onSet = { value -> onStateChanged(state.set(field, value)) },
            onClear = { onStateChanged(state.clear(field)) },
            onReset = { onStateChanged(state.reset(field)) }
        )
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun BatchFieldEditor(
    field: BatchMetadataField,
    state: BatchFieldState<BatchMetadataValue>,
    supported: Boolean,
    onSet: (String) -> Unit,
    onClear: () -> Unit,
    onReset: () -> Unit
) {
    val displayValue = when (val intent = state.intent) {
        BatchEditIntent.Clear -> ""
        is BatchEditIntent.Set -> intent.value.displayText()
        BatchEditIntent.Untouched -> when (val initial = state.initial) {
            is BatchInitialValue.Common -> initial.value.displayText()
            BatchInitialValue.Mixed -> ""
        }
    }
    val isMixed = state.initial == BatchInitialValue.Mixed &&
        state.intent == BatchEditIntent.Untouched
    val isClear = state.intent == BatchEditIntent.Clear
    val hasIntent = state.intent != BatchEditIntent.Untouched
    val bpmError = field == BatchMetadataField.BPM &&
        state.intent is BatchEditIntent.Set &&
        !state.intent.value.displayText().isValidMetadataBpm()
    val hasNoInitialValue = state.intent == BatchEditIntent.Untouched &&
        (state.initial as? BatchInitialValue.Common)?.value?.displayText().isNullOrEmpty()

    Column {
        OutlinedTextField(
            value = displayValue,
            onValueChange = onSet,
            label = { Text(stringResource(field.labelRes)) },
            placeholder = when {
                isMixed -> ({ Text(stringResource(R.string.metadata_multiple_values)) })
                hasNoInitialValue -> ({ Text(stringResource(R.string.metadata_no_value)) })
                else -> null
            },
            enabled = supported && !isClear,
            isError = bpmError,
            supportingText = {
                Text(
                    when {
                        !supported -> stringResource(R.string.metadata_not_supported_all)
                        isClear -> stringResource(R.string.metadata_will_clear_all)
                        isMixed -> stringResource(R.string.metadata_mixed_edit_replaces)
                        hasIntent && displayValue.isEmpty() ->
                            stringResource(R.string.metadata_empty_value_warning)
                        hasIntent -> stringResource(R.string.metadata_will_replace_all)
                        field.isMultiValue -> stringResource(R.string.metadata_multi_value_help)
                        state.initial is BatchInitialValue.Common &&
                            state.initial.value.displayText().isEmpty() && !hasIntent ->
                            stringResource(R.string.metadata_all_empty)
                        else -> stringResource(R.string.metadata_unchanged_unless_edited)
                    }
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (field == BatchMetadataField.BPM ||
                    field == BatchMetadataField.DISC_NUMBER ||
                    field == BatchMetadataField.DISC_TOTAL
                ) KeyboardType.Number else KeyboardType.Text
            ),
            singleLine = field != BatchMetadataField.COMMENT,
            minLines = if (field == BatchMetadataField.COMMENT) 3 else 1,
            modifier = Modifier.fillMaxWidth()
        )
        if (supported) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!isClear) {
                    TextButton(onClick = onClear) { Text(stringResource(R.string.metadata_clear_field)) }
                }
                if (hasIntent) {
                    TextButton(onClick = onReset) { Text(stringResource(R.string.metadata_reset)) }
                }
            }
        }
    }
}

@Composable
private fun BatchArtworkEditor(
    state: BatchFieldState<BatchArtworkValue>,
    supported: Boolean,
    onChooseArtwork: () -> Unit,
    onClear: () -> Unit,
    onReset: () -> Unit
) {
    val effective = when (val intent = state.intent) {
        is BatchEditIntent.Set -> intent.value
        BatchEditIntent.Clear -> BatchArtworkValue.None
        BatchEditIntent.Untouched -> (state.initial as? BatchInitialValue.Common)?.value
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.metadata_artwork), style = MaterialTheme.typography.titleSmall)
            when {
                !supported -> Text(stringResource(R.string.metadata_not_supported_all))
                state.intent == BatchEditIntent.Clear -> Text(stringResource(R.string.metadata_will_clear))
                effective is BatchArtworkValue.Present -> {
                    AsyncImage(
                        model = effective.artwork.previewUri,
                        contentDescription = stringResource(R.string.metadata_batch_artwork_preview),
                        modifier = Modifier.size(88.dp)
                    )
                    Text(stringResource(if (state.intent is BatchEditIntent.Set)
                        R.string.metadata_replacement_artwork else R.string.metadata_common_artwork))
                }
                state.initial == BatchInitialValue.Mixed -> Text(stringResource(R.string.metadata_multiple_artwork_values))
                else -> Text(stringResource(R.string.metadata_no_artwork_selected))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onChooseArtwork, enabled = supported) {
                    Text(stringResource(R.string.metadata_choose_replacement))
                }
                TextButton(onClick = onClear, enabled = supported) { Text(stringResource(R.string.metadata_clear)) }
                if (state.intent != BatchEditIntent.Untouched) {
                    TextButton(onClick = onReset) { Text(stringResource(R.string.metadata_reset)) }
                }
            }
        }
    }
}

@Composable
private fun PlanChangeRow(label: String, oldValue: String, newValue: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(
            stringResource(R.string.metadata_change_arrow, oldValue, newValue),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BatchInitialValue<BatchMetadataValue>.describeInitial(): String = when (this) {
    is BatchInitialValue.Common -> value.displayText().ifBlank { stringResource(R.string.metadata_empty) }
    BatchInitialValue.Mixed -> stringResource(R.string.metadata_multiple_values)
}

@Composable
private fun BatchEditIntent<BatchMetadataValue>.describeIntent(): String = when (this) {
    BatchEditIntent.Clear -> stringResource(R.string.metadata_cleared)
    is BatchEditIntent.Set -> value.displayText().ifBlank { stringResource(R.string.metadata_empty_value) }
    BatchEditIntent.Untouched -> stringResource(R.string.metadata_unchanged)
}

@Composable
private fun BatchInitialValue<BatchArtworkValue>.describeArtworkInitial(): String = when (this) {
    is BatchInitialValue.Common -> stringResource(if (value is BatchArtworkValue.Present)
        R.string.metadata_common_artwork else R.string.metadata_no_artwork)
    BatchInitialValue.Mixed -> stringResource(R.string.metadata_multiple_artwork_values)
}

@Composable
private fun BatchEditIntent<BatchArtworkValue>.describeArtworkIntent(): String = when (this) {
    BatchEditIntent.Clear -> stringResource(R.string.metadata_cleared)
    is BatchEditIntent.Set -> stringResource(R.string.metadata_new_artwork)
    BatchEditIntent.Untouched -> stringResource(R.string.metadata_unchanged)
}

private val albumPrimaryFields = listOf(
    BatchMetadataField.ALBUM,
    BatchMetadataField.ALBUM_ARTIST,
    BatchMetadataField.DATE,
    BatchMetadataField.GENRE,
    BatchMetadataField.COMPOSER,
    BatchMetadataField.PUBLISHER,
    BatchMetadataField.COPYRIGHT,
    BatchMetadataField.DISC_TOTAL
)

private val albumAdditionalFields = listOf(
    BatchMetadataField.COMMENT,
    BatchMetadataField.DISC_NUMBER,
    BatchMetadataField.BPM
)
