package com.example.cdplaya.ui.playlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.example.cdplaya.data.PlaylistSong
import com.example.cdplaya.data.Song

@Composable
fun PlaylistDetailScreen(
    playlistName: String,
    playlistSongs: List<Song>,
    playlistSongRows: List<PlaylistSong>,
    currentSongId: Long?,
    recentlyAddedSongIds: Set<Long>,
    favoriteSongKeys: Set<String>,
    onBackClick: () -> Unit,
    onPlayAllClick: () -> Unit,
    onShuffleAllClick: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayNextClick: (Song) -> Unit,
    onAddToQueueClick: (Song) -> Unit,
    onToggleFavoriteClick: (Song) -> Unit,
    onRemovePlaylistSongClick: (PlaylistSong) -> Unit,
    onMovePlaylistSongUpClick: (PlaylistSong) -> Unit,
    onMovePlaylistSongDownClick: (PlaylistSong) -> Unit,
    onEditSongTagsClick: (Song) -> Unit,
    bottomContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = playlistName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${playlistSongs.size} available song(s)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Button(
                onClick = onPlayAllClick,
                enabled = playlistSongs.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null
                )
                Text(text = "Play")
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = onShuffleAllClick,
                enabled = playlistSongs.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = null
                )
                Text(text = "Shuffle")
            }
        }

        if (playlistSongRows.isEmpty()) {
            Text(
                text = "This playlist is empty.",
                modifier = Modifier.padding(16.dp)
            )
        } else if (playlistSongs.isEmpty()) {
            Text(
                text = "The songs in this playlist are not currently available on this device.",
                modifier = Modifier.padding(16.dp)
            )
        } else {
            PlaylistSongList(
                playlistSongs = playlistSongs,
                playlistSongRows = playlistSongRows,
                currentSongId = currentSongId,
                recentlyAddedSongIds = recentlyAddedSongIds,
                favoriteSongKeys = favoriteSongKeys,
                onSongClick = onSongClick,
                onPlayNextClick = onPlayNextClick,
                onAddToQueueClick = onAddToQueueClick,
                onToggleFavoriteClick = onToggleFavoriteClick,
                onRemovePlaylistSongClick = onRemovePlaylistSongClick,
                onMovePlaylistSongUpClick = onMovePlaylistSongUpClick,
                onMovePlaylistSongDownClick = onMovePlaylistSongDownClick,
                onEditSongTagsClick = onEditSongTagsClick,
                bottomContentPadding = bottomContentPadding,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
