package com.example.cdplaya.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.LibraryFolder
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.data.Playlist
import com.example.cdplaya.data.PlaylistSong
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.RepeatMode
import com.example.cdplaya.ui.library.FolderSelectionScreen
import com.example.cdplaya.ui.library.LibrarySortOption
import com.example.cdplaya.ui.library.LibraryTab
import com.example.cdplaya.ui.library.MusicLibraryContent
import com.example.cdplaya.ui.queue.QueueSnackbarActions

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
    onFolderBackClick: () -> Unit,
    onSettingsBackClick: () -> Unit,
    onLibraryFoldersClick: () -> Unit,
    onLibraryFolderToggle: (String) -> Unit,
    onSelectAllLibraryFolders: () -> Unit,
    onClearSelectedLibraryFolders: () -> Unit,
    onTabSelected: (LibraryTab) -> Unit,
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
    onPlayerThemeSelected: (PlayerTheme) -> Unit,
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
                isSleepTimerActive = isSleepTimerActive,
                sleepTimerDisplayText = sleepTimerDisplayText,
                onSleepTimerClick = onSleepTimerClick,
                selectedPlayerTheme = selectedPlayerTheme,
                onPlayerThemeSelected = onPlayerThemeSelected,
                modifier = modifier.fillMaxSize()
            )
        }

        else -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .animateContentSize()
            ) {
                MusicScreenHeader(
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
                        onTabSelected = onTabSelected,
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