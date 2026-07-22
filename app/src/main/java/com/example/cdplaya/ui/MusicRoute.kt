package com.example.cdplaya.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.example.cdplaya.viewmodel.MusicViewModel
import com.example.cdplaya.ui.playlist.rememberPlaylistExportActions
import com.example.cdplaya.ui.playlist.rememberPlaylistImportActions
import com.example.cdplaya.ui.settings.rememberBackupExportActions
import com.example.cdplaya.ui.settings.rememberBackupRestoreActions

@Composable
fun MusicRoute(
    musicViewModel: MusicViewModel,
    permissionGranted: Boolean,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val playbackController = musicViewModel.playbackController
    val libraryController = musicViewModel.libraryController
    val libraryUiState by libraryController.uiState.collectAsState()
    val playlistExportActions = rememberPlaylistExportActions(
        snackbarHostState = snackbarHostState,
        onPrepareExport = musicViewModel::preparePlaylistExport,
        onExport = musicViewModel::exportM3uPlaylist
    )
    val playlistImportActions = rememberPlaylistImportActions(
        snackbarHostState = snackbarHostState,
        onImport = musicViewModel::importM3uPlaylist
    )
    val backupExportActions = rememberBackupExportActions(
        snackbarHostState = snackbarHostState,
        onExport = musicViewModel::exportBackup
    )
    val backupRestoreActions = rememberBackupRestoreActions(
        snackbarHostState = snackbarHostState,
        onRead = musicViewModel::readBackupFromUri,
        onSummarize = musicViewModel::summarizeBackupRestore,
        onRestore = musicViewModel::restoreBackup
    )

    MusicScreen(
        songs = libraryUiState.songs,
        recentlyPlayedSongs = libraryUiState.recentlyPlayedSongs,
        recentlyAddedLibrarySongs = libraryUiState.recentlyAddedSongs,
        mostPlayedSongs = libraryUiState.mostPlayedSongs,
        permissionGranted = permissionGranted,
        currentSong = playbackController.currentSong,
        isPlayerConnected = playbackController.isPlayerConnected,
        previousHistoryCount = playbackController.getPreviousHistoryCount(),
        forwardHistoryCount = playbackController.getForwardHistoryCount(),
        previousPreviewSong = playbackController.getPreviousSongForPreview(),
        nextPreviewSong = playbackController.getNextSongForPreview(),
        isPlaying = playbackController.isPlaying,
        isShuffleEnabled = playbackController.isShuffleEnabled,
        repeatMode = playbackController.repeatMode,
        currentPosition = playbackController.currentPosition,
        duration = playbackController.duration,
        queuedSongs = playbackController.playbackQueue,
        upcomingSongs = playbackController.getComingUpSongsForDisplay(),
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        libraryFolders = libraryUiState.folders,
        selectedLibraryFolders = libraryUiState.selectedFolders,
        favoriteMembershipKeys = libraryUiState.favoriteMembershipKeys,
        unresolvedFavoriteCount = libraryUiState.unresolvedFavoriteCount,
        unresolvedPlaylistRowCount = libraryUiState.unresolvedPlaylistRowCount,
        unresolvedListeningHistoryCount = libraryUiState.unresolvedListeningHistoryCount,
        playlists = libraryUiState.playlists,
        selectedPlaylistName = libraryUiState.selectedPlaylistName,
        selectedPlaylistSongs = libraryUiState.selectedPlaylistSongs,
        onSongClick = { song, playbackContext ->
            musicViewModel.playSelectedSong(
                song = song,
                playbackContext = playbackContext
            )
        },
        onPlaySongsClick = { playbackContext, shuffle ->
            musicViewModel.playSongsFromContext(
                playbackContext = playbackContext,
                shuffle = shuffle
            )
        },
        onPlayPauseClick = {
            musicViewModel.togglePlayPause()
        },
        onPreviousClick = {
            musicViewModel.skipToPrevious()
        },
        onNextClick = {
            musicViewModel.skipToNext()
        },
        onSeekChange = { position ->
            musicViewModel.seekTo(position)
        },
        onShuffleClick = {
            musicViewModel.toggleShuffle()
        },
        onRepeatClick = {
            musicViewModel.cycleRepeatMode()
        },
        onAddToQueueClick = { song ->
            musicViewModel.addSongToQueue(song)
        },
        onPlayNextClick = { song ->
            musicViewModel.addSongToPlayNext(song)
        },
        onUndoPlayNextClick = { song ->
            musicViewModel.removeFirstMatchingSongFromQueue(song)
        },
        onRemoveFromQueueClick = { index ->
            musicViewModel.removeSongFromQueue(index)
        },
        onMoveQueueItemUpClick = { index ->
            musicViewModel.moveQueuedSongUp(index)
        },
        onMoveQueueItemDownClick = { index ->
            musicViewModel.moveQueuedSongDown(index)
        },
        onClearQueueClick = {
            musicViewModel.clearQueue()
        },
        onUndoAddToQueueClick = { song ->
            musicViewModel.removeLastMatchingSongFromQueue(song)
        },
        onPlayNextSongsClick = { songs ->
            musicViewModel.addSongsToPlayNext(songs)
        },
        onAddSongsToQueueClick = { songs ->
            musicViewModel.addSongsToQueue(songs)
        },
        onUndoPlayNextSongsClick = { songs ->
            musicViewModel.removeFirstMatchingSongsFromQueue(songs)
        },
        onUndoAddSongsToQueueClick = { songs ->
            musicViewModel.removeLastMatchingSongsFromQueue(songs)
        },
        onLibraryFolderToggle = { folderPath ->
            musicViewModel.toggleLibraryFolder(folderPath)
        },
        onSelectAllLibraryFolders = {
            musicViewModel.selectAllLibraryFolders()
        },
        onClearSelectedLibraryFolders = {
            musicViewModel.clearSelectedLibraryFolders()
        },
        onToggleFavoriteClick = { song ->
            musicViewModel.toggleFavorite(song)
        },
        onCreatePlaylistClick = { playlistName ->
            musicViewModel.createPlaylist(playlistName)
        },
        onRenamePlaylistClick = { playlist, newName ->
            musicViewModel.renamePlaylist(
                playlist = playlist,
                newName = newName
            )
        },
        onDeletePlaylistClick = { playlist ->
            musicViewModel.deletePlaylist(playlist)
        },
        onExportPlaylistClick = playlistExportActions.exportPlaylist,
        onImportPlaylistClick = playlistImportActions.importPlaylist,
        onExportBackupClick = backupExportActions.exportBackup,
        onRestoreBackupClick = backupRestoreActions.restoreBackup,
        onPlaylistSelected = { playlist ->
            musicViewModel.loadSelectedPlaylist(playlist)
        },
        onAddSongToPlaylistClick = { playlist, song ->
            musicViewModel.addSongToPlaylist(
                playlist = playlist,
                song = song
            )
        },
        onAddSongsToPlaylistClick = { playlist, songs ->
            musicViewModel.addSongsToPlaylist(
                playlist = playlist,
                songs = songs
            )
        },
        onRemovePlaylistSongClick = { playlistSong ->
            musicViewModel.removePlaylistSong(playlistSong)
        },
        onMovePlaylistSongUpClick = { playlistSong ->
            musicViewModel.movePlaylistSongUp(playlistSong)
        },
        onMovePlaylistSongDownClick = { playlistSong ->
            musicViewModel.movePlaylistSongDown(playlistSong)
        },
        onTagsEdited = { originalSong, editedTags ->
            musicViewModel.refreshSongsAfterTagEdit(
                originalSong = originalSong,
                editedTags = editedTags
            )
        },
        isSleepTimerActive = musicViewModel.isSleepTimerActive,
        sleepTimerDisplayText = musicViewModel.getSleepTimerDisplayText(),
        onStartSleepTimerClick = { minutes ->
            musicViewModel.startSleepTimer(minutes)
        },
        onCancelSleepTimerClick = {
            musicViewModel.cancelSleepTimer()
        },
        selectedPlayerTheme = musicViewModel.selectedPlayerTheme,
        selectedPlayerThemeTokens = musicViewModel.selectedPlayerThemeTokens,
        onPlayerThemeSelected = { playerTheme ->
            musicViewModel.selectPlayerTheme(playerTheme)
        },
        onUpdatePlayerThemeTokenOverride = musicViewModel::updatePlayerThemeTokenOverride,
        onResetPlayerThemeTokenOverrides = musicViewModel::resetPlayerThemeTokenOverrides,
        selectedModernArtworkTransitionStyle =
            musicViewModel.selectedModernArtworkTransitionStyle,
        onModernArtworkTransitionStyleSelected =
            musicViewModel::selectModernArtworkTransitionStyle,
        selectedModernSeekbarStyle = musicViewModel.selectedModernSeekbarStyle,
        onModernSeekbarStyleSelected = musicViewModel::selectModernSeekbarStyle,
        selectedReplayGainMode = musicViewModel.selectedReplayGainMode,
        onReplayGainModeSelected = { replayGainMode ->
            musicViewModel.selectReplayGainMode(replayGainMode)
        },
    )
}
