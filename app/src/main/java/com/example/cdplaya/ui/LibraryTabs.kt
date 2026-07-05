package com.example.cdplaya.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LibraryTabs(
    selectedTab: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = LibraryTab.entries.indexOf(selectedTab),
        edgePadding = 8.dp
    ) {
        LibraryTab.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = {
                    onTabSelected(tab)
                },
                text = {
                    Text(
                        text = tab.title,
                        maxLines = 1
                    )
                }
            )
        }
    }
}