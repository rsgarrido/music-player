package com.example.cdplaya.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.cdplaya.ui.library.LibrarySortOption
import com.example.cdplaya.ui.library.LibraryTab
import com.example.cdplaya.ui.library.MusicLibraryContent
import com.example.cdplaya.ui.navigation.MainDestination
import com.example.cdplaya.ui.queue.QueueSnackbarActions
import com.example.cdplaya.ui.player.theme.PlayerThemeTokenField
import com.example.cdplaya.ui.player.theme.PlayerThemeTokens
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
    onHomeClick: () -> Unit,
    onOpenLibrary: (LibraryTab) -> Unit,
    onSearchClick: () -> Unit,
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
    selectedReplayGainMode: ReplayGainMode,
    onReplayGainModeSelected: (ReplayGainMode) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        isFolderScreenVisible -> {
            FolderSelectionScreen(
                libraryFolders = libraryFolders,
                selectedLibraryFolders = selectedLibraryFolders,
                onBackClick = onFolderBackClick,
                onFolderToggle = onLibraryFolderToggle,
                onSelectAllClick = onSelectAllLibraryFolders,
                onClearSelectionClick = onClearSelectedLibraryFolders,
                modifier = modifier.fillMaxSize()
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
                selectedReplayGainMode = selectedReplayGainMode,
                onReplayGainModeSelected = onReplayGainModeSelected,
                modifier = modifier.fillMaxSize()
            )
        }

        else -> {
            if (mainDestination == MainDestination.HOME) {
                HomeScreen(
                    permissionGranted = permissionGranted,
                    showContinueListening = currentSong != null && !isPlayerExpanded,
                    recentlyPlayedSongs = recentlyPlayedSongs,
                    favoriteSongCount = songs.count { song ->
                        song.favoriteKey() in favoriteSongKeys
                    },
                    onSettingsClick = onSettingsClick,
                    onOpenLibrary = onOpenLibrary,
                    onSearchClick = onSearchClick,
                    onRecentlyPlayedSongClick = { song ->
                        onSongClick(song, recentlyPlayedSongs)
                    },
                    modifier = modifier,
                    continueListeningContent = {
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
                            onExpandClick = onExpandPlayerClick,
                            onOpenUpNextClick = onMiniPlayerUpNextClick,
                            onToggleFavoriteClick = onToggleFavoriteClick,
                            isSleepTimerActive = isSleepTimerActive,
                            sleepTimerDisplayText = sleepTimerDisplayText,
                            onSleepTimerClick = onSleepTimerClick
                        )
                    }
                )
            } else {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .background(appShellBackgroundBrush())
                        .animateContentSize()
                ) {
                    MusicScreenHeader(
                        title = selectedLibraryTab.title,
                        onBackClick = onHomeClick,
                        onSettingsClick = onSettingsClick
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
                                onExpandClick = onExpandPlayerClick,
                                onOpenUpNextClick = onMiniPlayerUpNextClick,
                                onToggleFavoriteClick = onToggleFavoriteClick,
                                isSleepTimerActive = isSleepTimerActive,
                                sleepTimerDisplayText = sleepTimerDisplayText,
                                onSleepTimerClick = onSleepTimerClick
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
                            onSearchQueryChange = onSearchQueryChange,
                            onSongSortOptionSelected = onSongSortOptionSelected,
                            onArtistSortOptionSelected = onArtistSortOptionSelected,
                            onAlbumSortOptionSelected = onAlbumSortOptionSelected,
                            onFavoriteSortOptionSelected = onFavoriteSortOptionSelected
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
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
