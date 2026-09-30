package io.github.rsgarrido.sazanami.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.data.backup.AppBackup
import io.github.rsgarrido.sazanami.data.backup.BackupRestoreResult
import io.github.rsgarrido.sazanami.data.backup.BackupRestoreSummary
import kotlinx.coroutines.launch

data class BackupRestoreActions(
    val restoreBackup: () -> Unit
)

@Composable
fun rememberBackupRestoreActions(
    snackbarHostState: SnackbarHostState,
    onRead: (Uri, (Result<AppBackup>) -> Unit) -> Unit,
    onSummarize: (AppBackup) -> BackupRestoreSummary,
    onRestore: (AppBackup, (Result<BackupRestoreResult>) -> Unit) -> Unit
): BackupRestoreActions {
    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()
    var pendingRestore by remember {
        mutableStateOf<PendingBackupRestore?>(null)
    }
    var isRestoring by remember {
        mutableStateOf(false)
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

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onRead(uri) { result ->
                result.fold(
                    onSuccess = { backup ->
                        pendingRestore = PendingBackupRestore(
                            backup = backup,
                            summary = onSummarize(backup)
                        )
                    },
                    onFailure = {
                        showMessage(resources.getString(R.string.backup_invalid_file))
                    }
                )
            }
        }
    }

    val selectedRestore = pendingRestore

    if (selectedRestore != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isRestoring) {
                    pendingRestore = null
                }
            },
            title = {
                Text(text = stringResource(R.string.backup_restore_confirm_title))
            },
            text = {
                Column {
                    Text(text = stringResource(R.string.backup_restore_confirm_body))

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = resources.backupRestoreSummaryText(selectedRestore.summary),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isRestoring,
                    onClick = {
                        pendingRestore = null
                    }
                ) {
                    Text(text = stringResource(R.string.backup_cancel))
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isRestoring,
                    onClick = {
                        isRestoring = true

                        onRestore(selectedRestore.backup) { result ->
                            isRestoring = false
                            pendingRestore = null

                            result.fold(
                                onSuccess = { restoreResult ->
                                    showMessage(
                                        resources.backupRestoreSuccessMessage(restoreResult)
                                    )
                                },
                                onFailure = {
                                    showMessage(resources.getString(R.string.backup_restore_failure))
                                }
                            )
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.backup_restore_action))
                }
            }
        )
    }

    return BackupRestoreActions(
        restoreBackup = {
            openDocumentLauncher.launch(
                arrayOf(
                    "application/json",
                    "application/zip",
                    "text/plain",
                    "application/octet-stream",
                    "*/*"
                )
            )
        }
    )
}

private data class PendingBackupRestore(
    val backup: AppBackup,
    val summary: BackupRestoreSummary
)
