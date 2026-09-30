package io.github.rsgarrido.sazanami.ui.settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.controller.ListeningHistoryImportFile
import io.github.rsgarrido.sazanami.controller.SpotifyImportUiError
import io.github.rsgarrido.sazanami.controller.SpotifyImportUiState
import io.github.rsgarrido.sazanami.data.importing.ListeningImportExecutionPhase
import io.github.rsgarrido.sazanami.data.importing.ListeningImportExecutionResult
import io.github.rsgarrido.sazanami.data.importing.spotify.SpotifyListeningHistoryImportPreview
import io.github.rsgarrido.sazanami.ui.AppShellTypography
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class SpotifyImportUiActions(
    val onEnter: () -> Unit,
    val onFilesSelected: (List<ListeningHistoryImportFile>) -> Unit,
    val onAnalyze: () -> Unit,
    val onCancelAnalysis: () -> Unit,
    val onImport: () -> Unit,
    val onCancelImport: () -> Unit,
    val onRetry: () -> Unit,
    val onChangeFiles: () -> Unit,
    val onCleanStaleImport: () -> Unit,
    val onImportMore: () -> Unit,
    val onDone: () -> Unit,
    val onBack: () -> Unit
)

@Composable
fun ListeningHistoryImportScreen(
    state: SpotifyImportUiState,
    actions: SpotifyImportUiActions,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCancelDialog by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(OpenSpotifyHistoryDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            actions.onFilesSelected(
                SafListeningHistoryImportFile.fromUris(context.contentResolver, uris)
            )
        }
    }
    val activeImport = state is SpotifyImportUiState.Importing
    val blockedBack = state is SpotifyImportUiState.Cancelling ||
        state is SpotifyImportUiState.CleaningStaleImport ||
        state is SpotifyImportUiState.CheckingRecovery

    fun requestBack() {
        when {
            activeImport -> showCancelDialog = true
            state is SpotifyImportUiState.Analyzing -> actions.onCancelAnalysis()
            blockedBack -> Unit
            else -> actions.onBack()
        }
    }

    BackHandler { requestBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 10.dp, end = 20.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = ::requestBack, enabled = !blockedBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.history_import_back))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.history_import_title),
                    style = AppShellTypography.ScreenTitle,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.history_import_spotify_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when (state) {
            SpotifyImportUiState.Landing -> LandingContent(onSelect = { picker.launch(Unit) })
            SpotifyImportUiState.CheckingRecovery -> ProgressContent(
                title = stringResource(R.string.history_import_checking),
                description = stringResource(R.string.history_import_checking_detail)
            )
            is SpotifyImportUiState.StaleImportRecovery -> RecoveryContent(state, actions)
            SpotifyImportUiState.CleaningStaleImport -> ProgressContent(
                title = stringResource(R.string.history_import_cleaning),
                description = stringResource(R.string.history_import_cleaning_detail)
            )
            is SpotifyImportUiState.FilesSelected -> FilesSelectedContent(
                files = state.files,
                cancellationMessage = state.cancellationMessage,
                onChange = { picker.launch(Unit) },
                onAnalyze = actions.onAnalyze
            )
            is SpotifyImportUiState.Analyzing -> AnalysisProgressContent(state, actions)
            is SpotifyImportUiState.Preview -> PreviewContent(state.preview, actions)
            is SpotifyImportUiState.Importing -> ImportProgressContent(state, actions)
            is SpotifyImportUiState.Cancelling -> ProgressContent(
                title = stringResource(R.string.history_import_cancelling),
                description = stringResource(R.string.history_import_cancelling_detail)
            )
            is SpotifyImportUiState.Cancelled -> CancelledContent(actions)
            is SpotifyImportUiState.Success -> ResultContent(state.result, actions)
            is SpotifyImportUiState.Error -> ErrorContent(state, actions) { picker.launch(Unit) }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(stringResource(R.string.history_import_cancel_confirm_title)) },
            text = {
                Text(stringResource(R.string.history_import_cancel_confirm_detail))
            },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    actions.onCancelImport()
                }) { Text(stringResource(R.string.history_import_cancel_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text(stringResource(R.string.history_import_keep_importing)) }
            }
        )
    }
}

@Composable
private fun LandingContent(onSelect: () -> Unit) {
    ImportCard(
        title = stringResource(R.string.history_import_spotify),
        icon = { Icon(Icons.Default.History, contentDescription = null) }
    ) {
        Text(
            stringResource(R.string.history_import_select_help),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            stringResource(R.string.history_import_local_privacy),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 14.dp)
        )
        Button(onClick = onSelect, modifier = Modifier.padding(top = 20.dp)) {
            Text(stringResource(R.string.history_import_select_json))
        }
    }
}

@Composable
private fun FilesSelectedContent(
    files: List<ListeningHistoryImportFile>,
    cancellationMessage: Boolean,
    onChange: () -> Unit,
    onAnalyze: () -> Unit
) {
    ImportCard(
        title = pluralStringResource(R.plurals.history_import_files_selected, files.size, formatCount(files.size.toLong())),
        icon = { Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null) }
    ) {
        if (cancellationMessage) {
            Text(
                stringResource(R.string.history_import_analysis_cancelled),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        if (files.size <= 5) {
            files.forEach { file ->
                Text(
                    text = file.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
        ActionRow(
            primaryText = stringResource(R.string.history_import_analyze),
            onPrimary = onAnalyze,
            secondaryText = stringResource(R.string.history_import_change_files),
            onSecondary = onChange
        )
    }
}

@Composable
private fun AnalysisProgressContent(
    state: SpotifyImportUiState.Analyzing,
    actions: SpotifyImportUiActions
) {
    ProgressContent(
        title = stringResource(R.string.history_import_analyzing),
        description = if (state.recordsProcessed > 0) {
            pluralStringResource(R.plurals.history_import_records_processed, state.recordsProcessed.toInt(), formatCount(state.recordsProcessed))
        } else {
            pluralStringResource(R.plurals.history_import_reading_files, state.files.size, state.files.size)
        },
        actionText = stringResource(R.string.history_import_cancel_action),
        onAction = actions.onCancelAnalysis
    )
}

@Composable
private fun PreviewContent(
    preview: SpotifyListeningHistoryImportPreview,
    actions: SpotifyImportUiActions
) {
    val analysis = preview.analysis
    val dedupe = preview.dedupe
    ImportCard(title = stringResource(R.string.history_import_preview)) {
        StatRow(stringResource(R.string.history_import_records_found), analysis.totalRecords)
        StatRow(stringResource(R.string.history_import_music_records), analysis.validMusicRecords)
        StatRow(stringResource(R.string.history_import_new_records), dedupe.newOccurrences, emphasize = true)
        StatRow(stringResource(R.string.history_import_already_imported), dedupe.alreadyImportedOccurrences)
        StatRow(stringResource(R.string.history_import_overlap_ignored), dedupe.overlappingOccurrencesSuppressed)
        StatRow(
            stringResource(R.string.history_import_unsupported),
            analysis.podcastRecords + analysis.audiobookRecords +
                analysis.videoRecords + analysis.unknownRecords
        )
        StatRow(stringResource(R.string.history_import_invalid_records), analysis.invalidRecords)
        DateRangeRow(analysis.earliestAt, analysis.latestAt)
        if (dedupe.newOccurrences == 0L) {
            Text(
                stringResource(R.string.history_import_everything_imported),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp)
            )
            ActionRow(
                primaryText = stringResource(R.string.history_import_done),
                onPrimary = actions.onDone,
                secondaryText = stringResource(R.string.history_import_change_files),
                onSecondary = actions.onChangeFiles
            )
        } else {
            Text(
                pluralStringResource(R.plurals.history_import_preview_summary, dedupe.newOccurrences.toInt(), formatCount(dedupe.newOccurrences)),
                modifier = Modifier.padding(top = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ActionRow(
                primaryText = stringResource(R.string.history_import_import_history),
                onPrimary = actions.onImport,
                secondaryText = stringResource(R.string.history_import_change_files),
                onSecondary = actions.onChangeFiles
            )
        }
    }
}

@Composable
private fun ImportProgressContent(
    state: SpotifyImportUiState.Importing,
    actions: SpotifyImportUiActions
) {
    val progress = state.progress
    val phase = when (progress?.phase) {
        null, ListeningImportExecutionPhase.ANALYZING -> stringResource(R.string.history_import_preparing)
        ListeningImportExecutionPhase.IMPORTING -> stringResource(R.string.history_import_importing)
        ListeningImportExecutionPhase.PUBLISHING -> stringResource(R.string.history_import_publishing)
        ListeningImportExecutionPhase.COMPLETED -> stringResource(R.string.history_import_finishing)
    }
    val total = state.preview.analysis.totalRecords
    val determinate = progress?.phase == ListeningImportExecutionPhase.ANALYZING ||
        progress?.phase == ListeningImportExecutionPhase.IMPORTING
    val progressDescription = stringResource(
        R.string.history_import_progress_description,
        formatCount(progress?.recordsProcessed?.coerceIn(0L, total) ?: 0L),
        formatCount(total)
    )
    ImportCard(title = phase) {
        if (determinate && total > 0L) {
            val current = progress?.recordsProcessed?.coerceIn(0L, total) ?: 0L
            LinearProgressIndicator(
                progress = { current.toFloat() / total.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = progressDescription
                    }
            )
            Text(
                "${formatCount(current)} / ${formatCount(total)}",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = phase }
            )
        }
        OutlinedButton(onClick = actions.onCancelImport, modifier = Modifier.padding(top = 20.dp)) {
            Text(stringResource(R.string.history_import_cancel_action))
        }
    }
}

@Composable
private fun ResultContent(result: ListeningImportExecutionResult, actions: SpotifyImportUiActions) {
    ImportCard(title = stringResource(R.string.history_import_complete)) {
        Text(
            if (result.newPublished == 0L) {
                stringResource(R.string.history_import_no_new_records)
            } else {
                pluralStringResource(R.plurals.history_import_added_records, result.newPublished.toInt(), formatCount(result.newPublished))
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        StatRow(stringResource(R.string.history_import_imported), result.newPublished, emphasize = true)
        StatRow(stringResource(R.string.history_import_already_imported), result.alreadyImported)
        StatRow(stringResource(R.string.history_import_overlap_ignored), result.overlappingOccurrencesSuppressed)
        StatRow(stringResource(R.string.history_import_skipped_non_music), result.unsupportedMedia)
        StatRow(stringResource(R.string.history_import_invalid_records), result.invalid)
        DateRangeRow(result.sourceRangeStart, result.sourceRangeEnd)
        ActionRow(
            primaryText = stringResource(R.string.history_import_done),
            onPrimary = actions.onDone,
            secondaryText = stringResource(R.string.history_import_more),
            onSecondary = actions.onImportMore
        )
    }
}

@Composable
private fun CancelledContent(actions: SpotifyImportUiActions) {
    ImportCard(title = stringResource(R.string.history_import_cancelled)) {
        Text(stringResource(R.string.history_import_cancelled_detail))
        ActionRow(
            primaryText = stringResource(R.string.history_import_done),
            onPrimary = actions.onDone,
            secondaryText = stringResource(R.string.history_import_try_again),
            onSecondary = actions.onChangeFiles
        )
    }
}

@Composable
private fun RecoveryContent(
    state: SpotifyImportUiState.StaleImportRecovery,
    actions: SpotifyImportUiActions
) {
    ImportCard(title = stringResource(R.string.history_import_recovery_title)) {
        Text(
            if (state.pendingBatchCount == null) {
                stringResource(R.string.history_import_recovery_unverified)
            } else {
                stringResource(R.string.history_import_recovery_detail)
            }
        )
        if (state.cleanupFailed) {
            Text(
                stringResource(R.string.history_import_cleanup_failed),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        ActionRow(
            primaryText = if (state.cleanupFailed) stringResource(R.string.history_import_try_again) else stringResource(R.string.history_import_clean_up),
            onPrimary = actions.onCleanStaleImport,
            secondaryText = stringResource(R.string.history_import_back),
            onSecondary = actions.onBack
        )
    }
}

@Composable
private fun ErrorContent(
    state: SpotifyImportUiState.Error,
    actions: SpotifyImportUiActions,
    onChangeFiles: () -> Unit
) {
    val baseRes = when (state.error) {
        SpotifyImportUiError.ACCOUNT_DATA_FORMAT ->
            R.string.history_import_error_account_data
        SpotifyImportUiError.UNKNOWN_JSON ->
            R.string.history_import_error_unknown_json
        SpotifyImportUiError.MALFORMED_JSON ->
            R.string.history_import_error_malformed_json
        SpotifyImportUiError.FILE_ACCESS ->
            R.string.history_import_error_file_access
        SpotifyImportUiError.NO_MUSIC ->
            R.string.history_import_error_no_music
        SpotifyImportUiError.IMPORT_FAILED ->
            R.string.history_import_error_failed
    }
    ImportCard(title = stringResource(R.string.history_import_error_title)) {
        Text(stringResource(baseRes), color = MaterialTheme.colorScheme.error)
        state.failedDisplayName?.let { name ->
            Text(
                stringResource(R.string.history_import_file_name, name),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        ActionRow(
            primaryText = stringResource(R.string.history_import_try_again),
            onPrimary = actions.onRetry,
            secondaryText = stringResource(R.string.history_import_change_files),
            onSecondary = onChangeFiles
        )
    }
}

@Composable
private fun ProgressContent(
    title: String,
    description: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    ImportCard(title = title) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = title }
        )
        Text(
            description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )
        if (actionText != null && onAction != null) {
            OutlinedButton(onClick = onAction, modifier = Modifier.padding(top = 20.dp)) {
                Text(actionText)
            }
        }
    }
}

@Composable
private fun ImportCard(
    title: String,
    icon: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    icon()
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(title, style = AppShellTypography.SectionTitle)
            }
            Column(modifier = Modifier.padding(top = 14.dp), content = content)
        }
    }
}

@Composable
private fun ActionRow(
    primaryText: String,
    onPrimary: () -> Unit,
    secondaryText: String,
    onSecondary: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp)
    ) {
        Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth()) { Text(primaryText) }
        OutlinedButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth()) {
            Text(secondaryText)
        }
    }
}

@Composable
private fun StatRow(label: String, value: Long, emphasize: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            formatCount(value),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp),
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DateRangeRow(earliest: Instant?, latest: Instant?) {
    if (earliest == null || latest == null) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            stringResource(R.string.history_import_date_range),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            formatRange(earliest, latest),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

private fun formatCount(value: Long): String = NumberFormat.getIntegerInstance().format(value)

private fun formatRange(earliest: Instant, latest: Instant): String {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val zone = ZoneId.systemDefault()
    val first = formatter.format(earliest.atZone(zone))
    val last = formatter.format(latest.atZone(zone))
    return if (first == last) first else "$first – $last"
}
