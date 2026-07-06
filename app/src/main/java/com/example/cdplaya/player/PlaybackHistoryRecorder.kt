package com.example.cdplaya.player

import com.example.cdplaya.data.ListeningHistoryRepository
import com.example.cdplaya.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaybackHistoryRecorder(
    private val coroutineScope: CoroutineScope
) {
    private var listeningHistoryRepository: ListeningHistoryRepository? = null
    private var onListeningHistoryChanged: (() -> Unit)? = null

    private var playCountedForSongId: Long? = null
    private var listenedMsForCurrentSong = 0L
    private var lastObservedPositionForHistory: Int? = null

    fun setListeningHistoryRepository(repository: ListeningHistoryRepository) {
        listeningHistoryRepository = repository
    }

    fun setOnListeningHistoryChanged(listener: () -> Unit) {
        onListeningHistoryChanged = listener
    }

    fun resetForNewSong() {
        playCountedForSongId = null
        listenedMsForCurrentSong = 0L
        lastObservedPositionForHistory = null
    }

    fun onSeek(position: Int) {
        lastObservedPositionForHistory = position
    }

    fun onProgressUpdated(
        currentSong: Song?,
        isPlaying: Boolean,
        updatedPosition: Int,
        duration: Int
    ) {
        val song = currentSong ?: return

        updateListenedTimeForHistory(
            isPlaying = isPlaying,
            updatedPosition = updatedPosition
        )

        maybeRecordSongPlay(
            song = song,
            isPlaying = isPlaying,
            duration = duration
        )
    }

    private fun updateListenedTimeForHistory(
        isPlaying: Boolean,
        updatedPosition: Int
    ) {
        val previousPosition = lastObservedPositionForHistory

        if (isPlaying && previousPosition != null) {
            val positionDifference = updatedPosition - previousPosition

            if (
                positionDifference > 0 &&
                positionDifference <= MAX_POSITION_JUMP_FOR_HISTORY_MS
            ) {
                listenedMsForCurrentSong += positionDifference
            }
        }

        lastObservedPositionForHistory = updatedPosition
    }

    private fun maybeRecordSongPlay(
        song: Song,
        isPlaying: Boolean,
        duration: Int
    ) {
        if (!isPlaying) {
            return
        }

        if (playCountedForSongId == song.id) {
            return
        }

        val songDuration = duration.takeIf { value ->
            value > 0
        } ?: song.duration.toInt()

        if (songDuration <= 0) {
            return
        }

        val halfDuration = songDuration / 2
        val playThreshold = minOf(
            PLAY_COUNT_THRESHOLD_MS,
            halfDuration.toLong()
        )
            .coerceAtLeast(MIN_PLAY_COUNT_THRESHOLD_MS)
            .coerceAtMost(songDuration.toLong())

        if (listenedMsForCurrentSong < playThreshold) {
            return
        }

        playCountedForSongId = song.id

        coroutineScope.launch(Dispatchers.IO) {
            listeningHistoryRepository?.recordSongPlay(song)

            withContext(Dispatchers.Main) {
                onListeningHistoryChanged?.invoke()
            }
        }
    }

    companion object {
        private const val PLAY_COUNT_THRESHOLD_MS = 30_000L
        private const val MIN_PLAY_COUNT_THRESHOLD_MS = 5_000L
        private const val MAX_POSITION_JUMP_FOR_HISTORY_MS = 2_000
    }
}