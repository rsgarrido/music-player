package com.example.cdplaya.data.preferences

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.player.audio.AudioOffloadPreference
import com.example.cdplaya.player.replaygain.ReplayGainMode
import com.example.cdplaya.ui.library.LibraryGridColumns
import com.example.cdplaya.ui.library.LibraryViewCategory
import com.example.cdplaya.ui.library.LibraryViewMode
import com.example.cdplaya.ui.player.modern.ModernArtworkTransitionStyle
import com.example.cdplaya.ui.player.modern.ModernSeekbarStyle
import com.example.cdplaya.ui.player.theme.PlayerThemeTokenOverrides
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

data class AppPreferencesState(
    val selectedPlayerTheme: PlayerTheme = PlayerTheme.DEFAULT,
    val playerThemeTokenOverrides: Map<PlayerTheme, PlayerThemeTokenOverrides> = emptyMap(),
    val modernArtworkTransitionStyle: ModernArtworkTransitionStyle =
        ModernArtworkTransitionStyle.SLIDE,
    val modernSeekbarStyle: ModernSeekbarStyle = ModernSeekbarStyle.CLASSIC_BAR,
    val replayGainMode: ReplayGainMode = ReplayGainMode.OFF,
    val audioOffloadPreference: AudioOffloadPreference = AudioOffloadPreference.DISABLED,
    val selectedLibraryFolders: Set<String> = emptySet(),
    val songsViewMode: LibraryViewMode = LibraryViewMode.LIST,
    val albumsViewMode: LibraryViewMode = LibraryViewMode.LIST,
    val artistsViewMode: LibraryViewMode = LibraryViewMode.LIST,
    val songsGridColumnCount: Int = LibraryGridColumns.DEFAULT,
    val albumsGridColumnCount: Int = LibraryGridColumns.DEFAULT,
    val artistsGridColumnCount: Int = LibraryGridColumns.DEFAULT,
    val isLoaded: Boolean = false
)

class AppPreferencesRepository private constructor(
    private val dataStore: DataStore<Preferences>,
    scope: CoroutineScope
) {
    val state: StateFlow<AppPreferencesState> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(::decodeAppPreferences)
        .stateIn(scope, SharingStarted.Eagerly, AppPreferencesState())

    suspend fun awaitLoadedState(): AppPreferencesState = state.filter { it.isLoaded }.first()

    suspend fun setSelectedPlayerTheme(theme: PlayerTheme) = edit {
        it[Keys.selectedPlayerTheme] = theme.id
    }

    suspend fun setModernArtworkTransitionStyle(style: ModernArtworkTransitionStyle) = edit {
        it[Keys.modernArtworkTransitionStyle] = style.storageValue
    }

    suspend fun setModernSeekbarStyle(style: ModernSeekbarStyle) = edit {
        it[Keys.modernSeekbarStyle] = style.storageValue
    }

    suspend fun setReplayGainMode(mode: ReplayGainMode) = edit {
        it[Keys.replayGainMode] = mode.name
    }

    suspend fun setAudioOffloadPreference(preference: AudioOffloadPreference) = edit {
        it[Keys.audioOffloadPreference] = preference.name
    }

    suspend fun setSelectedLibraryFolders(folders: Set<String>) = edit {
        it[Keys.selectedLibraryFolders] = folders.toSet()
    }

    suspend fun setLibraryView(
        category: LibraryViewCategory,
        mode: LibraryViewMode,
        gridColumnCount: Int
    ) = edit { preferences ->
        preferences[Keys.viewMode(category)] = mode.storageValue
        preferences[Keys.gridColumns(category)] = LibraryGridColumns.normalize(gridColumnCount)
    }

    suspend fun setThemeTokenOverrides(
        theme: PlayerTheme,
        overrides: PlayerThemeTokenOverrides
    ) = edit { preferences ->
        Keys.themeFields.forEach { field -> preferences.remove(Keys.themeColor(theme, field)) }
        preferences.putColor(theme, Keys.SHELL, overrides.shellColor)
        preferences.putColor(theme, Keys.ACCENT, overrides.accentColor)
        preferences.putColor(theme, Keys.DISPLAY_BACKGROUND, overrides.displayBackgroundColor)
        preferences.putColor(theme, Keys.DISPLAY_TEXT, overrides.displayTextColor)
        preferences.putColor(theme, Keys.SECONDARY_ACCENT, overrides.secondaryAccentColor)
    }

    suspend fun clearThemeTokenOverrides(theme: PlayerTheme) = edit { preferences ->
        Keys.themeFields.forEach { field -> preferences.remove(Keys.themeColor(theme, field)) }
    }

    suspend fun replaceAll(restored: AppPreferencesState) = edit { preferences ->
        preferences.clear()
        preferences[Keys.selectedPlayerTheme] = restored.selectedPlayerTheme.id
        preferences[Keys.modernArtworkTransitionStyle] =
            restored.modernArtworkTransitionStyle.storageValue
        preferences[Keys.modernSeekbarStyle] = restored.modernSeekbarStyle.storageValue
        preferences[Keys.replayGainMode] = restored.replayGainMode.name
        preferences[Keys.audioOffloadPreference] = restored.audioOffloadPreference.name
        preferences[Keys.selectedLibraryFolders] = restored.selectedLibraryFolders.toSet()
        LibraryViewCategory.entries.forEach { category ->
            val (mode, columns) = restored.libraryView(category)
            preferences[Keys.viewMode(category)] = mode.storageValue
            preferences[Keys.gridColumns(category)] = LibraryGridColumns.normalize(columns)
        }
        restored.playerThemeTokenOverrides.forEach { (theme, overrides) ->
            preferences.putColor(theme, Keys.SHELL, overrides.shellColor)
            preferences.putColor(theme, Keys.ACCENT, overrides.accentColor)
            preferences.putColor(theme, Keys.DISPLAY_BACKGROUND, overrides.displayBackgroundColor)
            preferences.putColor(theme, Keys.DISPLAY_TEXT, overrides.displayTextColor)
            preferences.putColor(theme, Keys.SECONDARY_ACCENT, overrides.secondaryAccentColor)
        }
    }

    private suspend inline fun edit(crossinline transform: (MutablePreferences) -> Unit) {
        dataStore.edit { preferences -> transform(preferences) }
    }

    companion object {
        private const val DATASTORE_FILE = "app_preferences.preferences_pb"
        private val LEGACY_STORES = listOf(
            "player_theme_preferences",
            "player_theme_token_preferences",
            "modern_player_preferences",
            "replay_gain_preferences",
            "library_preferences",
            "library_view_preferences"
        )

        @Volatile private var instance: AppPreferencesRepository? = null

        fun getInstance(context: Context): AppPreferencesRepository {
            return instance ?: synchronized(this) {
                instance ?: create(context.applicationContext).also { instance = it }
            }
        }

        internal fun create(
            context: Context,
            scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            dataStoreFileName: String = DATASTORE_FILE,
            legacyStores: List<String> = LEGACY_STORES
        ): AppPreferencesRepository {
            val dataStore = PreferenceDataStoreFactory.create(
                migrations = legacyStores.map { name ->
                    SharedPreferencesMigration(context, name)
                },
                scope = scope,
                produceFile = { context.preferencesDataStoreFile(dataStoreFileName) }
            )
            return AppPreferencesRepository(dataStore, scope)
        }
    }
}

internal fun decodeAppPreferences(preferences: Preferences): AppPreferencesState = AppPreferencesState(
    selectedPlayerTheme = PlayerTheme.fromId(preferences[Keys.selectedPlayerTheme]),
    playerThemeTokenOverrides = PlayerTheme.entries.associateWith { emptyOverrides() }
        .mapValues { (theme, _) ->
            PlayerThemeTokenOverrides(
                shellColor = preferences.color(theme, Keys.SHELL),
                accentColor = preferences.color(theme, Keys.ACCENT),
                displayBackgroundColor = preferences.color(theme, Keys.DISPLAY_BACKGROUND),
                displayTextColor = preferences.color(theme, Keys.DISPLAY_TEXT),
                secondaryAccentColor = preferences.color(theme, Keys.SECONDARY_ACCENT)
            )
        }
        .filterValues { it != emptyOverrides() },
    modernArtworkTransitionStyle = ModernArtworkTransitionStyle.fromStorageValue(
        preferences[Keys.modernArtworkTransitionStyle]
    ),
    modernSeekbarStyle = ModernSeekbarStyle.fromStorageValue(preferences[Keys.modernSeekbarStyle]),
    replayGainMode = runCatching {
        ReplayGainMode.valueOf(preferences[Keys.replayGainMode].orEmpty())
    }.getOrDefault(ReplayGainMode.OFF),
    audioOffloadPreference = AudioOffloadPreference.fromStorageValue(
        preferences[Keys.audioOffloadPreference]
    ),
    selectedLibraryFolders = preferences[Keys.selectedLibraryFolders]?.toSet().orEmpty(),
    songsViewMode = LibraryViewMode.fromStorageValue(preferences[Keys.songsViewMode]),
    albumsViewMode = LibraryViewMode.fromStorageValue(preferences[Keys.albumsViewMode]),
    artistsViewMode = LibraryViewMode.fromStorageValue(preferences[Keys.artistsViewMode]),
    songsGridColumnCount = LibraryGridColumns.normalize(
        preferences[Keys.songsGridColumns] ?: LibraryGridColumns.DEFAULT
    ),
    albumsGridColumnCount = LibraryGridColumns.normalize(
        preferences[Keys.albumsGridColumns] ?: LibraryGridColumns.DEFAULT
    ),
    artistsGridColumnCount = LibraryGridColumns.normalize(
        preferences[Keys.artistsGridColumns] ?: LibraryGridColumns.DEFAULT
    ),
    isLoaded = true
)

private fun emptyOverrides() = PlayerThemeTokenOverrides()

private fun AppPreferencesState.libraryView(
    category: LibraryViewCategory
): Pair<LibraryViewMode, Int> = when (category) {
    LibraryViewCategory.SONGS -> songsViewMode to songsGridColumnCount
    LibraryViewCategory.ALBUMS -> albumsViewMode to albumsGridColumnCount
    LibraryViewCategory.ARTISTS -> artistsViewMode to artistsGridColumnCount
}

private fun Preferences.color(theme: PlayerTheme, field: String): Color? {
    val stored = this[Keys.themeColor(theme, field)] ?: return null
    if (stored.length != 9 || stored.first() != '#') return null
    return stored.drop(1).toUIntOrNull(16)?.let { Color(it.toInt()) }
}

private fun MutablePreferences.putColor(
    theme: PlayerTheme,
    field: String,
    color: Color?
) {
    color ?: return
    val encoded = color.toArgb().toUInt().toString(16).padStart(8, '0').uppercase()
    this[Keys.themeColor(theme, field)] = "#$encoded"
}

private object Keys {
    val selectedPlayerTheme = stringPreferencesKey("selected_player_theme")
    val modernArtworkTransitionStyle = stringPreferencesKey("artwork_transition_style")
    val modernSeekbarStyle = stringPreferencesKey("seekbar_style")
    val replayGainMode = stringPreferencesKey("replay_gain_mode")
    val audioOffloadPreference = stringPreferencesKey("audio_offload_preference")
    val selectedLibraryFolders = stringSetPreferencesKey("selected_folders")
    val songsViewMode = stringPreferencesKey("songs_view_mode")
    val albumsViewMode = stringPreferencesKey("albums_view_mode")
    val artistsViewMode = stringPreferencesKey("artists_view_mode")
    val songsGridColumns = intPreferencesKey("songs_view_mode_columns")
    val albumsGridColumns = intPreferencesKey("albums_view_mode_columns")
    val artistsGridColumns = intPreferencesKey("artists_view_mode_columns")

    const val SHELL = "shell"
    const val ACCENT = "accent"
    const val DISPLAY_BACKGROUND = "display_background"
    const val DISPLAY_TEXT = "display_text"
    const val SECONDARY_ACCENT = "secondary_accent"
    val themeFields = listOf(SHELL, ACCENT, DISPLAY_BACKGROUND, DISPLAY_TEXT, SECONDARY_ACCENT)

    fun themeColor(theme: PlayerTheme, field: String) =
        stringPreferencesKey("${theme.id}.$field")

    fun viewMode(category: LibraryViewCategory) = when (category) {
        LibraryViewCategory.SONGS -> songsViewMode
        LibraryViewCategory.ALBUMS -> albumsViewMode
        LibraryViewCategory.ARTISTS -> artistsViewMode
    }

    fun gridColumns(category: LibraryViewCategory) = when (category) {
        LibraryViewCategory.SONGS -> songsGridColumns
        LibraryViewCategory.ALBUMS -> albumsGridColumns
        LibraryViewCategory.ARTISTS -> artistsGridColumns
    }
}
