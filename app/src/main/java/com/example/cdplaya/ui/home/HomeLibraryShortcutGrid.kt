package com.example.cdplaya.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.cdplaya.ui.library.LibraryTab

private data class HomeLibraryShortcut(
    val tab: LibraryTab,
    val icon: ImageVector
)

private val primaryLibraryShortcuts = listOf(
    HomeLibraryShortcut(LibraryTab.SONGS, Icons.Filled.MusicNote),
    HomeLibraryShortcut(LibraryTab.ALBUMS, Icons.Filled.Album),
    HomeLibraryShortcut(LibraryTab.ARTISTS, Icons.Filled.People),
    HomeLibraryShortcut(LibraryTab.PLAYLISTS, Icons.Filled.LibraryMusic)
)

private val secondaryLibraryShortcuts = listOf(
    HomeLibraryShortcut(LibraryTab.FAVORITES, Icons.Filled.Favorite),
    HomeLibraryShortcut(LibraryTab.RECENTLY_PLAYED, Icons.Filled.History),
    HomeLibraryShortcut(LibraryTab.MOST_PLAYED, Icons.Filled.BarChart),
    HomeLibraryShortcut(LibraryTab.QUEUE, Icons.AutoMirrored.Filled.PlaylistPlay)
)

@Composable
fun HomeLibraryShortcutGrid(
    onOpenLibrary: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        primaryLibraryShortcuts.chunked(2).forEach { rowShortcuts ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowShortcuts.forEach { shortcut ->
                    LibraryShortcutCard(
                        shortcut = shortcut,
                        onClick = { onOpenLibrary(shortcut.tab) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (rowShortcuts.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LibraryShortcutCard(
    shortcut: HomeLibraryShortcut,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PressableHomeCard(
        onClick = onClick,
        modifier = modifier.heightIn(min = 68.dp),
        shape = RoundedCornerShape(20.dp),
        pressedContainerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = shortcut.icon,
                        contentDescription = null,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            Text(
                text = shortcut.tab.title,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun HomeSecondaryShortcutRow(
    onOpenLibrary: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = secondaryLibraryShortcuts,
            key = { shortcut -> shortcut.tab }
        ) { shortcut ->
            SecondaryLibraryShortcutCard(
                shortcut = shortcut,
                onClick = { onOpenLibrary(shortcut.tab) }
            )
        }
    }
}

@Composable
private fun SecondaryLibraryShortcutCard(
    shortcut: HomeLibraryShortcut,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PressableHomeCard(
        onClick = onClick,
        modifier = modifier
            .width(146.dp)
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        pressedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = shortcut.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = shortcut.tab.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1
            )
        }
    }
}
