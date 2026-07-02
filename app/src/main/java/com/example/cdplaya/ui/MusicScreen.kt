package com.example.cdplaya.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import android.net.Uri
import com.example.cdplaya.data.EditableSongTags
import com.example.cdplaya.data.LibraryFolder
import com.example.cdplaya.data.Song
import com.example.cdplaya.data.Playlist
import com.example.cdplaya.data.PlaylistSong
import com.example.cdplaya.data.TagEditorRepository
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
    onRenamePlaylistClick: (Playlist, String) -> Unit,
    onDeletePlaylistClick: (Playlist) -> Unit,
    onPlaylistSelected: (Playlist) -> Unit,
    onAddSongToPlaylistClick: (Playlist, Song) -> Unit,
    onAddSongsToPlaylistClick: (Playlist, List<Song>) -> Unit,
    onRemovePlaylistSongClick: (PlaylistSong) -> Unit,
    onTagsEdited: (Song, EditableSongTags) -> Unit
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
    var songsPendingPlaylistAdd by remember { mutableStateOf<List<Song>>(emptyList()) }
    var songPendingTagEdit by remember { mutableStateOf<Song?>(null) }

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

    BackHandler(
        enabled = songPendingTagEdit != null ||
                isExpandedUpNextSheetVisible ||
                isPlayerExpanded ||
                isFolderScreenVisible ||
                isSettingsScreenVisible ||
                selectedArtistName != null ||
                selectedAlbumFolderPath != null ||
                selectedLibraryTab == LibraryTab.QUEUE ||
                selectedPlaylistId != null
    ) {
        when {
            songPendingTagEdit != null -> {
                requestCloseTagEditor()
            }

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
        val selectedSongForTagEdit = songPendingTagEdit

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
                modifier = Modifier.fillMaxSize()
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
                onLibraryFolderToggle = onLibraryFolderToggle,
                onSelectAllLibraryFolders = onSelectAllLibraryFolders,
                onClearSelectedLibraryFolders = onClearSelectedLibraryFolders,
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
                },
                onExpandPlayerClick = {
                    isPlayerExpanded = true
                },
                onMiniPlayerUpNextClick = {
                    selectedLibraryTab = LibraryTab.QUEUE
                    selectedArtistName = null
                    selectedAlbumFolderPath = null
                    selectedPlaylistId = null
                },
                onSongClick = onSongClick,
                onPlaySongsClick = onPlaySongsClick,
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
                onBackFromPlaylist = {
                    selectedPlaylistId = null
                },
                onRemovePlaylistSongClick = { playlistSong ->
                    playlistSnackbarActions.removePlaylistSong(playlistSong)
                },
                onEditSongTagsClick = { song ->
                    isTagSaveInProgress = false
                    hasUnsavedTagChanges = false
                    isDiscardTagChangesDialogVisible = false
                    selectedArtworkUriForTagEdit = null
                    songPendingTagEdit = song
                },
                modifier = Modifier.fillMaxSize()
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
                songsPendingPlaylistAdd = songsPendingPlaylistAdd,
                onDismissBulkAddToPlaylistDialog = {
                    songsPendingPlaylistAdd = emptyList()
                },
                onAddSongToPlaylistClick = { playlist, song ->
                    playlistSnackbarActions.addSongToPlaylist(playlist, song)
                },
                onAddSongsToPlaylistClick = { playlist, songs ->
                    playlistSnackbarActions.addSongsToPlaylist(playlist, songs)
                }
            )
        }
    }
}