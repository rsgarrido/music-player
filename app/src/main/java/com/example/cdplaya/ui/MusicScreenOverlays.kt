package com.example.cdplaya.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.data.Playlist
import com.example.cdplaya.data.Song
import com.example.cdplaya.data.favoriteKey
import com.example.cdplaya.player.RepeatMode
import com.example.cdplaya.ui.player.ExpandedPlayerThemeHost
import com.example.cdplaya.ui.player.ImmersiveSystemBarsEffect
import com.example.cdplaya.ui.playlist.AddToPlaylistDialog
import com.example.cdplaya.ui.playlist.PlaylistNameDialog
import com.example.cdplaya.ui.queue.QueueScreen
import com.example.cdplaya.ui.settings.SleepTimerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreenOverlays(
    isPlayerExpanded: Boolean,
    currentSong: Song?,
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
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

    ImmersiveSystemBarsEffect(
        isImmersive = isPlayerExpanded &&
                selectedPlayerTheme == PlayerTheme.CLASSIC_WHEEL
    )

    if (isPlayerExpanded && currentSong != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            ExpandedPlayerThemeHost(
                selectedPlayerTheme = selectedPlayerTheme,
                currentSong = currentSong,
                isPlaying = isPlaying,
                isShuffleEnabled = isShuffleEnabled,
                repeatMode = repeatMode,
                currentPosition = currentPosition,
                duration = duration,
                isCurrentSongFavorite = currentSong.favoriteKey() in favoriteSongKeys,
                onPlayPauseClick = onPlayPauseClick,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onSeekChange = onSeekChange,
                onShuffleClick = onShuffleClick,
                onRepeatClick = onRepeatClick,
                onCollapseClick = onCollapseExpandedPlayer,
                onOpenUpNextClick = onShowExpandedUpNextSheet,
                onToggleFavoriteClick = onToggleFavoriteClick,
                songs = songs,
                onSongClick = onSongClick
            )
        }
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