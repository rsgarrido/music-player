package com.example.cdplaya.ui.library

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.cdplaya.ui.AppShellIconButton
import com.example.cdplaya.ui.AppShellIcons
import com.example.cdplaya.ui.AppShellTypography

enum class LibraryViewMode(val storageValue: String) {
    LIST("list"),
    GRID("grid");

    fun toggled(): LibraryViewMode {
        return if (this == LIST) GRID else LIST
    }

    companion object {
        fun fromStorageValue(value: String?): LibraryViewMode {
            return entries.firstOrNull { mode -> mode.storageValue == value } ?: LIST
        }
    }
}

enum class LibraryViewCategory(val storageKey: String) {
    SONGS("songs_view_mode"),
    ALBUMS("albums_view_mode"),
    ARTISTS("artists_view_mode")
}

object LibraryGridColumns {
    const val DEFAULT = 2
    val supported = 2..4

    fun normalize(value: Int): Int {
        return if (value in supported) value else DEFAULT
    }
}

enum class LibraryViewOption(
    val viewMode: LibraryViewMode,
    val gridColumnCount: Int?
) {
    LIST(LibraryViewMode.LIST, null),
    GRID_2(LibraryViewMode.GRID, 2),
    GRID_3(LibraryViewMode.GRID, 3),
    GRID_4(LibraryViewMode.GRID, 4);

    val label: String
        get() = if (this == LIST) "List" else "Grid: $gridColumnCount columns"
}

fun LibraryTab.viewCategory(): LibraryViewCategory? {
    return when (this) {
        LibraryTab.SONGS -> LibraryViewCategory.SONGS
        LibraryTab.ALBUMS -> LibraryViewCategory.ALBUMS
        LibraryTab.ARTISTS -> LibraryViewCategory.ARTISTS
        else -> null
    }
}

class LibraryViewPreferences internal constructor(
    private val preferences: SharedPreferences
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    )

    fun getViewMode(category: LibraryViewCategory): LibraryViewMode {
        val storedValue = runCatching {
            preferences.getString(category.storageKey, null)
        }.getOrNull()

        return LibraryViewMode.fromStorageValue(storedValue)
    }

    fun saveViewMode(category: LibraryViewCategory, mode: LibraryViewMode) {
        preferences.edit()
            .putString(category.storageKey, mode.storageValue)
            .apply()
    }

    fun getGridColumnCount(category: LibraryViewCategory): Int {
        val storedValue = runCatching {
            preferences.getInt(category.gridColumnStorageKey, LibraryGridColumns.DEFAULT)
        }.getOrDefault(LibraryGridColumns.DEFAULT)

        return LibraryGridColumns.normalize(storedValue)
    }

    fun saveGridColumnCount(category: LibraryViewCategory, columnCount: Int) {
        preferences.edit()
            .putInt(category.gridColumnStorageKey, LibraryGridColumns.normalize(columnCount))
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "library_view_preferences"
    }
}

internal val LibraryViewCategory.gridColumnStorageKey: String
    get() = "${storageKey}_columns"

@Stable
class LibraryViewModeState internal constructor(
    private val preferences: LibraryViewPreferences
) {
    private var songsMode by mutableStateOf(
        preferences.getViewMode(LibraryViewCategory.SONGS)
    )
    private var albumsMode by mutableStateOf(
        preferences.getViewMode(LibraryViewCategory.ALBUMS)
    )
    private var artistsMode by mutableStateOf(
        preferences.getViewMode(LibraryViewCategory.ARTISTS)
    )
    private var songsGridColumns by mutableStateOf(
        preferences.getGridColumnCount(LibraryViewCategory.SONGS)
    )
    private var albumsGridColumns by mutableStateOf(
        preferences.getGridColumnCount(LibraryViewCategory.ALBUMS)
    )
    private var artistsGridColumns by mutableStateOf(
        preferences.getGridColumnCount(LibraryViewCategory.ARTISTS)
    )

    fun modeFor(tab: LibraryTab): LibraryViewMode {
        return when (tab.viewCategory()) {
            LibraryViewCategory.SONGS -> songsMode
            LibraryViewCategory.ALBUMS -> albumsMode
            LibraryViewCategory.ARTISTS -> artistsMode
            null -> LibraryViewMode.LIST
        }
    }

    fun toggle(tab: LibraryTab) {
        val category = tab.viewCategory() ?: return
        val nextMode = modeFor(tab).toggled()

        when (category) {
            LibraryViewCategory.SONGS -> songsMode = nextMode
            LibraryViewCategory.ALBUMS -> albumsMode = nextMode
            LibraryViewCategory.ARTISTS -> artistsMode = nextMode
        }

        preferences.saveViewMode(category, nextMode)
    }

    fun gridColumnCountFor(tab: LibraryTab): Int {
        return when (tab.viewCategory()) {
            LibraryViewCategory.SONGS -> songsGridColumns
            LibraryViewCategory.ALBUMS -> albumsGridColumns
            LibraryViewCategory.ARTISTS -> artistsGridColumns
            null -> LibraryGridColumns.DEFAULT
        }
    }

    fun select(tab: LibraryTab, option: LibraryViewOption) {
        val category = tab.viewCategory() ?: return

        when (category) {
            LibraryViewCategory.SONGS -> songsMode = option.viewMode
            LibraryViewCategory.ALBUMS -> albumsMode = option.viewMode
            LibraryViewCategory.ARTISTS -> artistsMode = option.viewMode
        }
        preferences.saveViewMode(category, option.viewMode)

        option.gridColumnCount?.let { columnCount ->
            val normalizedCount = LibraryGridColumns.normalize(columnCount)
            when (category) {
                LibraryViewCategory.SONGS -> songsGridColumns = normalizedCount
                LibraryViewCategory.ALBUMS -> albumsGridColumns = normalizedCount
                LibraryViewCategory.ARTISTS -> artistsGridColumns = normalizedCount
            }
            preferences.saveGridColumnCount(category, normalizedCount)
        }
    }
}

@Composable
fun rememberLibraryViewModeState(): LibraryViewModeState {
    val applicationContext = LocalContext.current.applicationContext
    return remember(applicationContext) {
        LibraryViewModeState(LibraryViewPreferences(applicationContext))
    }
}

@Composable
fun LibraryViewOptionsButton(
    viewMode: LibraryViewMode,
    gridColumnCount: Int,
    onClick: () -> Unit
) {
    val description = if (viewMode == LibraryViewMode.LIST) {
        "View options, currently list"
    } else {
        "View options, currently grid, $gridColumnCount columns"
    }
    AppShellIconButton(
        onClick = onClick,
        imageVector = if (viewMode == LibraryViewMode.LIST) {
            AppShellIcons.ListView
        } else {
            AppShellIcons.GridView
        },
        contentDescription = description
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryViewOptionsSheet(
    viewMode: LibraryViewMode,
    gridColumnCount: Int,
    onOptionSelected: (LibraryViewOption) -> Unit,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text = "VIEW OPTIONS",
                style = AppShellTypography.Eyebrow,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )

            LibraryViewOption.entries.forEach { option ->
                val isSelected = option.viewMode == viewMode &&
                        (option.gridColumnCount == null ||
                                option.gridColumnCount == gridColumnCount)
                LibraryViewOptionRow(
                    option = option,
                    isSelected = isSelected,
                    onClick = {
                        onOptionSelected(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun LibraryViewOptionRow(
    option: LibraryViewOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon: ImageVector = if (option.viewMode == LibraryViewMode.LIST) {
        AppShellIcons.ListView
    } else {
        AppShellIcons.GridView
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            1.dp,
            if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.62f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.54f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = option.label,
                style = AppShellTypography.SongTitle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
