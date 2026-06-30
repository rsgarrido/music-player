package com.example.cdplaya.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.example.cdplaya.data.Playlist
import com.example.cdplaya.data.PlaylistSong
import com.example.cdplaya.data.Song
import kotlinx.coroutines.launch

data class PlaylistSnackbarActions(
    val addSongToPlaylist: (Playlist, Song) -> Unit,
    val removePlaylistSong: (PlaylistSong) -> Unit
)

@Composable
fun rememberPlaylistSnackbarActions(
    snackbarHostState: SnackbarHostState,
    onAddSongToPlaylistClick: (Playlist, Song) -> Unit,
    onRemovePlaylistSongClick: (PlaylistSong) -> Unit
): PlaylistSnackbarActions {
    val coroutineScope = rememberCoroutineScope()

    return PlaylistSnackbarActions(
        addSongToPlaylist = { playlist, song ->
            onAddSongToPlaylistClick(playlist, song)

            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "\"${song.title}\" added to \"${playlist.name}\"",
                    duration = SnackbarDuration.Short,
                    withDismissAction = true
                )
            }
        },
        removePlaylistSong = { playlistSong ->
            onRemovePlaylistSongClick(playlistSong)

            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "\"${playlistSong.title}\" removed from playlist",
                    duration = SnackbarDuration.Short,
                    withDismissAction = true
                )
            }
        }
    )
}