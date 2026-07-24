package com.example.cdplaya.data.preferences

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.player.audio.AudioOffloadPreference
import com.example.cdplaya.player.equalizer.EqualizerPreferencesState
import com.example.cdplaya.player.equalizer.GraphicEqualizerPresets
import com.example.cdplaya.player.equalizer.UserEqualizerPreset
import com.example.cdplaya.player.equalizer.normalizeBandGains
import com.example.cdplaya.player.equalizer.normalizeEqualizerDb
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class AppPreferencesState(
    val selectedPlayerTheme: PlayerTheme = PlayerTheme.DEFAULT,
    val playerThemeTokenOverrides: Map<PlayerTheme, PlayerThemeTokenOverrides> = emptyMap(),
    val modernArtworkTransitionStyle: ModernArtworkTransitionStyle =
        ModernArtworkTransitionStyle.SLIDE,
    val modernSeekbarStyle: ModernSeekbarStyle = ModernSeekbarStyle.CLASSIC_BAR,
    val replayGainMode: ReplayGainMode = ReplayGainMode.OFF,
    val audioOffloadPreference: AudioOffloadPreference = AudioOffloadPreference.DISABLED,
    val equalizerPreferences: EqualizerPreferencesState =
        EqualizerPreferencesState(),
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

    suspend fun setEqualizerEnabled(enabled: Boolean) = edit {
        it[Keys.equalizerEnabled] = enabled
    }

    suspend fun setEqualizerPreampDb(preampDb: Double) = edit {
        val updated = decodeAppPreferences(it).equalizerPreferences
            .withPreampDb(preampDb)
        it[Keys.equalizerPreampDb] = updated.preampDb
    }

    suspend fun setEqualizerAutomaticHeadroomEnabled(
        enabled: Boolean
    ) = edit {
        it[Keys.equalizerAutomaticHeadroom] = enabled
    }

    suspend fun setEqualizerBandGainDb(
        index: Int,
        gainDb: Double
    ) = edit { preferences ->
        val updated = decodeAppPreferences(preferences)
            .equalizerPreferences
            .withBandGainDb(index, gainDb)
        preferences[Keys.equalizerBandGains[index]] =
            updated.bandGainsDb[index]
    }

    suspend fun replaceEqualizerCurve(
        preampDb: Double,
        automaticHeadroomEnabled: Boolean,
        bandGainsDb: List<Double>
    ) = edit { preferences ->
        val updated = decodeAppPreferences(preferences)
            .equalizerPreferences.withCurve(
            preampDb = preampDb,
            automaticHeadroomEnabled =
                automaticHeadroomEnabled,
            bandGainsDb = bandGainsDb
        )
        preferences.writeEqualizerPreferences(updated)
    }

    suspend fun replaceEqualizerPreferences(
        equalizerPreferences: EqualizerPreferencesState
    ) = edit { preferences ->
        preferences.writeEqualizerPreferences(
            equalizerPreferences.copy(
                bandGainsDb =
                    equalizerPreferences.bandGainsDb.toList(),
                userPresets =
                    equalizerPreferences.userPresets.toList()
            )
        )
    }

    suspend fun saveUserEqualizerPreset(
        name: String,
        curve: EqualizerPreferencesState? = null
    ): UserEqualizerPreset {
        lateinit var preset: UserEqualizerPreset
        dataStore.edit { preferences ->
            val current = decodeAppPreferences(preferences)
                .equalizerPreferences
            val source = curve?.copy(
                userPresets = current.userPresets
            ) ?: current
            preset = GraphicEqualizerPresets.createUserPreset(
                name = name,
                state = source
            )
            preferences.writeEqualizerPreferences(
                source.copy(
                    userPresets =
                        current.userPresets + preset
                )
            )
        }
        return preset
    }

    suspend fun renameUserEqualizerPreset(
        presetId: String,
        newName: String
    ) = edit { preferences ->
        val current = decodeAppPreferences(preferences)
            .equalizerPreferences
        val updated = GraphicEqualizerPresets.renameUserPreset(
            presetId = presetId,
            newName = newName,
            userPresets = current.userPresets
        )
        preferences.writeUserEqualizerPresets(updated)
    }

    suspend fun deleteUserEqualizerPreset(
        presetId: String
    ) = edit { preferences ->
        val current = decodeAppPreferences(preferences)
            .equalizerPreferences
            .userPresets
        require(current.any { preset -> preset.id == presetId }) {
            "Unknown user equalizer preset ID: $presetId"
        }
        preferences.writeUserEqualizerPresets(
            current.filterNot { preset -> preset.id == presetId }
        )
    }

    suspend fun replaceUserEqualizerPresets(
        userPresets: List<UserEqualizerPreset>
    ) = edit { preferences ->
        EqualizerPreferencesState(
            userPresets = userPresets
        )
        preferences.writeUserEqualizerPresets(userPresets)
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
        preferences.writeEqualizerPreferences(
            restored.equalizerPreferences
        )
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
    equalizerPreferences = decodeEqualizerPreferences(preferences),
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

private fun decodeEqualizerPreferences(
    preferences: Preferences
): EqualizerPreferencesState {
    val default = EqualizerPreferencesState()
    val preampDb = preferences[Keys.equalizerPreampDb]
        ?.validNormalizedPreampOrNull()
        ?: default.preampDb
    val bandGainsDb = Keys.equalizerBandGains.mapIndexed {
            index,
            key ->
        preferences[key]
            ?.validNormalizedBandOrNull()
            ?: default.bandGainsDb[index]
    }
    val userPresets = preferences[Keys.equalizerUserPresets]
        ?.let(::decodeUserEqualizerPresets)
        .orEmpty()
    return EqualizerPreferencesState(
        enabled = preferences[Keys.equalizerEnabled]
            ?: default.enabled,
        preampDb = preampDb,
        automaticHeadroomEnabled =
            preferences[Keys.equalizerAutomaticHeadroom]
                ?: default.automaticHeadroomEnabled,
        bandGainsDb = bandGainsDb,
        userPresets = userPresets
    )
}

private fun decodeUserEqualizerPresets(
    encoded: String
): List<UserEqualizerPreset> {
    val decoded = runCatching {
        equalizerJson.decodeFromString<
            List<StoredUserEqualizerPreset>
        >(encoded)
    }.getOrDefault(emptyList())
    val names = mutableSetOf<String>()
    val ids = mutableSetOf<String>()
    return decoded.mapNotNull { stored ->
        runCatching {
            UserEqualizerPreset(
                id = stored.id,
                name = stored.name,
                preampDb = normalizeEqualizerDb(
                    stored.preampDb
                ),
                automaticHeadroomEnabled =
                    stored.automaticHeadroomEnabled,
                bandGainsDb =
                    normalizeBandGains(stored.bandGainsDb)
            )
        }.getOrNull()
    }.filter { preset ->
        preset.name.lowercase() !in
            GraphicEqualizerPresets.builtInNamesLowercase &&
            ids.add(preset.id) &&
            names.add(preset.name.lowercase())
    }
}

private fun MutablePreferences.writeEqualizerPreferences(
    state: EqualizerPreferencesState
) {
    this[Keys.equalizerEnabled] = state.enabled
    this[Keys.equalizerPreampDb] = state.preampDb
    this[Keys.equalizerAutomaticHeadroom] =
        state.automaticHeadroomEnabled
    Keys.equalizerBandGains.forEachIndexed { index, key ->
        this[key] = state.bandGainsDb[index]
    }
    writeUserEqualizerPresets(state.userPresets)
}

private fun MutablePreferences.writeUserEqualizerPresets(
    presets: List<UserEqualizerPreset>
) {
    val validated = EqualizerPreferencesState(
        userPresets = presets
    ).userPresets
    this[Keys.equalizerUserPresets] = equalizerJson.encodeToString(
        validated.map { preset ->
            StoredUserEqualizerPreset(
                id = preset.id,
                name = preset.name,
                preampDb = preset.preampDb,
                automaticHeadroomEnabled =
                    preset.automaticHeadroomEnabled,
                bandGainsDb = preset.bandGainsDb
            )
        }
    )
}

private fun Double.validNormalizedPreampOrNull(): Double? =
    runCatching {
        EqualizerPreferencesState().withPreampDb(this).preampDb
    }.getOrNull()

private fun Double.validNormalizedBandOrNull(): Double? =
    runCatching {
        EqualizerPreferencesState()
            .withBandGainDb(0, this)
            .bandGainsDb[0]
    }.getOrNull()

@Serializable
private data class StoredUserEqualizerPreset(
    val id: String,
    val name: String,
    val preampDb: Double,
    val automaticHeadroomEnabled: Boolean,
    val bandGainsDb: List<Double>
)

private val equalizerJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private object Keys {
    val selectedPlayerTheme = stringPreferencesKey("selected_player_theme")
    val modernArtworkTransitionStyle = stringPreferencesKey("artwork_transition_style")
    val modernSeekbarStyle = stringPreferencesKey("seekbar_style")
    val replayGainMode = stringPreferencesKey("replay_gain_mode")
    val audioOffloadPreference = stringPreferencesKey("audio_offload_preference")
    val equalizerEnabled =
        booleanPreferencesKey("equalizer_enabled")
    val equalizerPreampDb =
        doublePreferencesKey("equalizer_preamp_db")
    val equalizerAutomaticHeadroom =
        booleanPreferencesKey("equalizer_automatic_headroom")
    val equalizerBandGains = List(10) { index ->
        doublePreferencesKey("equalizer_band_${index}_db")
    }
    val equalizerUserPresets =
        stringPreferencesKey("equalizer_user_presets_json")
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
