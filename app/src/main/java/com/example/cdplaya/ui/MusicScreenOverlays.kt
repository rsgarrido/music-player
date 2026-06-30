package com.example.cdplaya.ui

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.cdplaya.data.Playlist
import com.example.cdplaya.data.Song
import com.example.cdplaya.data.favoriteKey
import com.example.cdplaya.player.RepeatMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreenOverlays(
    isPlayerExpanded: Boolean,
    currentSong: Song?,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    currentPosition: Int,
    duration: Int,
    favoriteSongKeys: Set<String>,
    isExpandedUpNextSheetVisible: Boolean,
    queuedSongs: List<Song>,
    upcomingSongs: List<Song>,
    isCreatePlaylistDialogVisible: Boolean,
    songPendingPlaylistAdd: Song?,
    playlists: List<Playlist>,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekChange: (Int) -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onCollapseExpandedPlayer: () -> Unit,
    onShowExpandedUpNextSheet: () -> Unit,
    onDismissExpandedUpNextSheet: () -> Unit,
    onRemoveFromQueueClick: (Int) -> Unit,
    onMoveQueueItemUpClick: (Int) -> Unit,
    onMoveQueueItemDownClick: (Int) -> Unit,
    onClearQueueClick: () -> Unit,
    onToggleFavoriteClick: (Song) -> Unit,
    onDismissCreatePlaylistDialog: () -> Unit,
    onCreatePlaylistClick: (String) -> Unit,
    onDismissAddToPlaylistDialog: () -> Unit,
    onAddSongToPlaylistClick: (Playlist, Song) -> Unit
) {
    if (isPlayerExpanded && currentSong != null) {
        PlayerCard(
            modifier = Modifier.fillMaxSize(),
            currentSong = currentSong,
            isPlaying = isPlaying,
            isExpanded = true,
            isShuffleEnabled = isShuffleEnabled,
            repeatMode = repeatMode,
            currentPosition = currentPosition,
            duration = duration,
            onPlayPauseClick = onPlayPauseClick,
            onPreviousClick = onPreviousClick,
            onNextClick = onNextClick,
            onSeekChange = onSeekChange,
            onShuffleClick = onShuffleClick,
            onRepeatClick = onRepeatClick,
            onExpandClick = {},
            onCollapseClick = onCollapseExpandedPlayer,
            onOpenUpNextClick = onShowExpandedUpNextSheet,
            isCurrentSongFavorite = currentSong.favoriteKey() in favoriteSongKeys,
            onToggleFavoriteClick = onToggleFavoriteClick
        )
    }

    if (isExpandedUpNextSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismissExpandedUpNextSheet
        ) {
            QueueScreen(
                queuedSongs = queuedSongs,
                upcomingSongs = upcomingSongs,
                isShuffleEnabled = isShuffleEnabled,
                onBackClick = onDismissExpandedUpNextSheet,
                onRemoveFromQueueClick = onRemoveFromQueueClick,
                onMoveQueueItemUpClick = onMoveQueueItemUpClick,
                onMoveQueueItemDownClick = onMoveQueueItemDownClick,
                onClearQueueClick = onClearQueueClick,
                modifier = Modifier.fillMaxHeight(0.86f)
            )
        }
    }

    if (isCreatePlaylistDialogVisible) {
        PlaylistNameDialog(
            onDismiss = onDismissCreatePlaylistDialog,
            onCreateClick = { playlistName ->
                onCreatePlaylistClick(playlistName)
                onDismissCreatePlaylistDialog()
            }
        )
    }

    if (songPendingPlaylistAdd != null) {
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = onDismissAddToPlaylistDialog,
            onPlaylistSelected = { playlist ->
                onAddSongToPlaylistClick(playlist, songPendingPlaylistAdd)
                onDismissAddToPlaylistDialog()
            }
        )
    }
}