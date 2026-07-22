package com.example.cdplaya.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cdplaya.R
import com.example.cdplaya.data.Song
import com.example.cdplaya.data.recentlyAddedShelfSongs
import com.example.cdplaya.ui.AppShellTypography
import com.example.cdplaya.ui.MusicScreenHeader
import com.example.cdplaya.ui.library.LibraryTab

@Composable
fun HomeScreen(
    permissionGranted: Boolean,
    recentlyPlayedSongs: List<Song>,
    recentlyAddedSongs: List<Song>,
    favoriteSongs: List<Song>,
    currentSongId: Long?,
    songCount: Int,
    albumCount: Int,
    artistCount: Int,
    playlistCount: Int,
    onSettingsClick: () -> Unit,
    onOpenLibrary: (LibraryTab) -> Unit,
    onRecentlyPlayedSongClick: (Song) -> Unit,
    onRecentlyAddedSongClick: (Song) -> Unit,
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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MusicScreenHeader(
                    title = "CDPlaya",
                    onSettingsClick = onSettingsClick,
                    modifier = Modifier.statusBarsPadding()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HomeStatBadge(
                        count = songCount,
                        label = pluralStringResource(R.plurals.song_label, songCount),
                        modifier = Modifier.weight(1f)
                    )
                    HomeStatBadge(
                        count = albumCount,
                        label = pluralStringResource(R.plurals.album_label, albumCount),
                        modifier = Modifier.weight(1f)
                    )
                    HomeStatBadge(
                        count = artistCount,
                        label = pluralStringResource(R.plurals.artist_label, artistCount),
                        modifier = Modifier.weight(1f)
                    )
                    HomeStatBadge(
                        count = playlistCount,
                        label = pluralStringResource(R.plurals.playlist_label, playlistCount),
                        modifier = Modifier.weight(1f)
                    )
                }
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

        val visibleRecentlyAddedSongs = recentlyAddedShelfSongs(recentlyAddedSongs)
        if (visibleRecentlyAddedSongs.isNotEmpty()) {
            item {
                HomeRecentlyAddedShelf(
                    songs = visibleRecentlyAddedSongs,
                    onSeeAllClick = { onOpenLibrary(LibraryTab.RECENTLY_ADDED) },
                    onSongClick = onRecentlyAddedSongClick
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

        if (permissionGranted && recentlyPlayedSongs.isEmpty() &&
            visibleRecentlyAddedSongs.isEmpty() && favoriteSongs.isEmpty()
        ) {
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

@Composable
private fun HomeStatBadge(
    count: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = count.toString(),
                style = AppShellTypography.StatNumber,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label.uppercase(),
                style = AppShellTypography.StatLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
