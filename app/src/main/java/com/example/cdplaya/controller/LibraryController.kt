package com.example.cdplaya.controller

import android.content.Context
import android.net.Uri
import android.util.Log
import android.os.SystemClock
import android.content.pm.ApplicationInfo
import androidx.room.withTransaction
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
import com.example.cdplaya.data.PersistedSongReferenceRows
import com.example.cdplaya.data.ReconciliationGenerationCoordinator
import com.example.cdplaya.data.Song
import com.example.cdplaya.data.SongReferenceIndex
import com.example.cdplaya.data.SongReferenceReconciliationPlanner
import com.example.cdplaya.data.SongReferenceResolution
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
import com.example.cdplaya.ui.state.LibraryUiState
import com.example.cdplaya.ui.state.libraryUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal suspend fun <T> runLibraryScanOffMain(block: suspend () -> T): T {
    return withContext(Dispatchers.IO) { block() }
}

internal class LibraryPublicationTracker {
    private var lastSnapshot: MusicLibraryData? = null

    fun shouldPublish(snapshot: MusicLibraryData): Boolean {
        if (snapshot == lastSnapshot) return false
        lastSnapshot = snapshot
        return true
    }
}

class LibraryController(
    context: Context,
    private val appDatabase: AppDatabase,
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
    private val reconciliationCoordinator = ReconciliationGenerationCoordinator()
    private var songReferenceIndex: SongReferenceIndex = SongReferenceIndex.EMPTY
    private var visibleSongMembershipKeys: Set<String> = emptySet()
    private var libraryPublishCount = 0L
    private val publicationTracker = LibraryPublicationTracker()
    private val libraryScanMutex = Mutex()

    private val _uiState = MutableStateFlow(LibraryUiState.Empty)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private var lastLibraryRefreshResult: com.example.cdplaya.data.LibraryRefreshResult? = null

    private var songs: List<Song>
        get() = _uiState.value.songs
        set(value) = updateState { copy(songs = value.toList()) }
    private val libraryFolders: List<LibraryFolder>
        get() = _uiState.value.folders
    private var selectedLibraryFolders: Set<String>
        get() = _uiState.value.selectedFolders
        set(value) = updateState { copy(selectedFolders = value.toSet()) }
    private var favoriteMembershipKeys: Set<String>
        get() = _uiState.value.favoriteMembershipKeys
        set(value) = updateState { copy(favoriteMembershipKeys = value.toSet()) }
    private var playlists: List<Playlist>
        get() = _uiState.value.playlists
        set(value) = updateState { copy(playlists = value.toList()) }
    private var selectedPlaylistName: String
        get() = _uiState.value.selectedPlaylistName
        set(value) = updateState { copy(selectedPlaylistName = value) }
    private var selectedPlaylistSongs: List<PlaylistSong>
        get() = _uiState.value.selectedPlaylistSongs
        set(value) = updateState { copy(selectedPlaylistSongs = value.toList()) }
    private var recentlyPlayedSongs: List<Song>
        get() = _uiState.value.recentlyPlayedSongs
        set(value) = updateState { copy(recentlyPlayedSongs = value.toList()) }
    private var mostPlayedSongs: List<Song>
        get() = _uiState.value.mostPlayedSongs
        set(value) = updateState { copy(mostPlayedSongs = value.toList()) }

    private inline fun updateState(transform: LibraryUiState.() -> LibraryUiState) {
        _uiState.update { current -> current.transform() }
    }

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
                val cachedLibraryData = loadCachedLibraryDataForPublication(savedSelectedFolders)

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
            val updatedUserData = withContext(Dispatchers.IO) {
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
                favoritesRepository.getFavoriteMembershipKeys() to
                    playlistsRepository.getPlaylists()
            }
            val updatedFavoriteMembershipKeys = updatedUserData.first
            val updatedPlaylists = updatedUserData.second

            val selectedPlaylistId = selectedPlaylistSongs.firstOrNull()?.playlistId

            val updatedSelectedPlaylistSongs = selectedPlaylistId?.let { playlistId ->
                getResolvedPlaylistSongs(playlistId)
            }

            val libraryData = withContext(Dispatchers.IO) {
                scanFreshLibraryAndUpdateCache(
                    selectedFolders = selectedLibraryFolders,
                    forceArtworkRefreshIds = setOf(originalSong.id)
                )
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
            selectedPlaylistSongs = getResolvedPlaylistSongs(playlist.playlistId)
        }
    }

    fun preparePlaylistExport(
        playlist: Playlist,
        onPrepared: (Result<PreparedPlaylistExport>) -> Unit
    ) {
        coroutineScope.launch {
            val result = runCatching {
                val playlistSongs = getResolvedPlaylistSongs(playlist.playlistId)
                val exportableSongs = playlistSongs.mapNotNull(PlaylistSong::resolvedSong)

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
                selectedPlaylistSongs = getResolvedPlaylistSongs(playlist.playlistId)
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
            selectedPlaylistSongs = getResolvedPlaylistSongs(playlistSong.playlistId)
        }
    }

    fun movePlaylistSongUp(playlistSong: PlaylistSong) {
        coroutineScope.launch {
            playlistsRepository.movePlaylistSongUp(
                playlistId = playlistSong.playlistId,
                playlistSongId = playlistSong.playlistSongId
            )

            loadPlaylists()
            selectedPlaylistSongs = getResolvedPlaylistSongs(
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
            selectedPlaylistSongs = getResolvedPlaylistSongs(
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
            val mappedHistory = withContext(Dispatchers.Default) {
                mapListeningHistoryEntriesToSongs(historyData.first) to
                    mapListeningHistoryEntriesToSongs(historyData.second)
            }

            recentlyPlayedSongs = mappedHistory.first
            mostPlayedSongs = mappedHistory.second
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
        val mappedHistory = withContext(Dispatchers.Default) {
            mapListeningHistoryEntriesToSongs(restoredData.recentlyPlayed) to
                mapListeningHistoryEntriesToSongs(restoredData.mostPlayed)
        }
        recentlyPlayedSongs = mappedHistory.first
        mostPlayedSongs = mappedHistory.second

        if (folderSelectionChanged) {
            reloadSongsAfterFolderChange()
        } else {
            reconcileUserSongReferences(songs, songReferenceIndex)
        }
    }

    private fun reloadSongsAfterFolderChange() {
        refreshJob?.cancel()
        refreshJob = coroutineScope.launch {
            val hasCachedSongs = withContext(Dispatchers.IO) {
                libraryCacheRepository.hasCachedSongs()
            }

            if (hasCachedSongs) {
                val cachedLibraryData =
                    loadCachedLibraryDataForPublication(selectedLibraryFolders)

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

    private suspend fun publishLibraryData(
        libraryData: MusicLibraryData,
        reconcilePlayback: Boolean
    ) {
        if (!publicationTracker.shouldPublish(libraryData)) return
        val indexedSnapshot = withContext(Dispatchers.Default) {
            IndexedLibrarySnapshot(
                index = SongReferenceIndex.build(libraryData.referenceSongs),
                visibleMembershipKeys = libraryData.songs.mapTo(mutableSetOf()) {
                    it.membershipKey()
                }
            )
        }
        songReferenceIndex = indexedSnapshot.index
        visibleSongMembershipKeys = indexedSnapshot.visibleMembershipKeys
        libraryPublishCount += 1

        val publishedSongs = libraryData.songs.toList()
        _uiState.update { current ->
            libraryUiState(
                songs = publishedSongs,
                folders = libraryData.libraryFolders,
                selectedFolders = current.selectedFolders,
                favoriteMembershipKeys = current.favoriteMembershipKeys,
                playlists = current.playlists,
                selectedPlaylistName = current.selectedPlaylistName,
                selectedPlaylistSongs = current.selectedPlaylistSongs,
                recentlyPlayedSongs = current.recentlyPlayedSongs,
                mostPlayedSongs = current.mostPlayedSongs,
                recentlyAddedSongs = sortSongsByDateAddedDescending(publishedSongs),
                unresolvedFavoriteCount = current.unresolvedFavoriteCount,
                unresolvedPlaylistRowCount = current.unresolvedPlaylistRowCount,
                unresolvedListeningHistoryCount = current.unresolvedListeningHistoryCount,
                lastRefreshResult = lastLibraryRefreshResult,
                isLoading = false,
                isRefreshing = false,
                errorMessage = null
            )
        }
        PlaybackLibraryBridge.updateSongs(publishedSongs)
        reconcileUserSongReferences(publishedSongs, indexedSnapshot.index)

        if (reconcilePlayback) {
            playbackController.handleLibrarySongsChanged(publishedSongs)
        } else {
            playbackController.setLibrarySongs(publishedSongs)
        }
    }

    private suspend fun scanFreshLibraryAndUpdateCache(
        selectedFolders: Set<String>,
        forceArtworkRefreshIds: Set<Long> = emptySet()
    ): MusicLibraryData = runLibraryScanOffMain {
        libraryScanMutex.withLock {
            val repository = MusicRepository(applicationContext)
            val cachedSongs = libraryCacheRepository.getAllCachedSongs()
            val startedAt = SystemClock.elapsedRealtime()
            val refreshResult = repository.refreshLibrary(
                cachedSongs = cachedSongs,
                forceArtworkRefreshIds = forceArtworkRefreshIds
            )
            lastLibraryRefreshResult = refreshResult

            if (applicationContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                Log.d(
                    "LibraryRefresh",
                    "elapsedMs=${SystemClock.elapsedRealtime() - startedAt} " +
                        "reused=${refreshResult.reusedCount} added=${refreshResult.addedCount} " +
                        "updated=${refreshResult.updatedCount} moved=${refreshResult.movedCount} " +
                        "removed=${refreshResult.removedCount} enriched=${refreshResult.enrichmentCount} " +
                        "artworkRepairs=${refreshResult.artworkRepairCount} " +
                        "complete=${refreshResult.successfulCompleteScan}"
                )
            }

            if (refreshResult.successfulCompleteScan && refreshResult.requiresCacheWrite) {
                libraryCacheRepository.replaceCachedSongs(refreshResult.songs)
            }
            com.example.cdplaya.data.buildMusicLibraryData(
                allSongs = refreshResult.songs,
                selectedFolders = selectedFolders
            )
        }
    }

    private suspend fun loadCachedLibraryDataForPublication(
        selectedFolders: Set<String>
    ): MusicLibraryData = runLibraryScanOffMain {
        val cachedSongs = libraryCacheRepository.getAllCachedSongs()
        val publicationSongs = MusicRepository(applicationContext)
            .prepareCachedSongsForPublication(cachedSongs)
        com.example.cdplaya.data.buildMusicLibraryData(
            allSongs = publicationSongs,
            selectedFolders = selectedFolders
        )
    }

    private fun loadFavoriteMembershipKeys() {
        coroutineScope.launch {
            favoriteMembershipKeys = favoritesRepository.getFavoriteMembershipKeys()
        }
    }

    private fun reconcileUserSongReferences(
        currentSongs: List<Song>,
        index: SongReferenceIndex
    ) {
        val generation = reconciliationCoordinator.nextGeneration()
        reconciliationJob?.cancel()
        val selectedPlaylistId = selectedPlaylistSongs.firstOrNull()?.playlistId
        val visibleMembershipKeys = currentSongs.mapTo(mutableSetOf()) { it.membershipKey() }
        reconciliationJob = coroutineScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            val reconciled = reconciliationCoordinator.runLatest(generation) {
                val persistedRows = withContext(Dispatchers.IO) {
                    PersistedSongReferenceRows(
                        favorites = favoritesRepository.loadReferenceRows(),
                        playlistRows = playlistsRepository.loadReferenceRows(),
                        historyRows = listeningHistoryRepository.loadReferenceRows()
                    )
                }
                val plan = withContext(Dispatchers.Default) {
                    SongReferenceReconciliationPlanner.plan(index, persistedRows)
                }
                val storedResults = withContext(Dispatchers.IO) {
                    appDatabase.withTransaction {
                        favoritesRepository.applyReferenceBackfill(plan.favorites)
                        playlistsRepository.applyReferenceBackfill(plan.playlists)
                        listeningHistoryRepository.applyReferenceBackfill(plan.history)
                    }
                    val recentlyPlayed = listeningHistoryRepository.getRecentlyPlayed()
                    val mostPlayed = listeningHistoryRepository.getMostPlayed()
                    val selectedPlaylistRows = selectedPlaylistId?.let {
                        playlistsRepository.getPlaylistSongs(it)
                    }
                    Triple(recentlyPlayed, mostPlayed, selectedPlaylistRows)
                }
                val mappedResults = withContext(Dispatchers.Default) {
                    val recentlyPlayed = mapListeningHistoryEntriesToSongs(
                        storedResults.first,
                        index,
                        visibleMembershipKeys
                    )
                    val mostPlayed = mapListeningHistoryEntriesToSongs(
                        storedResults.second,
                        index,
                        visibleMembershipKeys
                    )
                    val selectedPlaylistRows = storedResults.third?.let { rows ->
                        resolvePlaylistRows(rows, index, visibleMembershipKeys)
                    }
                    Triple(recentlyPlayed, mostPlayed, selectedPlaylistRows)
                }
                ReferenceReconciliationData(
                    favoriteMembershipKeys = plan.favorites.result.resolvedMembershipKeys,
                    recentlyPlayed = mappedResults.first,
                    mostPlayed = mappedResults.second,
                    selectedPlaylistSongs = mappedResults.third,
                    unresolvedFavorites = plan.favorites.result.unresolvedCount +
                        plan.favorites.result.ambiguousCount,
                    unresolvedPlaylistRows = plan.playlists.result.unresolvedCount +
                        plan.playlists.result.ambiguousCount,
                    unresolvedHistoryRows = plan.history.result.unresolvedCount +
                        plan.history.result.ambiguousCount,
                    inspectedRows = plan.inspectedRowCount,
                    writes = plan.writeCount,
                    favoriteInspected = plan.favorites.result.inspectedCount,
                    favoriteWrites = plan.favorites.result.backfilledCount,
                    playlistInspected = plan.playlists.result.inspectedCount,
                    playlistWrites = plan.playlists.result.backfilledCount,
                    historyInspected = plan.history.result.inspectedCount,
                    historyWrites = plan.history.result.backfilledCount
                )
            }
            if (reconciled == null || !reconciliationCoordinator.isCurrent(generation)) return@launch
            _uiState.update { current ->
                current.copy(
                    favoriteMembershipKeys = reconciled.favoriteMembershipKeys.toSet(),
                    recentlyPlayedSongs = reconciled.recentlyPlayed.toList(),
                    mostPlayedSongs = reconciled.mostPlayed.toList(),
                    unresolvedFavoriteCount = reconciled.unresolvedFavorites,
                    unresolvedPlaylistRowCount = reconciled.unresolvedPlaylistRows,
                    unresolvedListeningHistoryCount = reconciled.unresolvedHistoryRows,
                    selectedPlaylistSongs = reconciled.selectedPlaylistSongs?.toList()
                        ?: current.selectedPlaylistSongs
                )
            }
            if (applicationContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                Log.d(
                    "SongReferenceReconciliation",
                    "generation=$generation publish=$libraryPublishCount indexBuilds=1 active=1 " +
                        "favorites=${reconciled.favoriteInspected}/${reconciled.favoriteWrites} " +
                        "playlists=${reconciled.playlistInspected}/${reconciled.playlistWrites} " +
                        "history=${reconciled.historyInspected}/${reconciled.historyWrites} " +
                        "total=${reconciled.inspectedRows}/${reconciled.writes} " +
                        "elapsedMs=${SystemClock.elapsedRealtime() - startedAt}"
                )
            }
        }
    }

    private fun loadPlaylists() {
        coroutineScope.launch {
            playlists = playlistsRepository.getPlaylists()
        }
    }

    private suspend fun getResolvedPlaylistSongs(playlistId: Long): List<PlaylistSong> {
        val rows = withContext(Dispatchers.IO) {
            playlistsRepository.getPlaylistSongs(playlistId)
        }
        return withContext(Dispatchers.Default) {
            resolvePlaylistRows(rows, songReferenceIndex, visibleSongMembershipKeys)
        }
    }

    private fun resolvePlaylistRows(
        rows: List<PlaylistSong>,
        index: SongReferenceIndex,
        visibleMembershipKeys: Set<String>
    ): List<PlaylistSong> = rows.map { row ->
        val resolved = (index.resolve(row.reference) as? SongReferenceResolution.Resolved)?.song
            ?.takeIf { it.membershipKey() in visibleMembershipKeys }
        row.copy(resolvedSong = resolved)
    }

    private fun mapListeningHistoryEntriesToSongs(
        historyEntries: List<ListeningHistoryEntry>,
        index: SongReferenceIndex = songReferenceIndex,
        visibleMembershipKeys: Set<String> = visibleSongMembershipKeys
    ): List<Song> {
        return historyEntries.mapNotNull { historyEntry ->
            (index.resolve(historyEntry.reference) as? SongReferenceResolution.Resolved)?.song
                ?.takeIf { it.membershipKey() in visibleMembershipKeys }
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
    val recentlyPlayed: List<Song>,
    val mostPlayed: List<Song>,
    val selectedPlaylistSongs: List<PlaylistSong>?,
    val unresolvedFavorites: Int,
    val unresolvedPlaylistRows: Int,
    val unresolvedHistoryRows: Int,
    val inspectedRows: Int,
    val writes: Int,
    val favoriteInspected: Int,
    val favoriteWrites: Int,
    val playlistInspected: Int,
    val playlistWrites: Int,
    val historyInspected: Int,
    val historyWrites: Int
)

private data class IndexedLibrarySnapshot(
    val index: SongReferenceIndex,
    val visibleMembershipKeys: Set<String>
)
