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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.rsgarrido.sazanami.data.BatchMetadataOperationState
import io.github.rsgarrido.sazanami.data.BatchMetadataTargetId
import io.github.rsgarrido.sazanami.data.BatchPostWriteStageResult
import io.github.rsgarrido.sazanami.data.BatchPostWriteStageStatus
import io.github.rsgarrido.sazanami.data.BatchTargetResult
import io.github.rsgarrido.sazanami.data.BatchTargetStatus
import io.github.rsgarrido.sazanami.data.BatchTerminalOutcome
import io.github.rsgarrido.sazanami.data.isRetryableFailure
import io.github.rsgarrido.sazanami.R
import java.io.File

@Composable
fun BatchMetadataExecutionScreen(
    state: BatchMetadataOperationState,
    onCancel: () -> Unit,
    onRetryFailed: () -> Unit,
    onContinueUnprocessed: () -> Unit,
    onRetryRefresh: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        when (state) {
            is BatchMetadataOperationState.Complete,
            is BatchMetadataOperationState.Interrupted -> onDone()
            is BatchMetadataOperationState.Preparing,
            is BatchMetadataOperationState.AwaitingPermission,
            is BatchMetadataOperationState.Running,
            is BatchMetadataOperationState.PostProcessing -> onCancel()
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (state) {
            is BatchMetadataOperationState.Interrupted -> InterruptedContent(onDone)
            is BatchMetadataOperationState.Preparing -> PreparingContent(state)
            is BatchMetadataOperationState.AwaitingPermission -> PermissionContent(state, onCancel)
            is BatchMetadataOperationState.Running -> RunningContent(state, onCancel)
            is BatchMetadataOperationState.PostProcessing -> PostProcessingContent(state, onCancel)
            is BatchMetadataOperationState.Complete -> CompleteContent(
                state,
                onRetryFailed,
                onContinueUnprocessed,
                onRetryRefresh,
                onDone
            )
        }
    }
}

@Composable
private fun InterruptedContent(onDone: () -> Unit) {
    Text(stringResource(R.string.metadata_previous_batch_interrupted), style = MaterialTheme.typography.headlineSmall)
    Text(
        stringResource(R.string.metadata_previous_batch_detail)
    )
    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.metadata_acknowledge)) }
}

@Composable
private fun PreparingContent(state: BatchMetadataOperationState.Preparing) {
    Text(stringResource(R.string.metadata_preparing_batch), style = MaterialTheme.typography.headlineSmall)
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    Text(pluralStringResource(R.plurals.metadata_preparing_tracks,
        state.plan.selectedTrackCount, state.plan.selectedTrackCount))
}

@Composable
private fun PermissionContent(
    state: BatchMetadataOperationState.AwaitingPermission,
    onCancel: () -> Unit
) {
    Text(stringResource(R.string.metadata_requesting_access), style = MaterialTheme.typography.headlineSmall)
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    Text(
        stringResource(R.string.metadata_permission_group,
            state.batchIndex + 1, state.permissionBatches.size)
    )
    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.metadata_cancel_batch))
    }
}

@Composable
private fun RunningContent(
    state: BatchMetadataOperationState.Running,
    onCancel: () -> Unit
) {
    val progress = state.progress
    Text(stringResource(R.string.metadata_updating), style = MaterialTheme.typography.headlineSmall)
    LinearProgressIndicator(
        progress = { progress.completedCount.toFloat() / progress.totalCount.coerceAtLeast(1) },
        modifier = Modifier.fillMaxWidth()
    )
    Text(pluralStringResource(R.plurals.metadata_progress,
        progress.totalCount, progress.completedCount, progress.totalCount))
    progress.currentTarget?.let { target ->
        Card(colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )) {
            Column(Modifier.padding(14.dp)) {
                Text(stringResource(R.string.metadata_current), style = MaterialTheme.typography.labelLarge)
                Text(target.displayLabel(), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    Text(
        if (state.cancellationRequested) {
            stringResource(R.string.metadata_cancel_requested)
        } else {
            stringResource(R.string.metadata_cancel_explanation)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    OutlinedButton(
        onClick = onCancel,
        enabled = !state.cancellationRequested,
        modifier = Modifier.fillMaxWidth()
    ) { Text(stringResource(if (state.cancellationRequested)
        R.string.metadata_cancelling else R.string.metadata_cancel)) }
}

@Composable
private fun PostProcessingContent(
    state: BatchMetadataOperationState.PostProcessing,
    onCancel: () -> Unit
) {
    Text(stringResource(R.string.metadata_refreshing_library), style = MaterialTheme.typography.headlineSmall)
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    Text(pluralStringResource(R.plurals.metadata_writes_verified,
        state.result.successCount, state.result.successCount))
    Text(stringResource(R.string.metadata_media_store_scan, state.scan.displayLabel()))
    Text(stringResource(R.string.metadata_library_refresh, state.refresh.displayLabel()))
    Text(
        stringResource(R.string.metadata_stop_waiting_detail),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.metadata_stop_waiting))
    }
}

@Composable
private fun CompleteContent(
    state: BatchMetadataOperationState.Complete,
    onRetryFailed: () -> Unit,
    onContinueUnprocessed: () -> Unit,
    onRetryRefresh: () -> Unit,
    onDone: () -> Unit
) {
    val result = state.result
    var detailsExpanded by remember(state.operationId) { mutableStateOf(false) }
    Text(state.terminalOutcome.displayLabel(), style = MaterialTheme.typography.headlineSmall)
    if (result.successCount > 0) {
        Text(pluralStringResource(R.plurals.metadata_tracks_updated,
            result.successCount, result.successCount))
    }
    if (result.failureCount > 0) {
        Text(pluralStringResource(R.plurals.metadata_tracks_failed,
            result.failureCount, result.failureCount))
    }
    if (result.notProcessedCount > 0) {
        Text(pluralStringResource(R.plurals.metadata_tracks_not_processed,
            result.notProcessedCount, result.notProcessedCount))
    }
    if (state.scan.hasWarning || state.refresh.hasWarning) {
        Card(colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.metadata_refresh_pending), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.metadata_completed_not_repeated))
            }
        }
    }

    OutlinedButton(
        onClick = { detailsExpanded = !detailsExpanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(if (detailsExpanded) R.string.metadata_hide_details else R.string.metadata_show_details))
    }
    if (detailsExpanded) {
        if (state.scan.hasWarning || state.refresh.hasWarning) {
            Text(stringResource(R.string.metadata_media_store_scan, state.scan.displayLabel()))
            state.scan.message?.let { Text(stringResource(R.string.metadata_stage_warning), style = MaterialTheme.typography.bodySmall) }
            Text(stringResource(R.string.metadata_library_refresh, state.refresh.displayLabel()))
            state.refresh.message?.let { Text(stringResource(R.string.metadata_stage_warning), style = MaterialTheme.typography.bodySmall) }
        }
        result.targetResults.forEach { TargetResultCard(it) }
    }

    if (result.targetResults.any(BatchTargetResult::isRetryableFailure)) {
        Button(onClick = onRetryFailed, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.metadata_retry_failed))
        }
    }
    if (result.notProcessedCount > 0) {
        OutlinedButton(onClick = onContinueUnprocessed, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.metadata_continue_unprocessed))
        }
    }
    if (state.scan.hasWarning || state.refresh.hasWarning) {
        OutlinedButton(onClick = onRetryRefresh, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.metadata_retry_refresh))
        }
    }
    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.metadata_done)) }
}

@Composable
private fun TargetResultCard(targetResult: BatchTargetResult) {
    Card(colors = CardDefaults.cardColors(
        containerColor = if (targetResult.status == BatchTargetStatus.SUCCESS) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.errorContainer
        }
    )) {
        Column(Modifier.padding(12.dp)) {
            Text(
                targetResult.target.displayLabel(),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(targetResult.status.displayLabel())
            }
            targetResult.reason?.let {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.metadata_target_issue_detail), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BatchTargetStatus.displayLabel(): String = stringResource(when (this) {
    BatchTargetStatus.SUCCESS -> R.string.metadata_status_verified
    BatchTargetStatus.MISSING -> R.string.metadata_status_missing
    BatchTargetStatus.IDENTITY_MISMATCH -> R.string.metadata_status_changed
    BatchTargetStatus.UNSUPPORTED -> R.string.metadata_status_unsupported
    BatchTargetStatus.WRITE_FAILED -> R.string.metadata_status_write_failed
    BatchTargetStatus.VERIFICATION_FAILED -> R.string.metadata_status_verify_failed
    BatchTargetStatus.PERMISSION_DENIED -> R.string.metadata_status_permission_denied
    BatchTargetStatus.NOT_PROCESSED -> R.string.metadata_status_not_processed
})

@Composable
private fun BatchTerminalOutcome.displayLabel(): String = stringResource(when (this) {
    BatchTerminalOutcome.SUCCESS -> R.string.metadata_outcome_updated
    BatchTerminalOutcome.PARTIAL_SUCCESS -> R.string.metadata_outcome_partial
    BatchTerminalOutcome.CANCELLED -> R.string.metadata_outcome_cancelled
    BatchTerminalOutcome.FAILED -> R.string.metadata_outcome_failed
    BatchTerminalOutcome.PERMISSION_DENIED -> R.string.metadata_outcome_permission_denied
    BatchTerminalOutcome.REFRESH_WARNING -> R.string.metadata_outcome_updated
})

@Composable
private fun BatchPostWriteStageResult.displayLabel(): String = stringResource(when (status) {
    BatchPostWriteStageStatus.NOT_REQUIRED -> R.string.metadata_stage_not_required
    BatchPostWriteStageStatus.WAITING -> R.string.metadata_stage_waiting
    BatchPostWriteStageStatus.SUCCESS -> R.string.metadata_stage_success
    BatchPostWriteStageStatus.TIMED_OUT -> R.string.metadata_stage_timed_out
    BatchPostWriteStageStatus.FAILED -> R.string.metadata_stage_failed
    BatchPostWriteStageStatus.CANCELLED -> R.string.metadata_stage_cancelled
})

private fun BatchMetadataTargetId.displayLabel(): String = when {
    title.isNotBlank() && artist.isNotBlank() -> "$title — $artist"
    title.isNotBlank() -> title
    displayName.isNotBlank() -> displayName
    else -> File(filePath).name
}
