package io.github.rsgarrido.sazanami.ui.player.classicwheel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.ui.player.classicwheel.ClassicWheelMorphBounds
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.player.RepeatMode

@Composable
fun ClassicWheelScreen(
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
    morphBounds: ClassicWheelMorphBounds? = null,
    sharedContentVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = ClassicWheelColors.screenBezel,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp)
                .background(ClassicWheelColors.screenBackground)
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
                        onToggleFavoriteClick = onToggleFavoriteClick,
                        morphBounds = morphBounds,
                        sharedContentVisible = sharedContentVisible
                    )
                }

                ClassicWheelMenuScreen.MainMenu -> {
                    ClassicWheelMenuDisplay(
                        title = stringResource(R.string.player_wheel_music),
                        menuItems = mainMenuItems,
                        selectedIndex = menuState.selectedIndex,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ClassicWheelMenuScreen.Songs -> {
                    ClassicWheelMenuDisplay(
                        title = stringResource(R.string.player_wheel_songs),
                        menuItems = songMenuItems,
                        selectedIndex = menuState.selectedIndex,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ClassicWheelMenuScreen.Artists -> {
                    ClassicWheelMenuDisplay(
                        title = stringResource(R.string.player_wheel_artists),
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

@Composable
private fun ClassicScreenStatusBar(
    title: String,
    onCollapseClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ClassicWheelColors.statusBarBackground)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = ClassicWheelColors.screenText,
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
                contentDescription = stringResource(R.string.player_collapse),
                tint = ClassicWheelColors.screenText
            )
        }

        ClassicBatteryIndicator()
    }
}

@Composable
private fun buildClassicWheelStatusTitle(
    screen: ClassicWheelMenuScreen
): String {
    return when (screen) {
        ClassicWheelMenuScreen.NowPlaying -> stringResource(R.string.player_wheel_now_playing)
        ClassicWheelMenuScreen.MainMenu -> stringResource(R.string.player_wheel_music)
        ClassicWheelMenuScreen.Songs -> stringResource(R.string.player_wheel_songs)
        ClassicWheelMenuScreen.Artists -> stringResource(R.string.player_wheel_artists)
        is ClassicWheelMenuScreen.ArtistSongs -> screen.artistName.ifBlank {
            stringResource(R.string.player_unknown_artist)
        }
        ClassicWheelMenuScreen.Albums -> stringResource(R.string.player_wheel_albums)
        is ClassicWheelMenuScreen.AlbumSongs -> screen.albumTitle
    }
}
