package com.example.cdplaya.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private data class LibraryCategoryItem(
    val tab: LibraryTab,
    val icon: ImageVector
)

private val primaryCategories = listOf(
    LibraryCategoryItem(LibraryTab.SONGS, Icons.Filled.MusicNote),
    LibraryCategoryItem(LibraryTab.ALBUMS, Icons.Filled.Album),
    LibraryCategoryItem(LibraryTab.ARTISTS, Icons.Filled.People),
    LibraryCategoryItem(LibraryTab.PLAYLISTS, Icons.Filled.LibraryMusic)
)

private val secondaryCategories = listOf(
    LibraryCategoryItem(LibraryTab.FAVORITES, Icons.Filled.Favorite),
    LibraryCategoryItem(LibraryTab.RECENTLY_PLAYED, Icons.Filled.History),
    LibraryCategoryItem(LibraryTab.MOST_PLAYED, Icons.Filled.BarChart),
    LibraryCategoryItem(LibraryTab.QUEUE, Icons.AutoMirrored.Filled.PlaylistPlay)
)

@Composable
fun LibraryCategorySelector(
    selectedCategory: LibraryTab,
    onCategorySelected: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LibraryCategoryGroup(
            label = "Browse",
            categories = primaryCategories,
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected
        )

        LibraryCategoryGroup(
            label = "More",
            categories = secondaryCategories,
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected
        )
    }
}

@Composable
private fun LibraryCategoryGroup(
    label: String,
    categories: List<LibraryCategoryItem>,
    selectedCategory: LibraryTab,
    onCategorySelected: (LibraryTab) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                LibraryCategoryChip(
                    label = category.tab.title,
                    icon = category.icon,
                    selected = selectedCategory == category.tab,
                    onClick = { onCategorySelected(category.tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun LibraryCategoryChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(min = 72.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab
            ),
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        border = if (selected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, Color.Transparent)
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
