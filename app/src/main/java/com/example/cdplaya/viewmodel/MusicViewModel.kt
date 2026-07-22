package com.example.cdplaya.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cdplaya.controller.LibraryController
import com.example.cdplaya.controller.SleepTimerController
import com.example.cdplaya.data.Song
import com.example.cdplaya.data.EditableSongTags
import com.example.cdplaya.data.Playlist
import com.example.cdplaya.data.PlaylistSong
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.data.PlayerThemePreferences
import com.example.cdplaya.data.PlayerThemeTokenPreferences
import com.example.cdplaya.data.ListeningHistoryRepository
import com.example.cdplaya.data.ModernPlayerPreferences
import com.example.cdplaya.data.backup.AppBackup
import com.example.cdplaya.data.backup.BackupExportResult
import com.example.cdplaya.data.backup.BackupRepository
import com.example.cdplaya.data.backup.BackupRestoreResult
import com.example.cdplaya.data.backup.BackupRestoreSummary
import com.example.cdplaya.data.local.AppDatabase
import com.example.cdplaya.data.local.DatabaseProvider
import com.example.cdplaya.data.playlistfile.M3uExportResult
import com.example.cdplaya.data.playlistfile.PlaylistImportResult
import com.example.cdplaya.data.playlistfile.PreparedPlaylistExport
import com.example.cdplaya.player.PlaybackController
import com.example.cdplaya.player.replaygain.ReplayGainMode
import com.example.cdplaya.player.replaygain.ReplayGainPreferences
import com.example.cdplaya.ui.player.theme.PlayerThemeTokenField
import com.example.cdplaya.ui.player.theme.PlayerThemeTokenOverrides
import com.example.cdplaya.ui.player.theme.PlayerThemeTokens
import com.example.cdplaya.ui.player.theme.customizationOptions
import com.example.cdplaya.ui.player.modern.ModernArtworkTransitionStyle
import com.example.cdplaya.ui.player.modern.ModernSeekbarStyle
import kotlinx.coroutines.launch

class MusicViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

    private val appDatabase: AppDatabase = DatabaseProvider.getDatabase(appContext)

    private val playerThemePreferences = PlayerThemePreferences(appContext)

    private val playerThemeTokenPreferences = PlayerThemeTokenPreferences(appContext)

    private val modernPlayerPreferences = ModernPlayerPreferences(appContext)

    private val replayGainPreferences = ReplayGainPreferences(appContext)

    private var selectedPlayerThemeState by mutableStateOf(
        playerThemePreferences.getSelectedPlayerTheme()
    )

    var selectedPlayerThemeTokens by mutableStateOf(
        playerThemeTokenPreferences.getTokens(selectedPlayerThemeState)
    )
        private set

    var selectedPlayerTheme: PlayerTheme
        get() = selectedPlayerThemeState
        private set(value) {
            selectedPlayerThemeState = value
            selectedPlayerThemeTokens = playerThemeTokenPreferences.getTokens(value)
        }

    fun selectPlayerTheme(playerTheme: PlayerTheme) {
        selectedPlayerTheme = playerTheme
        playerThemePreferences.saveSelectedPlayerTheme(playerTheme)
    }

    var selectedModernArtworkTransitionStyle by mutableStateOf(
        modernPlayerPreferences.getArtworkTransitionStyle()
    )
        private set

    fun selectModernArtworkTransitionStyle(style: ModernArtworkTransitionStyle) {
        selectedModernArtworkTransitionStyle = style
        modernPlayerPreferences.saveArtworkTransitionStyle(style)
    }

    var selectedModernSeekbarStyle by mutableStateOf(
        modernPlayerPreferences.getSeekbarStyle()
    )
        private set

    fun selectModernSeekbarStyle(style: ModernSeekbarStyle) {
        selectedModernSeekbarStyle = style
        modernPlayerPreferences.saveSeekbarStyle(style)
    }

    fun updatePlayerThemeTokenOverride(
        playerTheme: PlayerTheme,
        field: PlayerThemeTokenField,
        color: Color
    ) {
        if (playerTheme.customizationOptions().none { option -> option.field == field }) {
            return
        }

        val currentOverrides = playerThemeTokenPreferences.getOverrides(playerTheme)
        val updatedOverrides = when (field) {
            PlayerThemeTokenField.SHELL -> currentOverrides.copy(shellColor = color)
            PlayerThemeTokenField.ACCENT -> currentOverrides.copy(accentColor = color)
            PlayerThemeTokenField.DISPLAY_BACKGROUND -> {
                currentOverrides.copy(displayBackgroundColor = color)
            }

            PlayerThemeTokenField.DISPLAY_TEXT -> currentOverrides.copy(displayTextColor = color)
            PlayerThemeTokenField.SECONDARY_ACCENT -> {
                currentOverrides.copy(secondaryAccentColor = color)
            }
        }

        playerThemeTokenPreferences.saveOverrides(playerTheme, updatedOverrides)
        refreshSelectedPlayerThemeTokens(playerTheme)
    }

    fun resetPlayerThemeTokenOverrides(playerTheme: PlayerTheme) {
        playerThemeTokenPreferences.clearOverrides(playerTheme)
        refreshSelectedPlayerThemeTokens(playerTheme)
    }

    fun getPlayerThemeTokenOverrides(playerTheme: PlayerTheme): PlayerThemeTokenOverrides {
        return if (playerTheme.customizationOptions().isEmpty()) {
            PlayerThemeTokenOverrides()
        } else {
            playerThemeTokenPreferences.getOverrides(playerTheme)
        }
    }

    private fun refreshSelectedPlayerThemeTokens(playerTheme: PlayerTheme) {
        if (selectedPlayerTheme == playerTheme) {
            selectedPlayerThemeTokens = playerThemeTokenPreferences.getTokens(playerTheme)
        }
    }

    var selectedReplayGainMode by mutableStateOf(
        replayGainPreferences.getReplayGainMode()
    )
        private set

    fun selectReplayGainMode(replayGainMode: ReplayGainMode) {
        selectedReplayGainMode = replayGainMode
        replayGainPreferences.setReplayGainMode(replayGainMode)
        playbackController.setReplayGainMode(replayGainMode)
    }
    val playbackController = PlaybackController(
        context = appContext,
        coroutineScope = viewModelScope
    )

    val sleepTimerController = SleepTimerController(
        coroutineScope = viewModelScope,
        onTimerFinished = {
            playbackController.pausePlayback()
        }
    )

    val libraryController = LibraryController(
        context = appContext,
        appDatabase = appDatabase,
        playbackController = playbackController,
        coroutineScope = viewModelScope
    )

    private val backupRepository = BackupRepository(
        context = appContext,
        favoritesRepository = libraryController.favoritesRepository,
        playlistsRepository = libraryController.playlistsRepository,
        listeningHistoryRepository = libraryController.listeningHistoryRepository,
        libraryPreferences = libraryController.libraryPreferences,
        playerThemePreferences = playerThemePreferences,
        replayGainPreferences = replayGainPreferences,
        modernPlayerPreferences = modernPlayerPreferences,
        playerThemeTokenPreferences = playerThemeTokenPreferences
    )

    init {
        val listeningHistoryRepository = ListeningHistoryRepository(
            appDatabase.songPlayStatsDao()
        )

        playbackController.setListeningHistoryRepository(listeningHistoryRepository)

        playbackController.setOnListeningHistoryChanged {
            libraryController.refreshListeningHistory()
        }

        playbackController.setReplayGainMode(selectedReplayGainMode)
        playbackController.connect()
        libraryController.loadSavedUserData()
    }

    val isSleepTimerActive: Boolean
        get() = sleepTimerController.isTimerActive

    fun getSleepTimerDisplayText(): String {
        return sleepTimerController.getDisplayText()
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerController.startTimer(minutes)
    }

    fun cancelSleepTimer() {
        sleepTimerController.cancelTimer()
    }

    fun loadSongs() {
        libraryController.loadSongs()
    }

    fun savePlayerState() {
        playbackController.savePlayerState()
    }

    fun playSelectedSong(
        song: Song,
        playbackContext: List<Song>
    ) {
        playbackController.playSelectedSong(
            song = song,
            playbackContext = playbackContext
        )
    }

    fun playSongsFromContext(
        playbackContext: List<Song>,
        shuffle: Boolean
    ) {
        playbackController.playSongsFromContext(
            playbackContext = playbackContext,
            shuffle = shuffle
        )
    }

    fun togglePlayPause() {
        playbackController.togglePlayPause()
    }

    fun skipToPrevious() {
        playbackController.skipToPrevious()
    }

    fun skipToNext() {
        playbackController.skipToNext()
    }

    fun seekTo(position: Int) {
        playbackController.seekTo(position)
    }

    fun toggleShuffle() {
        playbackController.toggleShuffle()
    }

    fun cycleRepeatMode() {
        playbackController.cycleRepeatMode()
    }

    fun addSongToQueue(song: Song) {
        playbackController.addSongToQueue(song)
    }

    fun addSongToPlayNext(song: Song) {
        playbackController.addSongToPlayNext(song)
    }

    fun removeFirstMatchingSongFromQueue(song: Song) {
        playbackController.removeFirstMatchingSongFromQueue(song)
    }

    fun removeLastMatchingSongFromQueue(song: Song) {
        playbackController.removeLastMatchingSongFromQueue(song)
    }

    fun removeSongFromQueue(index: Int) {
        playbackController.removeSongFromQueue(index)
    }

    fun moveQueuedSongUp(index: Int) {
        playbackController.moveQueuedSongUp(index)
    }

    fun moveQueuedSongDown(index: Int) {
        playbackController.moveQueuedSongDown(index)
    }

    fun clearQueue() {
        playbackController.clearQueue()
    }

    fun addSongsToPlayNext(songs: List<Song>) {
        playbackController.addSongsToPlayNext(songs)
    }

    fun addSongsToQueue(songs: List<Song>) {
        playbackController.addSongsToQueue(songs)
    }

    fun removeFirstMatchingSongsFromQueue(songs: List<Song>) {
        playbackController.removeFirstMatchingSongsFromQueue(songs)
    }

    fun removeLastMatchingSongsFromQueue(songs: List<Song>) {
        playbackController.removeLastMatchingSongsFromQueue(songs)
    }

    fun toggleLibraryFolder(folderPath: String) {
        libraryController.toggleLibraryFolder(folderPath)
    }

    fun selectAllLibraryFolders() {
        libraryController.selectAllLibraryFolders()
    }

    fun clearSelectedLibraryFolders() {
        libraryController.clearSelectedLibraryFolders()
    }

    fun toggleFavorite(song: Song) {
        libraryController.toggleFavorite(song)
    }

    fun createPlaylist(playlistName: String) {
        libraryController.createPlaylist(playlistName)
    }

    fun renamePlaylist(
        playlist: Playlist,
        newName: String
    ) {
        libraryController.renamePlaylist(
            playlist = playlist,
            newName = newName
        )
    }

    fun deletePlaylist(playlist: Playlist) {
        libraryController.deletePlaylist(playlist)
    }

    fun loadSelectedPlaylist(playlist: Playlist) {
        libraryController.loadSelectedPlaylist(playlist)
    }

    fun preparePlaylistExport(
        playlist: Playlist,
        onPrepared: (Result<PreparedPlaylistExport>) -> Unit
    ) {
        libraryController.preparePlaylistExport(
            playlist = playlist,
            onPrepared = onPrepared
        )
    }

    fun exportM3uPlaylist(
        uri: Uri,
        songs: List<Song>,
        onExported: (Result<M3uExportResult>) -> Unit
    ) {
        libraryController.exportM3uPlaylist(
            uri = uri,
            songs = songs,
            onExported = onExported
        )
    }

    fun importM3uPlaylist(
        uri: Uri,
        onImported: (Result<PlaylistImportResult>) -> Unit
    ) {
        libraryController.importM3uPlaylist(
            uri = uri,
            onImported = onImported
        )
    }

    fun exportBackup(
        uri: Uri,
        onExported: (Result<BackupExportResult>) -> Unit
    ) {
        viewModelScope.launch {
            onExported(
                runCatching {
                    backupRepository.writeBackupToUri(uri)
                }
            )
        }
    }

    fun readBackupFromUri(
        uri: Uri,
        onRead: (Result<AppBackup>) -> Unit
    ) {
        viewModelScope.launch {
            onRead(
                runCatching {
                    backupRepository.readBackupFromUri(uri)
                }
            )
        }
    }

    fun summarizeBackupRestore(backup: AppBackup): BackupRestoreSummary {
        return backupRepository.summarizeRestore(backup)
    }

    fun restoreBackup(
        backup: AppBackup,
        onRestored: (Result<BackupRestoreResult>) -> Unit
    ) {
        viewModelScope.launch {
            val result = runCatching {
                val restoreResult = backupRepository.restoreBackup(backup)

                libraryController.refreshAfterBackupRestore()

                selectedPlayerTheme = playerThemePreferences.getSelectedPlayerTheme()
                selectedReplayGainMode = replayGainPreferences.getReplayGainMode()
                playbackController.setReplayGainMode(selectedReplayGainMode)

                restoreResult
            }

            onRestored(result)
        }
    }

    fun addSongToPlaylist(
        playlist: Playlist,
        song: Song
    ) {
        libraryController.addSongToPlaylist(
            playlist = playlist,
            song = song
        )
    }

    fun addSongsToPlaylist(
        playlist: Playlist,
        songs: List<Song>
    ) {
        libraryController.addSongsToPlaylist(
            playlist = playlist,
            songs = songs
        )
    }

    fun removePlaylistSong(playlistSong: PlaylistSong) {
        libraryController.removePlaylistSong(playlistSong)
    }

    fun movePlaylistSongUp(playlistSong: PlaylistSong) {
        libraryController.movePlaylistSongUp(playlistSong)
    }

    fun movePlaylistSongDown(playlistSong: PlaylistSong) {
        libraryController.movePlaylistSongDown(playlistSong)
    }

    fun refreshSongsAfterTagEdit(
        originalSong: Song,
        editedTags: EditableSongTags
    ) {
        libraryController.refreshSongsAfterTagEdit(
            originalSong = originalSong,
            editedTags = editedTags
        )
    }

    override fun onCleared() {
        playbackController.release()
        sleepTimerController.release()
        super.onCleared()
    }
}
