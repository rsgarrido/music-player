package com.example.cdplaya.ui

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cdplaya.data.EditableSongTags
import com.example.cdplaya.data.Song

@Composable
fun TagEditorScreen(
    song: Song,
    initialTags: EditableSongTags,
    selectedArtworkUri: Uri?,
    isSaving: Boolean,
    unsupportedMessage: String?,
    isCurrentSong: Boolean,
    onBackClick: () -> Unit,
    onChangeArtworkClick: () -> Unit,
    onSaveClick: (EditableSongTags) -> Unit,
    onUnsavedChangesChanged: (Boolean) -> Unit,
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

    val currentTags = EditableSongTags(
        title = title,
        artist = artist,
        album = album,
        trackNumber = trackNumber,
        year = year
    )

    val hasUnsavedTagChanges = currentTags != initialTags

    LaunchedEffect(hasUnsavedTagChanges) {
        onUnsavedChangesChanged(hasUnsavedTagChanges)
    }

    val artworkPreviewUri = selectedArtworkUri ?: song.albumArtUri

    val titleError = title.trim().isBlank()
    val artistError = artist.trim().isBlank()
    val albumError = album.trim().isBlank()

    val hasValidationError = titleError || artistError || albumError
    val canEditFields = !isSaving && unsupportedMessage == null
    val canSave =
        canEditFields &&
                !hasValidationError &&
                (hasUnsavedTagChanges || selectedArtworkUri != null)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                enabled = !isSaving
            ) {
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

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = artworkPreviewUri,
                contentDescription = "Artwork for ${song.title}",
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
                error = painterResource(android.R.drawable.ic_media_play),
                placeholder = painterResource(android.R.drawable.ic_media_play)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title.ifBlank { "Unknown Title" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = song.artist.ifBlank { "Unknown Artist" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onChangeArtworkClick,
                    enabled = canEditFields
                ) {
                    Text(
                        text = if (selectedArtworkUri == null) {
                            "Change Artwork"
                        } else {
                            "Choose Different Artwork"
                        }
                    )
                }
            }
        }

        if (selectedArtworkUri != null) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "New artwork selected. Tap Save to write it into the audio file.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = song.filePath,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (unsupportedMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = unsupportedMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (isCurrentSong) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "This song is currently playing. If playback behaves strangely after saving, pause the song before editing next time.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

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
            enabled = canEditFields,
            isError = titleError,
            supportingText = {
                if (titleError) {
                    Text(text = "Title cannot be empty.")
                }
            },
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
            enabled = canEditFields,
            isError = artistError,
            supportingText = {
                if (artistError) {
                    Text(text = "Artist cannot be empty.")
                }
            },
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
            enabled = canEditFields,
            isError = albumError,
            supportingText = {
                if (albumError) {
                    Text(text = "Album cannot be empty.")
                }
            },
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
            enabled = canEditFields,
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
            enabled = canEditFields,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row {
            OutlinedButton(
                onClick = onBackClick,
                enabled = !isSaving,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Cancel")
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = {
                    onSaveClick(currentTags)
                },
                enabled = canSave,
                modifier = Modifier.weight(1f)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(text = "Saving")
                } else {
                    Text(text = "Save")
                }
            }
        }
    }
}