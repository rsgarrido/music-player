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
        return runCatching {
            ReplayGainMode.valueOf(getReplayGainModeName())
        }.getOrDefault(ReplayGainMode.OFF)
    }

    fun setReplayGainMode(replayGainMode: ReplayGainMode) {
        setReplayGainModeName(replayGainMode.name)
    }

    fun getReplayGainModeName(): String {
        return preferences.getString(
            REPLAY_GAIN_MODE_KEY,
            ReplayGainMode.OFF.name
        ) ?: ReplayGainMode.OFF.name
    }

    fun setReplayGainModeName(modeName: String) {
        preferences
            .edit()
            .putString(
                REPLAY_GAIN_MODE_KEY,
                modeName
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
