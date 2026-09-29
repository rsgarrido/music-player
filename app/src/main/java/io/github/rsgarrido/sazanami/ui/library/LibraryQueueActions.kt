package io.github.rsgarrido.sazanami.ui.library

import android.R
import io.github.rsgarrido.sazanami.R as AppR
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.github.rsgarrido.sazanami.controller.PlaybackQueueCardUiState
import io.github.rsgarrido.sazanami.data.Playlist
import io.github.rsgarrido.sazanami.data.Song

@Immutable
data class LibraryQueueUiEnvironment(
    val onPlayInNewQueue: (String, List<Song>) -> Unit = { _, _ -> },
    val onAddToAnotherQueue: (List<Song>) -> Unit = {},
    val onPlayPlaylistNext: (Playlist) -> Unit = {},
    val onPlayPlaylistInNewQueue: (Playlist) -> Unit = {},
    val onAddPlaylistToAnotherQueue: (Playlist) -> Unit = {}
)

val LocalLibraryQueueUi = staticCompositionLocalOf { LibraryQueueUiEnvironment() }

internal fun playlistQueueActions(
    playlist: Playlist,
    queueUi: LibraryQueueUiEnvironment,
    onAddToQueue: (Playlist) -> Unit,
    resolveString: (Int) -> String
): List<LibraryItemAction> = listOf(
    LibraryItemAction(resolveString(AppR.string.playlist_play_next), Icons.Filled.SkipNext) {
        queueUi.onPlayPlaylistNext(playlist)
    },
    LibraryItemAction(resolveString(AppR.string.playlist_add_to_queue), Icons.AutoMirrored.Filled.QueueMusic) {
        onAddToQueue(playlist)
    },
    LibraryItemAction(resolveString(AppR.string.playlist_add_to_another_queue), Icons.AutoMirrored.Filled.QueueMusic) {
        queueUi.onAddPlaylistToAnotherQueue(playlist)
    },
    LibraryItemAction(resolveString(AppR.string.playlist_play_in_new_queue), Icons.Filled.PlayArrow) {
        queueUi.onPlayPlaylistInNewQueue(playlist)
    }
)

@androidx.compose.runtime.Composable
fun AddToAnotherQueueDialog(
    queues: List<PlaybackQueueCardUiState>,
    activeQueueId: String?,
    onQueueSelected: (String) -> Unit,
    onCreateNewQueue: () -> Unit,
    isCreatingQueue: Boolean,
    onDismiss: () -> Unit
) {
    val availableQueues = queues.filterNot { queue -> queue.queueId == activeQueueId }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(AppR.string.library_queue_add_to_another_title)) },
        text = {
            if (availableQueues.isEmpty()) {
                Text(stringResource(AppR.string.library_queue_no_other))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(availableQueues, key = PlaybackQueueCardUiState::queueId) { queue ->
                        ListItem(
                            leadingContent = {
                                AsyncImage(
                                    model = queue.representativeTrack?.albumArtUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    error = painterResource(R.drawable.ic_media_play),
                                    placeholder = painterResource(R.drawable.ic_media_play),
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            },
                            headlineContent = { Text(queue.name) },
                            supportingContent = {
                                Text(
                                    queue.currentTrack?.title?.let { title ->
                                        pluralStringResource(AppR.plurals.library_queue_track_count_with_title, queue.entryCount, queue.entryCount, title)
                                    } ?: pluralStringResource(AppR.plurals.library_queue_track_count, queue.entryCount, queue.entryCount)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            trailingContent = {
                                TextButton(onClick = { onQueueSelected(queue.queueId) }) {
                                    Text(stringResource(AppR.string.library_queue_add_action))
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onCreateNewQueue,
                enabled = !isCreatingQueue
            ) {
                if (isCreatingQueue) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Add, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isCreatingQueue) stringResource(AppR.string.library_queue_creating) else stringResource(AppR.string.library_queue_create_new))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(end = 4.dp)
            ) { Text(stringResource(AppR.string.settings_cancel)) }
        }
    )
}
