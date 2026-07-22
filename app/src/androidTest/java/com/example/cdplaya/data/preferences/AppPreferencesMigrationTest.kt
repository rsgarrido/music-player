package com.example.cdplaya.data.preferences

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.player.replaygain.ReplayGainMode
import com.example.cdplaya.ui.library.LibraryViewCategory
import com.example.cdplaya.ui.library.LibraryViewMode
import com.example.cdplaya.ui.player.modern.ModernArtworkTransitionStyle
import com.example.cdplaya.ui.player.modern.ModernSeekbarStyle
import com.example.cdplaya.ui.player.theme.PlayerThemeTokenOverrides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppPreferencesMigrationTest {
    @Test
    fun existingDataStoreValueWinsOverLegacyValue() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storeName = "existing_value_wins_${System.nanoTime()}"
        context.getSharedPreferences(storeName, Context.MODE_PRIVATE).edit()
            .putString("selected_player_theme", PlayerTheme.RETRO_RACK.id)
            .commit()
        val key = stringPreferencesKey("selected_player_theme")
        val currentData = mutablePreferencesOf(key to PlayerTheme.POCKET_FLIP.id)

        val migrated = SharedPreferencesMigration(context, storeName).migrate(currentData)

        assertEquals(PlayerTheme.POCKET_FLIP.id, migrated[key])
    }

    @Test
    fun legacyStoresMigrateAndDataStoreBecomesTheSingleSourceOfTruth() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val suffix = System.nanoTime().toString()
        val dataStoreFile = "app_preferences_migration_test_$suffix.preferences_pb"
        seedLegacyPreferences(context)
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val repository = AppPreferencesRepository.create(
            context = context,
            scope = firstScope,
            dataStoreFileName = dataStoreFile
        )

        val migrated = withTimeout(5_000) { repository.awaitLoadedState() }
        assertEquals(PlayerTheme.RETRO_RACK, migrated.selectedPlayerTheme)
        assertEquals(ModernArtworkTransitionStyle.PARALLAX, migrated.modernArtworkTransitionStyle)
        assertEquals(ModernSeekbarStyle.THICK_CAPSULE, migrated.modernSeekbarStyle)
        assertEquals(ReplayGainMode.TRACK, migrated.replayGainMode)
        assertEquals(setOf("Music", "Podcasts"), migrated.selectedLibraryFolders)
        assertEquals(LibraryViewMode.GRID, migrated.songsViewMode)
        assertEquals(4, migrated.songsGridColumnCount)
        assertEquals(
            Color(0xFF123456.toInt()),
            migrated.playerThemeTokenOverrides[PlayerTheme.RETRO_RACK]?.accentColor
        )

        repository.setSelectedPlayerTheme(PlayerTheme.POCKET_FLIP)
        repository.setLibraryView(LibraryViewCategory.SONGS, LibraryViewMode.GRID, 99)
        repository.setThemeTokenOverrides(
            PlayerTheme.RETRO_RACK,
            PlayerThemeTokenOverrides(accentColor = Color(0xFFA1B2C3.toInt()))
        )
        repeat(20) { index ->
            repository.setModernSeekbarStyle(
                if (index == 19) ModernSeekbarStyle.SEGMENTED else ModernSeekbarStyle.SLIM_LINE
            )
        }
        val updated = withTimeout(5_000) {
            repository.state.firstMatching {
                it.selectedPlayerTheme == PlayerTheme.POCKET_FLIP &&
                    it.modernSeekbarStyle == ModernSeekbarStyle.SEGMENTED
            }
        }
        assertEquals(2, updated.songsGridColumnCount)
        assertEquals(
            Color(0xFFA1B2C3.toInt()),
            updated.playerThemeTokenOverrides[PlayerTheme.RETRO_RACK]?.accentColor
        )
        firstScope.cancel()
    }

    private fun seedLegacyPreferences(context: Context) {
        context.getSharedPreferences("player_theme_preferences", Context.MODE_PRIVATE).edit()
            .putString("selected_player_theme", PlayerTheme.RETRO_RACK.id).commit()
        context.getSharedPreferences("modern_player_preferences", Context.MODE_PRIVATE).edit()
            .putString("artwork_transition_style", "parallax")
            .putString("seekbar_style", "thick_capsule").commit()
        context.getSharedPreferences("replay_gain_preferences", Context.MODE_PRIVATE).edit()
            .putString("replay_gain_mode", ReplayGainMode.TRACK.name).commit()
        context.getSharedPreferences("library_preferences", Context.MODE_PRIVATE).edit()
            .putStringSet("selected_folders", setOf("Music", "Podcasts")).commit()
        context.getSharedPreferences("library_view_preferences", Context.MODE_PRIVATE).edit()
            .putString("songs_view_mode", "grid")
            .putInt("songs_view_mode_columns", 4).commit()
        context.getSharedPreferences("player_theme_token_preferences", Context.MODE_PRIVATE).edit()
            .putString("retro_rack.accent", "#FF123456").commit()
    }
}

private suspend fun kotlinx.coroutines.flow.StateFlow<AppPreferencesState>.firstMatching(
    predicate: (AppPreferencesState) -> Boolean
): AppPreferencesState = first(predicate)
