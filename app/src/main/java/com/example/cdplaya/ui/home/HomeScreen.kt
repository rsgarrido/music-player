package com.example.cdplaya.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.Song
import com.example.cdplaya.ui.MusicScreenHeader
import com.example.cdplaya.ui.appShellBackgroundBrush
import com.example.cdplaya.ui.library.LibraryTab

@Composable
fun HomeScreen(
    permissionGranted: Boolean,
    showContinueListening: Boolean,
    recentlyPlayedSongs: List<Song>,
    favoriteSongCount: Int,
    onSettingsClick: () -> Unit,
    onOpenLibrary: (LibraryTab) -> Unit,
    onSearchClick: () -> Unit,
    onRecentlyPlayedSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
    continueListeningContent: @Composable () -> Unit = {}
) {
    var hasEntered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        hasEntered = true
    }

    AnimatedVisibility(
        visible = hasEntered,
        enter = fadeIn(tween(180)) +
                slideInVertically(tween(220)) { height -> height / 24 }
    ) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(appShellBackgroundBrush()),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
        item {
            MusicScreenHeader(
                title = "CDPlaya",
                onSettingsClick = onSettingsClick
            )
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

        if (showContinueListening) {
            item {
                HomeSectionHeader(
                    text = "Continue listening",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                continueListeningContent()
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
            HomeFavoritesCard(
                favoriteCount = favoriteSongCount,
                onClick = {
                    onOpenLibrary(LibraryTab.FAVORITES)
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item {
            HomeSectionHeader(
                text = "Browse Library",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item {
            HomeLibraryShortcutGrid(
                onOpenLibrary = onOpenLibrary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item {
            Card(
                onClick = onSearchClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
}
