package com.example.cdplaya

import android.Manifest
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cdplaya.data.MusicRepository
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.MusicPlayer
import com.example.cdplaya.player.PlayerStateStorage
import com.example.cdplaya.player.RepeatMode
import com.example.cdplaya.ui.theme.CdplayaTheme
import com.example.cdplaya.ui.MusicScreen
import com.example.cdplaya.data.LibraryPreferences
import com.example.cdplaya.data.LibraryFolder
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private var songs by mutableStateOf<List<Song>>(emptyList())
    private var permissionGranted by mutableStateOf(false)
    private lateinit var musicPlayer: MusicPlayer
    private lateinit var playerStateStorage: PlayerStateStorage
    private lateinit var libraryPreferences: LibraryPreferences
    private var currentSong by mutableStateOf<Song?>(null)
    private var isPlaying by mutableStateOf(false)
    private var isShuffleEnabled by mutableStateOf(false)
    private var repeatMode by mutableStateOf(RepeatMode.OFF)
    private val previousSongHistory = mutableListOf<Song>()
    private val nextSongHistory = mutableListOf<Song>()
    private val playbackQueue = mutableStateListOf<Song>()
    private var currentPosition by mutableStateOf(0)
    private var duration by mutableStateOf(0)
    private val progressHandler = Handler(Looper.getMainLooper())
    private val libraryFolders = mutableStateListOf<LibraryFolder>()
    private var selectedLibraryFolders by mutableStateOf<Set<String>>(emptySet())

    private val progressRunnable = object : Runnable {
        override fun run() {
            if (currentSong != null) {
                currentPosition = musicPlayer.getCurrentPosition()
                duration = musicPlayer.getDuration()
                progressHandler.postDelayed(this, 500)
            }
        }
    }

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

        musicPlayer = MusicPlayer(this)
        playerStateStorage = PlayerStateStorage(this)
        libraryPreferences = LibraryPreferences(this)
        isShuffleEnabled = playerStateStorage.isShuffleEnabled()
        repeatMode = playerStateStorage.getRepeatMode()

        musicPlayer.onSongCompleted = {
            runOnUiThread {
                handleSongCompleted()
            }
        }

        requestAudioPermission()

        setContent {
            CdplayaTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                Scaffold(modifier = Modifier.fillMaxSize(),
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState)
                    }) { innerPadding ->
                    MusicScreen(
                        songs = songs,
                        permissionGranted = permissionGranted,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        isShuffleEnabled = isShuffleEnabled,
                        repeatMode = repeatMode,
                        currentPosition = currentPosition,
                        duration = duration,
                        queuedSongs = playbackQueue,
                        snackbarHostState = snackbarHostState,
                        modifier = Modifier.padding(innerPadding),
                        libraryFolders = libraryFolders,
                        selectedLibraryFolders = selectedLibraryFolders,
                        onSongClick = { song ->
                            playSelectedSong(song)
                        },
                        onPlayPauseClick = {
                            if (musicPlayer.isPlaying()) {
                                musicPlayer.pause()
                                isPlaying = false
                            } else {
                                musicPlayer.resume()
                                isPlaying = true
                            }
                        },
                        onPreviousClick = {
                            playPreviousSong()
                        },
                        onNextClick = {
                            playNextSong()
                        },
                        onSeekChange = { position ->
                            musicPlayer.seekTo(position)
                            currentPosition = position
                            savePlayerState()
                        },
                        onShuffleClick = {
                            toggleShuffle()
                        },
                        onRepeatClick = {
                            cycleRepeatMode()
                        },
                        onAddToQueueClick = { song ->
                            addSongToQueue(song)
                        },
                        onRemoveFromQueueClick = { index ->
                            removeSongFromQueue(index)
                        },
                        onMoveQueueItemUpClick = { index ->
                            moveQueuedSongUp(index)
                        },
                        onMoveQueueItemDownClick = { index ->
                            moveQueuedSongDown(index)
                        },
                        onUndoAddToQueueClick = { song ->
                            removeLastMatchingSongFromQueue(song)
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

        restorePlayerState()
    }

    private fun restorePlayerState() {
        val savedSongId = playerStateStorage.getCurrentSongId() ?: return

        val restoredSong = songs.firstOrNull { song ->
            song.id == savedSongId
        } ?: return

        currentSong = restoredSong
        currentPosition = playerStateStorage.getCurrentPosition()
        duration = restoredSong.duration.toInt()
        isPlaying = false

        isShuffleEnabled = playerStateStorage.isShuffleEnabled()
        repeatMode = playerStateStorage.getRepeatMode()

        playbackQueue.clear()
        playbackQueue.addAll(
            playerStateStorage.getQueueSongIds().mapNotNull { savedId ->
                songs.firstOrNull { song -> song.id == savedId }
            }
        )

        previousSongHistory.clear()
        previousSongHistory.addAll(
            playerStateStorage.getPreviousSongIds().mapNotNull { savedId ->
                songs.firstOrNull { song -> song.id == savedId }
            }
        )

        nextSongHistory.clear()
        nextSongHistory.addAll(
            playerStateStorage.getNextSongIds().mapNotNull { savedId ->
                songs.firstOrNull { song -> song.id == savedId }
            }
        )

        musicPlayer.playSong(restoredSong, shouldStart = false)
        musicPlayer.seekTo(currentPosition)

        startProgressUpdates()
    }

    private fun savePlayerState() {
        playerStateStorage.saveState(
            currentSongId = currentSong?.id,
            currentPosition = musicPlayer.getCurrentPosition(),
            isShuffleEnabled = isShuffleEnabled,
            repeatMode = repeatMode,
            previousSongIds = previousSongHistory.map { song -> song.id },
            nextSongIds = nextSongHistory.map { song -> song.id },
            queueSongIds = playbackQueue.map { song -> song.id }
        )
    }

    override fun onPause() {
        super.onPause()
        savePlayerState()
    }

    private fun playSelectedSong(
        song: Song,
        addCurrentToHistory: Boolean = true,
        clearForwardHistory: Boolean = true
    ) {
        val previousSong = currentSong

        if (addCurrentToHistory && previousSong != null && previousSong.id != song.id) {
            previousSongHistory.add(previousSong)
        }

        if (clearForwardHistory) {
            nextSongHistory.clear()
        }

        musicPlayer.playSong(song)
        currentSong = song
        isPlaying = true
        currentPosition = 0
        duration = musicPlayer.getDuration()
        startProgressUpdates()
    }

    private fun playNextSong() {
        if (songs.isEmpty()) {
            return
        }

        if (repeatMode == RepeatMode.ONE) {
            currentSong?.let { song ->
                playSelectedSong(
                    song = song,
                    addCurrentToHistory = false,
                    clearForwardHistory = false
                )
            }
            return
        }

        if (playNextQueuedSong()) {
            return
        }

        val nextSong = if (isShuffleEnabled) {
            if (nextSongHistory.isNotEmpty()) {
                nextSongHistory.removeAt(nextSongHistory.lastIndex)
            } else {
                getRandomSongExceptCurrent()
            }
        } else {
            val currentIndex = songs.indexOfFirst { it.id == currentSong?.id }

            val nextIndex = if (currentIndex == -1 || currentIndex == songs.lastIndex) {
                0
            } else {
                currentIndex + 1
            }

            songs[nextIndex]
        }

        playSelectedSong(nextSong)
    }

    private fun playPreviousSong() {
        if (songs.isEmpty()) {
            return
        }

        val previousSong = if (isShuffleEnabled && previousSongHistory.isNotEmpty()) {
            currentSong?.let { song ->
                nextSongHistory.add(song)
            }

            previousSongHistory.removeAt(previousSongHistory.lastIndex)
        } else {
            val currentIndex = songs.indexOfFirst { it.id == currentSong?.id }

            val previousIndex = if (currentIndex <= 0) {
                songs.lastIndex
            } else {
                currentIndex - 1
            }

            songs[previousIndex]
        }

        playSelectedSong(
            song = previousSong,
            addCurrentToHistory = false,
            clearForwardHistory = false
        )
    }

    private fun startProgressUpdates() {
        progressHandler.removeCallbacks(progressRunnable)
        progressHandler.post(progressRunnable)
    }

    private fun handleSongCompleted() {
        when (repeatMode) {
            RepeatMode.ONE -> {
                currentSong?.let { song ->
                    playSelectedSong(
                        song = song,
                        addCurrentToHistory = false,
                        clearForwardHistory = false
                    )
                }
            }

            RepeatMode.ALL -> {
                playNextSong()
            }

            RepeatMode.OFF -> {
                if (isShuffleEnabled) {
                    playNextSong()
                    return
                }

                val currentIndex = songs.indexOfFirst { it.id == currentSong?.id }

                if (currentIndex == songs.lastIndex) {
                    isPlaying = false
                    currentPosition = duration
                } else {
                    playNextSong()
                }
            }
        }
    }

    private fun getRandomSongExceptCurrent(): Song {
        val availableSongs = songs.filter { it.id != currentSong?.id }

        return if (availableSongs.isNotEmpty()) {
            availableSongs[Random.nextInt(availableSongs.size)]
        } else {
            songs.first()
        }
    }

    private fun toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled
        previousSongHistory.clear()
        nextSongHistory.clear()
        savePlayerState()
    }

    private fun cycleRepeatMode() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        savePlayerState()
    }

    private fun addSongToQueue(song: Song) {
        playbackQueue.add(song)
        savePlayerState()
    }

    private fun removeSongFromQueue(index: Int) {
        if (index in playbackQueue.indices) {
            playbackQueue.removeAt(index)
            savePlayerState()
        }
    }

    private fun moveQueuedSongUp(index: Int) {
        if (index > 0 && index in playbackQueue.indices) {
            val song = playbackQueue.removeAt(index)
            playbackQueue.add(index - 1, song)
            savePlayerState()
        }
    }

    private fun moveQueuedSongDown(index: Int) {
        if (index >= 0 && index < playbackQueue.lastIndex) {
            val song = playbackQueue.removeAt(index)
            playbackQueue.add(index + 1, song)
            savePlayerState()
        }
    }

    private fun playNextQueuedSong(): Boolean {
        if (playbackQueue.isEmpty()) {
            return false
        }

        val nextQueuedSong = playbackQueue.removeAt(0)
        playSelectedSong(nextQueuedSong)
        savePlayerState()

        return true
    }

    private fun removeLastMatchingSongFromQueue(song: Song) {
        for (index in playbackQueue.lastIndex downTo 0) {
            if (playbackQueue[index].id == song.id) {
                playbackQueue.removeAt(index)
                savePlayerState()
                return
            }
        }
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
        selectedLibraryFolders = libraryFolders.map { folder -> folder.path }.toSet()

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

        if (currentSong != null && songs.none { song -> song.id == currentSong?.id }) {
            musicPlayer.stop()
            currentSong = null
            isPlaying = false
            currentPosition = 0
            duration = 0
        }

        playbackQueue.removeAll { queuedSong ->
            songs.none { song -> song.id == queuedSong.id }
        }

        previousSongHistory.removeAll { historySong ->
            songs.none { song -> song.id == historySong.id }
        }

        nextSongHistory.removeAll { historySong ->
            songs.none { song -> song.id == historySong.id }
        }

        savePlayerState()
    }

    override fun onDestroy() {
        savePlayerState()
        super.onDestroy()
        progressHandler.removeCallbacks(progressRunnable)
        musicPlayer.stop()
    }


}
