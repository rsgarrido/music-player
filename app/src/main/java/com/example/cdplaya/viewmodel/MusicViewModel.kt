package com.example.cdplaya.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cdplaya.controller.LibraryController
import com.example.cdplaya.controller.SleepTimerController
import com.example.cdplaya.data.Song
import com.example.cdplaya.data.EditableSongTags
import com.example.cdplaya.data.Playlist
import com.example.cdplaya.data.PlaylistSong
import com.example.cdplaya.data.ListeningHistoryRepository
import com.example.cdplaya.data.local.AppDatabase
import com.example.cdplaya.data.local.DatabaseProvider
import com.example.cdplaya.player.PlaybackController

class MusicViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

    private val appDatabase: AppDatabase = DatabaseProvider.getDatabase(appContext)

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

    init {
        val listeningHistoryRepository = ListeningHistoryRepository(
            appDatabase.songPlayStatsDao()
        )

        playbackController.setListeningHistoryRepository(listeningHistoryRepository)

        playbackController.setOnListeningHistoryChanged {
            libraryController.refreshListeningHistory()
        }

        playbackController.connect()
        libraryController.loadSavedUserData()
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