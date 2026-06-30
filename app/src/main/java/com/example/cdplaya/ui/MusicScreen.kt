package com.example.cdplaya.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.favoriteKey
import com.example.cdplaya.data.LibraryFolder
import com.example.cdplaya.data.Song
import com.example.cdplaya.data.Playlist
import com.example.cdplaya.data.PlaylistSong
import com.example.cdplaya.player.RepeatMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    songs: List<Song>,
    permissionGranted: Boolean,
    currentSong: Song?,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    currentPosition: Int,
    duration: Int,
    snackbarHostState: SnackbarHostState,
    onUndoAddToQueueClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlaySongsClick: (List<Song>, Boolean) -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekChange: (Int) -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    queuedSongs: List<Song>,
    upcomingSongs: List<Song>,
    onAddToQueueClick: (Song) -> Unit,
    onPlayNextClick: (Song) -> Unit,
    onUndoPlayNextClick: (Song) -> Unit,
    onRemoveFromQueueClick: (Int) -> Unit,
    onMoveQueueItemUpClick: (Int) -> Unit,
    onMoveQueueItemDownClick: (Int) -> Unit,
    onClearQueueClick: () -> Unit,
    onPlayNextSongsClick: (List<Song>) -> Unit,
    onAddSongsToQueueClick: (List<Song>) -> Unit,
    onUndoPlayNextSongsClick: (List<Song>) -> Unit,
    onUndoAddSongsToQueueClick: (List<Song>) -> Unit,
    libraryFolders: List<LibraryFolder>,
    selectedLibraryFolders: Set<String>,
    onLibraryFolderToggle: (String) -> Unit,
    onSelectAllLibraryFolders: () -> Unit,
    onClearSelectedLibraryFolders: () -> Unit,
    favoriteSongKeys: Set<String>,
    onToggleFavoriteClick: (Song) -> Unit,
    playlists: List<Playlist>,
    selectedPlaylistName: String,
    selectedPlaylistSongs: List<PlaylistSong>,
    onCreatePlaylistClick: (String) -> Unit,
    onDeletePlaylistClick: (Playlist) -> Unit,
    onPlaylistSelected: (Playlist) -> Unit,
    onAddSongToPlaylistClick: (Playlist, Song) -> Unit,
    onRemovePlaylistSongClick: (PlaylistSong) -> Unit
) {
    var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }
    var isFolderScreenVisible by rememberSaveable { mutableStateOf(false) }
    var selectedLibraryTab by rememberSaveable { mutableStateOf(LibraryTab.SONGS) }
    var isSettingsScreenVisible by rememberSaveable { mutableStateOf(false) }
    var isExpandedUpNextSheetVisible by rememberSaveable { mutableStateOf(false) }
    var selectedArtistName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAlbumFolderPath by rememberSaveable { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedSongSortOption by rememberSaveable { mutableStateOf(LibrarySortOption.TITLE) }
    var selectedArtistSortOption by rememberSaveable { mutableStateOf(LibrarySortOption.NAME) }
    var selectedAlbumSortOption by rememberSaveable { mutableStateOf(LibrarySortOption.TITLE) }
    var selectedFavoriteSortOption by rememberSaveable { mutableStateOf(LibrarySortOption.TITLE) }
    var selectedPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
    var isCreatePlaylistDialogVisible by rememberSaveable { mutableStateOf(false) }
    var songPendingPlaylistAdd by remember { mutableStateOf<Song?>(null) }

    val coroutineScope = rememberCoroutineScope()
    var recentlyAddedSongIds by remember { mutableStateOf(setOf<Long>()) }

    BackHandler(
        enabled = isExpandedUpNextSheetVisible ||
                isPlayerExpanded ||
                isFolderScreenVisible ||
                isSettingsScreenVisible ||
                selectedArtistName != null ||
                selectedAlbumFolderPath != null ||
                selectedLibraryTab == LibraryTab.QUEUE ||
                selectedPlaylistId != null
    ) {
        when {
            isExpandedUpNextSheetVisible -> {
                isExpandedUpNextSheetVisible = false
            }

            isPlayerExpanded -> {
                isPlayerExpanded = false
            }

            isFolderScreenVisible -> {
                isFolderScreenVisible = false
                isSettingsScreenVisible = true
            }

            isSettingsScreenVisible -> {
                isSettingsScreenVisible = false
            }

            selectedArtistName != null -> {
                selectedArtistName = null
            }

            selectedAlbumFolderPath != null -> {
                selectedAlbumFolderPath = null
            }

            selectedPlaylistId != null -> {
                selectedPlaylistId = null
            }

            selectedLibraryTab == LibraryTab.QUEUE -> {
                selectedLibraryTab = LibraryTab.SONGS
            }
        }
    }

    fun handleAddToQueue(song: Song) {
        onAddToQueueClick(song)
        recentlyAddedSongIds = recentlyAddedSongIds + song.id

        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "\"${song.title}\" added to queue",
                actionLabel = "Undo",
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )

            if (result == SnackbarResult.ActionPerformed) {
                onUndoAddToQueueClick(song)
            }

            delay(300)
            recentlyAddedSongIds = recentlyAddedSongIds - song.id
        }
    }

    fun handlePlayNext(song: Song) {
        onPlayNextClick(song)
        recentlyAddedSongIds = recentlyAddedSongIds + song.id

        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "\"${song.title}\" will play next",
                actionLabel = "Undo",
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )

            if (result == SnackbarResult.ActionPerformed) {
                onUndoPlayNextClick(song)
            }

            delay(300)
            recentlyAddedSongIds = recentlyAddedSongIds - song.id
        }
    }

    fun handlePlayNextSongs(
        label: String,
        songsToAdd: List<Song>
    ) {
        if (songsToAdd.isEmpty()) {
            return
        }

        onPlayNextSongsClick(songsToAdd)
        recentlyAddedSongIds = recentlyAddedSongIds + songsToAdd.map { song -> song.id }.toSet()

        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "\"$label\" will play next",
                actionLabel = "Undo",
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )

            if (result == SnackbarResult.ActionPerformed) {
                onUndoPlayNextSongsClick(songsToAdd)
            }

            delay(300)
            recentlyAddedSongIds = recentlyAddedSongIds - songsToAdd.map { song -> song.id }.toSet()
        }
    }

    fun handleAddSongsToQueue(
        label: String,
        songsToAdd: List<Song>
    ) {
        if (songsToAdd.isEmpty()) {
            return
        }

        onAddSongsToQueueClick(songsToAdd)
        recentlyAddedSongIds = recentlyAddedSongIds + songsToAdd.map { song -> song.id }.toSet()

        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "\"$label\" added to queue",
                actionLabel = "Undo",
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )

            if (result == SnackbarResult.ActionPerformed) {
                onUndoAddSongsToQueueClick(songsToAdd)
            }

            delay(300)
            recentlyAddedSongIds = recentlyAddedSongIds - songsToAdd.map { song -> song.id }.toSet()
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when {
            isFolderScreenVisible -> {
                FolderSelectionScreen(
                    libraryFolders = libraryFolders,
                    selectedLibraryFolders = selectedLibraryFolders,
                    onBackClick = {
                        isFolderScreenVisible = false
                        isSettingsScreenVisible = true
                    },
                    onFolderToggle = onLibraryFolderToggle,
                    onSelectAllClick = onSelectAllLibraryFolders,
                    onClearSelectionClick = onClearSelectedLibraryFolders,
                    modifier = Modifier.fillMaxSize()
                )
            }

            isSettingsScreenVisible -> {
                SettingsScreen(
                    totalSongCount = songs.size,
                    availableFolderCount = libraryFolders.size,
                    selectedFolderCount = selectedLibraryFolders.size,
                    onBackClick = {
                        isSettingsScreenVisible = false
                    },
                    onLibraryFoldersClick = {
                        isSettingsScreenVisible = false
                        isFolderScreenVisible = true
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .animateContentSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CDPlaya",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        IconButton(
                            onClick = {
                                isSettingsScreenVisible = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }

                    if (!permissionGranted) {
                        Text(
                            text = "Audio and image permissions are needed to show your music.",
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        if (!isPlayerExpanded) {
                            PlayerCard(
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                isExpanded = false,
                                isShuffleEnabled = isShuffleEnabled,
                                repeatMode = repeatMode,
                                currentPosition = currentPosition,
                                duration = duration,
                                onPlayPauseClick = onPlayPauseClick,
                                onPreviousClick = onPreviousClick,
                                onNextClick = onNextClick,
                                onSeekChange = onSeekChange,
                                onShuffleClick = onShuffleClick,
                                onRepeatClick = onRepeatClick,
                                onExpandClick = {
                                    isPlayerExpanded = true
                                },
                                onCollapseClick = {
                                    isPlayerExpanded = false
                                },
                                onOpenUpNextClick = {
                                    selectedLibraryTab = LibraryTab.QUEUE
                                    selectedArtistName = null
                                    selectedAlbumFolderPath = null
                                },
                                isCurrentSongFavorite = currentSong?.let { song ->
                                    song.favoriteKey() in favoriteSongKeys
                                } == true,
                                onToggleFavoriteClick = onToggleFavoriteClick
                            )
                        }

                        LibraryTabs(
                            selectedTab = selectedLibraryTab,
                            onTabSelected = { tab ->
                                selectedLibraryTab = tab
                                selectedArtistName = null
                                selectedAlbumFolderPath = null
                                selectedPlaylistId = null
                            }
                        )

                        if (selectedLibraryTab != LibraryTab.QUEUE) {
                            LibrarySearchBar(
                                searchQuery = searchQuery,
                                onSearchQueryChange = { query ->
                                    searchQuery = query
                                }
                            )
                        }

                        val shouldShowSortDropdown =
                            selectedLibraryTab == LibraryTab.SONGS ||
                                    selectedLibraryTab == LibraryTab.FAVORITES ||
                                    selectedLibraryTab == LibraryTab.ARTISTS && selectedArtistName == null ||
                                    selectedLibraryTab == LibraryTab.ALBUMS && selectedAlbumFolderPath == null

                        if (shouldShowSortDropdown) {
                            val selectedSortOption = when (selectedLibraryTab) {
                                LibraryTab.SONGS -> selectedSongSortOption
                                LibraryTab.FAVORITES -> selectedFavoriteSortOption
                                LibraryTab.ARTISTS -> selectedArtistSortOption
                                LibraryTab.ALBUMS -> selectedAlbumSortOption
                                LibraryTab.QUEUE -> selectedSongSortOption
                                LibraryTab.PLAYLISTS -> selectedSongSortOption
                            }

                            val availableSortOptions = when (selectedLibraryTab) {
                                LibraryTab.SONGS,
                                LibraryTab.FAVORITES -> listOf(
                                    LibrarySortOption.TITLE,
                                    LibrarySortOption.ARTIST,
                                    LibrarySortOption.ALBUM,
                                )

                                LibraryTab.ARTISTS -> listOf(
                                    LibrarySortOption.NAME,
                                    LibrarySortOption.SONG_COUNT
                                )

                                LibraryTab.ALBUMS -> listOf(
                                    LibrarySortOption.TITLE,
                                    LibrarySortOption.ARTIST,
                                    LibrarySortOption.SONG_COUNT
                                )

                                LibraryTab.QUEUE -> emptyList()
                                LibraryTab.PLAYLISTS -> emptyList()
                            }

                            LibrarySortDropdown(
                                selectedOption = selectedSortOption,
                                options = availableSortOptions,
                                onOptionSelected = { option ->
                                    when (selectedLibraryTab) {
                                        LibraryTab.SONGS -> {
                                            selectedSongSortOption = option
                                        }

                                        LibraryTab.FAVORITES -> {
                                            selectedFavoriteSortOption = option
                                        }

                                        LibraryTab.ARTISTS -> {
                                            selectedArtistSortOption = option
                                        }

                                        LibraryTab.ALBUMS -> {
                                            selectedAlbumSortOption = option
                                        }

                                        LibraryTab.QUEUE -> Unit
                                        LibraryTab.PLAYLISTS -> Unit
                                    }
                                }
                            )
                        }

                        when (selectedLibraryTab) {
                            LibraryTab.SONGS -> {
                                SongsTabContent(
                                    songs = songs,
                                    searchQuery = searchQuery,
                                    sortOption = selectedSongSortOption,
                                    currentSong = currentSong,
                                    recentlyAddedSongIds = recentlyAddedSongIds,
                                    onSongClick = onSongClick,
                                    onPlayNextClick = { song ->
                                        handlePlayNext(song)
                                    },
                                    onAddToQueueClick = { song ->
                                        handleAddToQueue(song)
                                    },
                                    favoriteSongKeys = favoriteSongKeys,
                                    onToggleFavoriteClick = onToggleFavoriteClick,
                                    onAddToPlaylistClick = { song ->
                                        songPendingPlaylistAdd = song
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            LibraryTab.ARTISTS -> {
                                ArtistsTabContent(
                                    songs = songs,
                                    searchQuery = searchQuery,
                                    selectedArtistName = selectedArtistName,
                                    sortOption = selectedArtistSortOption,
                                    currentSong = currentSong,
                                    recentlyAddedSongIds = recentlyAddedSongIds,
                                    onArtistSelected = { artistName ->
                                        selectedArtistName = artistName
                                    },
                                    onBackFromArtist = {
                                        selectedArtistName = null
                                    },
                                    onPlaySongsClick = onPlaySongsClick,
                                    onPlayNextClick = { song ->
                                        handlePlayNext(song)
                                    },
                                    onSongClick = onSongClick,
                                    onAddToQueueClick = { song ->
                                        handleAddToQueue(song)
                                    },
                                    onPlayNextSongsClick = { label, songs ->
                                        handlePlayNextSongs(label, songs)
                                    },
                                    onAddSongsToQueueClick = { label, songs ->
                                        handleAddSongsToQueue(label, songs)
                                    },
                                    favoriteSongKeys = favoriteSongKeys,
                                    onToggleFavoriteClick = onToggleFavoriteClick,
                                    onAddToPlaylistClick = { song ->
                                        songPendingPlaylistAdd = song
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            LibraryTab.ALBUMS -> {
                                AlbumsTabContent(
                                    songs = songs,
                                    searchQuery = searchQuery,
                                    selectedAlbumFolderPath = selectedAlbumFolderPath,
                                    currentSong = currentSong,
                                    sortOption = selectedAlbumSortOption,
                                    recentlyAddedSongIds = recentlyAddedSongIds,
                                    onAlbumSelected = { albumFolderPath ->
                                        selectedAlbumFolderPath = albumFolderPath
                                    },
                                    onBackFromAlbum = {
                                        selectedAlbumFolderPath = null
                                    },
                                    onPlaySongsClick = onPlaySongsClick,
                                    onPlayNextClick = { song ->
                                        handlePlayNext(song)
                                    },
                                    onSongClick = onSongClick,
                                    onAddToQueueClick = { song ->
                                        handleAddToQueue(song)
                                    },
                                    onPlayNextSongsClick = { label, songs ->
                                        handlePlayNextSongs(label, songs)
                                    },
                                    onAddSongsToQueueClick = { label, songs ->
                                        handleAddSongsToQueue(label, songs)
                                    },
                                    favoriteSongKeys = favoriteSongKeys,
                                    onToggleFavoriteClick = onToggleFavoriteClick,
                                    onAddToPlaylistClick = { song ->
                                        songPendingPlaylistAdd = song
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            LibraryTab.FAVORITES -> {
                                FavoritesTabContent(
                                    songs = songs,
                                    favoriteSongKeys = favoriteSongKeys,
                                    searchQuery = searchQuery,
                                    sortOption = selectedFavoriteSortOption,
                                    currentSong = currentSong,
                                    recentlyAddedSongIds = recentlyAddedSongIds,
                                    onSongClick = onSongClick,
                                    onPlayNextClick = { song ->
                                        handlePlayNext(song)
                                    },
                                    onAddToQueueClick = { song ->
                                        handleAddToQueue(song)
                                    },
                                    onToggleFavoriteClick = onToggleFavoriteClick,
                                    onAddToPlaylistClick = { song ->
                                        songPendingPlaylistAdd = song
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            LibraryTab.QUEUE -> {
                                QueueScreen(
                                    queuedSongs = queuedSongs,
                                    upcomingSongs = upcomingSongs,
                                    isShuffleEnabled = isShuffleEnabled,
                                    onBackClick = {
                                        selectedLibraryTab = LibraryTab.SONGS
                                    },
                                    onRemoveFromQueueClick = onRemoveFromQueueClick,
                                    onMoveQueueItemUpClick = onMoveQueueItemUpClick,
                                    onMoveQueueItemDownClick = onMoveQueueItemDownClick,
                                    onClearQueueClick = onClearQueueClick,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            LibraryTab.PLAYLISTS -> {
                                PlaylistsTabContent(
                                    songs = songs,
                                    playlists = playlists,
                                    selectedPlaylistId = selectedPlaylistId,
                                    selectedPlaylistName = selectedPlaylistName,
                                    selectedPlaylistSongs = selectedPlaylistSongs,
                                    currentSong = currentSong,
                                    recentlyAddedSongIds = recentlyAddedSongIds,
                                    favoriteSongKeys = favoriteSongKeys,
                                    onCreatePlaylistClick = {
                                        isCreatePlaylistDialogVisible = true
                                    },
                                    onPlaylistClick = { playlist ->
                                        selectedPlaylistId = playlist.playlistId
                                        onPlaylistSelected(playlist)
                                    },
                                    onDeletePlaylistClick = onDeletePlaylistClick,
                                    onBackFromPlaylist = {
                                        selectedPlaylistId = null
                                    },
                                    onPlaySongsClick = onPlaySongsClick,
                                    onSongClick = onSongClick,
                                    onPlayNextClick = { song ->
                                        handlePlayNext(song)
                                    },
                                    onAddToQueueClick = { song ->
                                        handleAddToQueue(song)
                                    },
                                    onToggleFavoriteClick = onToggleFavoriteClick,
                                    onRemovePlaylistSongClick = onRemovePlaylistSongClick,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isPlayerExpanded && currentSong != null) {
            PlayerCard(
                modifier = Modifier.fillMaxSize(),
                currentSong = currentSong,
                isPlaying = isPlaying,
                isExpanded = true,
                isShuffleEnabled = isShuffleEnabled,
                repeatMode = repeatMode,
                currentPosition = currentPosition,
                duration = duration,
                onPlayPauseClick = onPlayPauseClick,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onSeekChange = onSeekChange,
                onShuffleClick = onShuffleClick,
                onRepeatClick = onRepeatClick,
                onExpandClick = {
                    isPlayerExpanded = true
                },
                onCollapseClick = {
                    isPlayerExpanded = false
                },
                onOpenUpNextClick = {
                    isExpandedUpNextSheetVisible = true
                },
                isCurrentSongFavorite = currentSong?.let { song ->
                    song.favoriteKey() in favoriteSongKeys
                } == true,
                onToggleFavoriteClick = onToggleFavoriteClick
            )
        }

        if (isExpandedUpNextSheetVisible) {
            ModalBottomSheet(
                onDismissRequest = {
                    isExpandedUpNextSheetVisible = false
                }
            ) {
                QueueScreen(
                    queuedSongs = queuedSongs,
                    upcomingSongs = upcomingSongs,
                    isShuffleEnabled = isShuffleEnabled,
                    onBackClick = {
                        isExpandedUpNextSheetVisible = false
                    },
                    onRemoveFromQueueClick = onRemoveFromQueueClick,
                    onMoveQueueItemUpClick = onMoveQueueItemUpClick,
                    onMoveQueueItemDownClick = onMoveQueueItemDownClick,
                    onClearQueueClick = onClearQueueClick,
                    modifier = Modifier.fillMaxHeight(0.86f)
                )
            }
        }

        if (isCreatePlaylistDialogVisible) {
            PlaylistNameDialog(
                onDismiss = {
                    isCreatePlaylistDialogVisible = false
                },
                onCreateClick = { playlistName ->
                    onCreatePlaylistClick(playlistName)
                    isCreatePlaylistDialogVisible = false
                }
            )
        }

        val selectedSongForPlaylist = songPendingPlaylistAdd

        if (selectedSongForPlaylist != null) {
            AddToPlaylistDialog(
                playlists = playlists,
                onDismiss = {
                    songPendingPlaylistAdd = null
                },
                onPlaylistSelected = { playlist ->
                    onAddSongToPlaylistClick(playlist, selectedSongForPlaylist)
                    songPendingPlaylistAdd = null
                }
            )
        }
    }
}
