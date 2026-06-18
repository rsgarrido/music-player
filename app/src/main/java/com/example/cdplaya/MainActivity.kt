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
    private var upcomingSongs by mutableStateOf<List<Song>>(emptyList())
    private var currentPosition by mutableStateOf(0)
    private var duration by mutableStateOf(0)
    private val progressHandler = Handler(Looper.getMainLooper())
    private val libraryFolders = mutableStateListOf<LibraryFolder>()
    private var selectedLibraryFolders by mutableStateOf<Set<String>>(emptySet())
    private var isPlayerConnected = false
    private var playbackContextSongs: List<Song> = emptyList()

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

        playerStateStorage = PlayerStateStorage(this)
        libraryPreferences = LibraryPreferences(this)
        isShuffleEnabled = playerStateStorage.isShuffleEnabled()
        repeatMode = playerStateStorage.getRepeatMode()

        musicPlayer = MusicPlayer(this)

        musicPlayer.connect {
            isPlayerConnected = true
            musicPlayer.setShuffleEnabled(isShuffleEnabled)
            musicPlayer.setRepeatMode(repeatMode)

            if (songs.isNotEmpty()) {
                restorePlayerState()
            }
        }

        musicPlayer.onSongCompleted = {
            runOnUiThread {
                handleSongCompleted()
            }
        }

        musicPlayer.onPlaybackStateChanged = { playerIsPlaying ->
            runOnUiThread {
                isPlaying = playerIsPlaying
            }
        }

        musicPlayer.onCurrentSongChanged = { songId ->
            runOnUiThread {
                handleServiceSongChanged(songId)
            }
        }

        requestAudioPermission()

        setContent {
            CdplayaTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { innerPadding ->
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
                        upcomingSongs = getComingUpSongsForDisplay(),
                        snackbarHostState = snackbarHostState,
                        modifier = Modifier.padding(innerPadding),
                        libraryFolders = libraryFolders,
                        selectedLibraryFolders = selectedLibraryFolders,
                        onSongClick = { song, playbackContext ->
                            playSelectedSong(song = song, playbackContext = playbackContext)
                        },
                        onPlaySongsClick = { playbackContext, shuffle ->
                            playSongsFromContext(
                                playbackContext = playbackContext,
                                shuffle = shuffle
                            )
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
                            musicPlayer.skipToPrevious()
                        },
                        onNextClick = {
                            musicPlayer.skipToNext()
                        },
                        onSeekChange = { position ->
                            musicPlayer.seekTo(position)
                            currentPosition = position
                            savePlayerState()
                        },
                        onShuffleClick = { toggleShuffle() },
                        onRepeatClick = { cycleRepeatMode() },
                        onAddToQueueClick = { song -> addSongToQueue(song) },
                        onRemoveFromQueueClick = { index -> removeSongFromQueue(index) },
                        onMoveQueueItemUpClick = { index -> moveQueuedSongUp(index) },
                        onMoveQueueItemDownClick = { index -> moveQueuedSongDown(index) },
                        onClearQueueClick = { clearQueue() },
                        onUndoAddToQueueClick = { song -> removeLastMatchingSongFromQueue(song) },
                        onLibraryFolderToggle = { folderPath -> toggleLibraryFolder(folderPath) },
                        onSelectAllLibraryFolders = { selectAllLibraryFolders() },
                        onClearSelectedLibraryFolders = { clearSelectedLibraryFolders() }
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

        if (isPlayerConnected) {
            restorePlayerState()
        }
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

        musicPlayer.playSong(
            song = restoredSong,
            shouldStart = false,
            startPosition = currentPosition,
            playlist = buildPlaybackPlaylist(restoredSong)
        )

        musicPlayer.setShuffleEnabled(isShuffleEnabled)
        musicPlayer.setRepeatMode(repeatMode)

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

    private fun playSongsFromContext(
        playbackContext: List<Song>,
        shuffle: Boolean
    ) {
        if (playbackContext.isEmpty()) {
            return
        }

        isShuffleEnabled = shuffle
        previousSongHistory.clear()
        nextSongHistory.clear()

        val songToPlay = if (shuffle && playbackContext.size > 1) {
            playbackContext[Random.nextInt(playbackContext.size)]
        } else {
            playbackContext.first()
        }

        playSelectedSong(
            song = songToPlay,
            playbackContext = playbackContext
        )
    }

    private fun playSelectedSong(
        song: Song,
        playbackContext: List<Song>? = null,
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

        playbackContextSongs = playbackContext ?: getPlaybackSourceSongs()

        musicPlayer.playSong(
            song = song,
            playlist = buildPlaybackPlaylist(song)
        )

        currentSong = song
        isPlaying = true
        currentPosition = 0
        duration = song.duration.toInt()

        musicPlayer.setShuffleEnabled(isShuffleEnabled)
        musicPlayer.setRepeatMode(repeatMode)

        startProgressUpdates()
        savePlayerState()
    }

    private fun playNextSong() {
        val playbackSourceSongs = getPlaybackSourceSongs()

        if (playbackSourceSongs.isEmpty()) {
            return
        }

        if (repeatMode == RepeatMode.ONE) {
            currentSong?.let { song ->
                playSelectedSong(
                    song = song,
                    playbackContext = playbackSourceSongs,
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
                getRandomSongExceptCurrent(playbackSourceSongs)
            }
        } else {
            val currentIndex = playbackSourceSongs.indexOfFirst { song ->
                song.id == currentSong?.id
            }

            val nextIndex = if (currentIndex == -1 || currentIndex == playbackSourceSongs.lastIndex) {
                0
            } else {
                currentIndex + 1
            }

            playbackSourceSongs[nextIndex]
        }

        playSelectedSong(
            song = nextSong,
            playbackContext = playbackSourceSongs
        )
    }

    private fun playPreviousSong() {
        val playbackSourceSongs = getPlaybackSourceSongs()

        if (playbackSourceSongs.isEmpty()) {
            return
        }

        val previousSong = if (isShuffleEnabled && previousSongHistory.isNotEmpty()) {
            currentSong?.let { song ->
                nextSongHistory.add(song)
            }

            previousSongHistory.removeAt(previousSongHistory.lastIndex)
        } else {
            val currentIndex = playbackSourceSongs.indexOfFirst { song ->
                song.id == currentSong?.id
            }

            val previousIndex = if (currentIndex <= 0) {
                playbackSourceSongs.lastIndex
            } else {
                currentIndex - 1
            }

            playbackSourceSongs[previousIndex]
        }

        playSelectedSong(
            song = previousSong,
            playbackContext = playbackSourceSongs,
            addCurrentToHistory = false,
            clearForwardHistory = false
        )
    }

    private fun startProgressUpdates() {
        progressHandler.removeCallbacks(progressRunnable)
        progressHandler.post(progressRunnable)
    }

    private fun handleSongCompleted() {
        val playbackSourceSongs = getPlaybackSourceSongs()

        when (repeatMode) {
            RepeatMode.ONE -> {
                currentSong?.let { song ->
                    playSelectedSong(
                        song = song,
                        playbackContext = playbackSourceSongs,
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

                val currentIndex = playbackSourceSongs.indexOfFirst { song ->
                    song.id == currentSong?.id
                }

                if (currentIndex == playbackSourceSongs.lastIndex) {
                    isPlaying = false
                    currentPosition = duration
                } else {
                    playNextSong()
                }
            }
        }
    }

    private fun getRandomSongExceptCurrent(playbackSourceSongs: List<Song>): Song {
        val availableSongs = playbackSourceSongs.filter { song ->
            song.id != currentSong?.id
        }

        return if (availableSongs.isNotEmpty()) {
            availableSongs[Random.nextInt(availableSongs.size)]
        } else {
            playbackSourceSongs.first()
        }
    }

    private fun toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled
        previousSongHistory.clear()
        nextSongHistory.clear()
        syncServicePlaylistKeepingCurrent()
        savePlayerState()
    }

    private fun cycleRepeatMode() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        syncServicePlaylistKeepingCurrent()
        savePlayerState()
    }

    private fun addSongToQueue(song: Song) {
        playbackQueue.add(song)
        syncServicePlaylistKeepingCurrent()
        savePlayerState()
    }

    private fun removeSongFromQueue(index: Int) {
        if (index in playbackQueue.indices) {
            playbackQueue.removeAt(index)
            syncServicePlaylistKeepingCurrent()
            savePlayerState()
        }
    }

    private fun moveQueuedSongUp(index: Int) {
        if (index > 0 && index in playbackQueue.indices) {
            val song = playbackQueue.removeAt(index)
            playbackQueue.add(index - 1, song)
            syncServicePlaylistKeepingCurrent()
            savePlayerState()
        }
    }

    private fun moveQueuedSongDown(index: Int) {
        if (index >= 0 && index < playbackQueue.lastIndex) {
            val song = playbackQueue.removeAt(index)
            playbackQueue.add(index + 1, song)
            syncServicePlaylistKeepingCurrent()
            savePlayerState()
        }
    }

    private fun playNextQueuedSong(): Boolean {
        if (playbackQueue.isEmpty()) {
            return false
        }

        val playbackSourceSongs = getPlaybackSourceSongs()
        val nextQueuedSong = playbackQueue.removeAt(0)

        playSelectedSong(
            song = nextQueuedSong,
            playbackContext = playbackSourceSongs
        )

        savePlayerState()

        return true
    }

    private fun clearQueue() {
        playbackQueue.clear()
        syncServicePlaylistKeepingCurrent()
        savePlayerState()
    }

    private fun removeLastMatchingSongFromQueue(song: Song) {
        for (index in playbackQueue.lastIndex downTo 0) {
            if (playbackQueue[index].id == song.id) {
                playbackQueue.removeAt(index)
                syncServicePlaylistKeepingCurrent()
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

    private fun buildPlaybackPlaylist(startSong: Song): List<Song> {
        val refreshedUpcomingSongs = refreshUpcomingSongs(startSong)

        return listOf(startSong) + refreshedUpcomingSongs
    }

    private fun refreshUpcomingSongs(startSong: Song): List<Song> {
        upcomingSongs = buildUpcomingPlaylistAfterCurrent(startSong)
        return upcomingSongs
    }

    private fun buildUpcomingPlaylistAfterCurrent(startSong: Song): List<Song> {
        val queuedSongsAfterCurrent = playbackQueue.filter { queuedSong ->
            queuedSong.id != startSong.id
        }

        val excludedSongIds = mutableSetOf<Long>()
        excludedSongIds.add(startSong.id)
        excludedSongIds.addAll(queuedSongsAfterCurrent.map { song -> song.id })

        val playbackSourceSongs = getPlaybackSourceSongs()

        val startIndex = playbackSourceSongs.indexOfFirst { song ->
            song.id == startSong.id
        }

        val songsAfterCurrent = if (startIndex == -1) {
            playbackSourceSongs
        } else {
            playbackSourceSongs.drop(startIndex + 1) + playbackSourceSongs.take(startIndex)
        }

        val remainingContextSongs = songsAfterCurrent.filter { song ->
            song.id !in excludedSongIds
        }

        val orderedRemainingSongs = if (isShuffleEnabled) {
            remainingContextSongs.shuffled()
        } else {
            remainingContextSongs
        }

        return queuedSongsAfterCurrent + orderedRemainingSongs
    }

    private fun handleServiceSongChanged(songId: Long?) {
        val newSong = songs.firstOrNull { song ->
            song.id == songId
        } ?: return

        if (currentSong?.id == newSong.id) {
            musicPlayer.setRepeatMode(repeatMode)
            musicPlayer.setShuffleEnabled(isShuffleEnabled)
            return
        }

        currentSong = newSong
        currentPosition = musicPlayer.getCurrentPosition()
        duration = newSong.duration.toInt()
        isPlaying = musicPlayer.isPlaying()

        if (playbackQueue.firstOrNull()?.id == newSong.id) {
            playbackQueue.removeAt(0)
        }

        syncServicePlaylistKeepingCurrent()

        startProgressUpdates()
        savePlayerState()
    }

    private fun syncServicePlaylistKeepingCurrent() {
        val song = currentSong ?: return

        val refreshedUpcomingSongs = refreshUpcomingSongs(song)

        musicPlayer.updateUpcomingPlaylist(
            upcomingSongs = refreshedUpcomingSongs
        )

        musicPlayer.setShuffleEnabled(isShuffleEnabled)
        musicPlayer.setRepeatMode(repeatMode)
    }

    private fun getPlaybackSourceSongs(): List<Song> {
        return if (playbackContextSongs.isNotEmpty()) {
            playbackContextSongs
        } else {
            songs
        }
    }

    private fun getComingUpSongsForDisplay(): List<Song> {
        val queuedSongCount = playbackQueue.count { queuedSong ->
            queuedSong.id != currentSong?.id
        }

        return upcomingSongs.drop(queuedSongCount)
    }

    override fun onDestroy() {
        savePlayerState()
        super.onDestroy()
        progressHandler.removeCallbacks(progressRunnable)
        musicPlayer.release()
    }
}