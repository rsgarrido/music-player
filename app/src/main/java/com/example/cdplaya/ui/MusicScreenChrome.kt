package com.example.cdplaya.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.Song
import com.example.cdplaya.data.favoriteKey
import com.example.cdplaya.player.RepeatMode
import com.example.cdplaya.ui.player.PlayerCard
import com.example.cdplaya.ui.player.SleepTimerStatusBanner

@Composable
fun MusicScreenHeader(
    onSettingsClick: () -> Unit
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
            onClick = onSettingsClick
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings"
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
    onSleepTimerClick: () -> Unit
) {
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

@Composable
fun LibraryChromeControls(
    selectedLibraryTab: LibraryTab,
    selectedArtistName: String?,
    selectedAlbumFolderPath: String?,
    searchQuery: String,
    selectedSongSortOption: LibrarySortOption,
    selectedArtistSortOption: LibrarySortOption,
    selectedAlbumSortOption: LibrarySortOption,
    selectedFavoriteSortOption: LibrarySortOption,
    onTabSelected: (LibraryTab) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSongSortOptionSelected: (LibrarySortOption) -> Unit,
    onArtistSortOptionSelected: (LibrarySortOption) -> Unit,
    onAlbumSortOptionSelected: (LibrarySortOption) -> Unit,
    onFavoriteSortOptionSelected: (LibrarySortOption) -> Unit
) {
    LibraryTabs(
        selectedTab = selectedLibraryTab,
        onTabSelected = onTabSelected
    )

    if (selectedLibraryTab != LibraryTab.QUEUE) {
        LibrarySearchBar(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange
        )
    }

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