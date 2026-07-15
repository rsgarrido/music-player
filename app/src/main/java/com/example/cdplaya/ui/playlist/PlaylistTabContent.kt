package com.example.cdplaya.ui.playlist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.cdplaya.data.Playlist
import com.example.cdplaya.data.PlaylistSong
import com.example.cdplaya.data.Song
import com.example.cdplaya.data.stableKey

@Composable
fun PlaylistsTabContent(
    songs: List<Song>,
    playlists: List<Playlist>,
    selectedPlaylistId: Long?,
    selectedPlaylistName: String,
    selectedPlaylistSongs: List<PlaylistSong>,
    currentSong: Song?,
    recentlyAddedSongIds: Set<Long>,
    favoriteSongKeys: Set<String>,
    onCreatePlaylistClick: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onDeletePlaylistClick: (Playlist) -> Unit,
    onExportPlaylistClick: (Playlist) -> Unit,
    onBackFromPlaylist: () -> Unit,
    onPlaySongsClick: (List<Song>, Boolean) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayNextClick: (Song) -> Unit,
    onAddToQueueClick: (Song) -> Unit,
    onToggleFavoriteClick: (Song) -> Unit,
    onRemovePlaylistSongClick: (PlaylistSong) -> Unit,
    onRenamePlaylistClick: (Playlist, String) -> Unit,
    onMovePlaylistSongUpClick: (PlaylistSong) -> Unit,
    onMovePlaylistSongDownClick: (PlaylistSong) -> Unit,
    onEditSongTagsClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedPlaylistId == null) {
        PlaylistListScreen(
            playlists = playlists,
            onCreatePlaylistClick = onCreatePlaylistClick,
            onPlaylistClick = onPlaylistClick,
            onDeletePlaylistClick = onDeletePlaylistClick,
            onExportPlaylistClick = onExportPlaylistClick,
            onRenamePlaylistClick = onRenamePlaylistClick,
            modifier = modifier
        )
    } else {
        val availablePlaylistSongs = selectedPlaylistSongs.mapNotNull { playlistSong ->
            songs.firstOrNull { song ->
                song.stableKey() == playlistSong.songKey
            }
        }

        PlaylistDetailScreen(
            playlistName = selectedPlaylistName,
            playlistSongs = availablePlaylistSongs,
            playlistSongRows = selectedPlaylistSongs,
            currentSongId = currentSong?.id,
            recentlyAddedSongIds = recentlyAddedSongIds,
            favoriteSongKeys = favoriteSongKeys,
            onBackClick = onBackFromPlaylist,
            onPlayAllClick = {
                onPlaySongsClick(availablePlaylistSongs, false)
            },
            onShuffleAllClick = {
                onPlaySongsClick(availablePlaylistSongs, true)
            },
            onSongClick = onSongClick,
            onPlayNextClick = onPlayNextClick,
            onAddToQueueClick = onAddToQueueClick,
            onToggleFavoriteClick = onToggleFavoriteClick,
            onRemovePlaylistSongClick = onRemovePlaylistSongClick,
            onEditSongTagsClick = onEditSongTagsClick,
            onMovePlaylistSongUpClick = onMovePlaylistSongUpClick,
            onMovePlaylistSongDownClick = onMovePlaylistSongDownClick,
            modifier = modifier
        )
    }
}
