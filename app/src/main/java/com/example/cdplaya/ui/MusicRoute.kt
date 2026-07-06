package com.example.cdplaya.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.cdplaya.viewmodel.MusicViewModel

@Composable
fun MusicRoute(
    musicViewModel: MusicViewModel,
    permissionGranted: Boolean,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val playbackController = musicViewModel.playbackController
    val libraryController = musicViewModel.libraryController
    val sleepTimerController = musicViewModel.sleepTimerController

    MusicScreen(
        songs = libraryController.songs,
        recentlyPlayedSongs = libraryController.recentlyPlayedSongs,
        mostPlayedSongs = libraryController.mostPlayedSongs,
        permissionGranted = permissionGranted,
        currentSong = playbackController.currentSong,
        isPlaying = playbackController.isPlaying,
        isShuffleEnabled = playbackController.isShuffleEnabled,
        repeatMode = playbackController.repeatMode,
        currentPosition = playbackController.currentPosition,
        duration = playbackController.duration,
        queuedSongs = playbackController.playbackQueue,
        upcomingSongs = playbackController.getComingUpSongsForDisplay(),
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        libraryFolders = libraryController.libraryFolders,
        selectedLibraryFolders = libraryController.selectedLibraryFolders,
        favoriteSongKeys = libraryController.favoriteSongKeys,
        playlists = libraryController.playlists,
        selectedPlaylistName = libraryController.selectedPlaylistName,
        selectedPlaylistSongs = libraryController.selectedPlaylistSongs,
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
            libraryController.toggleLibraryFolder(folderPath)
        },
        onSelectAllLibraryFolders = {
            libraryController.selectAllLibraryFolders()
        },
        onClearSelectedLibraryFolders = {
            libraryController.clearSelectedLibraryFolders()
        },
        onToggleFavoriteClick = { song ->
            libraryController.toggleFavorite(song)
        },
        onCreatePlaylistClick = { playlistName ->
            libraryController.createPlaylist(playlistName)
        },
        onRenamePlaylistClick = { playlist, newName ->
            libraryController.renamePlaylist(
                playlist = playlist,
                newName = newName
            )
        },
        onDeletePlaylistClick = { playlist ->
            libraryController.deletePlaylist(playlist)
        },
        onPlaylistSelected = { playlist ->
            libraryController.loadSelectedPlaylist(playlist)
        },
        onAddSongToPlaylistClick = { playlist, song ->
            libraryController.addSongToPlaylist(
                playlist = playlist,
                song = song
            )
        },
        onAddSongsToPlaylistClick = { playlist, songs ->
            libraryController.addSongsToPlaylist(
                playlist = playlist,
                songs = songs
            )
        },
        onRemovePlaylistSongClick = { playlistSong ->
            libraryController.removePlaylistSong(playlistSong)
        },
        onMovePlaylistSongUpClick = { playlistSong ->
            libraryController.movePlaylistSongUp(playlistSong)
        },
        onMovePlaylistSongDownClick = { playlistSong ->
            libraryController.movePlaylistSongDown(playlistSong)
        },
        onTagsEdited = { originalSong, editedTags ->
            libraryController.refreshSongsAfterTagEdit(
                originalSong = originalSong,
                editedTags = editedTags
            )
        },
        isSleepTimerActive = sleepTimerController.isTimerActive,
        sleepTimerDisplayText = sleepTimerController.getDisplayText(),
        onStartSleepTimerClick = { minutes ->
            sleepTimerController.startTimer(minutes)
        },
        onCancelSleepTimerClick = {
            sleepTimerController.cancelTimer()
        }
    )
}