package com.example.cdplaya.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val primaryLibraryTabs = listOf(
    LibraryTab.SONGS,
    LibraryTab.ALBUMS,
    LibraryTab.ARTISTS,
    LibraryTab.PLAYLISTS
)

val songCollectionTabs = listOf(
    LibraryTab.SONGS,
    LibraryTab.FAVORITES,
    LibraryTab.RECENTLY_PLAYED,
    LibraryTab.MOST_PLAYED
)

fun LibraryTab.primaryBrowseTab(): LibraryTab? {
    return when (this) {
        LibraryTab.FAVORITES,
        LibraryTab.RECENTLY_PLAYED,
        LibraryTab.MOST_PLAYED -> LibraryTab.SONGS

        LibraryTab.QUEUE -> null
        else -> this
    }
}

@Composable
fun LibraryBrowseSwitcher(
    selectedTab: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedPrimaryTab = selectedTab.primaryBrowseTab() ?: return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(22.dp)
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                primaryLibraryTabs.forEach { tab ->
                    LibraryPrimaryTab(
                        tab = tab,
                        selected = tab == selectedPrimaryTab,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (selectedPrimaryTab == LibraryTab.SONGS) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = songCollectionTabs,
                    key = { tab -> tab }
                ) { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        label = {
                            Text(
                                text = when (tab) {
                                    LibraryTab.SONGS -> "All"
                                    LibraryTab.RECENTLY_PLAYED -> "Recent"
                                    LibraryTab.MOST_PLAYED -> "Most played"
                                    else -> tab.title
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryPrimaryTab(
    tab: LibraryTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        shape = RoundedCornerShape(18.dp)
    ) {
        Box(
            modifier = Modifier
                .clickable(role = Role.Tab, onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tab.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
