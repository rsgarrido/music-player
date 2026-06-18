package com.example.cdplaya

import android.Manifest
import android.os.Build
import android.os.Bundle
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.cdplaya.data.LibraryFolder
import com.example.cdplaya.data.LibraryPreferences
import com.example.cdplaya.data.MusicRepository
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.PlaybackController
import com.example.cdplaya.ui.MusicScreen
import com.example.cdplaya.ui.theme.CdplayaTheme

class MainActivity : ComponentActivity() {

    private var songs by mutableStateOf<List<Song>>(emptyList())
    private var permissionGranted by mutableStateOf(false)

    private lateinit var playbackController: PlaybackController
    private lateinit var libraryPreferences: LibraryPreferences

    private val libraryFolders = mutableStateListOf<LibraryFolder>()
    private var selectedLibraryFolders by mutableStateOf<Set<String>>(emptySet())

    private val mediaPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.READ_MEDIA_AUDIO] == true
        val imagesGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES] == true

        permissionGranted = audioGranted && imagesGranted

        if (permissionGranted) {
            loadSongs()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        libraryPreferences = LibraryPreferences(this)
        playbackController = PlaybackController(this)
        playbackController.connect()

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
                        songs = songs,
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
                        libraryFolders = libraryFolders,
                        selectedLibraryFolders = selectedLibraryFolders,
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
                        onLibraryFolderToggle = { folderPath ->
                            toggleLibraryFolder(folderPath)
                        },
                        onSelectAllLibraryFolders = {
                            selectAllLibraryFolders()
                        },
                        onClearSelectedLibraryFolders = {
                            clearSelectedLibraryFolders()
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
            loadSongs()
        }
    }

    private fun loadSongs() {
        val repository = MusicRepository(this)

        selectedLibraryFolders = libraryPreferences.getSelectedFolders()

        libraryFolders.clear()
        libraryFolders.addAll(repository.getLibraryFolders())

        songs = repository.getSongs(selectedLibraryFolders)
        playbackController.setLibrarySongs(songs)
    }

    private fun toggleLibraryFolder(folderPath: String) {
        selectedLibraryFolders = if (folderPath in selectedLibraryFolders) {
            selectedLibraryFolders - folderPath
        } else {
            selectedLibraryFolders + folderPath
        }

        libraryPreferences.saveSelectedFolders(selectedLibraryFolders)
        reloadSongsAfterFolderChange()
    }

    private fun selectAllLibraryFolders() {
        selectedLibraryFolders = libraryFolders.map { folder ->
            folder.path
        }.toSet()

        libraryPreferences.saveSelectedFolders(selectedLibraryFolders)
        reloadSongsAfterFolderChange()
    }

    private fun clearSelectedLibraryFolders() {
        selectedLibraryFolders = emptySet()

        libraryPreferences.saveSelectedFolders(selectedLibraryFolders)
        reloadSongsAfterFolderChange()
    }

    private fun reloadSongsAfterFolderChange() {
        val repository = MusicRepository(this)

        songs = repository.getSongs(selectedLibraryFolders)
        playbackController.handleLibrarySongsChanged(songs)
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