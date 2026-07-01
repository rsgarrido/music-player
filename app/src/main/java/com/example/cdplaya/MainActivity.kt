package com.example.cdplaya

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.cdplaya.controller.LibraryController
import com.example.cdplaya.data.local.AppDatabase
import com.example.cdplaya.data.local.DatabaseProvider
import com.example.cdplaya.player.PlaybackController
import com.example.cdplaya.ui.MusicScreen
import com.example.cdplaya.ui.theme.CdplayaTheme

class MainActivity : ComponentActivity() {

    private var permissionGranted by mutableStateOf(false)

    private lateinit var appDatabase: AppDatabase
    private lateinit var playbackController: PlaybackController
    private lateinit var libraryController: LibraryController

    private val mediaPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.READ_MEDIA_AUDIO] == true
        val imagesGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES] == true

        permissionGranted = audioGranted && imagesGranted

        if (permissionGranted) {
            libraryController.loadSongs()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appDatabase = DatabaseProvider.getDatabase(this)
        Log.d("CDPlayaDatabase", "Room database initialized")

        playbackController = PlaybackController(this)
        playbackController.connect()

        libraryController = LibraryController(
            context = this,
            appDatabase = appDatabase,
            playbackController = playbackController,
            coroutineScope = lifecycleScope
        )
        libraryController.loadSavedUserData()

        requestAudioPermission()

        setContent {
            CdplayaTheme {
                val snackbarHostState = remember { SnackbarHostState() }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState)
                    }
                ) { innerPadding ->
                    MusicScreen(
                        songs = libraryController.songs,
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
                        modifier = Modifier.padding(innerPadding),
                        libraryFolders = libraryController.libraryFolders,
                        selectedLibraryFolders = libraryController.selectedLibraryFolders,
                        favoriteSongKeys = libraryController.favoriteSongKeys,
                        playlists = libraryController.playlists,
                        selectedPlaylistName = libraryController.selectedPlaylistName,
                        selectedPlaylistSongs = libraryController.selectedPlaylistSongs,
                        onSongClick = { song, playbackContext ->
                            playbackController.playSelectedSong(
                                song = song,
                                playbackContext = playbackContext
                            )
                        },
                        onPlaySongsClick = { playbackContext, shuffle ->
                            playbackController.playSongsFromContext(
                                playbackContext = playbackContext,
                                shuffle = shuffle
                            )
                        },
                        onPlayPauseClick = {
                            playbackController.togglePlayPause()
                        },
                        onPreviousClick = {
                            playbackController.skipToPrevious()
                        },
                        onNextClick = {
                            playbackController.skipToNext()
                        },
                        onSeekChange = { position ->
                            playbackController.seekTo(position)
                        },
                        onShuffleClick = {
                            playbackController.toggleShuffle()
                        },
                        onRepeatClick = {
                            playbackController.cycleRepeatMode()
                        },
                        onAddToQueueClick = { song ->
                            playbackController.addSongToQueue(song)
                        },
                        onPlayNextClick = { song ->
                            playbackController.addSongToPlayNext(song)
                        },
                        onUndoPlayNextClick = { song ->
                            playbackController.removeFirstMatchingSongFromQueue(song)
                        },
                        onRemoveFromQueueClick = { index ->
                            playbackController.removeSongFromQueue(index)
                        },
                        onMoveQueueItemUpClick = { index ->
                            playbackController.moveQueuedSongUp(index)
                        },
                        onMoveQueueItemDownClick = { index ->
                            playbackController.moveQueuedSongDown(index)
                        },
                        onClearQueueClick = {
                            playbackController.clearQueue()
                        },
                        onUndoAddToQueueClick = { song ->
                            playbackController.removeLastMatchingSongFromQueue(song)
                        },
                        onPlayNextSongsClick = { songs ->
                            playbackController.addSongsToPlayNext(songs)
                        },
                        onAddSongsToQueueClick = { songs ->
                            playbackController.addSongsToQueue(songs)
                        },
                        onUndoPlayNextSongsClick = { songs ->
                            playbackController.removeFirstMatchingSongsFromQueue(songs)
                        },
                        onUndoAddSongsToQueueClick = { songs ->
                            playbackController.removeLastMatchingSongsFromQueue(songs)
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
                        onTagsEdited = { originalSong, editedTags ->
                            libraryController.refreshSongsAfterTagEdit(
                                originalSong = originalSong,
                                editedTags = editedTags
                            )
                        }
                    )
                }
            }
        }
    }

    private fun requestAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mediaPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            )
        } else {
            permissionGranted = true
            libraryController.loadSongs()
        }
    }

    override fun onPause() {
        super.onPause()
        playbackController.savePlayerState()
    }

    override fun onDestroy() {
        playbackController.release()
        super.onDestroy()
    }
}