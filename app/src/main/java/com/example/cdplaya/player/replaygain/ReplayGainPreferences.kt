package com.example.cdplaya.player.replaygain

import android.content.Context

class ReplayGainPreferences(
    context: Context
) {
    private val preferences = context.getSharedPreferences(
        REPLAY_GAIN_PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun getReplayGainMode(): ReplayGainMode {
        val savedModeName = preferences.getString(
            REPLAY_GAIN_MODE_KEY,
            ReplayGainMode.OFF.name
        )

        return savedModeName
            ?.let { modeName ->
                runCatching {
                    ReplayGainMode.valueOf(modeName)
                }.getOrNull()
            }
            ?: ReplayGainMode.OFF
    }

    fun setReplayGainMode(replayGainMode: ReplayGainMode) {
        preferences
            .edit()
            .putString(
                REPLAY_GAIN_MODE_KEY,
                replayGainMode.name
            )
            .apply()
    }

    fun isReplayGainEnabled(): Boolean {
        return getReplayGainMode() != ReplayGainMode.OFF
    }

    companion object {
        private const val REPLAY_GAIN_PREFERENCES_NAME = "replay_gain_preferences"
        private const val REPLAY_GAIN_MODE_KEY = "replay_gain_mode"
    }
}