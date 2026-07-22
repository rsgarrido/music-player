package com.example.cdplaya.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.data.Song
import com.example.cdplaya.data.favoriteKey
import com.example.cdplaya.player.RepeatMode
import com.example.cdplaya.ui.library.LibrarySearchBar
import com.example.cdplaya.ui.library.LibrarySortDropdown
import com.example.cdplaya.ui.library.LibrarySortOption
import com.example.cdplaya.ui.library.LibraryTab
import com.example.cdplaya.ui.player.PlayerCard
import com.example.cdplaya.ui.player.SleepTimerStatusBanner
import com.example.cdplaya.ui.player.theme.PlayerThemeTokens

@Composable
fun MusicScreenHeader(
    title: String = "CDPlaya",
    onBackClick: (() -> Unit)? = null,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModeAction: (@Composable () -> Unit)? = null,
    sortAction: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                AppShellIconButton(
                    onClick = onBackClick,
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Home"
                )
            }

            Text(
                text = title,
                style = AppShellTypography.ScreenTitle,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            viewModeAction?.invoke()

            sortAction?.invoke()

            AppShellIconButton(
                onClick = onSettingsClick,
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Settings"
            )
        }
    }
}

@Composable
fun AppShellIconButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    accented: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(40.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (accented) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (accented) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        border = BorderStroke(
            1.dp,
            if (accented) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.36f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
            }
        )
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun MiniPlayerSection(
    currentSong: Song?,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    currentPosition: Int,
    duration: Int,
    selectedPlayerTheme: PlayerTheme,
    selectedPlayerThemeTokens: PlayerThemeTokens,
    favoriteSongKeys: Set<String>,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekChange: (Int) -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onExpandClick: () -> Unit,
    onOpenUpNextClick: () -> Unit,
    onToggleFavoriteClick: (Song) -> Unit,
    isSleepTimerActive: Boolean,
    sleepTimerDisplayText: String,
    onSleepTimerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SleepTimerStatusBanner(
            isSleepTimerActive = isSleepTimerActive,
            sleepTimerDisplayText = sleepTimerDisplayText,
            onSleepTimerClick = onSleepTimerClick
        )

        PlayerCard(
            currentSong = currentSong,
            isPlaying = isPlaying,
            isExpanded = false,
            isShuffleEnabled = isShuffleEnabled,
            repeatMode = repeatMode,
            currentPosition = currentPosition,
            duration = duration,
            selectedPlayerTheme = selectedPlayerTheme,
            selectedPlayerThemeTokens = selectedPlayerThemeTokens,
            onPlayPauseClick = onPlayPauseClick,
            onPreviousClick = onPreviousClick,
            onNextClick = onNextClick,
            onSeekChange = onSeekChange,
            onShuffleClick = onShuffleClick,
            onRepeatClick = onRepeatClick,
            onExpandClick = onExpandClick,
            onCollapseClick = {},
            onOpenUpNextClick = onOpenUpNextClick,
            isCurrentSongFavorite = currentSong?.let { song ->
                song.favoriteKey() in favoriteSongKeys
            } == true,
            onToggleFavoriteClick = onToggleFavoriteClick
        )
    }
}

@Composable
fun LibrarySearchControl(
    selectedLibraryTab: LibraryTab,
    isSearchVisible: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    if (selectedLibraryTab != LibraryTab.QUEUE && isSearchVisible) {
        LibrarySearchBar(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange
        )
    }
}

@Composable
fun LibrarySortAction(
    selectedLibraryTab: LibraryTab,
    selectedArtistName: String?,
    selectedAlbumFolderPath: String?,
    selectedSongSortOption: LibrarySortOption,
    selectedArtistSortOption: LibrarySortOption,
    selectedAlbumSortOption: LibrarySortOption,
    selectedFavoriteSortOption: LibrarySortOption,
    onSongSortOptionSelected: (LibrarySortOption) -> Unit,
    onArtistSortOptionSelected: (LibrarySortOption) -> Unit,
    onAlbumSortOptionSelected: (LibrarySortOption) -> Unit,
    onFavoriteSortOptionSelected: (LibrarySortOption) -> Unit
) {
    val shouldShowSortDropdown =
        selectedLibraryTab == LibraryTab.SONGS ||
                selectedLibraryTab == LibraryTab.FAVORITES ||
                selectedLibraryTab == LibraryTab.ARTISTS && selectedArtistName == null ||
                selectedLibraryTab == LibraryTab.ALBUMS && selectedAlbumFolderPath == null

    if (shouldShowSortDropdown) {
        val selectedSortOption = when (selectedLibraryTab) {
            LibraryTab.SONGS -> selectedSongSortOption
            LibraryTab.FAVORITES -> selectedFavoriteSortOption
            LibraryTab.ARTISTS -> selectedArtistSortOption
            LibraryTab.ALBUMS -> selectedAlbumSortOption
            LibraryTab.PLAYLISTS -> selectedSongSortOption
            LibraryTab.RECENTLY_PLAYED -> selectedSongSortOption
            LibraryTab.MOST_PLAYED -> selectedSongSortOption
            LibraryTab.QUEUE -> selectedSongSortOption
        }

        val availableSortOptions = when (selectedLibraryTab) {
            LibraryTab.SONGS,
            LibraryTab.FAVORITES -> listOf(
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

            LibraryTab.PLAYLISTS,
            LibraryTab.RECENTLY_PLAYED,
            LibraryTab.MOST_PLAYED,
            LibraryTab.QUEUE -> emptyList()
        }

        LibrarySortDropdown(
            selectedOption = selectedSortOption,
            options = availableSortOptions,
            onOptionSelected = { option ->
                when (selectedLibraryTab) {
                    LibraryTab.SONGS -> onSongSortOptionSelected(option)
                    LibraryTab.FAVORITES -> onFavoriteSortOptionSelected(option)
                    LibraryTab.ARTISTS -> onArtistSortOptionSelected(option)
                    LibraryTab.ALBUMS -> onAlbumSortOptionSelected(option)
                    LibraryTab.PLAYLISTS -> Unit
                    LibraryTab.RECENTLY_PLAYED -> Unit
                    LibraryTab.MOST_PLAYED -> Unit
                    LibraryTab.QUEUE -> Unit
                }
            }
        )
    }
}
