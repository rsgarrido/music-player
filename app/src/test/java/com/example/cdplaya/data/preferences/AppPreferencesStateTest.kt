package com.example.cdplaya.data.preferences

import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.player.replaygain.ReplayGainMode
import com.example.cdplaya.ui.library.LibraryViewMode
import com.example.cdplaya.ui.player.modern.ModernArtworkTransitionStyle
import com.example.cdplaya.ui.player.modern.ModernSeekbarStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPreferencesStateTest {
    @Test
    fun invalidEnumsAndGridCountsFallBackSafely() {
        val preferences = mutablePreferencesOf(
            stringPreferencesKey("selected_player_theme") to "missing-theme",
            stringPreferencesKey("replay_gain_mode") to "LOUDER_THAN_INFINITY",
            stringPreferencesKey("artwork_transition_style") to "missing-transition",
            stringPreferencesKey("seekbar_style") to "missing-seekbar",
            intPreferencesKey("songs_view_mode_columns") to 99
        )

        val state = decodeAppPreferences(preferences)

        assertEquals(PlayerTheme.DEFAULT, state.selectedPlayerTheme)
        assertEquals(ReplayGainMode.OFF, state.replayGainMode)
        assertEquals(ModernArtworkTransitionStyle.SLIDE, state.modernArtworkTransitionStyle)
        assertEquals(ModernSeekbarStyle.CLASSIC_BAR, state.modernSeekbarStyle)
        assertEquals(2, state.songsGridColumnCount)
        assertTrue(state.isLoaded)
    }

    @Test
    fun argbTokensFolderSelectionsAndLibraryAppearanceRoundTripFromMigratedKeys() {
        val preferences = mutablePreferencesOf(
            stringPreferencesKey("retro_rack.accent") to "#80123456",
            stringSetPreferencesKey("selected_folders") to setOf("Music", "Card/Music"),
            stringPreferencesKey("songs_view_mode") to "grid",
            intPreferencesKey("songs_view_mode_columns") to 3
        )

        val state = decodeAppPreferences(preferences)

        assertEquals(
            Color(0x80123456.toInt()),
            state.playerThemeTokenOverrides[PlayerTheme.RETRO_RACK]?.accentColor
        )
        assertEquals(setOf("Music", "Card/Music"), state.selectedLibraryFolders)
        assertEquals(LibraryViewMode.GRID, state.songsViewMode)
        assertEquals(3, state.songsGridColumnCount)
    }
}
