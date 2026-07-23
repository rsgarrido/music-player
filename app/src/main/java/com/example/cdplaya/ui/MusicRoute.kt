package com.example.cdplaya.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import com.example.cdplaya.viewmodel.MusicViewModel
import com.example.cdplaya.ui.state.displayText
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
    val playbackUiState by musicViewModel.playbackUiState.collectAsStateWithLifecycle()
    val libraryUiState by musicViewModel.libraryUiState.collectAsStateWithLifecycle()
    val sleepTimerUiState by musicViewModel.sleepTimerUiState.collectAsStateWithLifecycle()
    val playerAppearanceUiState by
        musicViewModel.playerAppearanceUiState.collectAsStateWithLifecycle()
    val libraryAppearanceUiState by
        musicViewModel.libraryAppearanceUiState.collectAsStateWithLifecycle()
    val audioOffloadPreference by
        musicViewModel.audioOffloadPreference.collectAsStateWithLifecycle()
    val audioOutputUiState by
        musicViewModel.audioOutputUiState.collectAsStateWithLifecycle()
    if (!playerAppearanceUiState.isLoaded || !libraryAppearanceUiState.isLoaded) return
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
        currentSong = playbackUiState.currentSong,
        isPlayerConnected = playbackUiState.isConnected,
        previousHistoryCount = playbackUiState.previousHistoryCount,
        forwardHistoryCount = playbackUiState.forwardHistoryCount,
        previousPreviewSong = playbackUiState.previousPreviewSong,
        nextPreviewSong = playbackUiState.nextPreviewSong,
        isPlaying = playbackUiState.isPlaying,
        isShuffleEnabled = playbackUiState.isShuffleEnabled,
        repeatMode = playbackUiState.repeatMode,
        playbackProgressUiState = musicViewModel.playbackProgressUiState,
        queuedSongs = playbackUiState.queuedSongs,
        upcomingSongs = playbackUiState.upcomingSongs,
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
        isSleepTimerActive = sleepTimerUiState.isActive,
        sleepTimerDisplayText = sleepTimerUiState.displayText(),
        onStartSleepTimerClick = { minutes ->
            musicViewModel.startSleepTimer(minutes)
        },
        onCancelSleepTimerClick = {
            musicViewModel.cancelSleepTimer()
        },
        selectedPlayerTheme = playerAppearanceUiState.selectedTheme,
        selectedPlayerThemeTokens = playerAppearanceUiState.themeTokens,
        onPlayerThemeSelected = { playerTheme ->
            musicViewModel.selectPlayerTheme(playerTheme)
        },
        onUpdatePlayerThemeTokenOverride = musicViewModel::updatePlayerThemeTokenOverride,
        onResetPlayerThemeTokenOverrides = musicViewModel::resetPlayerThemeTokenOverrides,
        selectedModernArtworkTransitionStyle =
            playerAppearanceUiState.modernArtworkTransitionStyle,
        onModernArtworkTransitionStyleSelected =
            musicViewModel::selectModernArtworkTransitionStyle,
        selectedModernSeekbarStyle = playerAppearanceUiState.modernSeekbarStyle,
        onModernSeekbarStyleSelected = musicViewModel::selectModernSeekbarStyle,
        selectedReplayGainMode = playerAppearanceUiState.replayGainMode,
        onReplayGainModeSelected = { replayGainMode ->
            musicViewModel.selectReplayGainMode(replayGainMode)
        },
        selectedAudioOffloadPreference = audioOffloadPreference,
        onAudioOffloadPreferenceSelected = musicViewModel::selectAudioOffloadPreference,
        audioOutputUiState = audioOutputUiState,
        onReadEditableSongTags = musicViewModel::readEditableSongTags,
        onGetUnsupportedTagEditingMessage = musicViewModel::getUnsupportedTagEditingMessage,
        onWriteTagsAndArtwork = musicViewModel::writeTagsAndArtwork,
        libraryAppearanceUiState = libraryAppearanceUiState,
        onLibraryViewOptionSelected = musicViewModel::selectLibraryViewOption,
    )
}
