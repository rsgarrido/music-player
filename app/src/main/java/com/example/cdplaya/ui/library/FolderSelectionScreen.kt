package com.example.cdplaya.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.LibraryFolder

@Composable
fun FolderSelectionScreen(
    libraryFolders: List<LibraryFolder>,
    selectedLibraryFolders: Set<String>,
    onBackClick: () -> Unit,
    onFolderToggle: (String) -> Unit,
    onSelectAllClick: () -> Unit,
    onClearSelectionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back to library"
                )
            }

            Text(
                text = "Library Folders",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Text(
            text = if (selectedLibraryFolders.isEmpty()) {
                "No folders selected. Showing all detected music."
            } else {
                "${selectedLibraryFolders.size} folder(s) selected."
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Button(
                onClick = onSelectAllClick,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(text = "Select All")
            }

            Button(onClick = onClearSelectionClick) {
                Text(text = "Clear")
            }
        }

        if (libraryFolders.isEmpty()) {
            Text(
                text = "No music folders found.",
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn {
                items(
                    items = libraryFolders,
                    key = { folder -> folder.path }
                ) { folder ->
                    val isSelected = folder.path in selectedLibraryFolders

                    ListItem(
                        headlineContent = {
                            Text(text = folder.name)
                        },
                        supportingContent = {
                            Text(
                                text = "${folder.songCount} song(s)\n${folder.path}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        trailingContent = {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    onFolderToggle(folder.path)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}