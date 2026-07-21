package com.example.cdplaya.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.LibraryFolder
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.data.Playlist
import com.example.cdplaya.data.PlaylistSong
import com.example.cdplaya.data.Song
import com.example.cdplaya.data.favoriteKey
import com.example.cdplaya.player.RepeatMode
import com.example.cdplaya.player.replaygain.ReplayGainMode
import com.example.cdplaya.ui.home.HomeScreen
import com.example.cdplaya.ui.library.FolderSelectionScreen
import com.example.cdplaya.ui.library.LibraryBrowseSwitcher
import com.example.cdplaya.ui.library.LibrarySortOption
import com.example.cdplaya.ui.library.LibraryTab
import com.example.cdplaya.ui.library.LibraryViewMode
import com.example.cdplaya.ui.library.LibraryViewModeToggle
import com.example.cdplaya.ui.library.MusicLibraryContent
import com.example.cdplaya.ui.library.rememberLibraryViewModeState
import com.example.cdplaya.ui.library.viewCategory
import com.example.cdplaya.ui.navigation.MainDestination
import com.example.cdplaya.ui.queue.QueueSnackbarActions
import com.example.cdplaya.ui.player.theme.PlayerThemeTokenField
import com.example.cdplaya.ui.player.theme.PlayerThemeTokens
import com.example.cdplaya.ui.player.modern.ModernArtworkTransitionStyle
import com.example.cdplaya.ui.player.modern.ModernSeekbarStyle
import com.example.cdplaya.ui.settings.SettingsScreen

@Composable
fun MusicScreenBody(
    songs: List<Song>,
    permissionGranted: Boolean,
    currentSong: Song?,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    currentPosition: Int,
    duration: Int,
    queuedSongs: List<Song>,
    upcomingSongs: List<Song>,
    libraryFolders: List<LibraryFolder>,
    selectedLibraryFolders: Set<String>,
    favoriteSongKeys: Set<String>,
    playlists: List<Playlist>,
    selectedPlaylistName: String,
    selectedPlaylistSongs: List<PlaylistSong>,
    mainDestination: MainDestination,
    selectedLibraryTab: LibraryTab,
    selectedArtistName: String?,
    selectedAlbumFolderPath: String?,
    selectedPlaylistId: Long?,
    searchQuery: String,
    selectedSongSortOption: LibrarySortOption,
    selectedArtistSortOption: LibrarySortOption,
    selectedAlbumSortOption: LibrarySortOption,
    selectedFavoriteSortOption: LibrarySortOption,
    recentlyAddedSongIds: Set<Long>,
    isPlayerExpanded: Boolean,
    isFolderScreenVisible: Boolean,
    isSettingsScreenVisible: Boolean,
    queueSnackbarActions: QueueSnackbarActions,
    onSettingsClick: () -> Unit,
    onOpenLibrary: (LibraryTab) -> Unit,
    onFolderBackClick: () -> Unit,
    onSettingsBackClick: () -> Unit,
    onLibraryFoldersClick: () -> Unit,
    onExportBackupClick: () -> Unit,
    onRestoreBackupClick: () -> Unit,
    onLibraryFolderToggle: (String) -> Unit,
    onSelectAllLibraryFolders: () -> Unit,
    onClearSelectedLibraryFolders: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSongSortOptionSelected: (LibrarySortOption) -> Unit,
    onArtistSortOptionSelected: (LibrarySortOption) -> Unit,
    onAlbumSortOptionSelected: (LibrarySortOption) -> Unit,
    onFavoriteSortOptionSelected: (LibrarySortOption) -> Unit,
    onExpandPlayerClick: () -> Unit,
    onMiniPlayerUpNextClick: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlaySongsClick: (List<Song>, Boolean) -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekChange: (Int) -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onToggleFavoriteClick: (Song) -> Unit,
    onAddToPlaylistClick: (Song) -> Unit,
    onArtistSelected: (String) -> Unit,
    onBackFromArtist: () -> Unit,
    onAlbumSelected: (String) -> Unit,
    onBackFromAlbum: () -> Unit,
    onBackFromQueue: () -> Unit,
    onRemoveFromQueueClick: (Int) -> Unit,
    onMoveQueueItemUpClick: (Int) -> Unit,
    onMoveQueueItemDownClick: (Int) -> Unit,
    onClearQueueClick: () -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onRenamePlaylistClick: (Playlist, String) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onDeletePlaylistClick: (Playlist) -> Unit,
    onExportPlaylistClick: (Playlist) -> Unit,
    onImportPlaylistClick: () -> Unit,
    onBackFromPlaylist: () -> Unit,
    onRemovePlaylistSongClick: (PlaylistSong) -> Unit,
    onAddSongsToPlaylistClick: (List<Song>) -> Unit,
    onMovePlaylistSongUpClick: (PlaylistSong) -> Unit,
    onMovePlaylistSongDownClick: (PlaylistSong) -> Unit,
    onEditSongTagsClick: (Song) -> Unit,
    isSleepTimerActive: Boolean,
    sleepTimerDisplayText: String,
    onSleepTimerClick: () -> Unit,
    recentlyPlayedSongs: List<Song>,
    mostPlayedSongs: List<Song>,
    selectedPlayerTheme: PlayerTheme,
    selectedPlayerThemeTokens: PlayerThemeTokens,
    onPlayerThemeSelected: (PlayerTheme) -> Unit,
    onUpdatePlayerThemeTokenOverride: (PlayerTheme, PlayerThemeTokenField, Color) -> Unit,
    onResetPlayerThemeTokenOverrides: (PlayerTheme) -> Unit,
    selectedModernArtworkTransitionStyle: ModernArtworkTransitionStyle,
    onModernArtworkTransitionStyleSelected: (ModernArtworkTransitionStyle) -> Unit,
    selectedModernSeekbarStyle: ModernSeekbarStyle,
    onModernSeekbarStyleSelected: (ModernSeekbarStyle) -> Unit,
    selectedReplayGainMode: ReplayGainMode,
    onReplayGainModeSelected: (ReplayGainMode) -> Unit,
    bottomContentPadding: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    val libraryViewModeState = rememberLibraryViewModeState()

    when {
        isFolderScreenVisible -> {
            FolderSelectionScreen(
                libraryFolders = libraryFolders,
                selectedLibraryFolders = selectedLibraryFolders,
                onBackClick = onFolderBackClick,
                onFolderToggle = onLibraryFolderToggle,
                onSelectAllClick = onSelectAllLibraryFolders,
                onClearSelectionClick = onClearSelectedLibraryFolders,
                modifier = modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            )
        }

        isSettingsScreenVisible -> {
            SettingsScreen(
                totalSongCount = songs.size,
                availableFolderCount = libraryFolders.size,
                selectedFolderCount = selectedLibraryFolders.size,
                onBackClick = onSettingsBackClick,
                onLibraryFoldersClick = onLibraryFoldersClick,
                onExportBackupClick = onExportBackupClick,
                onRestoreBackupClick = onRestoreBackupClick,
                isSleepTimerActive = isSleepTimerActive,
                sleepTimerDisplayText = sleepTimerDisplayText,
                onSleepTimerClick = onSleepTimerClick,
                selectedPlayerTheme = selectedPlayerTheme,
                selectedPlayerThemeTokens = selectedPlayerThemeTokens,
                onPlayerThemeSelected = onPlayerThemeSelected,
                onUpdatePlayerThemeTokenOverride = onUpdatePlayerThemeTokenOverride,
                onResetPlayerThemeTokenOverrides = onResetPlayerThemeTokenOverrides,
                selectedModernArtworkTransitionStyle = selectedModernArtworkTransitionStyle,
                onModernArtworkTransitionStyleSelected = onModernArtworkTransitionStyleSelected,
                selectedModernSeekbarStyle = selectedModernSeekbarStyle,
                onModernSeekbarStyleSelected = onModernSeekbarStyleSelected,
                selectedReplayGainMode = selectedReplayGainMode,
                onReplayGainModeSelected = onReplayGainModeSelected,
                modifier = modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            )
        }

        else -> {
            AnimatedContent(
                targetState = mainDestination,
                transitionSpec = {
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1

                    (fadeIn(tween(190)) +
                            scaleIn(tween(210), initialScale = 0.985f) +
                            slideInHorizontally(tween(210)) { width ->
                                direction * width / 28
                            })
                        .togetherWith(
                            fadeOut(tween(145)) +
                                    scaleOut(tween(170), targetScale = 0.995f) +
                                    slideOutHorizontally(tween(175)) { width ->
                                        -direction * width / 36
                                    }
                        )
                },
                label = "appShellDestination"
            ) { destination ->
                if (destination == MainDestination.HOME) {
                HomeScreen(
                    permissionGranted = permissionGranted,
                    recentlyPlayedSongs = recentlyPlayedSongs,
                    favoriteSongs = songs.filter { song ->
                        song.favoriteKey() in favoriteSongKeys
                    },
                    currentSongId = currentSong?.id,
                    songCount = songs.size,
                    albumCount = songs
                        .mapTo(mutableSetOf()) { song -> song.folderPath }
                        .size,
                    artistCount = songs
                        .mapTo(mutableSetOf()) { song ->
                            song.artist.ifBlank { "Unknown Artist" }
                        }
                        .size,
                    playlistCount = playlists.size,
                    onSettingsClick = onSettingsClick,
                    onOpenLibrary = { tab ->
                        onOpenLibrary(tab)
                    },
                    onRecentlyPlayedSongClick = { song ->
                        onSongClick(song, recentlyPlayedSongs)
                    },
                    onFavoriteSongClick = { song ->
                        onSongClick(
                            song,
                            songs.filter { candidate ->
                                candidate.favoriteKey() in favoriteSongKeys
                            }
                        )
                    },
                    modifier = modifier,
                    bottomContentPadding = bottomContentPadding
                )
                } else {
                    val isSearchDestination = destination == MainDestination.SEARCH
                    val isLibraryDetail = selectedArtistName != null ||
                            selectedAlbumFolderPath != null ||
                            selectedPlaylistId != null
                    val selectedViewMode = if (isSearchDestination) {
                        LibraryViewMode.LIST
                    } else {
                        libraryViewModeState.modeFor(selectedLibraryTab)
                    }

                    Column(
                    modifier = modifier
                        .fillMaxSize()
                        .animateContentSize()
                ) {
                    MusicScreenHeader(
                        title = when {
                            isSearchDestination -> "Search"
                            selectedLibraryTab == LibraryTab.QUEUE -> "Up Next"
                            else -> "Library"
                        },
                        onBackClick = null,
                        onSettingsClick = onSettingsClick,
                        modifier = Modifier.statusBarsPadding(),
                        viewModeAction = if (!isSearchDestination &&
                            !isLibraryDetail &&
                            selectedLibraryTab.viewCategory() != null
                        ) {
                            {
                                LibraryViewModeToggle(
                                    viewMode = selectedViewMode,
                                    onToggle = {
                                        libraryViewModeState.toggle(selectedLibraryTab)
                                    }
                                )
                            }
                        } else {
                            null
                        },
                        sortAction = {
                            LibrarySortAction(
                                selectedLibraryTab = selectedLibraryTab,
                                selectedArtistName = selectedArtistName,
                                selectedAlbumFolderPath = selectedAlbumFolderPath,
                                selectedSongSortOption = selectedSongSortOption,
                                selectedArtistSortOption = selectedArtistSortOption,
                                selectedAlbumSortOption = selectedAlbumSortOption,
                                selectedFavoriteSortOption = selectedFavoriteSortOption,
                                onSongSortOptionSelected = onSongSortOptionSelected,
                                onArtistSortOptionSelected = onArtistSortOptionSelected,
                                onAlbumSortOptionSelected = onAlbumSortOptionSelected,
                                onFavoriteSortOptionSelected = onFavoriteSortOptionSelected
                            )
                        }
                    )

                    if (!permissionGranted) {
                        Text(
                            text = "Audio and image permissions are needed to show your music.",
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        if (!isSearchDestination &&
                            !isLibraryDetail &&
                            selectedLibraryTab != LibraryTab.QUEUE
                        ) {
                            LibraryBrowseSwitcher(
                                selectedTab = selectedLibraryTab,
                                onTabSelected = { tab ->
                                    onOpenLibrary(tab)
                                },
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        LibrarySearchControl(
                            selectedLibraryTab = selectedLibraryTab,
                            isSearchVisible = isSearchDestination,
                            searchQuery = searchQuery,
                            onSearchQueryChange = onSearchQueryChange
                        )

                        MusicLibraryContent(
                            selectedLibraryTab = selectedLibraryTab,
                            songs = songs,
                            searchQuery = searchQuery,
                            selectedSongSortOption = selectedSongSortOption,
                            selectedArtistSortOption = selectedArtistSortOption,
                            selectedAlbumSortOption = selectedAlbumSortOption,
                            selectedFavoriteSortOption = selectedFavoriteSortOption,
                            viewMode = selectedViewMode,
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
                            onPlayNextSongsClick = { label, songsToAdd ->
                                queueSnackbarActions.playNextSongs(label, songsToAdd)
                            },
                            onAddSongsToQueueClick = { label, songsToAdd ->
                                queueSnackbarActions.addSongsToQueue(label, songsToAdd)
                            },
                            onToggleFavoriteClick = onToggleFavoriteClick,
                            onAddToPlaylistClick = onAddToPlaylistClick,
                            onArtistSelected = onArtistSelected,
                            onBackFromArtist = onBackFromArtist,
                            onAlbumSelected = onAlbumSelected,
                            onBackFromAlbum = onBackFromAlbum,
                            onBackFromQueue = onBackFromQueue,
                            onRemoveFromQueueClick = onRemoveFromQueueClick,
                            onMoveQueueItemUpClick = onMoveQueueItemUpClick,
                            onMoveQueueItemDownClick = onMoveQueueItemDownClick,
                            onClearQueueClick = onClearQueueClick,
                            onCreatePlaylistClick = onCreatePlaylistClick,
                            onRenamePlaylistClick = onRenamePlaylistClick,
                            onPlaylistClick = onPlaylistClick,
                            onDeletePlaylistClick = onDeletePlaylistClick,
                            onExportPlaylistClick = onExportPlaylistClick,
                            onImportPlaylistClick = onImportPlaylistClick,
                            onBackFromPlaylist = onBackFromPlaylist,
                            onRemovePlaylistSongClick = onRemovePlaylistSongClick,
                            onMovePlaylistSongUpClick = onMovePlaylistSongUpClick,
                            onMovePlaylistSongDownClick = onMovePlaylistSongDownClick,
                            onAddSongsToPlaylistClick = onAddSongsToPlaylistClick,
                            onEditSongTagsClick = onEditSongTagsClick,
                            recentlyPlayedSongs = recentlyPlayedSongs,
                            mostPlayedSongs = mostPlayedSongs,
                            bottomContentPadding = bottomContentPadding,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    }
                }
            }
        }
    }
}
