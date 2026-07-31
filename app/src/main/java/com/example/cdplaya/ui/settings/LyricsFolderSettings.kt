package com.example.cdplaya.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Local lyrics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        ListItem(
            headlineContent = { Text("Lyrics folders") },
            supportingContent = {
                Text(
                    when {
                        state.roots.isEmpty() ->
                            "No folders selected. Local .lrc files will not be indexed."
                        else ->
                            "${state.roots.size} folder(s) • ${state.indexedFileCount} .lrc file(s)"
                    }
                )
            }
        )

        state.roots.forEach { item ->
            ListItem(
                headlineContent = {
                    Text(item.root.displayName.ifBlank { item.root.uri })
                },
                supportingContent = {
                    Text(
                        if (item.hasPersistedAccess) {
                            item.root.uri
                        } else {
                            "Persisted folder access is missing"
                        }
                    )
                },
                trailingContent = {
                    TextButton(onClick = { controller.removeRoot(item.root.uri) }) {
                        Text("Remove")
                    }
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                enabled = !state.isScanning,
                onClick = { picker.launch(null) }
            ) {
                Text("Add folder")
            }
            TextButton(
                enabled = !state.isScanning && state.roots.isNotEmpty(),
                onClick = controller::rescan
            ) {
                Text("Rescan")
            }
            if (state.isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        state.message?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        Text(
            text = "Removing a folder stops indexing it but does not revoke Android's " +
                "persisted folder permission.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}
