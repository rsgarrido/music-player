package com.example.cdplaya.controller

import android.content.Context
import android.net.Uri
import android.util.Log
import android.os.SystemClock
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.cdplaya.data.EditableSongTags
import com.example.cdplaya.data.FavoritesRepository
import com.example.cdplaya.data.LibraryFolder
import com.example.cdplaya.data.LibraryPreferences
import com.example.cdplaya.data.ListeningHistoryEntry
import com.example.cdplaya.data.ListeningHistoryRepository
import com.example.cdplaya.data.LibraryCacheRepository
import com.example.cdplaya.data.MusicLibraryData
import com.example.cdplaya.data.stableKey
import com.example.cdplaya.data.MusicRepository
import com.example.cdplaya.data.Playlist
import com.example.cdplaya.data.PlaylistSong
import com.example.cdplaya.data.PlaylistsRepository
import com.example.cdplaya.data.Song
import com.example.cdplaya.data.SongReferenceResolution
import com.example.cdplaya.data.SongReferenceResolver
import com.example.cdplaya.data.membershipKey
import com.example.cdplaya.data.sortSongsByDateAddedDescending
import com.example.cdplaya.data.local.AppDatabase
import com.example.cdplaya.data.playlistfile.M3uExportResult
import com.example.cdplaya.data.playlistfile.PlaylistFileRepository
import com.example.cdplaya.data.playlistfile.PlaylistImportResult
import com.example.cdplaya.data.playlistfile.PreparedPlaylistExport
import com.example.cdplaya.data.playlistfile.defaultImportedPlaylistName
import com.example.cdplaya.player.PlaybackController
import com.example.cdplaya.player.PlaybackLibraryBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job

class LibraryController(
    context: Context,
    appDatabase: AppDatabase,
    private val playbackController: PlaybackController,
    private val coroutineScope: CoroutineScope
) {
    private val applicationContext = context.applicationContext

    internal val libraryPreferences = LibraryPreferences(applicationContext)
    internal val favoritesRepository = FavoritesRepository(appDatabase.favoriteSongDao())
    internal val playlistsRepository = PlaylistsRepository(appDatabase.playlistDao())
    internal val listeningHistoryRepository = ListeningHistoryRepository(
        appDatabase.songPlayStatsDao()
    )
    private val libraryCacheRepository = LibraryCacheRepository(appDatabase.cachedSongDao())
    private val playlistFileRepository = PlaylistFileRepository(applicationContext)
    private var refreshJob: Job? = null
    private var reconciliationJob: Job? = null

    var lastLibraryRefreshResult: com.example.cdplaya.data.LibraryRefreshResult? = null
        private set

    var unresolvedFavoriteCount: Int = 0
        private set
    var unresolvedPlaylistRowCount: Int = 0
        private set
    var unresolvedListeningHistoryCount: Int = 0
        private set

    var songs by mutableStateOf<List<Song>>(emptyList())
        private set

    val libraryFolders = mutableStateListOf<LibraryFolder>()

    var selectedLibraryFolders by mutableStateOf<Set<String>>(emptySet())
        private set

    var favoriteMembershipKeys by mutableStateOf<Set<String>>(emptySet())
        private set

    var playlists by mutableStateOf<List<Playlist>>(emptyList())
        private set

    var selectedPlaylistName by mutableStateOf("Playlist")
        private set

    var selectedPlaylistSongs by mutableStateOf<List<PlaylistSong>>(emptyList())
        private set

    var recentlyPlayedSongs by mutableStateOf<List<Song>>(emptyList())
        private set

    var mostPlayedSongs by mutableStateOf<List<Song>>(emptyList())
        private set

    var recentlyAddedSongs by mutableStateOf<List<Song>>(emptyList())
        private set

    fun loadSavedUserData() {
        loadFavoriteMembershipKeys()
        loadPlaylists()
    }

    fun loadSongs() {
        refreshJob?.cancel()
        refreshJob = coroutineScope.launch {
            val savedSelectedFolders = libraryPreferences.getSelectedFolders()
            selectedLibraryFolders = savedSelectedFolders

            val hasCachedSongs = withContext(Dispatchers.IO) {
                libraryCacheRepository.hasCachedSongs()
            }

            if (hasCachedSongs) {
                val cachedLibraryData = withContext(Dispatchers.IO) {
                    libraryCacheRepository.getCachedLibraryData(savedSelectedFolders)
                }

                publishLibraryData(
                    libraryData = cachedLibraryData,
                    reconcilePlayback = false
                )
            }

            val freshLibraryData = withContext(Dispatchers.IO) {
                scanFreshLibraryAndUpdateCache(savedSelectedFolders)
            }

            publishLibraryData(
                libraryData = freshLibraryData,
                reconcilePlayback = hasCachedSongs
            )
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
        refreshJob?.cancel()
        refreshJob = coroutineScope.launch {
            favoritesRepository.updateSongReferenceAfterTagEdit(
                originalSong = originalSong,
                editedTags = editedTags
            )

            playlistsRepository.updateSongReferencesAfterTagEdit(
                originalSong = originalSong,
                editedTags = editedTags
            )

            listeningHistoryRepository.updateSongReferenceAfterTagEdit(
                originalSong = originalSong,
                editedTags = editedTags
            )

            val updatedFavoriteMembershipKeys = favoritesRepository.getFavoriteMembershipKeys()
            val updatedPlaylists = playlistsRepository.getPlaylists()

            val selectedPlaylistId = selectedPlaylistSongs.firstOrNull()?.playlistId

            val updatedSelectedPlaylistSongs = selectedPlaylistId?.let { playlistId ->
                playlistsRepository.getPlaylistSongs(playlistId)
            }

            val libraryData = withContext(Dispatchers.IO) {
                scanFreshLibraryAndUpdateCache(selectedLibraryFolders)
            }

            favoriteMembershipKeys = updatedFavoriteMembershipKeys
            playlists = updatedPlaylists

            if (updatedSelectedPlaylistSongs != null) {
                selectedPlaylistSongs = updatedSelectedPlaylistSongs
            }
            publishLibraryData(libraryData, reconcilePlayback = true)
        }
    }

    fun toggleFavorite(song: Song) {
        val membershipKey = song.membershipKey()
        val shouldFavorite = membershipKey !in favoriteMembershipKeys

        favoriteMembershipKeys = if (shouldFavorite) {
            favoriteMembershipKeys + membershipKey
        } else {
            favoriteMembershipKeys - membershipKey
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

    fun preparePlaylistExport(
        playlist: Playlist,
        onPrepared: (Result<PreparedPlaylistExport>) -> Unit
    ) {
        coroutineScope.launch {
            val result = runCatching {
                val playlistSongs = playlistsRepository.getPlaylistSongs(playlist.playlistId)
                val exportableSongs = playlistSongs.mapNotNull { playlistSong ->
                    (SongReferenceResolver.resolve(playlistSong.reference, songs)
                        as? SongReferenceResolution.Resolved)?.song
                }

                PreparedPlaylistExport(
                    playlistName = playlist.name,
                    songs = exportableSongs,
                    unavailableSongCount = playlistSongs.size - exportableSongs.size
                )
            }

            onPrepared(result)
        }
    }

    fun exportM3uPlaylist(
        uri: Uri,
        songs: List<Song>,
        onExported: (Result<M3uExportResult>) -> Unit
    ) {
        coroutineScope.launch {
            val result = runCatching {
                playlistFileRepository.exportM3uPlaylist(
                    uri = uri,
                    songs = songs
                )
            }

            onExported(result)
        }
    }

    fun importM3uPlaylist(
        uri: Uri,
        onImported: (Result<PlaylistImportResult>) -> Unit
    ) {
        coroutineScope.launch {
            val result = runCatching {
                val fileImportResult = playlistFileRepository.importM3uPlaylist(
                    uri = uri,
                    librarySongs = songs
                )

                if (fileImportResult.matchedSongs.isEmpty()) {
                    PlaylistImportResult(
                        playlistName = null,
                        importedSongCount = 0,
                        unmatchedEntryCount = fileImportResult.unmatchedEntryCount
                    )
                } else {
                    val importedPlaylist = playlistsRepository.createPlaylistWithUniqueName(
                        preferredName = defaultImportedPlaylistName(
                            fileImportResult.sourceDisplayName
                        ),
                        songs = fileImportResult.matchedSongs
                    )

                    playlists = playlistsRepository.getPlaylists()

                    PlaylistImportResult(
                        playlistName = importedPlaylist.name,
                        importedSongCount = fileImportResult.matchedSongCount,
                        unmatchedEntryCount = fileImportResult.unmatchedEntryCount
                    )
                }
            }

            onImported(result)
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

    fun refreshListeningHistory() {
        coroutineScope.launch {
            val historyData = withContext(Dispatchers.IO) {
                val recentlyPlayed = listeningHistoryRepository.getRecentlyPlayed()
                val mostPlayed = listeningHistoryRepository.getMostPlayed()

                recentlyPlayed to mostPlayed
            }

            recentlyPlayedSongs = mapListeningHistoryEntriesToSongs(historyData.first)
            mostPlayedSongs = mapListeningHistoryEntriesToSongs(historyData.second)
        }
    }

    internal suspend fun refreshAfterBackupRestore() {
        val restoredData = withContext(Dispatchers.IO) {
            BackupRestoredUserData(
                selectedLibraryFolders = libraryPreferences.getSelectedFolders(),
                favoriteMembershipKeys = favoritesRepository.getFavoriteMembershipKeys(),
                playlists = playlistsRepository.getPlaylists(),
                recentlyPlayed = listeningHistoryRepository.getRecentlyPlayed(),
                mostPlayed = listeningHistoryRepository.getMostPlayed()
            )
        }
        val resolvedSelectedFolders = resolveRestoredFolderSelections(
            restoredData.selectedLibraryFolders
        )
        if (resolvedSelectedFolders != restoredData.selectedLibraryFolders) {
            libraryPreferences.saveSelectedFolders(resolvedSelectedFolders)
        }
        val folderSelectionChanged = selectedLibraryFolders != resolvedSelectedFolders

        selectedLibraryFolders = resolvedSelectedFolders
        favoriteMembershipKeys = restoredData.favoriteMembershipKeys
        playlists = restoredData.playlists
        selectedPlaylistName = "Playlist"
        selectedPlaylistSongs = emptyList()
        recentlyPlayedSongs = mapListeningHistoryEntriesToSongs(restoredData.recentlyPlayed)
        mostPlayedSongs = mapListeningHistoryEntriesToSongs(restoredData.mostPlayed)

        if (folderSelectionChanged) {
            reloadSongsAfterFolderChange()
        } else {
            reconcileUserSongReferences(songs)
        }
    }

    private fun reloadSongsAfterFolderChange() {
        refreshJob?.cancel()
        refreshJob = coroutineScope.launch {
            val hasCachedSongs = withContext(Dispatchers.IO) {
                libraryCacheRepository.hasCachedSongs()
            }

            if (hasCachedSongs) {
                val cachedLibraryData = withContext(Dispatchers.IO) {
                    libraryCacheRepository.getCachedLibraryData(selectedLibraryFolders)
                }

                publishLibraryData(
                    libraryData = cachedLibraryData,
                    reconcilePlayback = true
                )
            }

            val freshLibraryData = withContext(Dispatchers.IO) {
                scanFreshLibraryAndUpdateCache(selectedLibraryFolders)
            }

            publishLibraryData(
                libraryData = freshLibraryData,
                reconcilePlayback = true
            )
        }
    }

    private fun publishLibraryData(
        libraryData: MusicLibraryData,
        reconcilePlayback: Boolean
    ) {
        libraryFolders.clear()
        libraryFolders.addAll(libraryData.libraryFolders)

        songs = libraryData.songs
        recentlyAddedSongs = sortSongsByDateAddedDescending(songs)
        PlaybackLibraryBridge.updateSongs(songs)
        reconcileUserSongReferences(songs)

        if (reconcilePlayback) {
            playbackController.handleLibrarySongsChanged(songs)
        } else {
            playbackController.setLibrarySongs(songs)
        }
    }

    private suspend fun scanFreshLibraryAndUpdateCache(
        selectedFolders: Set<String>
    ): MusicLibraryData {
        val repository = MusicRepository(applicationContext)
        val cachedSongs = libraryCacheRepository.getAllCachedSongs()
        val startedAt = SystemClock.elapsedRealtime()
        val refreshResult = repository.refreshLibrary(cachedSongs)
        lastLibraryRefreshResult = refreshResult

        if (applicationContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            Log.d(
                "LibraryRefresh",
                "elapsedMs=${SystemClock.elapsedRealtime() - startedAt} " +
                    "reused=${refreshResult.reusedCount} added=${refreshResult.addedCount} " +
                    "updated=${refreshResult.updatedCount} moved=${refreshResult.movedCount} " +
                    "removed=${refreshResult.removedCount} enriched=${refreshResult.enrichmentCount} " +
                    "complete=${refreshResult.successfulCompleteScan}"
            )
        }

        if (refreshResult.successfulCompleteScan) {
            libraryCacheRepository.replaceCachedSongs(refreshResult.songs)
        }
        return com.example.cdplaya.data.buildMusicLibraryData(
            allSongs = refreshResult.songs,
            selectedFolders = selectedFolders
        )
    }

    private fun loadFavoriteMembershipKeys() {
        coroutineScope.launch {
            favoriteMembershipKeys = favoritesRepository.getFavoriteMembershipKeys()
        }
    }

    private fun reconcileUserSongReferences(currentSongs: List<Song>) {
        reconciliationJob?.cancel()
        reconciliationJob = coroutineScope.launch {
            val reconciled = withContext(Dispatchers.IO) {
                val referenceLibrarySongs = libraryCacheRepository.getAllCachedSongs()
                    .ifEmpty { currentSongs }
                val favoriteResult = favoritesRepository.reconcileSongReferences(referenceLibrarySongs)
                val playlistResult = playlistsRepository.reconcileSongReferences(referenceLibrarySongs)
                val historyResult = listeningHistoryRepository.reconcileSongReferences(referenceLibrarySongs)
                val recentlyPlayed = listeningHistoryRepository.getRecentlyPlayed()
                val mostPlayed = listeningHistoryRepository.getMostPlayed()
                ReferenceReconciliationData(
                    favoriteMembershipKeys = favoriteResult.resolvedMembershipKeys,
                    recentlyPlayed = recentlyPlayed,
                    mostPlayed = mostPlayed,
                    unresolvedFavorites = favoriteResult.unresolvedCount + favoriteResult.ambiguousCount,
                    unresolvedPlaylistRows = playlistResult.unresolvedCount + playlistResult.ambiguousCount,
                    unresolvedHistoryRows = historyResult.unresolvedCount + historyResult.ambiguousCount
                )
            }
            favoriteMembershipKeys = reconciled.favoriteMembershipKeys
            recentlyPlayedSongs = mapListeningHistoryEntriesToSongs(reconciled.recentlyPlayed)
            mostPlayedSongs = mapListeningHistoryEntriesToSongs(reconciled.mostPlayed)
            unresolvedFavoriteCount = reconciled.unresolvedFavorites
            unresolvedPlaylistRowCount = reconciled.unresolvedPlaylistRows
            unresolvedListeningHistoryCount = reconciled.unresolvedHistoryRows
            val selectedPlaylistId = selectedPlaylistSongs.firstOrNull()?.playlistId
            if (selectedPlaylistId != null) {
                selectedPlaylistSongs = playlistsRepository.getPlaylistSongs(selectedPlaylistId)
            }
        }
    }

    private fun loadPlaylists() {
        coroutineScope.launch {
            playlists = playlistsRepository.getPlaylists()
        }
    }

    private fun mapListeningHistoryEntriesToSongs(
        historyEntries: List<ListeningHistoryEntry>
    ): List<Song> {
        return historyEntries.mapNotNull { historyEntry ->
            (SongReferenceResolver.resolve(historyEntry.reference, songs)
                as? SongReferenceResolution.Resolved)?.song
        }
    }

    private fun resolveRestoredFolderSelections(restored: Set<String>): Set<String> {
        if (restored.isEmpty()) return emptySet()
        val available = libraryFolders.map { it.path }.toSet()
        return restored.mapNotNullTo(mutableSetOf()) { storedPath ->
            if (storedPath in available) return@mapNotNullTo storedPath
            val token = storedPath.replace('\\', '/').trim().trim('/')
            if (token.isBlank()) return@mapNotNullTo null
            val matches = available.filter { candidate ->
                val normalizedCandidate = candidate.replace('\\', '/').trimEnd('/')
                normalizedCandidate.equals(token, ignoreCase = true) ||
                    normalizedCandidate.endsWith("/$token", ignoreCase = true)
            }
            matches.singleOrNull()
        }
    }
}

private data class BackupRestoredUserData(
    val selectedLibraryFolders: Set<String>,
    val favoriteMembershipKeys: Set<String>,
    val playlists: List<Playlist>,
    val recentlyPlayed: List<ListeningHistoryEntry>,
    val mostPlayed: List<ListeningHistoryEntry>
)

private data class ReferenceReconciliationData(
    val favoriteMembershipKeys: Set<String>,
    val recentlyPlayed: List<ListeningHistoryEntry>,
    val mostPlayed: List<ListeningHistoryEntry>,
    val unresolvedFavorites: Int,
    val unresolvedPlaylistRows: Int,
    val unresolvedHistoryRows: Int
)
