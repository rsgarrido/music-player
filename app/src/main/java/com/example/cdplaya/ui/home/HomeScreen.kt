package com.example.cdplaya.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.Song
import com.example.cdplaya.ui.MusicScreenHeader
import com.example.cdplaya.ui.appShellBackgroundBrush
import com.example.cdplaya.ui.library.LibraryTab

@Composable
fun HomeScreen(
    permissionGranted: Boolean,
    recentlyPlayedSongs: List<Song>,
    songCount: Int,
    albumCount: Int,
    artistCount: Int,
    playlistCount: Int,
    onSettingsClick: () -> Unit,
    onOpenLibrary: (LibraryTab) -> Unit,
    onSearchClick: () -> Unit,
    onRecentlyPlayedSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 24.dp
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(appShellBackgroundBrush())
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                MusicScreenHeader(
                    title = "CDPlaya",
                    onSettingsClick = onSettingsClick
                )

                Text(
                    text = buildLibrarySummary(
                        songCount = songCount,
                        albumCount = albumCount,
                        artistCount = artistCount,
                        playlistCount = playlistCount
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!permissionGranted) {
            item {
                Text(
                    text = "Audio and image permissions are needed to show your music.",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (recentlyPlayedSongs.isNotEmpty()) {
            item {
                HomeRecentlyPlayedShelf(
                    songs = recentlyPlayedSongs.take(8),
                    onSeeAllClick = {
                        onOpenLibrary(LibraryTab.RECENTLY_PLAYED)
                    },
                    onSongClick = onRecentlyPlayedSongClick
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeSectionHeader(
                    text = "Your Library",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                HomeLibraryShortcutGrid(
                    onOpenLibrary = onOpenLibrary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeSectionHeader(
                    text = "More",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                HomeSecondaryShortcutRow(onOpenLibrary = onOpenLibrary)
            }
        }

        item {
            PressableHomeCard(
                onClick = onSearchClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.extraLarge,
                pressedContainerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Search",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

private fun buildLibrarySummary(
    songCount: Int,
    albumCount: Int,
    artistCount: Int,
    playlistCount: Int
): String {
    return listOf(
        libraryCountLabel(songCount, "song"),
        libraryCountLabel(albumCount, "album"),
        libraryCountLabel(artistCount, "artist"),
        libraryCountLabel(playlistCount, "playlist")
    ).joinToString(separator = " \u2022 ")
}

private fun libraryCountLabel(count: Int, singularLabel: String): String {
    val label = if (count == 1) singularLabel else "${singularLabel}s"
    return "$count $label"
}
