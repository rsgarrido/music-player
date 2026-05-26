package com.example.cdplaya.player

import android.content.Context

class PlayerStateStorage(context: Context) {

    private val preferences = context.getSharedPreferences(
        "player_state",
        Context.MODE_PRIVATE
    )

    fun saveState(
        currentSongId: Long?,
        currentPosition: Int,
        isShuffleEnabled: Boolean,
        repeatMode: RepeatMode,
        previousSongIds: List<Long>,
        nextSongIds: List<Long>
    ) {
        preferences.edit()
            .putLong(KEY_CURRENT_SONG_ID, currentSongId ?: NO_SONG_ID)
            .putInt(KEY_CURRENT_POSITION, currentPosition)
            .putBoolean(KEY_SHUFFLE_ENABLED, isShuffleEnabled)
            .putString(KEY_REPEAT_MODE, repeatMode.name)
            .putString(KEY_PREVIOUS_HISTORY, previousSongIds.joinToString(","))
            .putString(KEY_NEXT_HISTORY, nextSongIds.joinToString(","))
            .apply()
    }

    fun getCurrentSongId(): Long? {
        val songId = preferences.getLong(KEY_CURRENT_SONG_ID, NO_SONG_ID)

        return if (songId == NO_SONG_ID) {
            null
        } else {
            songId
        }
    }

    fun getCurrentPosition(): Int {
        return preferences.getInt(KEY_CURRENT_POSITION, 0)
    }

    fun isShuffleEnabled(): Boolean {
        return preferences.getBoolean(KEY_SHUFFLE_ENABLED, false)
    }

    fun getRepeatMode(): RepeatMode {
        val savedMode = preferences.getString(KEY_REPEAT_MODE, RepeatMode.OFF.name)

        return try {
            RepeatMode.valueOf(savedMode ?: RepeatMode.OFF.name)
        } catch (exception: IllegalArgumentException) {
            RepeatMode.OFF
        }
    }

    fun getPreviousSongIds(): List<Long> {
        return getSongIds(KEY_PREVIOUS_HISTORY)
    }

    fun getNextSongIds(): List<Long> {
        return getSongIds(KEY_NEXT_HISTORY)
    }

    private fun getSongIds(key: String): List<Long> {
        val savedIds = preferences.getString(key, "") ?: ""

        if (savedIds.isBlank()) {
            return emptyList()
        }

        return savedIds
            .split(",")
            .mapNotNull { id ->
                id.toLongOrNull()
            }
    }

    companion object {
        private const val KEY_CURRENT_SONG_ID = "current_song_id"
        private const val KEY_CURRENT_POSITION = "current_position"
        private const val KEY_SHUFFLE_ENABLED = "shuffle_enabled"
        private const val KEY_REPEAT_MODE = "repeat_mode"
        private const val KEY_PREVIOUS_HISTORY = "previous_history"
        private const val KEY_NEXT_HISTORY = "next_history"

        private const val NO_SONG_ID = -1L
    }
}