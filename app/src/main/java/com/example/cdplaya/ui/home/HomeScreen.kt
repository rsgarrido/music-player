package com.example.cdplaya.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.Song
import com.example.cdplaya.ui.MusicScreenHeader
import com.example.cdplaya.ui.AppShellTypography
import com.example.cdplaya.ui.library.LibraryTab

@Composable
fun HomeScreen(
    permissionGranted: Boolean,
    recentlyPlayedSongs: List<Song>,
    favoriteSongs: List<Song>,
    currentSongId: Long?,
    songCount: Int,
    albumCount: Int,
    artistCount: Int,
    playlistCount: Int,
    onSettingsClick: () -> Unit,
    onOpenLibrary: (LibraryTab) -> Unit,
    onRecentlyPlayedSongClick: (Song) -> Unit,
    onFavoriteSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 24.dp
) {
    val visibleRecentlyPlayedSongs = recentlyPlayedSongs
        .filterNot { song -> song.id == currentSongId }
        .ifEmpty { recentlyPlayedSongs }

    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                MusicScreenHeader(
                    title = "CDPlaya",
                    onSettingsClick = onSettingsClick,
                    modifier = Modifier.statusBarsPadding()
                )

                Text(
                    text = buildLibrarySummary(
                        songCount = songCount,
                        albumCount = albumCount,
                        artistCount = artistCount,
                        playlistCount = playlistCount
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = AppShellTypography.Eyebrow,
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

        if (visibleRecentlyPlayedSongs.isNotEmpty()) {
            item {
                HomeRecentlyPlayedShelf(
                    songs = visibleRecentlyPlayedSongs.take(8),
                    onSeeAllClick = {
                        onOpenLibrary(LibraryTab.RECENTLY_PLAYED)
                    },
                    onSongClick = onRecentlyPlayedSongClick
                )
            }
        }

        if (favoriteSongs.isNotEmpty()) {
            item {
                HomeFavoritesShelf(
                    songs = favoriteSongs.take(8),
                    onSeeAllClick = {
                        onOpenLibrary(LibraryTab.FAVORITES)
                    },
                    onSongClick = onFavoriteSongClick
                )
            }
        }

        if (permissionGranted && recentlyPlayedSongs.isEmpty() && favoriteSongs.isEmpty()) {
            item {
                Text(
                    text = "Choose something from Library to start building your listening history.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    return "$count ${label.uppercase()}"
}
