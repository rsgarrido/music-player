package com.example.cdplaya.data

import android.content.Context

class PlayerThemePreferences(
    context: Context
) {
    private val preferences = context.getSharedPreferences(
        PLAYER_THEME_PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun getSelectedPlayerTheme(): PlayerTheme {
        return PlayerTheme.fromId(
            preferences.getString(KEY_SELECTED_PLAYER_THEME, null)
        )
    }

    fun saveSelectedPlayerTheme(playerTheme: PlayerTheme) {
        preferences.edit()
            .putString(KEY_SELECTED_PLAYER_THEME, playerTheme.id)
            .apply()
    }

    companion object {
        private const val PLAYER_THEME_PREFERENCES_NAME = "player_theme_preferences"
        private const val KEY_SELECTED_PLAYER_THEME = "selected_player_theme"
    }
}