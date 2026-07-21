package com.example.cdplaya.ui.library

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.cdplaya.ui.AppShellIconButton
import com.example.cdplaya.ui.AppShellIcons

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

    private companion object {
        const val PREFERENCES_NAME = "library_view_preferences"
    }
}

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
}

@Composable
fun rememberLibraryViewModeState(): LibraryViewModeState {
    val applicationContext = LocalContext.current.applicationContext
    return remember(applicationContext) {
        LibraryViewModeState(LibraryViewPreferences(applicationContext))
    }
}

@Composable
fun LibraryViewModeToggle(
    viewMode: LibraryViewMode,
    onToggle: () -> Unit
) {
    val isList = viewMode == LibraryViewMode.LIST
    AppShellIconButton(
        onClick = onToggle,
        imageVector = if (isList) AppShellIcons.GridView else AppShellIcons.ListView,
        contentDescription = if (isList) "Grid view" else "List view"
    )
}
