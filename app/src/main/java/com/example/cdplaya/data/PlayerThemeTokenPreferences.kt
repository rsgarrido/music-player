package com.example.cdplaya.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.cdplaya.ui.player.theme.PlayerThemeTokenOverrides
import com.example.cdplaya.ui.player.theme.PlayerThemeTokens
import com.example.cdplaya.ui.player.theme.applyOverrides
import com.example.cdplaya.ui.player.theme.defaultTokens

class PlayerThemeTokenPreferences internal constructor(
    private val preferences: SharedPreferences
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(
            PLAYER_THEME_TOKEN_PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
    )

    fun getOverrides(playerTheme: PlayerTheme): PlayerThemeTokenOverrides {
        return PlayerThemeTokenOverrides(
            shellColor = getColor(playerTheme, FIELD_SHELL),
            accentColor = getColor(playerTheme, FIELD_ACCENT),
            displayBackgroundColor = getColor(playerTheme, FIELD_DISPLAY_BACKGROUND),
            displayTextColor = getColor(playerTheme, FIELD_DISPLAY_TEXT),
            secondaryAccentColor = getColor(playerTheme, FIELD_SECONDARY_ACCENT)
        )
    }

    fun getTokens(playerTheme: PlayerTheme): PlayerThemeTokens {
        return playerTheme.defaultTokens().applyOverrides(getOverrides(playerTheme))
    }

    fun saveOverrides(
        playerTheme: PlayerTheme,
        overrides: PlayerThemeTokenOverrides
    ) {
        preferences.edit()
            .removeThemeFields(playerTheme)
            .putColor(playerTheme, FIELD_SHELL, overrides.shellColor)
            .putColor(playerTheme, FIELD_ACCENT, overrides.accentColor)
            .putColor(playerTheme, FIELD_DISPLAY_BACKGROUND, overrides.displayBackgroundColor)
            .putColor(playerTheme, FIELD_DISPLAY_TEXT, overrides.displayTextColor)
            .putColor(playerTheme, FIELD_SECONDARY_ACCENT, overrides.secondaryAccentColor)
            .apply()
    }

    fun clearOverrides(playerTheme: PlayerTheme) {
        preferences.edit()
            .removeThemeFields(playerTheme)
            .apply()
    }

    fun clearAllOverrides() {
        preferences.edit()
            .clear()
            .apply()
    }

    private fun getColor(
        playerTheme: PlayerTheme,
        field: String
    ): Color? {
        val storedValue = runCatching {
            preferences.getString(key(playerTheme, field), null)
        }.getOrNull() ?: return null

        if (storedValue.length != ENCODED_COLOR_LENGTH || storedValue.first() != COLOR_PREFIX) {
            return null
        }

        val argb = storedValue
            .drop(1)
            .toUIntOrNull(radix = COLOR_RADIX)
            ?: return null
        return Color(argb.toInt())
    }

    private fun SharedPreferences.Editor.removeThemeFields(
        playerTheme: PlayerTheme
    ): SharedPreferences.Editor = apply {
        fields.forEach { field -> remove(key(playerTheme, field)) }
    }

    private fun SharedPreferences.Editor.putColor(
        playerTheme: PlayerTheme,
        field: String,
        color: Color?
    ): SharedPreferences.Editor = apply {
        color ?: return@apply
        val encodedArgb = color.toArgb()
            .toUInt()
            .toString(radix = COLOR_RADIX)
            .padStart(ARGB_HEX_LENGTH, '0')
            .uppercase()
        putString(key(playerTheme, field), "$COLOR_PREFIX$encodedArgb")
    }

    private fun key(playerTheme: PlayerTheme, field: String): String {
        return "${playerTheme.id}.$field"
    }

    private companion object {
        const val PLAYER_THEME_TOKEN_PREFERENCES_NAME = "player_theme_token_preferences"
        const val FIELD_SHELL = "shell"
        const val FIELD_ACCENT = "accent"
        const val FIELD_DISPLAY_BACKGROUND = "display_background"
        const val FIELD_DISPLAY_TEXT = "display_text"
        const val FIELD_SECONDARY_ACCENT = "secondary_accent"
        const val COLOR_PREFIX = '#'
        const val COLOR_RADIX = 16
        const val ARGB_HEX_LENGTH = 8
        const val ENCODED_COLOR_LENGTH = ARGB_HEX_LENGTH + 1

        val fields = listOf(
            FIELD_SHELL,
            FIELD_ACCENT,
            FIELD_DISPLAY_BACKGROUND,
            FIELD_DISPLAY_TEXT,
            FIELD_SECONDARY_ACCENT
        )
    }
}
