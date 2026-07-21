package com.example.cdplaya.data

import android.content.Context
import android.content.SharedPreferences
import com.example.cdplaya.ui.player.modern.ModernArtworkTransitionStyle
import com.example.cdplaya.ui.player.modern.ModernSeekbarStyle

class ModernPlayerPreferences internal constructor(
    private val preferences: SharedPreferences
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(
            MODERN_PLAYER_PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
    )

    fun getArtworkTransitionStyle(): ModernArtworkTransitionStyle {
        val storedValue = runCatching {
            preferences.getString(ARTWORK_TRANSITION_STYLE_KEY, null)
        }.getOrNull()

        return ModernArtworkTransitionStyle.fromStorageValue(storedValue)
    }

    fun saveArtworkTransitionStyle(style: ModernArtworkTransitionStyle) {
        preferences.edit()
            .putString(ARTWORK_TRANSITION_STYLE_KEY, style.storageValue)
            .apply()
    }

    fun getSeekbarStyle(): ModernSeekbarStyle {
        val storedValue = runCatching {
            preferences.getString(SEEKBAR_STYLE_KEY, null)
        }.getOrNull()

        return ModernSeekbarStyle.fromStorageValue(storedValue)
    }

    fun saveSeekbarStyle(style: ModernSeekbarStyle) {
        preferences.edit()
            .putString(SEEKBAR_STYLE_KEY, style.storageValue)
            .apply()
    }

    private companion object {
        const val MODERN_PLAYER_PREFERENCES_NAME = "modern_player_preferences"
        const val ARTWORK_TRANSITION_STYLE_KEY = "artwork_transition_style"
        const val SEEKBAR_STYLE_KEY = "seekbar_style"
    }
}
