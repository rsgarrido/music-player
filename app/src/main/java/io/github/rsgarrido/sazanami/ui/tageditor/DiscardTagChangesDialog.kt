package io.github.rsgarrido.sazanami.ui.tageditor

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.rsgarrido.sazanami.R

@Composable
fun DiscardTagChangesDialog(
    onDismiss: () -> Unit,
    onConfirmDiscardClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.metadata_discard_confirm_title))
        },
        text = {
            Text(
                text = stringResource(R.string.metadata_discard_tag_warning)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDiscardClick
            ) {
                Text(text = stringResource(R.string.metadata_discard))
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss
            ) {
                Text(text = stringResource(R.string.metadata_keep_editing))
            }
        }
    )
}
