package io.github.rsgarrido.sazanami.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.ui.AppShellIcons
import io.github.rsgarrido.sazanami.ui.state.resolve

@Composable
fun LyricsFolderSettings(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val controller = remember(context.applicationContext) {
        LyricsSettingsController.shared(context.applicationContext)
    }
    val state by controller.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) controller.addRoot(uri)
    }

    val folderSummary = when {
        state.roots.isEmpty() ->
            stringResource(R.string.lyrics_no_folders_selected)
        else ->
            pluralStringResource(R.plurals.lyrics_folder_summary, state.roots.size,
                state.roots.size,
                pluralStringResource(R.plurals.lyrics_lrc_file_count,
                    state.indexedFileCount, state.indexedFileCount))
    }

    SettingsSection(
        title = stringResource(R.string.lyrics_local_title),
        description = stringResource(R.string.lyrics_local_description),
        icon = AppShellIcons.Lyrics,
        modifier = modifier
    ) {
        SettingsRow(
            title = stringResource(R.string.lyrics_folders),
            summary = folderSummary,
            icon = AppShellIcons.Lyrics,
            emphasizeSummary = state.roots.isNotEmpty()
        )

        state.roots.forEach { item ->
            SettingsDivider()

            SettingsRow(
                title = item.root.displayName.ifBlank { item.root.uri },
                summary = if (item.hasPersistedAccess) {
                    item.root.uri
                } else {
                    stringResource(R.string.lyrics_persisted_access_missing)
                },
                icon = AppShellIcons.Folder,
                emphasizeSummary = !item.hasPersistedAccess,
                trailingContent = {
                    TextButton(onClick = { controller.removeRoot(item.root.uri) }) {
                        Text(stringResource(R.string.lyrics_remove))
                    }
                }
            )
        }

        SettingsDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                enabled = !state.isScanning,
                onClick = { picker.launch(null) }
            ) {
                Text(stringResource(R.string.lyrics_add_folder))
            }

            OutlinedButton(
                enabled = !state.isScanning && state.roots.isNotEmpty(),
                onClick = controller::rescan
            ) {
                Text(stringResource(R.string.lyrics_rescan))
            }

            if (state.isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            }
        }

        state.message?.let { message ->
            Text(
                text = message.resolve(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
            )
        }

        SettingsFooterNote(
            text = stringResource(R.string.lyrics_remove_folder_note)
        )
    }
}
