package com.example.cdplaya.controller

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.cdplaya.data.EditableSongTags
import com.example.cdplaya.data.FavoritesRepository
import com.example.cdplaya.data.LibraryFolder
import com.example.cdplaya.data.LibraryPreferences
import com.example.cdplaya.data.MusicRepository
import com.example.cdplaya.data.Playlist
import com.example.cdplaya.data.PlaylistSong
import com.example.cdplaya.data.PlaylistsRepository
import com.example.cdplaya.data.Song
import com.example.cdplaya.data.favoriteKey
import com.example.cdplaya.data.local.AppDatabase
import com.example.cdplaya.player.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LibraryController(
    context: Context,
    appDatabase: AppDatabase,
    private val playbackController: PlaybackController,
    private val coroutineScope: CoroutineScope
) {
    private val applicationContext = context.applicationContext

    private val libraryPreferences = LibraryPreferences(applicationContext)
    private val favoritesRepository = FavoritesRepository(appDatabase.favoriteSongDao())
    private val playlistsRepository = PlaylistsRepository(appDatabase.playlistDao())

    var songs by mutableStateOf<List<Song>>(emptyList())
        private set

    val libraryFolders = mutableStateListOf<LibraryFolder>()

    var selectedLibraryFolders by mutableStateOf<Set<String>>(emptySet())
        private set

    var favoriteSongKeys by mutableStateOf<Set<String>>(emptySet())
        private set

    var playlists by mutableStateOf<List<Playlist>>(emptyList())
        private set

    var selectedPlaylistName by mutableStateOf("Playlist")
        private set

    var selectedPlaylistSongs by mutableStateOf<List<PlaylistSong>>(emptyList())
        private set

    fun loadSavedUserData() {
        loadFavoriteSongKeys()
        loadPlaylists()
    }

    fun loadSongs() {
        coroutineScope.launch {
            val savedSelectedFolders = libraryPreferences.getSelectedFolders()

            val libraryData = withContext(Dispatchers.IO) {
                val repository = MusicRepository(applicationContext)
                repository.getLibraryData(savedSelectedFolders)
            }

            selectedLibraryFolders = savedSelectedFolders

            libraryFolders.clear()
            libraryFolders.addAll(libraryData.libraryFolders)

            songs = libraryData.songs
            playbackController.setLibrarySongs(songs)
        }
    }

    fun toggleLibraryFolder(folderPath: String) {
        selectedLibraryFolders = if (folderPath in selectedLibraryFolders) {
            selectedLibraryFolders - folderPath
        } else {
            selectedLibraryFolders + folderPath
        }

        libraryPreferences.saveSelectedFolders(selectedLibraryFolders)
        reloadSongsAfterFolderChange()
    }

    fun selectAllLibraryFolders() {
        selectedLibraryFolders = libraryFolders.map { folder ->
            folder.path
        }.toSet()

        libraryPreferences.saveSelectedFolders(selectedLibraryFolders)
        reloadSongsAfterFolderChange()
    }

    fun clearSelectedLibraryFolders() {
        selectedLibraryFolders = emptySet()

        libraryPreferences.saveSelectedFolders(selectedLibraryFolders)
        reloadSongsAfterFolderChange()
    }

    fun refreshSongsAfterTagEdit(
        originalSong: Song,
        editedTags: EditableSongTags
    ) {
        coroutineScope.launch {
            favoritesRepository.updateSongReferenceAfterTagEdit(
                originalSong = originalSong,
                editedTags = editedTags
            )

            playlistsRepository.updateSongReferencesAfterTagEdit(
                originalSong = originalSong,
                editedTags = editedTags
            )

            val updatedFavoriteSongKeys = favoritesRepository.getFavoriteSongKeys()
            val updatedPlaylists = playlistsRepository.getPlaylists()

            val selectedPlaylistId = selectedPlaylistSongs.firstOrNull()?.playlistId

            val updatedSelectedPlaylistSongs = selectedPlaylistId?.let { playlistId ->
                playlistsRepository.getPlaylistSongs(playlistId)
            }

            val libraryData = withContext(Dispatchers.IO) {
                val repository = MusicRepository(applicationContext)
                repository.getLibraryData(selectedLibraryFolders)
            }

            favoriteSongKeys = updatedFavoriteSongKeys
            playlists = updatedPlaylists

            if (updatedSelectedPlaylistSongs != null) {
                selectedPlaylistSongs = updatedSelectedPlaylistSongs
            }

            libraryFolders.clear()
            libraryFolders.addAll(libraryData.libraryFolders)

            songs = libraryData.songs
            playbackController.handleLibrarySongsChanged(songs)
        }
    }

    fun toggleFavorite(song: Song) {
        val songKey = song.favoriteKey()
        val shouldFavorite = songKey !in favoriteSongKeys

        favoriteSongKeys = if (shouldFavorite) {
            favoriteSongKeys + songKey
        } else {
            favoriteSongKeys - songKey
        }

        coroutineScope.launch {
            if (shouldFavorite) {
                favoritesRepository.addFavorite(song)
            } else {
                favoritesRepository.removeFavorite(song)
            }
        }
    }

    fun createPlaylist(playlistName: String) {
        coroutineScope.launch {
            val wasCreated = playlistsRepository.createPlaylist(playlistName)

            if (wasCreated) {
                loadPlaylists()
            }
        }
    }

    fun renamePlaylist(
        playlist: Playlist,
        newName: String
    ) {
        coroutineScope.launch {
            val trimmedName = newName.trim()

            val wasRenamed = playlistsRepository.renamePlaylist(
                playlistId = playlist.playlistId,
                newName = trimmedName
            )

            if (wasRenamed) {
                loadPlaylists()

                val renamedPlaylistWasSelected =
                    selectedPlaylistSongs.any { playlistSong ->
                        playlistSong.playlistId == playlist.playlistId
                    }

                if (renamedPlaylistWasSelected) {
                    selectedPlaylistName = trimmedName
                }
            }
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        coroutineScope.launch {
            playlistsRepository.deletePlaylist(playlist.playlistId)
            loadPlaylists()

            val deletedPlaylistWasSelected =
                selectedPlaylistName == playlist.name ||
                        selectedPlaylistSongs.any { playlistSong ->
                            playlistSong.playlistId == playlist.playlistId
                        }

            if (deletedPlaylistWasSelected) {
                selectedPlaylistName = "Playlist"
                selectedPlaylistSongs = emptyList()
            }
        }
    }

    fun loadSelectedPlaylist(playlist: Playlist) {
        coroutineScope.launch {
            selectedPlaylistName = playlist.name
            selectedPlaylistSongs = playlistsRepository.getPlaylistSongs(playlist.playlistId)
        }
    }

    fun addSongToPlaylist(
        playlist: Playlist,
        song: Song
    ) {
        addSongsToPlaylist(
            playlist = playlist,
            songs = listOf(song)
        )
    }

    fun addSongsToPlaylist(
        playlist: Playlist,
        songs: List<Song>
    ) {
        if (songs.isEmpty()) {
            return
        }

        coroutineScope.launch {
            playlistsRepository.addSongsToPlaylist(
                playlistId = playlist.playlistId,
                songs = songs
            )

            loadPlaylists()

            val addedToSelectedPlaylist =
                selectedPlaylistSongs.any { playlistSong ->
                    playlistSong.playlistId == playlist.playlistId
                }

            if (addedToSelectedPlaylist) {
                selectedPlaylistSongs = playlistsRepository.getPlaylistSongs(playlist.playlistId)
            }
        }
    }

    fun removePlaylistSong(playlistSong: PlaylistSong) {
        coroutineScope.launch {
            playlistsRepository.removePlaylistSong(
                playlistId = playlistSong.playlistId,
                playlistSongId = playlistSong.playlistSongId
            )

            loadPlaylists()
            selectedPlaylistSongs = playlistsRepository.getPlaylistSongs(playlistSong.playlistId)
        }
    }

    fun movePlaylistSongUp(playlistSong: PlaylistSong) {
        coroutineScope.launch {
            playlistsRepository.movePlaylistSongUp(
                playlistId = playlistSong.playlistId,
                playlistSongId = playlistSong.playlistSongId
            )

            loadPlaylists()
            selectedPlaylistSongs = playlistsRepository.getPlaylistSongs(
                playlistSong.playlistId
            )
        }
    }

    fun movePlaylistSongDown(playlistSong: PlaylistSong) {
        coroutineScope.launch {
            playlistsRepository.movePlaylistSongDown(
                playlistId = playlistSong.playlistId,
                playlistSongId = playlistSong.playlistSongId
            )

            loadPlaylists()
            selectedPlaylistSongs = playlistsRepository.getPlaylistSongs(
                playlistSong.playlistId
            )
        }
    }

    private fun reloadSongsAfterFolderChange() {
        coroutineScope.launch {
            val libraryData = withContext(Dispatchers.IO) {
                val repository = MusicRepository(applicationContext)
                repository.getLibraryData(selectedLibraryFolders)
            }

            libraryFolders.clear()
            libraryFolders.addAll(libraryData.libraryFolders)

            songs = libraryData.songs
            playbackController.handleLibrarySongsChanged(songs)
        }
    }

    private fun loadFavoriteSongKeys() {
        coroutineScope.launch {
            favoriteSongKeys = favoritesRepository.getFavoriteSongKeys()
        }
    }

    private fun loadPlaylists() {
        coroutineScope.launch {
            playlists = playlistsRepository.getPlaylists()
        }
    }
}