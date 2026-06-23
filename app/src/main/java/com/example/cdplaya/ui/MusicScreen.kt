package com.example.cdplaya.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.cdplaya.data.LibraryFolder
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.RepeatMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    onClearSelectedLibraryFolders: () -> Unit
) {
    var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }
    var isFolderScreenVisible by rememberSaveable { mutableStateOf(false) }
    var selectedLibraryTab by rememberSaveable { mutableStateOf(LibraryTab.SONGS) }
    var isSettingsScreenVisible by rememberSaveable { mutableStateOf(false) }
    var selectedArtistName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAlbumFolderPath by rememberSaveable { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedSongSortOption by rememberSaveable { mutableStateOf(LibrarySortOption.TITLE) }
    var selectedArtistSortOption by rememberSaveable { mutableStateOf(LibrarySortOption.NAME) }
    var selectedAlbumSortOption by rememberSaveable { mutableStateOf(LibrarySortOption.TITLE) }

    val coroutineScope = rememberCoroutineScope()
    var recentlyAddedSongIds by remember { mutableStateOf(setOf<Long>()) }

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

        if (isFolderScreenVisible) {
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
                modifier = Modifier.weight(1f)
            )
        } else if (isSettingsScreenVisible) {
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
                modifier = Modifier.weight(1f)
            )
        } else if (!permissionGranted) {
            Text(
                text = "Audio and image permissions are needed to show your music.",
                modifier = Modifier.padding(16.dp)
            )
        } else if (!isPlayerExpanded) {
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
                    isPlayerExpanded = false
                }
            )
        }

            LibraryTabs(
                selectedTab = selectedLibraryTab,
                onTabSelected = { tab ->
                    selectedLibraryTab = tab
                    selectedArtistName = null
                    selectedAlbumFolderPath = null
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
                        selectedLibraryTab == LibraryTab.ARTISTS && selectedArtistName == null ||
                        selectedLibraryTab == LibraryTab.ALBUMS && selectedAlbumFolderPath == null

            if (shouldShowSortDropdown) {
                val selectedSortOption = when (selectedLibraryTab) {
                    LibraryTab.SONGS -> selectedSongSortOption
                    LibraryTab.ARTISTS -> selectedArtistSortOption
                    LibraryTab.ALBUMS -> selectedAlbumSortOption
                    LibraryTab.QUEUE -> selectedSongSortOption
                }

                val availableSortOptions = when (selectedLibraryTab) {
                    LibraryTab.SONGS -> listOf(
                        LibrarySortOption.TITLE,
                        LibrarySortOption.ARTIST,
                        LibrarySortOption.ALBUM
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
                }

                LibrarySortDropdown(
                    selectedOption = selectedSortOption,
                    options = availableSortOptions,
                    onOptionSelected = { option ->
                        when (selectedLibraryTab) {
                            LibraryTab.SONGS -> {
                                selectedSongSortOption = option
                            }

                            LibraryTab.ARTISTS -> {
                                selectedArtistSortOption = option
                            }

                            LibraryTab.ALBUMS -> {
                                selectedAlbumSortOption = option
                            }

                            LibraryTab.QUEUE -> Unit
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
                    selectedLibraryTab = LibraryTab.QUEUE
                    selectedArtistName = null
                    selectedAlbumFolderPath = null
                    isPlayerExpanded = false
                }
            )
        }
}

@Composable
private fun SongsTabContent(
    songs: List<Song>,
    searchQuery: String,
    sortOption: LibrarySortOption,
    currentSong: Song?,
    recentlyAddedSongIds: Set<Long>,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayNextClick: (Song) -> Unit,
    onAddToQueueClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredSongs = filterSongsForSearch(
        songs = songs,
        searchQuery = searchQuery
    )

    val displayedSongs = sortSongsForLibrary(
        songs = filteredSongs,
        sortOption = sortOption
    )

    if (songs.isEmpty()) {
        Text(
            text = "No songs found.",
            modifier = Modifier.padding(16.dp)
        )
    } else if (filteredSongs.isEmpty()) {
        Text(
            text = "No songs match your search.",
            modifier = Modifier.padding(16.dp)
        )
    } else {
        SongList(
            songs = displayedSongs,
            currentSongId = currentSong?.id,
            recentlyAddedSongIds = recentlyAddedSongIds,
            onSongClick = onSongClick,
            onPlayNextClick = onPlayNextClick,
            onAddToQueueClick = onAddToQueueClick,
            modifier = modifier
        )
    }
}

@Composable
private fun ArtistsTabContent(
    songs: List<Song>,
    searchQuery: String,
    selectedArtistName: String?,
    currentSong: Song?,
    sortOption: LibrarySortOption,
    recentlyAddedSongIds: Set<Long>,
    onArtistSelected: (String) -> Unit,
    onBackFromArtist: () -> Unit,
    onPlaySongsClick: (List<Song>, Boolean) -> Unit,
    onPlayNextClick: (Song) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onAddToQueueClick: (Song) -> Unit,
    onPlayNextSongsClick: (String, List<Song>) -> Unit,
    onAddSongsToQueueClick: (String, List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    val artistSearchSongs = filterSongsByArtistSearch(
        songs = songs,
        searchQuery = searchQuery
    )

    if (songs.isEmpty()) {
        Text(
            text = "No artists found.",
            modifier = Modifier.padding(16.dp)
        )
    } else if (selectedArtistName == null) {
        if (artistSearchSongs.isEmpty()) {
            Text(
                text = "No artists match your search.",
                modifier = Modifier.padding(16.dp)
            )
        } else {
            ArtistListScreen(
                songs = artistSearchSongs,
                onArtistClick = onArtistSelected,
                sortOption = sortOption,
                onArtistPlayClick = { _, artistSongs ->
                    onPlaySongsClick(artistSongs, false)
                },
                onArtistShuffleClick = { _, artistSongs ->
                    onPlaySongsClick(artistSongs, true)
                },
                onArtistPlayNextClick = { artistName, artistSongs ->
                    onPlayNextSongsClick(artistName, artistSongs)
                },
                onArtistAddToQueueClick = { artistName, artistSongs ->
                    onAddSongsToQueueClick(artistName, artistSongs)
                },
                modifier = modifier
            )
        }
    } else {
        val artistSongs = sortSongsForArtistDetail(
            songs.filter { song ->
                song.artist.ifBlank { "Unknown Artist" } == selectedArtistName
            }
        )
            .sortedWith(
                compareBy<Song> { song ->
                    song.album.lowercase()
                }.thenBy { song ->
                    if (song.trackNumber > 0) song.trackNumber else Int.MAX_VALUE
                }.thenBy { song ->
                    song.title.lowercase()
                }
            )

        val displayedArtistSongs = filterSongsForSearch(
            songs = artistSongs,
            searchQuery = searchQuery
        )

        val subtitle = if (searchQuery.isBlank()) {
            "${artistSongs.size} song(s)"
        } else {
            "${displayedArtistSongs.size} of ${artistSongs.size} song(s)"
        }

        SongGroupDetailScreen(
            title = selectedArtistName,
            subtitle = subtitle,
            artworkUri = artistSongs.firstOrNull()?.albumArtUri,
            songs = displayedArtistSongs,
            currentSongId = currentSong?.id,
            recentlyAddedSongIds = recentlyAddedSongIds,
            showAlbumName = true,
            showTrackNumbers = false,
            onBackClick = onBackFromArtist,
            onPlayAllClick = {
                onPlaySongsClick(displayedArtistSongs, false)
            },
            onShuffleAllClick = {
                onPlaySongsClick(displayedArtistSongs, true)
            },
            onSongClick = onSongClick,
            onPlayNextClick = onPlayNextClick,
            onAddToQueueClick = onAddToQueueClick,
            modifier = modifier
        )
    }
}

@Composable
private fun AlbumsTabContent(
    songs: List<Song>,
    searchQuery: String,
    selectedAlbumFolderPath: String?,
    currentSong: Song?,
    sortOption: LibrarySortOption,
    recentlyAddedSongIds: Set<Long>,
    onAlbumSelected: (String) -> Unit,
    onBackFromAlbum: () -> Unit,
    onPlaySongsClick: (List<Song>, Boolean) -> Unit,
    onPlayNextClick: (Song) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onAddToQueueClick: (Song) -> Unit,
    onPlayNextSongsClick: (String, List<Song>) -> Unit,
    onAddSongsToQueueClick: (String, List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    val albumSearchSongs = filterSongsByAlbumSearch(
        songs = songs,
        searchQuery = searchQuery
    )

    if (songs.isEmpty()) {
        Text(
            text = "No albums found.",
            modifier = Modifier.padding(16.dp)
        )
    } else if (selectedAlbumFolderPath == null) {
        if (albumSearchSongs.isEmpty()) {
            Text(
                text = "No albums match your search.",
                modifier = Modifier.padding(16.dp)
            )
        } else {
            AlbumListScreen(
                songs = albumSearchSongs,
                onAlbumClick = onAlbumSelected,
                sortOption = sortOption,
                onAlbumPlayClick = { _, albumSongs ->
                    onPlaySongsClick(albumSongs, false)
                },
                onAlbumShuffleClick = { _, albumSongs ->
                    onPlaySongsClick(albumSongs, true)
                },
                onAlbumPlayNextClick = { albumTitle, albumSongs ->
                    onPlayNextSongsClick(albumTitle, albumSongs)
                },
                onAlbumAddToQueueClick = { albumTitle, albumSongs ->
                    onAddSongsToQueueClick(albumTitle, albumSongs)
                },
                modifier = modifier
            )
        }
    } else {
        val albumSongs = sortSongsByAlbumOrder(
            songs.filter { song ->
                song.folderPath == selectedAlbumFolderPath
            }
        )

        val displayedAlbumSongs = filterSongsForSearch(
            songs = albumSongs,
            searchQuery = searchQuery
        )

        val firstSong = albumSongs.firstOrNull()

        val subtitle = if (searchQuery.isBlank()) {
            "${firstSong?.artist ?: "Unknown Artist"} • ${albumSongs.size} song(s)"
        } else {
            "${firstSong?.artist ?: "Unknown Artist"} • ${displayedAlbumSongs.size} of ${albumSongs.size} song(s)"
        }

        SongGroupDetailScreen(
            title = firstSong?.album?.ifBlank { "Unknown Album" } ?: "Album",
            subtitle = subtitle,
            artworkUri = firstSong?.albumArtUri,
            songs = displayedAlbumSongs,
            currentSongId = currentSong?.id,
            recentlyAddedSongIds = recentlyAddedSongIds,
            showAlbumName = false,
            showTrackNumbers = true,
            onBackClick = onBackFromAlbum,
            onPlayAllClick = {
                onPlaySongsClick(displayedAlbumSongs, false)
            },
            onShuffleAllClick = {
                onPlaySongsClick(displayedAlbumSongs, true)
            },
            onSongClick = onSongClick,
            onPlayNextClick = onPlayNextClick,
            onAddToQueueClick = onAddToQueueClick,
            modifier = modifier
        )
    }
}