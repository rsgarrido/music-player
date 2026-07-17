package com.example.cdplaya.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

private val homeLibraryShortcuts = listOf(
    HomeLibraryShortcut(LibraryTab.SONGS, Icons.Filled.MusicNote),
    HomeLibraryShortcut(LibraryTab.ALBUMS, Icons.Filled.Album),
    HomeLibraryShortcut(LibraryTab.ARTISTS, Icons.Filled.People),
    HomeLibraryShortcut(LibraryTab.PLAYLISTS, Icons.Filled.LibraryMusic),
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
        homeLibraryShortcuts.chunked(2).forEach { rowShortcuts ->
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
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 76.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
