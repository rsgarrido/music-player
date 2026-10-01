package io.github.rsgarrido.sazanami.ui.tageditor

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.data.BatchArtworkValue
import io.github.rsgarrido.sazanami.data.BatchEditIntent
import io.github.rsgarrido.sazanami.data.BatchMetadataOperationState
import io.github.rsgarrido.sazanami.data.BatchMetadataPlan
import io.github.rsgarrido.sazanami.data.Song

data class BatchMetadataActions(
    val apply: (BatchMetadataPlan) -> Unit,
    val cancel: () -> Unit,
    val retryFailed: () -> Unit,
    val continueUnprocessed: () -> Unit,
    val retryRefresh: () -> Unit,
    val closeResults: () -> Unit
)

private data class LaunchedPermissionRequest(
    val operationId: String,
    val batchIndex: Int
)

@Composable
fun rememberBatchMetadataActions(
    state: BatchMetadataOperationState?,
    songs: List<Song>,
    onBegin: (BatchMetadataPlan, List<Song>, Uri?, Boolean) -> Unit,
    onConsumePermissionRequest: (String, Int) -> List<Uri>?,
    onPermissionResult: (String, Int, Boolean, String?) -> Unit,
    onCancel: () -> Unit,
    onRetryFailed: (List<Song>) -> Unit,
    onContinueUnprocessed: (List<Song>) -> Unit,
    onRetryRefresh: () -> Unit,
    onDismiss: () -> Unit
): BatchMetadataActions {
    val context = LocalContext.current
    val resources = LocalResources.current
    var launchedRequest by remember { mutableStateOf<LaunchedPermissionRequest?>(null) }
    val latestAwaiting by rememberUpdatedState(
        state as? BatchMetadataOperationState.AwaitingPermission
    )

    val writeRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        val request = launchedRequest ?: latestAwaiting
            ?.takeIf { it.requestLaunched }
            ?.let { LaunchedPermissionRequest(it.operationId, it.batchIndex) }
        launchedRequest = null
        if (request != null) {
            onPermissionResult(
                request.operationId,
                request.batchIndex,
                activityResult.resultCode == Activity.RESULT_OK,
                if (activityResult.resultCode == Activity.RESULT_OK) null
                else resources.getString(R.string.metadata_write_permission_denied)
            )
        }
    }

    val awaiting = state as? BatchMetadataOperationState.AwaitingPermission
    LaunchedEffect(awaiting?.operationId, awaiting?.batchIndex, awaiting?.requestLaunched) {
        if (awaiting == null || awaiting.requestLaunched) return@LaunchedEffect
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            onPermissionResult(
                awaiting.operationId,
                awaiting.batchIndex,
                false,
                resources.getString(R.string.metadata_bulk_permission_version)
            )
            return@LaunchedEffect
        }
        val uris = onConsumePermissionRequest(awaiting.operationId, awaiting.batchIndex)
            ?: return@LaunchedEffect
        try {
            val request = MediaStore.createWriteRequest(context.contentResolver, uris)
            launchedRequest = LaunchedPermissionRequest(
                awaiting.operationId,
                awaiting.batchIndex
            )
            writeRequestLauncher.launch(
                IntentSenderRequest.Builder(request.intentSender).build()
            )
        } catch (exception: Exception) {
            Log.w("BatchMetadata", "Could not request write permission", exception)
            launchedRequest = null
            onPermissionResult(
                awaiting.operationId,
                awaiting.batchIndex,
                false,
                resources.getString(R.string.metadata_write_permission_request_failed)
            )
        }
    }

    return BatchMetadataActions(
        apply = { plan ->
            onBegin(
                plan,
                songs.toList(),
                plan.replacementArtworkUri(),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            )
        },
        cancel = onCancel,
        retryFailed = { onRetryFailed(songs.toList()) },
        continueUnprocessed = { onContinueUnprocessed(songs.toList()) },
        retryRefresh = onRetryRefresh,
        closeResults = onDismiss
    )
}

private fun BatchMetadataPlan.replacementArtworkUri(): Uri? {
    val set = artworkChange?.intent as? BatchEditIntent.Set ?: return null
    val present = set.value as? BatchArtworkValue.Present ?: return null
    return Uri.parse(present.artwork.previewUri)
}
