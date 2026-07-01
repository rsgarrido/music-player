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
    val addSongsToPlaylist: (Playlist, List<Song>) -> Unit,
    val removePlaylistSong: (PlaylistSong) -> Unit
)

@Composable
fun rememberPlaylistSnackbarActions(
    snackbarHostState: SnackbarHostState,
    onAddSongToPlaylistClick: (Playlist, Song) -> Unit,
    onAddSongsToPlaylistClick: (Playlist, List<Song>) -> Unit,
    onRemovePlaylistSongClick: (PlaylistSong) -> Unit
): PlaylistSnackbarActions {
    val coroutineScope = rememberCoroutineScope()

    fun showAddedSnackbar(
        playlist: Playlist,
        songs: List<Song>
    ) {
        coroutineScope.launch {
            val message = if (songs.size == 1) {
                "\"${songs.first().title}\" added to \"${playlist.name}\""
            } else {
                "${songs.size} songs added to \"${playlist.name}\""
            }

            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
                withDismissAction = true
            )
        }
    }

    return PlaylistSnackbarActions(
        addSongToPlaylist = { playlist, song ->
            onAddSongToPlaylistClick(playlist, song)
            showAddedSnackbar(
                playlist = playlist,
                songs = listOf(song)
            )
        },
        addSongsToPlaylist = { playlist, songs ->
            if (songs.isNotEmpty()) {
                onAddSongsToPlaylistClick(playlist, songs)
                showAddedSnackbar(
                    playlist = playlist,
                    songs = songs
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