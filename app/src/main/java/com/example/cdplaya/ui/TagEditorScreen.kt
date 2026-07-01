package com.example.cdplaya.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.EditableSongTags
import com.example.cdplaya.data.Song

@Composable
fun TagEditorScreen(
    song: Song,
    initialTags: EditableSongTags,
    isSaveEnabled: Boolean,
    onBackClick: () -> Unit,
    onSaveClick: (EditableSongTags) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember(song.id, initialTags) {
        mutableStateOf(initialTags.title)
    }

    var artist by remember(song.id, initialTags) {
        mutableStateOf(initialTags.artist)
    }

    var album by remember(song.id, initialTags) {
        mutableStateOf(initialTags.album)
    }

    var trackNumber by remember(song.id, initialTags) {
        mutableStateOf(initialTags.trackNumber)
    }

    var year by remember(song.id, initialTags) {
        mutableStateOf(initialTags.year)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Text(
                text = "Edit Tags",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = song.title.ifBlank { "Unknown Title" },
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = song.filePath,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { value ->
                title = value
            },
            label = {
                Text(text = "Title")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = artist,
            onValueChange = { value ->
                artist = value
            },
            label = {
                Text(text = "Artist")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = album,
            onValueChange = { value ->
                album = value
            },
            label = {
                Text(text = "Album")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = trackNumber,
            onValueChange = { value ->
                trackNumber = value
            },
            label = {
                Text(text = "Track number")
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = year,
            onValueChange = { value ->
                year = value
            },
            label = {
                Text(text = "Year")
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row {
            Button(
                onClick = onBackClick,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Cancel")
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = {
                    onSaveClick(
                        EditableSongTags(
                            title = title,
                            artist = artist,
                            album = album,
                            trackNumber = trackNumber,
                            year = year
                        )
                    )
                },
                enabled = isSaveEnabled,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Save")
            }
        }
    }
}