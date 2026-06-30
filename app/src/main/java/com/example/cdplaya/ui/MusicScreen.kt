package com.example.cdplaya.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.LibraryFolder
import com.example.cdplaya.data.Song
import com.example.cdplaya.data.Playlist
import com.example.cdplaya.data.PlaylistSong
import com.example.cdplaya.player.RepeatMode


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

    val queueSnackbarActions = rememberQueueSnackbarActions(
        snackbarHostState = snackbarHostState,
        onAddToQueueClick = onAddToQueueClick,
        onUndoAddToQueueClick = onUndoAddToQueueClick,
        onPlayNextClick = onPlayNextClick,
        onUndoPlayNextClick = onUndoPlayNextClick,
        onPlayNextSongsClick = onPlayNextSongsClick,
        onUndoPlayNextSongsClick = onUndoPlayNextSongsClick,
        onAddSongsToQueueClick = onAddSongsToQueueClick,
        onUndoAddSongsToQueueClick = onUndoAddSongsToQueueClick
    )

    val recentlyAddedSongIds = queueSnackbarActions.recentlyAddedSongIds

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
                    MusicScreenHeader(
                        onSettingsClick = {
                            isSettingsScreenVisible = true
                        }
                    )

                    if (!permissionGranted) {
                        Text(
                            text = "Audio and image permissions are needed to show your music.",
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        if (!isPlayerExpanded) {
                            MiniPlayerSection(
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                isShuffleEnabled = isShuffleEnabled,
                                repeatMode = repeatMode,
                                currentPosition = currentPosition,
                                duration = duration,
                                favoriteSongKeys = favoriteSongKeys,
                                onPlayPauseClick = onPlayPauseClick,
                                onPreviousClick = onPreviousClick,
                                onNextClick = onNextClick,
                                onSeekChange = onSeekChange,
                                onShuffleClick = onShuffleClick,
                                onRepeatClick = onRepeatClick,
                                onExpandClick = {
                                    isPlayerExpanded = true
                                },
                                onOpenUpNextClick = {
                                    selectedLibraryTab = LibraryTab.QUEUE
                                    selectedArtistName = null
                                    selectedAlbumFolderPath = null
                                    selectedPlaylistId = null
                                },
                                onToggleFavoriteClick = onToggleFavoriteClick
                            )
                        }

                        LibraryChromeControls(
                            selectedLibraryTab = selectedLibraryTab,
                            selectedArtistName = selectedArtistName,
                            selectedAlbumFolderPath = selectedAlbumFolderPath,
                            searchQuery = searchQuery,
                            selectedSongSortOption = selectedSongSortOption,
                            selectedArtistSortOption = selectedArtistSortOption,
                            selectedAlbumSortOption = selectedAlbumSortOption,
                            selectedFavoriteSortOption = selectedFavoriteSortOption,
                            onTabSelected = { tab ->
                                selectedLibraryTab = tab
                                selectedArtistName = null
                                selectedAlbumFolderPath = null
                                selectedPlaylistId = null
                            },
                            onSearchQueryChange = { query ->
                                searchQuery = query
                            },
                            onSongSortOptionSelected = { option ->
                                selectedSongSortOption = option
                            },
                            onArtistSortOptionSelected = { option ->
                                selectedArtistSortOption = option
                            },
                            onAlbumSortOptionSelected = { option ->
                                selectedAlbumSortOption = option
                            },
                            onFavoriteSortOptionSelected = { option ->
                                selectedFavoriteSortOption = option
                            }
                        )

                        MusicLibraryContent(
                            selectedLibraryTab = selectedLibraryTab,
                            songs = songs,
                            searchQuery = searchQuery,
                            selectedSongSortOption = selectedSongSortOption,
                            selectedArtistSortOption = selectedArtistSortOption,
                            selectedAlbumSortOption = selectedAlbumSortOption,
                            selectedFavoriteSortOption = selectedFavoriteSortOption,
                            selectedArtistName = selectedArtistName,
                            selectedAlbumFolderPath = selectedAlbumFolderPath,
                            selectedPlaylistId = selectedPlaylistId,
                            playlists = playlists,
                            selectedPlaylistName = selectedPlaylistName,
                            selectedPlaylistSongs = selectedPlaylistSongs,
                            currentSong = currentSong,
                            recentlyAddedSongIds = recentlyAddedSongIds,
                            favoriteSongKeys = favoriteSongKeys,
                            queuedSongs = queuedSongs,
                            upcomingSongs = upcomingSongs,
                            isShuffleEnabled = isShuffleEnabled,
                            onSongClick = onSongClick,
                            onPlaySongsClick = onPlaySongsClick,
                            onPlayNextClick = { song ->
                                queueSnackbarActions.playNext(song)
                            },
                            onAddToQueueClick = { song ->
                                queueSnackbarActions.addToQueue(song)
                            },
                            onPlayNextSongsClick = { label, songs ->
                                queueSnackbarActions.playNextSongs(label, songs)
                            },
                            onAddSongsToQueueClick = { label, songs ->
                                queueSnackbarActions.addSongsToQueue(label, songs)
                            },
                            onToggleFavoriteClick = onToggleFavoriteClick,
                            onAddToPlaylistClick = { song ->
                                songPendingPlaylistAdd = song
                            },
                            onArtistSelected = { artistName ->
                                selectedArtistName = artistName
                            },
                            onBackFromArtist = {
                                selectedArtistName = null
                            },
                            onAlbumSelected = { albumFolderPath ->
                                selectedAlbumFolderPath = albumFolderPath
                            },
                            onBackFromAlbum = {
                                selectedAlbumFolderPath = null
                            },
                            onBackFromQueue = {
                                selectedLibraryTab = LibraryTab.SONGS
                            },
                            onRemoveFromQueueClick = onRemoveFromQueueClick,
                            onMoveQueueItemUpClick = onMoveQueueItemUpClick,
                            onMoveQueueItemDownClick = onMoveQueueItemDownClick,
                            onClearQueueClick = onClearQueueClick,
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
                            onRemovePlaylistSongClick = onRemovePlaylistSongClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        MusicScreenOverlays(
            isPlayerExpanded = isPlayerExpanded,
            currentSong = currentSong,
            isPlaying = isPlaying,
            isShuffleEnabled = isShuffleEnabled,
            repeatMode = repeatMode,
            currentPosition = currentPosition,
            duration = duration,
            favoriteSongKeys = favoriteSongKeys,
            isExpandedUpNextSheetVisible = isExpandedUpNextSheetVisible,
            queuedSongs = queuedSongs,
            upcomingSongs = upcomingSongs,
            isCreatePlaylistDialogVisible = isCreatePlaylistDialogVisible,
            songPendingPlaylistAdd = songPendingPlaylistAdd,
            playlists = playlists,
            onPlayPauseClick = onPlayPauseClick,
            onPreviousClick = onPreviousClick,
            onNextClick = onNextClick,
            onSeekChange = onSeekChange,
            onShuffleClick = onShuffleClick,
            onRepeatClick = onRepeatClick,
            onCollapseExpandedPlayer = {
                isPlayerExpanded = false
            },
            onShowExpandedUpNextSheet = {
                isExpandedUpNextSheetVisible = true
            },
            onDismissExpandedUpNextSheet = {
                isExpandedUpNextSheetVisible = false
            },
            onRemoveFromQueueClick = onRemoveFromQueueClick,
            onMoveQueueItemUpClick = onMoveQueueItemUpClick,
            onMoveQueueItemDownClick = onMoveQueueItemDownClick,
            onClearQueueClick = onClearQueueClick,
            onToggleFavoriteClick = onToggleFavoriteClick,
            onDismissCreatePlaylistDialog = {
                isCreatePlaylistDialogVisible = false
            },
            onCreatePlaylistClick = onCreatePlaylistClick,
            onDismissAddToPlaylistDialog = {
                songPendingPlaylistAdd = null
            },
            onAddSongToPlaylistClick = onAddSongToPlaylistClick
        )
    }
}
