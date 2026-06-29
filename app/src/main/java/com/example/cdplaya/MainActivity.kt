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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.cdplaya.data.local.AppDatabase
import com.example.cdplaya.data.local.DatabaseProvider
import com.example.cdplaya.data.FavoritesRepository
import com.example.cdplaya.data.favoriteKey
import com.example.cdplaya.data.LibraryFolder
import com.example.cdplaya.data.LibraryPreferences
import com.example.cdplaya.data.MusicRepository
import com.example.cdplaya.data.Song
import com.example.cdplaya.data.Playlist
import com.example.cdplaya.data.PlaylistSong
import com.example.cdplaya.data.PlaylistsRepository
import com.example.cdplaya.data.stableKey
import com.example.cdplaya.player.PlaybackController
import com.example.cdplaya.ui.MusicScreen
import com.example.cdplaya.ui.theme.CdplayaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var songs by mutableStateOf<List<Song>>(emptyList())
    private var permissionGranted by mutableStateOf(false)

    private lateinit var playbackController: PlaybackController
    private lateinit var libraryPreferences: LibraryPreferences
    private lateinit var appDatabase: AppDatabase
    private lateinit var favoritesRepository: FavoritesRepository
    private var favoriteSongKeys by mutableStateOf<Set<String>>(emptySet())
    private val libraryFolders = mutableStateListOf<LibraryFolder>()
    private var selectedLibraryFolders by mutableStateOf<Set<String>>(emptySet())

    private lateinit var playlistsRepository: PlaylistsRepository
    private var playlists by mutableStateOf<List<Playlist>>(emptyList())
    private var selectedPlaylistName by mutableStateOf("Playlist")
    private var selectedPlaylistSongs by mutableStateOf<List<PlaylistSong>>(emptyList())

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

        appDatabase = DatabaseProvider.getDatabase(this)
        Log.d("CDPlayaDatabase", "Room database initialized")

        favoritesRepository = FavoritesRepository(appDatabase.favoriteSongDao())
        loadFavoriteSongKeys()

        playlistsRepository = PlaylistsRepository(appDatabase.playlistDao())
        loadPlaylists()

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
                        playlists = playlists,
                        selectedPlaylistName = selectedPlaylistName,
                        selectedPlaylistSongs = selectedPlaylistSongs,
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
                            toggleLibraryFolder(folderPath)
                        },
                        onSelectAllLibraryFolders = {
                            selectAllLibraryFolders()
                        },
                        onClearSelectedLibraryFolders = {
                            clearSelectedLibraryFolders()
                        },
                        favoriteSongKeys = favoriteSongKeys,
                        onToggleFavoriteClick = { song ->
                            toggleFavorite(song)
                        },
                        onCreatePlaylistClick = { playlistName ->
                            createPlaylist(playlistName)
                        },
                        onDeletePlaylistClick = { playlist ->
                            deletePlaylist(playlist)
                        },
                        onPlaylistSelected = { playlist ->
                            loadSelectedPlaylist(playlist)
                        },
                        onAddSongToPlaylistClick = { playlist, song ->
                            addSongToPlaylist(playlist, song)
                        },
                        onRemovePlaylistSongClick = { playlistSong ->
                            removePlaylistSong(playlistSong)
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

    private fun loadFavoriteSongKeys() {
        lifecycleScope.launch {
            favoriteSongKeys = favoritesRepository.getFavoriteSongKeys()
        }
    }

    private fun toggleFavorite(song: Song) {
        val songKey = song.favoriteKey()
        val shouldFavorite = songKey !in favoriteSongKeys

        favoriteSongKeys = if (shouldFavorite) {
            favoriteSongKeys + songKey
        } else {
            favoriteSongKeys - songKey
        }

        lifecycleScope.launch {
            if (shouldFavorite) {
                favoritesRepository.addFavorite(song)
            } else {
                favoritesRepository.removeFavorite(song)
            }
        }
    }

    private fun loadPlaylists() {
        lifecycleScope.launch {
            playlists = playlistsRepository.getPlaylists()
        }
    }

    private fun createPlaylist(playlistName: String) {
        lifecycleScope.launch {
            playlistsRepository.createPlaylist(playlistName)
            loadPlaylists()
        }
    }

    private fun deletePlaylist(playlist: Playlist) {
        lifecycleScope.launch {
            playlistsRepository.deletePlaylist(playlist.playlistId)
            loadPlaylists()

            if (selectedPlaylistName == playlist.name) {
                selectedPlaylistName = "Playlist"
                selectedPlaylistSongs = emptyList()
            }
        }
    }

    private fun loadSelectedPlaylist(playlist: Playlist) {
        lifecycleScope.launch {
            selectedPlaylistName = playlist.name
            selectedPlaylistSongs = playlistsRepository.getPlaylistSongs(playlist.playlistId)
        }
    }

    private fun addSongToPlaylist(
        playlist: Playlist,
        song: Song
    ) {
        lifecycleScope.launch {
            playlistsRepository.addSongToPlaylist(
                playlistId = playlist.playlistId,
                song = song
            )

            loadPlaylists()
            selectedPlaylistSongs = playlistsRepository.getPlaylistSongs(playlist.playlistId)
        }
    }

    private fun removePlaylistSong(playlistSong: PlaylistSong) {
        lifecycleScope.launch {
            playlistsRepository.removePlaylistSong(
                playlistId = playlistSong.playlistId,
                playlistSongId = playlistSong.playlistSongId
            )

            loadPlaylists()
            selectedPlaylistSongs = playlistsRepository.getPlaylistSongs(playlistSong.playlistId)
        }
    }

    override fun onDestroy() {
        playbackController.release()
        super.onDestroy()
    }
}