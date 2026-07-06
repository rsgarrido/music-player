package com.example.cdplaya.ui

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.cdplaya.data.PlayerTheme
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
    onAddSongToPlaylistClick: (Playlist, Song) -> Unit,
    onAddSongsToPlaylistClick: (Playlist, List<Song>) -> Unit,
    songsPendingPlaylistAdd: List<Song>,
    onDismissBulkAddToPlaylistDialog: () -> Unit,
    isSleepTimerDialogVisible: Boolean,
    isSleepTimerActive: Boolean,
    sleepTimerDisplayText: String,
    onStartSleepTimerClick: (Int) -> Unit,
    onCancelSleepTimerClick: () -> Unit,
    onDismissSleepTimerDialog: () -> Unit,
    selectedPlayerTheme: PlayerTheme
) {
    if (isPlayerExpanded && currentSong != null) {
        ExpandedPlayerThemeHost(
            selectedPlayerTheme = selectedPlayerTheme,
            currentSong = currentSong,
            isPlaying = isPlaying,
            isShuffleEnabled = isShuffleEnabled,
            repeatMode = repeatMode,
            currentPosition = currentPosition,
            duration = duration,
            isCurrentSongFavorite = currentSong?.let { song ->
                song.favoriteKey() in favoriteSongKeys
            } == true,
            onPlayPauseClick = onPlayPauseClick,
            onPreviousClick = onPreviousClick,
            onNextClick = onNextClick,
            onSeekChange = onSeekChange,
            onShuffleClick = onShuffleClick,
            onRepeatClick = onRepeatClick,
            onCollapseClick = onCollapseExpandedPlayer,
            onOpenUpNextClick = onShowExpandedUpNextSheet,
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
            title = "Create Playlist",
            confirmButtonText = "Create",
            existingPlaylistNames = playlists.map { playlist ->
                playlist.name
            },
            onDismiss = onDismissCreatePlaylistDialog,
            onConfirmClick = { playlistName ->
                onCreatePlaylistClick(playlistName)
                onDismissCreatePlaylistDialog()
            }
        )
    }

    if (isSleepTimerDialogVisible) {
        SleepTimerDialog(
            isTimerActive = isSleepTimerActive,
            sleepTimerDisplayText = sleepTimerDisplayText,
            onStartTimerClick = onStartSleepTimerClick,
            onCancelTimerClick = onCancelSleepTimerClick,
            onDismiss = onDismissSleepTimerDialog
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

    if (songsPendingPlaylistAdd.isNotEmpty()) {
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = onDismissBulkAddToPlaylistDialog,
            onPlaylistSelected = { playlist ->
                onAddSongsToPlaylistClick(playlist, songsPendingPlaylistAdd)
                onDismissBulkAddToPlaylistDialog()
            }
        )
    }
}