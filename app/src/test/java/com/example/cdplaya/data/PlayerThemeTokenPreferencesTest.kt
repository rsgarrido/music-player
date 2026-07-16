package com.example.cdplaya.data

import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import com.example.cdplaya.ui.player.theme.PlayerThemeTokenOverrides
import com.example.cdplaya.ui.player.theme.applyOverrides
import com.example.cdplaya.ui.player.theme.defaultTokens
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.nullable
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class PlayerThemeTokenPreferencesTest {
    @Test
    fun saveOverrides_roundTripsThemeOverrides() {
        val harness = SharedPreferencesHarness()
        val preferences = PlayerThemeTokenPreferences(harness.preferences)
        val overrides = PlayerThemeTokenOverrides(
            shellColor = Color(0xFFAABBCC),
            accentColor = Color(0xFF123456),
            secondaryAccentColor = Color(0x80112233)
        )

        preferences.saveOverrides(PlayerTheme.CLASSIC_WHEEL, overrides)

        assertEquals(overrides, preferences.getOverrides(PlayerTheme.CLASSIC_WHEEL))
        assertEquals(
            PlayerTheme.CLASSIC_WHEEL.defaultTokens().applyOverrides(overrides),
            preferences.getTokens(PlayerTheme.CLASSIC_WHEEL)
        )
        assertEquals("#FFAABBCC", harness.values["classic_wheel.shell"])
        assertEquals("#80112233", harness.values["classic_wheel.secondary_accent"])
    }

    @Test
    fun getTokens_missingValuesFallBackToThemeDefaults() {
        val preferences = PlayerThemeTokenPreferences(SharedPreferencesHarness().preferences)

        assertEquals(
            PlayerTheme.POCKET_CASSETTE.defaultTokens(),
            preferences.getTokens(PlayerTheme.POCKET_CASSETTE)
        )
    }

    @Test
    fun getTokens_invalidValuesAreIgnoredAndValidValuesStillApply() {
        val harness = SharedPreferencesHarness().apply {
            values["retro_rack.shell"] = "not-a-color"
            values["retro_rack.accent"] = 0xFF123456.toInt()
            values["retro_rack.secondary_accent"] = "#FFABCDEF"
        }
        val preferences = PlayerThemeTokenPreferences(harness.preferences)
        val expected = PlayerTheme.RETRO_RACK.defaultTokens().applyOverrides(
            PlayerThemeTokenOverrides(secondaryAccentColor = Color(0xFFABCDEF))
        )

        assertEquals(expected, preferences.getTokens(PlayerTheme.RETRO_RACK))
    }

    @Test
    fun clearOverrides_removesOnlySelectedTheme() {
        val preferences = PlayerThemeTokenPreferences(SharedPreferencesHarness().preferences)
        val classicOverrides = PlayerThemeTokenOverrides(shellColor = Color(0xFF111111))
        val flipOverrides = PlayerThemeTokenOverrides(shellColor = Color(0xFF222222))
        preferences.saveOverrides(PlayerTheme.CLASSIC_WHEEL, classicOverrides)
        preferences.saveOverrides(PlayerTheme.POCKET_FLIP, flipOverrides)

        preferences.clearOverrides(PlayerTheme.CLASSIC_WHEEL)

        assertEquals(PlayerThemeTokenOverrides(), preferences.getOverrides(PlayerTheme.CLASSIC_WHEEL))
        assertEquals(
            PlayerTheme.CLASSIC_WHEEL.defaultTokens(),
            preferences.getTokens(PlayerTheme.CLASSIC_WHEEL)
        )
        assertEquals(flipOverrides, preferences.getOverrides(PlayerTheme.POCKET_FLIP))
    }

    @Test
    fun getTokens_defaultThemeIgnoresStoredOverrides() {
        val preferences = PlayerThemeTokenPreferences(SharedPreferencesHarness().preferences)
        preferences.saveOverrides(
            PlayerTheme.DEFAULT,
            PlayerThemeTokenOverrides(shellColor = Color.Red)
        )

        assertEquals(PlayerTheme.DEFAULT.defaultTokens(), preferences.getTokens(PlayerTheme.DEFAULT))
    }

    @Test
    fun clearAllOverrides_removesEveryTheme() {
        val preferences = PlayerThemeTokenPreferences(SharedPreferencesHarness().preferences)
        preferences.saveOverrides(
            PlayerTheme.CLASSIC_WHEEL,
            PlayerThemeTokenOverrides(shellColor = Color.Red)
        )
        preferences.saveOverrides(
            PlayerTheme.RETRO_RACK,
            PlayerThemeTokenOverrides(accentColor = Color.Green)
        )

        preferences.clearAllOverrides()

        assertEquals(PlayerThemeTokenOverrides(), preferences.getOverrides(PlayerTheme.CLASSIC_WHEEL))
        assertEquals(PlayerThemeTokenOverrides(), preferences.getOverrides(PlayerTheme.RETRO_RACK))
    }

    private class SharedPreferencesHarness {
        val values = mutableMapOf<String, Any>()
        val preferences: SharedPreferences = mock(SharedPreferences::class.java)
        private val editor: SharedPreferences.Editor = mock(SharedPreferences.Editor::class.java)

        init {
            doAnswer { invocation ->
                val key = invocation.getArgument<String>(0)
                val defaultValue = invocation.getArgument<String?>(1)
                when (val value = values[key]) {
                    null -> defaultValue
                    is String -> value
                    else -> throw ClassCastException("Value for $key is not a String")
                }
            }.`when`(preferences).getString(anyString(), nullable(String::class.java))
            `when`(preferences.edit()).thenReturn(editor)
            doAnswer { invocation ->
                values.remove(invocation.getArgument<String>(0))
                editor
            }.`when`(editor).remove(anyString())
            doAnswer { invocation ->
                val key = invocation.getArgument<String>(0)
                val value = invocation.getArgument<String?>(1)
                if (value == null) values.remove(key) else values[key] = value
                editor
            }.`when`(editor).putString(anyString(), nullable(String::class.java))
            doAnswer {
                values.clear()
                editor
            }.`when`(editor).clear()
        }
    }
}
