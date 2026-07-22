package com.example.cdplaya.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import android.net.Uri
import com.example.cdplaya.data.EditableSongTags
import com.example.cdplaya.data.LibraryFolder
import com.example.cdplaya.data.Song
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.data.Playlist
import com.example.cdplaya.data.PlaylistSong
import com.example.cdplaya.data.TagEditorRepository
import com.example.cdplaya.player.RepeatMode
import com.example.cdplaya.player.replaygain.ReplayGainMode
import com.example.cdplaya.ui.library.LibrarySortOption
import com.example.cdplaya.ui.library.LibraryTab
import com.example.cdplaya.ui.navigation.MainDestination
import com.example.cdplaya.ui.navigation.PlaybackLaunchContext
import com.example.cdplaya.ui.navigation.capturePlaybackLaunchContext
import com.example.cdplaya.ui.navigation.playbackLaunchContextSaver
import com.example.cdplaya.ui.navigation.withValidDetails
import com.example.cdplaya.ui.playlist.rememberPlaylistSnackbarActions
import com.example.cdplaya.ui.player.theme.PlayerThemeTokenField
import com.example.cdplaya.ui.player.theme.PlayerThemeTokens
import com.example.cdplaya.ui.player.modern.ModernArtworkTransitionStyle
import com.example.cdplaya.ui.player.modern.ModernSeekbarStyle
import com.example.cdplaya.ui.queue.rememberQueueSnackbarActions
import com.example.cdplaya.ui.tageditor.DiscardTagChangesDialog
import com.example.cdplaya.ui.tageditor.TagEditorScreen
import com.example.cdplaya.ui.tageditor.rememberTagEditorActions


@Composable
fun MusicScreen(
    songs: List<Song>,
    permissionGranted: Boolean,
    currentSong: Song?,
    previousPreviewSong: Song?,
    nextPreviewSong: Song?,
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
    onRenamePlaylistClick: (Playlist, String) -> Unit,
    onDeletePlaylistClick: (Playlist) -> Unit,
    onExportPlaylistClick: (Playlist) -> Unit,
    onImportPlaylistClick: () -> Unit,
    onExportBackupClick: () -> Unit,
    onRestoreBackupClick: () -> Unit,
    onPlaylistSelected: (Playlist) -> Unit,
    onAddSongToPlaylistClick: (Playlist, Song) -> Unit,
    onAddSongsToPlaylistClick: (Playlist, List<Song>) -> Unit,
    onRemovePlaylistSongClick: (PlaylistSong) -> Unit,
    onMovePlaylistSongUpClick: (PlaylistSong) -> Unit,
    onMovePlaylistSongDownClick: (PlaylistSong) -> Unit,
    onTagsEdited: (Song, EditableSongTags) -> Unit,
    isSleepTimerActive: Boolean,
    sleepTimerDisplayText: String,
    onStartSleepTimerClick: (Int) -> Unit,
    onCancelSleepTimerClick: () -> Unit,
    recentlyPlayedSongs: List<Song>,
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
    mostPlayedSongs: List<Song>
) {
    var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }
    var isFolderScreenVisible by rememberSaveable { mutableStateOf(false) }
    var mainDestination by rememberSaveable { mutableStateOf(MainDestination.HOME) }
    var selectedLibraryTab by rememberSaveable { mutableStateOf(LibraryTab.SONGS) }
    var playbackLaunchContext by rememberSaveable(
        stateSaver = playbackLaunchContextSaver
    ) {
        mutableStateOf<PlaybackLaunchContext>(PlaybackLaunchContext.Home)
    }
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
    var songsPendingPlaylistAdd by remember { mutableStateOf<List<Song>>(emptyList()) }
    var songPendingTagEdit by remember { mutableStateOf<Song?>(null) }
    var isSleepTimerDialogVisible by remember { mutableStateOf(false) }

    val tagEditorRepository = remember { TagEditorRepository() }
    var isTagSaveInProgress by remember { mutableStateOf(false) }
    var hasUnsavedTagChanges by remember { mutableStateOf(false) }
    var isDiscardTagChangesDialogVisible by remember { mutableStateOf(false) }
    var selectedArtworkUriForTagEdit by remember { mutableStateOf<Uri?>(null) }

    val tagEditorActions = rememberTagEditorActions(
        snackbarHostState = snackbarHostState,
        tagEditorRepository = tagEditorRepository,
        onTagsSaved = { originalSong, editedTags ->
            onTagsEdited(originalSong, editedTags)
        },
        onSavingChanged = { isSaving ->
            isTagSaveInProgress = isSaving
        },
        onCloseEditor = {
            songPendingTagEdit = null
            isTagSaveInProgress = false
            hasUnsavedTagChanges = false
            selectedArtworkUriForTagEdit = null
        }
    )

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

    val playlistSnackbarActions = rememberPlaylistSnackbarActions(
        snackbarHostState = snackbarHostState,
        onAddSongToPlaylistClick = onAddSongToPlaylistClick,
        onAddSongsToPlaylistClick = onAddSongsToPlaylistClick,
        onRemovePlaylistSongClick = onRemovePlaylistSongClick
    )

    val artworkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { selectedUri ->
        if (selectedUri != null) {
            selectedArtworkUriForTagEdit = selectedUri
        }
    }

    val recentlyAddedSongIds = queueSnackbarActions.recentlyAddedSongIds

    fun requestCloseTagEditor() {
        if (isTagSaveInProgress) {
            return
        }

        if (hasUnsavedTagChanges || selectedArtworkUriForTagEdit != null) {
            isDiscardTagChangesDialogVisible = true
        } else {
            songPendingTagEdit = null
            hasUnsavedTagChanges = false
            selectedArtworkUriForTagEdit = null
        }
    }

    fun recordPlaybackLaunchContext() {
        playbackLaunchContext = capturePlaybackLaunchContext(
            mainDestination = mainDestination,
            selectedLibraryTab = selectedLibraryTab,
            selectedAlbumFolderPath = selectedAlbumFolderPath,
            selectedArtistName = selectedArtistName,
            selectedPlaylistId = selectedPlaylistId,
            searchQuery = searchQuery
        )
    }

    fun restorePlaybackLaunchContext() {
        val validContext = playbackLaunchContext.withValidDetails(
            albumFolderPaths = songs.mapTo(mutableSetOf()) { song -> song.folderPath },
            artistNames = songs.mapTo(mutableSetOf()) { song ->
                song.artist.ifBlank { "Unknown Artist" }
            },
            playlistIds = playlists.mapTo(mutableSetOf()) { playlist -> playlist.playlistId }
        )

        isPlayerExpanded = false
        selectedArtistName = null
        selectedAlbumFolderPath = null
        selectedPlaylistId = null

        when (validContext) {
            PlaybackLaunchContext.Home -> {
                mainDestination = MainDestination.HOME
            }

            is PlaybackLaunchContext.LibrarySection -> {
                selectedLibraryTab = validContext.tab
                searchQuery = ""
                mainDestination = MainDestination.LIBRARY
            }

            is PlaybackLaunchContext.AlbumDetail -> {
                selectedLibraryTab = LibraryTab.ALBUMS
                selectedAlbumFolderPath = validContext.folderPath
                searchQuery = ""
                mainDestination = MainDestination.LIBRARY
            }

            is PlaybackLaunchContext.ArtistDetail -> {
                selectedLibraryTab = LibraryTab.ARTISTS
                selectedArtistName = validContext.artistName
                searchQuery = ""
                mainDestination = MainDestination.LIBRARY
            }

            is PlaybackLaunchContext.PlaylistDetail -> {
                selectedLibraryTab = LibraryTab.PLAYLISTS
                playlists.firstOrNull { playlist ->
                    playlist.playlistId == validContext.playlistId
                }?.let { playlist ->
                    selectedPlaylistId = playlist.playlistId
                    onPlaylistSelected(playlist)
                }
                searchQuery = ""
                mainDestination = MainDestination.LIBRARY
            }

            is PlaybackLaunchContext.Search -> {
                selectedLibraryTab = LibraryTab.SONGS
                searchQuery = validContext.query
                mainDestination = MainDestination.SEARCH
            }
        }
    }

    BackHandler(
        enabled = songPendingTagEdit != null ||
                isExpandedUpNextSheetVisible ||
                isPlayerExpanded ||
                isFolderScreenVisible ||
                isSettingsScreenVisible ||
                selectedArtistName != null ||
                selectedAlbumFolderPath != null ||
                selectedPlaylistId != null ||
                mainDestination != MainDestination.HOME
    ) {
        when {
            songPendingTagEdit != null -> {
                requestCloseTagEditor()
            }

            isExpandedUpNextSheetVisible -> {
                isExpandedUpNextSheetVisible = false
            }

            isPlayerExpanded -> {
                restorePlaybackLaunchContext()
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

            mainDestination != MainDestination.HOME -> {
                mainDestination = MainDestination.HOME
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .appShellBackground()
    ) {
        val selectedSongForTagEdit = songPendingTagEdit
        val shouldShowBottomMiniPlayer = currentSong != null &&
                !isPlayerExpanded &&
                !isFolderScreenVisible &&
                !isSettingsScreenVisible &&
                selectedSongForTagEdit == null
        val shouldShowBottomNavigation = !isPlayerExpanded &&
                !isFolderScreenVisible &&
                !isSettingsScreenVisible &&
                selectedSongForTagEdit == null
        val navigationBarInset = WindowInsets.navigationBars
            .asPaddingValues()
            .calculateBottomPadding()
        val bottomContentPadding = navigationBarInset +
                (if (shouldShowBottomNavigation) AppBottomNavigationHeight else 0.dp) +
                when {
                    !shouldShowBottomMiniPlayer -> 24.dp
                    isSleepTimerActive -> 176.dp
                    else -> 96.dp
                }

        if (selectedSongForTagEdit != null) {
            val initialEditableTags = remember(
                selectedSongForTagEdit.id,
                selectedSongForTagEdit.filePath
            ) {
                tagEditorRepository.readTags(selectedSongForTagEdit)
            }

            val unsupportedTagEditingMessage = remember(
                selectedSongForTagEdit.id,
                selectedSongForTagEdit.filePath
            ) {
                tagEditorRepository.getUnsupportedEditingMessage(selectedSongForTagEdit)
            }

            TagEditorScreen(
                song = selectedSongForTagEdit,
                initialTags = initialEditableTags,
                isSaving = isTagSaveInProgress,
                unsupportedMessage = unsupportedTagEditingMessage,
                isCurrentSong = currentSong?.id == selectedSongForTagEdit.id,
                selectedArtworkUri = selectedArtworkUriForTagEdit,
                onChangeArtworkClick = {
                    artworkPickerLauncher.launch("image/*")
                },
                onBackClick = {
                    requestCloseTagEditor()
                },
                onSaveClick = { editedTags ->
                    tagEditorActions.saveTags(
                        selectedSongForTagEdit,
                        editedTags,
                        selectedArtworkUriForTagEdit
                    )
                },
                onUnsavedChangesChanged = { hasChanges ->
                    hasUnsavedTagChanges = hasChanges
                },
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            )
        } else {
            MusicScreenBody(
                songs = songs,
                permissionGranted = permissionGranted,
                currentSong = currentSong,
                isPlaying = isPlaying,
                isShuffleEnabled = isShuffleEnabled,
                repeatMode = repeatMode,
                currentPosition = currentPosition,
                duration = duration,
                queuedSongs = queuedSongs,
                upcomingSongs = upcomingSongs,
                libraryFolders = libraryFolders,
                selectedLibraryFolders = selectedLibraryFolders,
                favoriteSongKeys = favoriteSongKeys,
                playlists = playlists,
                selectedPlaylistName = selectedPlaylistName,
                selectedPlaylistSongs = selectedPlaylistSongs,
                mainDestination = mainDestination,
                selectedLibraryTab = selectedLibraryTab,
                selectedArtistName = selectedArtistName,
                selectedAlbumFolderPath = selectedAlbumFolderPath,
                selectedPlaylistId = selectedPlaylistId,
                searchQuery = searchQuery,
                selectedSongSortOption = selectedSongSortOption,
                selectedArtistSortOption = selectedArtistSortOption,
                selectedAlbumSortOption = selectedAlbumSortOption,
                selectedFavoriteSortOption = selectedFavoriteSortOption,
                recentlyAddedSongIds = recentlyAddedSongIds,
                isPlayerExpanded = isPlayerExpanded,
                isFolderScreenVisible = isFolderScreenVisible,
                isSettingsScreenVisible = isSettingsScreenVisible,
                queueSnackbarActions = queueSnackbarActions,
                onSettingsClick = {
                    isSettingsScreenVisible = true
                },
                onOpenLibrary = { tab ->
                    selectedLibraryTab = tab
                    selectedArtistName = null
                    selectedAlbumFolderPath = null
                    selectedPlaylistId = null
                    searchQuery = ""
                    mainDestination = MainDestination.LIBRARY
                },
                onFolderBackClick = {
                    isFolderScreenVisible = false
                    isSettingsScreenVisible = true
                },
                onSettingsBackClick = {
                    isSettingsScreenVisible = false
                },
                onLibraryFoldersClick = {
                    isSettingsScreenVisible = false
                    isFolderScreenVisible = true
                },
                onExportBackupClick = onExportBackupClick,
                onRestoreBackupClick = onRestoreBackupClick,
                onLibraryFolderToggle = onLibraryFolderToggle,
                onSelectAllLibraryFolders = onSelectAllLibraryFolders,
                onClearSelectedLibraryFolders = onClearSelectedLibraryFolders,
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
                },
                onExpandPlayerClick = {
                    isPlayerExpanded = true
                },
                onMiniPlayerUpNextClick = {
                    selectedLibraryTab = LibraryTab.QUEUE
                    selectedArtistName = null
                    selectedAlbumFolderPath = null
                    selectedPlaylistId = null
                    mainDestination = MainDestination.LIBRARY
                },
                onSongClick = { song, playbackContext ->
                    recordPlaybackLaunchContext()
                    onSongClick(song, playbackContext)
                },
                onPlaySongsClick = { playbackContext, shuffle ->
                    recordPlaybackLaunchContext()
                    onPlaySongsClick(playbackContext, shuffle)
                },
                onPlayPauseClick = onPlayPauseClick,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onSeekChange = onSeekChange,
                onShuffleClick = onShuffleClick,
                onRepeatClick = onRepeatClick,
                onToggleFavoriteClick = onToggleFavoriteClick,
                onAddToPlaylistClick = { song ->
                    songPendingPlaylistAdd = song
                },
                onAddSongsToPlaylistClick = { songs ->
                    songsPendingPlaylistAdd = songs
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
                    mainDestination = MainDestination.LIBRARY
                },
                onRemoveFromQueueClick = onRemoveFromQueueClick,
                onMoveQueueItemUpClick = onMoveQueueItemUpClick,
                onMoveQueueItemDownClick = onMoveQueueItemDownClick,
                onClearQueueClick = onClearQueueClick,
                onCreatePlaylistClick = {
                    isCreatePlaylistDialogVisible = true
                },
                onRenamePlaylistClick = onRenamePlaylistClick,
                onPlaylistClick = { playlist ->
                    selectedPlaylistId = playlist.playlistId
                    onPlaylistSelected(playlist)
                },
                onDeletePlaylistClick = onDeletePlaylistClick,
                onExportPlaylistClick = onExportPlaylistClick,
                onImportPlaylistClick = onImportPlaylistClick,
                onBackFromPlaylist = {
                    selectedPlaylistId = null
                },
                onRemovePlaylistSongClick = { playlistSong ->
                    playlistSnackbarActions.removePlaylistSong(playlistSong)
                },
                onMovePlaylistSongUpClick = onMovePlaylistSongUpClick,
                onMovePlaylistSongDownClick = onMovePlaylistSongDownClick,
                onEditSongTagsClick = { song ->
                    isTagSaveInProgress = false
                    hasUnsavedTagChanges = false
                    isDiscardTagChangesDialogVisible = false
                    selectedArtworkUriForTagEdit = null
                    songPendingTagEdit = song
                },
                isSleepTimerActive = isSleepTimerActive,
                sleepTimerDisplayText = sleepTimerDisplayText,
                onSleepTimerClick = {
                    isSleepTimerDialogVisible = true
                },
                recentlyPlayedSongs = recentlyPlayedSongs,
                mostPlayedSongs = mostPlayedSongs,
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
                bottomContentPadding = bottomContentPadding,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (shouldShowBottomMiniPlayer) {
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
                    mainDestination = MainDestination.LIBRARY
                },
                onToggleFavoriteClick = onToggleFavoriteClick,
                isSleepTimerActive = isSleepTimerActive,
                sleepTimerDisplayText = sleepTimerDisplayText,
                onSleepTimerClick = {
                    isSleepTimerDialogVisible = true
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = AppBottomNavigationHeight)
            )
        }

        if (shouldShowBottomNavigation) {
            AppBottomNavigation(
                selectedDestination = mainDestination,
                onDestinationSelected = { destination ->
                    selectedArtistName = null
                    selectedAlbumFolderPath = null
                    selectedPlaylistId = null
                    if (destination == MainDestination.SEARCH) {
                        selectedLibraryTab = LibraryTab.SONGS
                    }
                    if (destination != MainDestination.SEARCH) {
                        searchQuery = ""
                    }
                    mainDestination = destination
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }

        if (isDiscardTagChangesDialogVisible) {
            DiscardTagChangesDialog(
                onDismiss = {
                    isDiscardTagChangesDialogVisible = false
                },
                onConfirmDiscardClick = {
                    isDiscardTagChangesDialogVisible = false
                    hasUnsavedTagChanges = false
                    selectedArtworkUriForTagEdit = null
                    songPendingTagEdit = null
                }
            )
        }

        if (selectedSongForTagEdit == null) {
            MusicScreenOverlays(
                isPlayerExpanded = isPlayerExpanded,
                currentSong = currentSong,
                previousPreviewSong = previousPreviewSong,
                nextPreviewSong = nextPreviewSong,
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
                    restorePlaybackLaunchContext()
                },
                onShowExpandedUpNextSheet = {
                    isExpandedUpNextSheetVisible = true
                },
                onShowExpandedSleepTimer = {
                    isSleepTimerDialogVisible = true
                },
                onShowExpandedMore = {
                    restorePlaybackLaunchContext()
                    isSettingsScreenVisible = true
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
                songsPendingPlaylistAdd = songsPendingPlaylistAdd,
                onDismissBulkAddToPlaylistDialog = {
                    songsPendingPlaylistAdd = emptyList()
                },
                onAddSongToPlaylistClick = { playlist, song ->
                    playlistSnackbarActions.addSongToPlaylist(playlist, song)
                },
                onAddSongsToPlaylistClick = { playlist, songs ->
                    playlistSnackbarActions.addSongsToPlaylist(playlist, songs)
                },
                isSleepTimerDialogVisible = isSleepTimerDialogVisible,
                isSleepTimerActive = isSleepTimerActive,
                sleepTimerDisplayText = sleepTimerDisplayText,
                onStartSleepTimerClick = onStartSleepTimerClick,
                onCancelSleepTimerClick = onCancelSleepTimerClick,
                onDismissSleepTimerDialog = {
                    isSleepTimerDialogVisible = false
                },
                selectedPlayerTheme = selectedPlayerTheme,
                selectedPlayerThemeTokens = selectedPlayerThemeTokens,
                selectedModernArtworkTransitionStyle = selectedModernArtworkTransitionStyle,
                selectedModernSeekbarStyle = selectedModernSeekbarStyle,
                songs = songs,
                onSongClick = onSongClick
            )
        }
    }
}
