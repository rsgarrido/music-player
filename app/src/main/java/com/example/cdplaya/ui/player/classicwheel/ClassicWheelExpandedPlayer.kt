package com.example.cdplaya.ui.player.classicwheel

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.RepeatMode
import java.io.File
import kotlin.math.roundToInt
import kotlin.math.PI
import kotlin.math.atan2
import kotlinx.coroutines.delay


@Composable
fun ClassicWheelExpandedPlayer(
    currentSong: Song?,
    songs: List<Song>,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    currentPosition: Int,
    duration: Int,
    isCurrentSongFavorite: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekChange: (Int) -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onCollapseClick: () -> Unit,
    onOpenUpNextClick: () -> Unit,
    onToggleFavoriteClick: (Song) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1EDE0))
            .padding(horizontal = 14.dp, vertical = 16.dp)
    ) {
        val menuState = remember {
            ClassicWheelMenuState()
        }

        val context = androidx.compose.ui.platform.LocalContext.current

        val audioManager = remember {
            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }

        val maxMusicVolume = remember {
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        }

        var musicVolume by remember {
            mutableIntStateOf(
                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            )
        }

        var isVolumeOverlayVisible by remember {
            mutableStateOf(false)
        }

        fun changeMusicVolume(delta: Int) {
            val currentSystemVolume = audioManager.getStreamVolume(
                AudioManager.STREAM_MUSIC
            )

            val updatedVolume = (currentSystemVolume + delta)
                .coerceIn(0, maxMusicVolume)

            if (updatedVolume == currentSystemVolume) {
                musicVolume = currentSystemVolume
                isVolumeOverlayVisible = true
                return
            }

            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                updatedVolume,
                0
            )

            musicVolume = updatedVolume
            isVolumeOverlayVisible = true
        }

        LaunchedEffect(isVolumeOverlayVisible, musicVolume) {
            if (isVolumeOverlayVisible) {
                delay(1200)
                isVolumeOverlayVisible = false
            }
        }

        val mainMenuItems = buildClassicWheelMainMenuItems()
        val songMenuItems = buildClassicWheelSongMenuItems(songs)

        val artistGroups = buildClassicWheelArtistGroups(songs)
        val albumGroups = buildClassicWheelAlbumGroups(songs)

        val artistMenuItems = buildClassicWheelArtistMenuItems(artistGroups)
        val albumMenuItems = buildClassicWheelAlbumMenuItems(albumGroups)

        val albumCarouselItems = buildClassicWheelAlbumCarouselItems(albumGroups)

        val currentScreen = menuState.currentScreen

        val selectedArtistSongs = if (currentScreen is ClassicWheelMenuScreen.ArtistSongs) {
            artistGroups.firstOrNull { artistGroup ->
                artistGroup.name == currentScreen.artistName
            }?.songs ?: emptyList()
        } else {
            emptyList()
        }

        val selectedAlbumSongs = if (currentScreen is ClassicWheelMenuScreen.AlbumSongs) {
            albumGroups.firstOrNull { albumGroup ->
                albumGroup.key == currentScreen.albumKey
            }?.songs ?: emptyList()
        } else {
            emptyList()
        }

        val selectedArtistSongMenuItems = buildClassicWheelSongMenuItems(selectedArtistSongs)
        val selectedAlbumSongMenuItems = buildClassicWheelSongMenuItems(selectedAlbumSongs)

        val onMenuClick = {
            if (menuState.currentScreen == ClassicWheelMenuScreen.NowPlaying) {
                menuState.openMainMenu()
            } else {
                menuState.goBack()
            }
        }

        val onCenterClick = {
            when (val screen = menuState.currentScreen) {
                ClassicWheelMenuScreen.NowPlaying -> {
                    menuState.openMainMenu()
                }

                ClassicWheelMenuScreen.MainMenu -> {
                    val selectedItem = mainMenuItems
                        .getOrNull(menuState.selectedIndex)

                    if (selectedItem != null) {
                        handleClassicWheelMenuAction(
                            action = selectedItem.action,
                            menuState = menuState
                        )
                    }
                }

                ClassicWheelMenuScreen.Songs -> {
                    val selectedSong = songs.getOrNull(menuState.selectedIndex)

                    if (selectedSong != null) {
                        onSongClick(selectedSong, songs)
                        menuState.openNowPlaying()
                    }
                }

                ClassicWheelMenuScreen.Artists -> {
                    val selectedArtist = artistGroups.getOrNull(menuState.selectedIndex)

                    if (selectedArtist != null) {
                        menuState.openArtistSongs(selectedArtist.name)
                    }
                }

                is ClassicWheelMenuScreen.ArtistSongs -> {
                    val selectedSong = selectedArtistSongs.getOrNull(menuState.selectedIndex)

                    if (selectedSong != null) {
                        onSongClick(selectedSong, selectedArtistSongs)
                        menuState.openNowPlaying()
                    }
                }

                ClassicWheelMenuScreen.Albums -> {
                    val selectedAlbum = albumGroups.getOrNull(menuState.selectedIndex)

                    if (selectedAlbum != null) {
                        menuState.openAlbumSongs(
                            albumKey = selectedAlbum.key,
                            albumTitle = selectedAlbum.title
                        )
                    }
                }

                is ClassicWheelMenuScreen.AlbumSongs -> {
                    val selectedSong = selectedAlbumSongs.getOrNull(menuState.selectedIndex)

                    if (selectedSong != null) {
                        onSongClick(selectedSong, selectedAlbumSongs)
                        menuState.openNowPlaying()
                    }
                }
            }
        }

        val currentMenuItemCount = when (menuState.currentScreen) {
            ClassicWheelMenuScreen.NowPlaying -> 0
            ClassicWheelMenuScreen.MainMenu -> mainMenuItems.size
            ClassicWheelMenuScreen.Songs -> songMenuItems.size
            ClassicWheelMenuScreen.Artists -> artistMenuItems.size
            is ClassicWheelMenuScreen.ArtistSongs -> selectedArtistSongMenuItems.size
            ClassicWheelMenuScreen.Albums -> albumMenuItems.size
            is ClassicWheelMenuScreen.AlbumSongs -> selectedAlbumSongMenuItems.size
        }

        val onRotateClockwise = {
            if (menuState.currentScreen == ClassicWheelMenuScreen.NowPlaying) {
                changeMusicVolume(1)
            } else if (currentMenuItemCount > 1) {
                menuState.moveSelectionDown(currentMenuItemCount)
            }
        }

        val onRotateCounterClockwise = {
            if (menuState.currentScreen == ClassicWheelMenuScreen.NowPlaying) {
                changeMusicVolume(-1)
            } else if (currentMenuItemCount > 1) {
                menuState.moveSelectionUp(currentMenuItemCount)
            }
        }

        val screenHeight = minOf(
            maxHeight * 0.42f,
            360.dp
        )

        val wheelSize = minOf(
            maxWidth * 0.9f,
            maxHeight * 0.43f,
            370.dp
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            ClassicWheelScreen(
                currentSong = currentSong,
                currentPosition = currentPosition,
                duration = duration,
                isCurrentSongFavorite = isCurrentSongFavorite,
                isShuffleEnabled = isShuffleEnabled,
                repeatMode = repeatMode,
                menuState = menuState,
                mainMenuItems = mainMenuItems,
                songMenuItems = songMenuItems,
                artistMenuItems = artistMenuItems,
                albumMenuItems = albumMenuItems,
                albumCarouselItems = albumCarouselItems,
                selectedArtistSongMenuItems = selectedArtistSongMenuItems,
                selectedAlbumSongMenuItems = selectedAlbumSongMenuItems,
                musicVolume = musicVolume,
                maxMusicVolume = maxMusicVolume,
                isVolumeOverlayVisible = isVolumeOverlayVisible,
                onSeekChange = onSeekChange,
                onShuffleClick = onShuffleClick,
                onRepeatClick = onRepeatClick,
                onCollapseClick = onCollapseClick,
                onOpenUpNextClick = onOpenUpNextClick,
                onToggleFavoriteClick = onToggleFavoriteClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight)
            )

            ClassicControlWheel(
                isPlaying = isPlaying,
                onPlayPauseClick = onPlayPauseClick,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onMenuClick = onMenuClick,
                onCenterClick = onCenterClick,
                onRotateClockwise = onRotateClockwise,
                onRotateCounterClockwise = onRotateCounterClockwise,
                rotationItemCount = currentMenuItemCount,
                isRotationEnabled = menuState.currentScreen == ClassicWheelMenuScreen.NowPlaying ||
                        currentMenuItemCount > 1,
                rotationStepDegrees = if (menuState.currentScreen == ClassicWheelMenuScreen.NowPlaying) {
                    95f
                } else {
                    55f
                },
                modifier = Modifier.size(wheelSize)
            )
        }
    }
}


@Composable
private fun ClassicWheelScreen(
    currentSong: Song?,
    currentPosition: Int,
    duration: Int,
    isCurrentSongFavorite: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    menuState: ClassicWheelMenuState,
    mainMenuItems: List<ClassicWheelMenuItem>,
    songMenuItems: List<ClassicWheelMenuItem>,
    artistMenuItems: List<ClassicWheelMenuItem>,
    albumMenuItems: List<ClassicWheelMenuItem>,
    albumCarouselItems: List<ClassicWheelAlbumCarouselItem>,
    selectedArtistSongMenuItems: List<ClassicWheelMenuItem>,
    selectedAlbumSongMenuItems: List<ClassicWheelMenuItem>,
    musicVolume: Int,
    maxMusicVolume: Int,
    isVolumeOverlayVisible: Boolean,
    onSeekChange: (Int) -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onCollapseClick: () -> Unit,
    onOpenUpNextClick: () -> Unit,
    onToggleFavoriteClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.Black,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp)
                .background(Color(0xFFF7F7F2))
        ) {
            ClassicScreenStatusBar(
                title = buildClassicWheelStatusTitle(menuState.currentScreen),
                onCollapseClick = onCollapseClick
            )

            when (val screen = menuState.currentScreen) {
                ClassicWheelMenuScreen.NowPlaying -> {
                    ClassicWheelNowPlayingDisplay(
                        currentSong = currentSong,
                        currentPosition = currentPosition,
                        duration = duration,
                        isCurrentSongFavorite = isCurrentSongFavorite,
                        isShuffleEnabled = isShuffleEnabled,
                        repeatMode = repeatMode,
                        musicVolume = musicVolume,
                        maxMusicVolume = maxMusicVolume,
                        isVolumeIndicatorVisible = isVolumeOverlayVisible,
                        onSeekChange = onSeekChange,
                        onShuffleClick = onShuffleClick,
                        onRepeatClick = onRepeatClick,
                        onOpenUpNextClick = onOpenUpNextClick,
                        onToggleFavoriteClick = onToggleFavoriteClick
                    )
                }

                ClassicWheelMenuScreen.MainMenu -> {
                    ClassicWheelMenuDisplay(
                        title = "Music",
                        menuItems = mainMenuItems,
                        selectedIndex = menuState.selectedIndex,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ClassicWheelMenuScreen.Songs -> {
                    ClassicWheelMenuDisplay(
                        title = "Songs",
                        menuItems = songMenuItems,
                        selectedIndex = menuState.selectedIndex,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ClassicWheelMenuScreen.Artists -> {
                    ClassicWheelMenuDisplay(
                        title = "Artists",
                        menuItems = artistMenuItems,
                        selectedIndex = menuState.selectedIndex,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is ClassicWheelMenuScreen.ArtistSongs -> {
                    ClassicWheelMenuDisplay(
                        title = screen.artistName,
                        menuItems = selectedArtistSongMenuItems,
                        selectedIndex = menuState.selectedIndex,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ClassicWheelMenuScreen.Albums -> {
                    ClassicWheelAlbumCarouselDisplay(
                        items = albumCarouselItems,
                        selectedIndex = menuState.selectedIndex,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is ClassicWheelMenuScreen.AlbumSongs -> {
                    ClassicWheelMenuDisplay(
                        title = screen.albumTitle,
                        menuItems = selectedAlbumSongMenuItems,
                        selectedIndex = menuState.selectedIndex,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

private fun buildClassicWheelStatusTitle(
    screen: ClassicWheelMenuScreen
): String {
    return when (screen) {
        ClassicWheelMenuScreen.NowPlaying -> "Now Playing"
        ClassicWheelMenuScreen.MainMenu -> "Music"
        ClassicWheelMenuScreen.Songs -> "Songs"
        ClassicWheelMenuScreen.Artists -> "Artists"
        is ClassicWheelMenuScreen.ArtistSongs -> screen.artistName
        ClassicWheelMenuScreen.Albums -> "Albums"
        is ClassicWheelMenuScreen.AlbumSongs -> screen.albumTitle
    }
}

@Composable
private fun ClassicWheelNowPlayingDisplay(
    currentSong: Song?,
    currentPosition: Int,
    duration: Int,
    isCurrentSongFavorite: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    musicVolume: Int,
    maxMusicVolume: Int,
    isVolumeIndicatorVisible: Boolean,
    onSeekChange: (Int) -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onOpenUpNextClick: () -> Unit,
    onToggleFavoriteClick: (Song) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = currentSong?.albumArtUri,
                contentDescription = currentSong?.let { song ->
                    "Album art for ${song.title}"
                },
                modifier = Modifier
                    .weight(0.95f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(3.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                error = painterResource(android.R.drawable.ic_media_play),
                placeholder = painterResource(android.R.drawable.ic_media_play)
            )

            Column(
                modifier = Modifier.weight(1.15f)
            ) {
                Text(
                    text = currentSong?.title?.ifBlank { "Unknown Title" }
                        ?: "No song selected",
                    color = Color.Black,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = currentSong?.artist?.ifBlank { "Unknown Artist" }
                        ?: "Choose a song",
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = currentSong?.album?.ifBlank { "Unknown Album" }
                        ?: "",
                    color = Color.DarkGray,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onShuffleClick,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffleEnabled) {
                                Color(0xFF2F80D8)
                            } else {
                                Color.Black
                            },
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = onRepeatClick,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (repeatMode == RepeatMode.ONE) {
                                Icons.Filled.RepeatOne
                            } else {
                                Icons.Filled.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (repeatMode == RepeatMode.OFF) {
                                Color.Black
                            } else {
                                Color(0xFF2F80D8)
                            },
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            currentSong?.let { song ->
                                onToggleFavoriteClick(song)
                            }
                        },
                        modifier = Modifier.size(38.dp),
                        enabled = currentSong != null
                    ) {
                        Icon(
                            imageVector = if (isCurrentSongFavorite) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Filled.FavoriteBorder
                            },
                            contentDescription = "Favorite",
                            tint = if (isCurrentSongFavorite) {
                                Color(0xFF2F80D8)
                            } else {
                                Color.Black
                            },
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenUpNextClick,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QueueMusic,
                            contentDescription = "Up Next",
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Text(
                    text = buildPlaybackModeText(
                        isShuffleEnabled = isShuffleEnabled,
                        repeatMode = repeatMode
                    ),
                    color = Color.DarkGray,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isVolumeIndicatorVisible) {
            ClassicWheelVolumeProgress(
                volume = musicVolume,
                maxVolume = maxMusicVolume
            )
        } else {
            ClassicWheelProgress(
                currentPosition = currentPosition,
                duration = duration,
                onSeekChange = onSeekChange
            )
        }
    }
}


@Composable
private fun ClassicScreenStatusBar(
    title: String,
    onCollapseClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE4E4E0))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.Black,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onCollapseClick,
            modifier = Modifier.size(26.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Collapse player",
                tint = Color.Black
            )
        }

        ClassicBatteryIndicator()
    }
}

private fun buildClassicWheelMainMenuItems(): List<ClassicWheelMenuItem> {
    return listOf(
        ClassicWheelMenuItem(
            title = "Now Playing",
            action = ClassicWheelMenuAction.OPEN_NOW_PLAYING
        ),
        ClassicWheelMenuItem(
            title = "Songs",
            action = ClassicWheelMenuAction.OPEN_SONGS
        ),
        ClassicWheelMenuItem(
            title = "Artists",
            action = ClassicWheelMenuAction.OPEN_ARTISTS
        ),
        ClassicWheelMenuItem(
            title = "Albums",
            action = ClassicWheelMenuAction.OPEN_ALBUMS
        )
    )
}

@Composable
private fun ClassicWheelProgress(
    currentPosition: Int,
    duration: Int,
    onSeekChange: (Int) -> Unit
) {
    val safeDuration = duration.coerceAtLeast(1)
    val clampedPosition = currentPosition.coerceIn(0, safeDuration)
    val progress = clampedPosition.toFloat() / safeDuration.toFloat()

    var progressBarWidthPx by remember {
        mutableStateOf(1)
    }

    fun seekToPositionFromX(x: Float) {
        val seekRatio = (x / progressBarWidthPx.toFloat())
            .coerceIn(0f, 1f)

        onSeekChange(
            (seekRatio * safeDuration).roundToInt()
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatTime(clampedPosition),
            color = Color.Black,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .padding(horizontal = 10.dp)
                .onSizeChanged { size ->
                    progressBarWidthPx = size.width.coerceAtLeast(1)
                }
                .pointerInput(safeDuration, progressBarWidthPx) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            seekToPositionFromX(offset.x)
                        },
                        onDrag = { change, _ ->
                            seekToPositionFromX(change.position.x)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val trackHeight = 10.dp.toPx()
                val trackTop = (size.height - trackHeight) / 2f
                val trackCorner = CornerRadius(
                    x = trackHeight / 2f,
                    y = trackHeight / 2f
                )

                val progressWidth = size.width * progress.coerceIn(0f, 1f)

                drawRoundRect(
                    color = Color(0xFFD8D8D2),
                    topLeft = Offset(
                        x = 0f,
                        y = trackTop
                    ),
                    size = Size(
                        width = size.width,
                        height = trackHeight
                    ),
                    cornerRadius = trackCorner
                )

                drawRoundRect(
                    color = Color(0xFF67AEE7),
                    topLeft = Offset(
                        x = 0f,
                        y = trackTop
                    ),
                    size = Size(
                        width = progressWidth,
                        height = trackHeight
                    ),
                    cornerRadius = trackCorner
                )

                val handleWidth = 5.dp.toPx()
                val handleHeight = 28.dp.toPx()
                val handleCorner = CornerRadius(
                    x = 3.dp.toPx(),
                    y = 3.dp.toPx()
                )

                val handleLeft = (progressWidth - handleWidth / 2f)
                    .coerceIn(0f, size.width - handleWidth)

                drawRoundRect(
                    color = Color(0xFF9DB2FF),
                    topLeft = Offset(
                        x = handleLeft,
                        y = (size.height - handleHeight) / 2f
                    ),
                    size = Size(
                        width = handleWidth,
                        height = handleHeight
                    ),
                    cornerRadius = handleCorner
                )
            }
        }

        Text(
            text = "-${formatTime((safeDuration - clampedPosition).coerceAtLeast(0))}",
            color = Color.Black,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ClassicWheelVolumeProgress(
    volume: Int,
    maxVolume: Int
) {
    val safeMaxVolume = maxVolume.coerceAtLeast(1)
    val clampedVolume = volume.coerceIn(0, safeMaxVolume)
    val progress = clampedVolume.toFloat() / safeMaxVolume.toFloat()
    val volumePercent = (progress * 100).roundToInt()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Volume",
            color = Color.Black,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val trackHeight = 10.dp.toPx()
                val trackTop = (size.height - trackHeight) / 2f
                val trackCorner = CornerRadius(
                    x = trackHeight / 2f,
                    y = trackHeight / 2f
                )

                val progressWidth = size.width * progress.coerceIn(0f, 1f)

                drawRoundRect(
                    color = Color(0xFFD8D8D2),
                    topLeft = Offset(
                        x = 0f,
                        y = trackTop
                    ),
                    size = Size(
                        width = size.width,
                        height = trackHeight
                    ),
                    cornerRadius = trackCorner
                )

                drawRoundRect(
                    color = Color(0xFF67AEE7),
                    topLeft = Offset(
                        x = 0f,
                        y = trackTop
                    ),
                    size = Size(
                        width = progressWidth,
                        height = trackHeight
                    ),
                    cornerRadius = trackCorner
                )

                val handleWidth = 5.dp.toPx()
                val handleHeight = 28.dp.toPx()
                val handleCorner = CornerRadius(
                    x = 3.dp.toPx(),
                    y = 3.dp.toPx()
                )

                val handleLeft = (progressWidth - handleWidth / 2f)
                    .coerceIn(0f, size.width - handleWidth)

                drawRoundRect(
                    color = Color(0xFF9DB2FF),
                    topLeft = Offset(
                        x = handleLeft,
                        y = (size.height - handleHeight) / 2f
                    ),
                    size = Size(
                        width = handleWidth,
                        height = handleHeight
                    ),
                    cornerRadius = handleCorner
                )
            }
        }

        Text(
            text = "$volumePercent%",
            color = Color.Black,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ClassicControlWheel(
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onMenuClick: () -> Unit,
    onCenterClick: () -> Unit,
    onRotateClockwise: () -> Unit,
    onRotateCounterClockwise: () -> Unit,
    rotationItemCount: Int,
    isRotationEnabled: Boolean,
    rotationStepDegrees: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(rotationItemCount, isRotationEnabled, rotationStepDegrees) {
                var previousAngle: Float? = null
                var accumulatedAngleDelta = 0f
                val selectionStepDegrees = rotationStepDegrees

                detectDragGestures(
                    onDragStart = { offset ->
                        previousAngle = offset.angleDegreesFromCenter(
                            width = size.width.toFloat(),
                            height = size.height.toFloat()
                        )
                        accumulatedAngleDelta = 0f
                    },
                    onDrag = { change, _ ->
                        if (!isRotationEnabled) {
                            return@detectDragGestures
                        }

                        val currentAngle = change.position.angleDegreesFromCenter(
                            width = size.width.toFloat(),
                            height = size.height.toFloat()
                        )

                        val oldAngle = previousAngle ?: currentAngle
                        var angleDelta = currentAngle - oldAngle

                        if (angleDelta > 180f) {
                            angleDelta -= 360f
                        }

                        if (angleDelta < -180f) {
                            angleDelta += 360f
                        }

                        accumulatedAngleDelta += angleDelta

                        while (accumulatedAngleDelta >= selectionStepDegrees) {
                            onRotateClockwise()
                            accumulatedAngleDelta -= selectionStepDegrees
                        }

                        while (accumulatedAngleDelta <= -selectionStepDegrees) {
                            onRotateCounterClockwise()
                            accumulatedAngleDelta += selectionStepDegrees
                        }

                        previousAngle = currentAngle
                        change.consume()
                    },
                    onDragEnd = {
                        previousAngle = null
                        accumulatedAngleDelta = 0f
                    },
                    onDragCancel = {
                        previousAngle = null
                        accumulatedAngleDelta = 0f
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = Color(0xFFC8C6BC),
                shadowElevation = 6.dp
            ) {}

        Text(
            text = "MENU",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp)
                .clickable {
                    onMenuClick()
                }
        )

        IconButton(
            onClick = onPreviousClick,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 26.dp)
                .size(68.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous",
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )
        }

        IconButton(
            onClick = onNextClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 26.dp)
                .size(68.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )
        }

        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp)
                .size(72.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) {
                    Icons.Filled.Pause
                } else {
                    Icons.Filled.PlayArrow
                },
                contentDescription = if (isPlaying) {
                    "Pause"
                } else {
                    "Play"
                },
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxSize(0.28f)
                .clickable {
                    onCenterClick()
                },
            shape = CircleShape,
            color = Color(0xFFF1EDE0),
            shadowElevation = 4.dp
        ) {}
    }
}

private fun formatTime(milliseconds: Int): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private fun Offset.angleDegreesFromCenter(
    width: Float,
    height: Float
): Float {
    val centerX = width / 2f
    val centerY = height / 2f

    val angleRadians = atan2(
        y = y - centerY,
        x = x - centerX
    )

    return (angleRadians * 180f / PI).toFloat()
}

private fun buildPlaybackModeText(
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode
): String {
    val shuffleText = if (isShuffleEnabled) {
        "Shuffle On"
    } else {
        "Shuffle Off"
    }

    val repeatText = when (repeatMode) {
        RepeatMode.OFF -> "Repeat Off"
        RepeatMode.ALL -> "Repeat All"
        RepeatMode.ONE -> "Repeat One"
    }

    return "$shuffleText • $repeatText"
}


private fun buildClassicWheelSongMenuItems(
    songs: List<Song>
): List<ClassicWheelMenuItem> {
    if (songs.isEmpty()) {
        return listOf(
            ClassicWheelMenuItem(
                title = "No songs found",
                subtitle = "Check your library",
                action = ClassicWheelMenuAction.OPEN_NOW_PLAYING
            )
        )
    }

    return songs.map { song ->
        ClassicWheelMenuItem(
            title = song.title.ifBlank { "Unknown Title" },
            subtitle = song.artist.ifBlank { "Unknown Artist" },
            action = ClassicWheelMenuAction.OPEN_NOW_PLAYING
        )
    }
}

private fun handleClassicWheelMenuAction(
    action: ClassicWheelMenuAction,
    menuState: ClassicWheelMenuState
) {
    when (action) {
        ClassicWheelMenuAction.OPEN_NOW_PLAYING -> {
            menuState.openNowPlaying()
        }

        ClassicWheelMenuAction.OPEN_SONGS -> {
            menuState.openSongs()
        }

        ClassicWheelMenuAction.OPEN_ARTISTS -> {
            menuState.openArtists()
        }

        ClassicWheelMenuAction.OPEN_ALBUMS -> {
            menuState.openAlbums()
        }
    }
}

private data class ClassicWheelArtistGroup(
    val name: String,
    val songs: List<Song>
)

private data class ClassicWheelAlbumGroup(
    val key: String,
    val title: String,
    val artist: String,
    val songs: List<Song>
)

private fun buildClassicWheelArtistGroups(
    songs: List<Song>
): List<ClassicWheelArtistGroup> {
    return songs
        .groupBy { song ->
            song.artist.ifBlank { "Unknown Artist" }
        }
        .map { entry ->
            ClassicWheelArtistGroup(
                name = entry.key,
                songs = sortClassicWheelArtistSongs(entry.value)
            )
        }
        .sortedBy { artistGroup ->
            artistGroup.name.lowercase()
        }
}

private fun sortClassicWheelArtistSongs(
    songs: List<Song>
): List<Song> {
    return songs.sortedWith(
        compareBy<Song> { song ->
            song.album.ifBlank { "Unknown Album" }.lowercase()
        }.thenBy { song ->
            song.trackNumber.takeIf { trackNumber ->
                trackNumber > 0
            } ?: Int.MAX_VALUE
        }.thenBy { song ->
            song.title.lowercase()
        }
    )
}

private fun buildClassicWheelArtistMenuItems(
    artistGroups: List<ClassicWheelArtistGroup>
): List<ClassicWheelMenuItem> {
    if (artistGroups.isEmpty()) {
        return listOf(
            ClassicWheelMenuItem(
                title = "No artists found",
                subtitle = "Check your library",
                action = ClassicWheelMenuAction.OPEN_NOW_PLAYING
            )
        )
    }

    return artistGroups.map { artistGroup ->
        ClassicWheelMenuItem(
            title = artistGroup.name,
            subtitle = "${artistGroup.songs.size} songs",
            action = ClassicWheelMenuAction.OPEN_NOW_PLAYING
        )
    }
}

private fun buildClassicWheelAlbumGroups(
    songs: List<Song>
): List<ClassicWheelAlbumGroup> {
    return songs
        .groupBy { song ->
            buildClassicWheelAlbumKey(song)
        }
        .map { entry ->
            val albumSongs = sortClassicWheelAlbumSongs(entry.value)
            val firstSong = albumSongs.first()

            ClassicWheelAlbumGroup(
                key = entry.key,
                title = firstSong.album.ifBlank { "Unknown Album" },
                artist = firstSong.artist.ifBlank { "Unknown Artist" },
                songs = albumSongs
            )
        }
        .sortedBy { albumGroup ->
            albumGroup.title.lowercase()
        }
}

private fun buildClassicWheelAlbumMenuItems(
    albumGroups: List<ClassicWheelAlbumGroup>
): List<ClassicWheelMenuItem> {
    if (albumGroups.isEmpty()) {
        return listOf(
            ClassicWheelMenuItem(
                title = "No albums found",
                subtitle = "Check your library",
                action = ClassicWheelMenuAction.OPEN_NOW_PLAYING
            )
        )
    }

    return albumGroups.map { albumGroup ->
        ClassicWheelMenuItem(
            title = albumGroup.title,
            subtitle = albumGroup.artist,
            action = ClassicWheelMenuAction.OPEN_NOW_PLAYING
        )
    }
}

private fun buildClassicWheelAlbumKey(song: Song): String {
    val albumTitle = song.album.ifBlank { "Unknown Album" }
    val artistName = song.artist.ifBlank { "Unknown Artist" }
    val folderPath = File(song.filePath).parent ?: ""

    return "$albumTitle|$artistName|$folderPath"
}

private fun sortClassicWheelAlbumSongs(
    songs: List<Song>
): List<Song> {
    return songs.sortedWith(
        compareBy<Song> { song ->
            song.trackNumber.takeIf { trackNumber ->
                trackNumber > 0
            } ?: Int.MAX_VALUE
        }.thenBy { song ->
            song.title.lowercase()
        }
    )
}

private fun buildClassicWheelAlbumCarouselItems(
    albumGroups: List<ClassicWheelAlbumGroup>
): List<ClassicWheelAlbumCarouselItem> {
    return albumGroups.map { albumGroup ->
        ClassicWheelAlbumCarouselItem(
            title = albumGroup.title,
            artist = albumGroup.artist,
            albumArtUri = albumGroup.songs.firstOrNull()?.albumArtUri
        )
    }
}