package com.example.cdplaya.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cdplaya.data.LibraryFolder
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.RepeatMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

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
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekChange: (Int) -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    queuedSongs: List<Song>,
    onAddToQueueClick: (Song) -> Unit,
    onRemoveFromQueueClick: (Int) -> Unit,
    onMoveQueueItemUpClick: (Int) -> Unit,
    onMoveQueueItemDownClick: (Int) -> Unit,
    libraryFolders: List<LibraryFolder>,
    selectedLibraryFolders: Set<String>,
    onLibraryFolderToggle: (String) -> Unit,
    onSelectAllLibraryFolders: () -> Unit,
    onClearSelectedLibraryFolders: () -> Unit
) {
    var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }
    var isFolderScreenVisible by rememberSaveable { mutableStateOf(false) }
    var selectedLibraryTab by rememberSaveable { mutableStateOf(LibraryTab.SONGS) }
    var selectedArtistName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAlbumFolderPath by rememberSaveable { mutableStateOf<String?>(null) }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .animateContentSize()
    ) {
        Text(
            text = "CDPlaya",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        Row(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Button(
                onClick = {
                    isFolderScreenVisible = true
                }
            ) {
                Text(text = "Folders")
            }
        }


        if (isFolderScreenVisible) {
            FolderSelectionScreen(
                libraryFolders = libraryFolders,
                selectedLibraryFolders = selectedLibraryFolders,
                onBackClick = {
                    isFolderScreenVisible = false
                },
                onFolderToggle = onLibraryFolderToggle,
                onSelectAllClick = onSelectAllLibraryFolders,
                onClearSelectionClick = onClearSelectedLibraryFolders,
                modifier = Modifier.weight(1f)
            )
        } else if (!permissionGranted) {
            Text(
                text = "Audio and image permissions are needed to show your music.",
                modifier = Modifier.padding(16.dp)
            )
        } else {
            PlayerCard(
                currentSong = currentSong,
                isPlaying = isPlaying,
                isExpanded = isPlayerExpanded,
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
                }
            )

            LibraryTabs(
                selectedTab = selectedLibraryTab,
                onTabSelected = { tab ->
                    selectedLibraryTab = tab
                    selectedArtistName = null
                    selectedAlbumFolderPath = null
                }
            )

            when (selectedLibraryTab) {
                LibraryTab.SONGS -> {
                    if (songs.isEmpty()) {
                        Text(
                            text = "No songs found.",
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        SongList(
                            songs = songs,
                            currentSongId = currentSong?.id,
                            recentlyAddedSongIds = recentlyAddedSongIds,
                            onSongClick = onSongClick,
                            onAddToQueueClick = { song ->
                                handleAddToQueue(song)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                LibraryTab.ARTISTS -> {
                    if (songs.isEmpty()) {
                        Text(
                            text = "No artists found.",
                            modifier = Modifier.padding(16.dp)
                        )
                    } else if (selectedArtistName == null) {
                        ArtistListScreen(
                            songs = songs,
                            onArtistClick = { artistName ->
                                selectedArtistName = artistName
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        val artistSongs = songs
                            .filter { song ->
                                song.artist.ifBlank { "Unknown Artist" } == selectedArtistName
                            }
                            .sortedWith(
                                compareBy<Song> { song ->
                                    song.album.lowercase()
                                }.thenBy { song ->
                                    if (song.trackNumber > 0) song.trackNumber else Int.MAX_VALUE
                                }.thenBy { song ->
                                    song.title.lowercase()
                                }
                            )

                        SongGroupDetailScreen(
                            title = selectedArtistName ?: "Artist",
                            subtitle = "${artistSongs.size} song(s)",
                            songs = artistSongs,
                            currentSongId = currentSong?.id,
                            recentlyAddedSongIds = recentlyAddedSongIds,
                            onBackClick = {
                                selectedArtistName = null
                            },
                            onSongClick = onSongClick,
                            onAddToQueueClick = { song ->
                                handleAddToQueue(song)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                LibraryTab.ALBUMS -> {
                    if (songs.isEmpty()) {
                        Text(
                            text = "No albums found.",
                            modifier = Modifier.padding(16.dp)
                        )
                    } else if (selectedAlbumFolderPath == null) {
                        AlbumListScreen(
                            songs = songs,
                            onAlbumClick = { albumFolderPath ->
                                selectedAlbumFolderPath = albumFolderPath
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        val albumSongs = sortSongsByAlbumOrder(
                            songs.filter { song ->
                                song.folderPath == selectedAlbumFolderPath
                            }
                        )

                        val firstSong = albumSongs.firstOrNull()

                        SongGroupDetailScreen(
                            title = firstSong?.album?.ifBlank { "Unknown Album" } ?: "Album",
                            subtitle = firstSong?.artist ?: "${albumSongs.size} song(s)",
                            songs = albumSongs,
                            currentSongId = currentSong?.id,
                            recentlyAddedSongIds = recentlyAddedSongIds,
                            onBackClick = {
                                selectedAlbumFolderPath = null
                            },
                            onSongClick = onSongClick,
                            onAddToQueueClick = { song ->
                                handleAddToQueue(song)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                LibraryTab.QUEUE -> {
                    QueueScreen(
                        queuedSongs = queuedSongs,
                        onBackClick = {
                            selectedLibraryTab = LibraryTab.SONGS
                        },
                        onRemoveFromQueueClick = onRemoveFromQueueClick,
                        onMoveQueueItemUpClick = onMoveQueueItemUpClick,
                        onMoveQueueItemDownClick = onMoveQueueItemDownClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryTabs(
    selectedTab: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit
) {
    val tabs = LibraryTab.entries

    TabRow(
        selectedTabIndex = tabs.indexOf(selectedTab),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = {
                    onTabSelected(tab)
                },
                text = {
                    Text(text = tab.title)
                }
            )
        }
    }
}

@Composable
fun PlayerCard(
    currentSong: Song?,
    isPlaying: Boolean,
    isExpanded: Boolean,
    currentPosition: Int,
    duration: Int,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekChange: (Int) -> Unit,
    onExpandClick: () -> Unit,
    onCollapseClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit
) {
    if (currentSong == null) {
        return
    }

    val albumArtSize by animateDpAsState(
        targetValue = if (isExpanded) 180.dp else 56.dp,
        animationSpec = tween(durationMillis = 300),
        label = "albumArtSize"
    )

    val cardCornerSize by animateDpAsState(
        targetValue = if (isExpanded) 24.dp else 16.dp,
        animationSpec = tween(durationMillis = 300),
        label = "cardCornerSize"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize(
                animationSpec = tween(durationMillis = 300)
            )
            .playerSwipeGestures(
                onSwipeDown = onExpandClick,
                onSwipeUp = onCollapseClick,
                onSwipeLeft = onNextClick,
                onSwipeRight = onPreviousClick
            ),
        shape = RoundedCornerShape(cardCornerSize),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        if (isExpanded) {
            ExpandedPlayerContent(
                currentSong = currentSong,
                isPlaying = isPlaying,
                albumArtSize = albumArtSize,
                currentPosition = currentPosition,
                duration = duration,
                isShuffleEnabled = isShuffleEnabled,
                repeatMode = repeatMode,
                onShuffleClick = onShuffleClick,
                onRepeatClick = onRepeatClick,
                onPlayPauseClick = onPlayPauseClick,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onSeekChange = onSeekChange,
                onCollapseClick = onCollapseClick
            )
        } else {
            MiniPlayerContent(
                currentSong = currentSong,
                isPlaying = isPlaying,
                albumArtSize = albumArtSize,
                onPlayPauseClick = onPlayPauseClick,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onExpandClick = onExpandClick
            )
        }
    }
}

@Composable
fun MiniPlayerContent(
    currentSong: Song,
    isPlaying: Boolean,
    albumArtSize: Dp,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onExpandClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = currentSong.albumArtUri,
            contentDescription = "Album art for ${currentSong.title}",
            modifier = Modifier
                .size(albumArtSize)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop,
            error = painterResource(android.R.drawable.ic_media_play),
            placeholder = painterResource(android.R.drawable.ic_media_play)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = currentSong.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )

            Text(
                text = currentSong.artist,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }

        IconButton(onClick = onPreviousClick) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous song"
            )
        }

        IconButton(onClick = onPlayPauseClick) {
            Icon(
                imageVector = if (isPlaying) {
                    Icons.Filled.Pause
                } else {
                    Icons.Filled.PlayArrow
                },
                contentDescription = if (isPlaying) "Pause" else "Play"
            )
        }

        IconButton(onClick = onNextClick) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next song"
            )
        }

        IconButton(onClick = onExpandClick) {
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = "Expand player"
            )
        }
    }
}

@Composable
fun ExpandedPlayerContent(
    currentSong: Song,
    isPlaying: Boolean,
    albumArtSize: Dp,
    currentPosition: Int,
    duration: Int,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekChange: (Int) -> Unit,
    onCollapseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = currentSong.albumArtUri,
            contentDescription = "Album art for ${currentSong.title}",
            modifier = Modifier
                .size(albumArtSize)
                .clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Crop,
            error = painterResource(android.R.drawable.ic_media_play),
            placeholder = painterResource(android.R.drawable.ic_media_play)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = currentSong.title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = currentSong.artist,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(12.dp))

        Slider(
            value = currentPosition.toFloat(),
            onValueChange = { newPosition ->
                onSeekChange(newPosition.toInt())
            },
            valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = formatDuration(currentPosition))
            Text(text = formatDuration(duration))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onShuffleClick) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = if (isShuffleEnabled) "Shuffle on" else "Shuffle off",
                    tint = if (isShuffleEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onPreviousClick) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous song"
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onPlayPauseClick) {
                Icon(
                    imageVector = if (isPlaying) {
                        Icons.Filled.Pause
                    } else {
                        Icons.Filled.PlayArrow
                    },
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onNextClick) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next song"
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onRepeatClick) {
                Icon(
                    imageVector = if (repeatMode == RepeatMode.ONE) {
                        Icons.Filled.RepeatOne
                    } else {
                        Icons.Filled.Repeat
                    },
                    contentDescription = when (repeatMode) {
                        RepeatMode.OFF -> "Repeat off"
                        RepeatMode.ALL -> "Repeat all"
                        RepeatMode.ONE -> "Repeat one"
                    },
                    tint = if (repeatMode == RepeatMode.OFF) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onCollapseClick) {
                Icon(
                    imageVector = Icons.Filled.ExpandLess,
                    contentDescription = "Collapse player"
                )
            }
        }
    }
}

@Composable
fun SongGroupDetailScreen(
    title: String,
    subtitle: String,
    songs: List<Song>,
    currentSongId: Long?,
    recentlyAddedSongIds: Set<Long>,
    onBackClick: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onAddToQueueClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Column(
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
        }

        SongList(
            songs = songs,
            currentSongId = currentSongId,
            recentlyAddedSongIds = recentlyAddedSongIds,
            onSongClick = onSongClick,
            onAddToQueueClick = onAddToQueueClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SongList(
    songs: List<Song>,
    currentSongId: Long?,
    recentlyAddedSongIds: Set<Long>,
    onSongClick: (Song, List<Song>) -> Unit,
    onAddToQueueClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
    ) {
        items(
            items = songs,
            key = { song -> song.id }
        ) { song ->
            val isCurrentSong = song.id == currentSongId
            val wasRecentlyAdded = song.id in recentlyAddedSongIds

            ListItem(
                leadingContent = {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = "Album art for ${song.title}",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(android.R.drawable.ic_media_play),
                        placeholder = painterResource(android.R.drawable.ic_media_play)
                    )
                },
                headlineContent = {
                    Text(
                        text = song.title,
                        fontWeight = if (isCurrentSong) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )
                },
                supportingContent = {
                    Text(text = song.artist)
                },
                trailingContent = {
                    IconButton(
                        onClick = {
                            onAddToQueueClick(song)
                        }
                    ) {
                        Icon(
                            imageVector = if (wasRecentlyAdded) {
                                Icons.Filled.Check
                            } else {
                                Icons.Filled.PlaylistAdd
                            },
                            contentDescription = if (wasRecentlyAdded) {
                                "${song.title} added to queue"
                            } else {
                                "Add ${song.title} to queue"
                            },
                            tint = if (wasRecentlyAdded) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = if (isCurrentSong) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                modifier = Modifier.clickable {
                    onSongClick(song, songs)
                }
            )
        }
    }
}

fun sortSongsByAlbumOrder(songs: List<Song>): List<Song> {
    return songs.sortedWith(
        compareBy<Song> { song ->
            if (song.trackNumber > 0) song.trackNumber else Int.MAX_VALUE
        }.thenBy { song ->
            song.title.lowercase()
        }
    )
}

fun formatDuration(milliseconds: Int): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "%d:%02d".format(minutes, seconds)
}

fun Modifier.playerSwipeGestures(
    onSwipeUp: (() -> Unit)? = null,
    onSwipeDown: (() -> Unit)? = null,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null
): Modifier {
    return this.pointerInput(Unit) {
        var totalDragX = 0f
        var totalDragY = 0f

        detectDragGestures(
            onDragStart = {
                totalDragX = 0f
                totalDragY = 0f
            },
            onDrag = { change, dragAmount ->
                change.consume()
                totalDragX += dragAmount.x
                totalDragY += dragAmount.y
            },
            onDragEnd = {
                val swipeThreshold = 120f

                if (abs(totalDragX) > abs(totalDragY)) {
                    if (totalDragX > swipeThreshold) {
                        onSwipeRight?.invoke()
                    } else if (totalDragX < -swipeThreshold) {
                        onSwipeLeft?.invoke()
                    }
                } else {
                    if (totalDragY > swipeThreshold) {
                        onSwipeDown?.invoke()
                    } else if (totalDragY < -swipeThreshold) {
                        onSwipeUp?.invoke()
                    }
                }
            }
        )
    }
}