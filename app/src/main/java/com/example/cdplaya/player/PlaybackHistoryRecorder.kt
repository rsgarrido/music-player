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

    private val progressTracker = PlaybackHistoryProgressTracker()

    fun setListeningHistoryRepository(repository: ListeningHistoryRepository) {
        listeningHistoryRepository = repository
    }

    fun setOnListeningHistoryChanged(listener: () -> Unit) {
        onListeningHistoryChanged = listener
    }

    fun resetForNewSong() {
        progressTracker.resetForNewSong()
    }

    fun onSeek(position: Int) {
        progressTracker.onSeek(position)
    }

    fun onProgressUpdated(
        currentSong: Song?,
        isPlaying: Boolean,
        updatedPosition: Int,
        duration: Int
    ) {
        val song = currentSong ?: return
        if (!progressTracker.shouldRecordPlay(
            song = song,
            isPlaying = isPlaying,
            updatedPosition = updatedPosition,
            duration = duration
        )) return

        coroutineScope.launch(Dispatchers.IO) {
            listeningHistoryRepository?.recordSongPlay(song)

            withContext(Dispatchers.Main) {
                onListeningHistoryChanged?.invoke()
            }
        }
    }

}

internal class PlaybackHistoryProgressTracker {
    private var playCountedForSongId: Long? = null
    private var listenedMsForCurrentSong = 0L
    private var lastObservedPositionForHistory: Int? = null

    fun resetForNewSong() {
        playCountedForSongId = null
        listenedMsForCurrentSong = 0L
        lastObservedPositionForHistory = null
    }

    fun onSeek(position: Int) {
        lastObservedPositionForHistory = position
    }

    fun shouldRecordPlay(
        song: Song,
        isPlaying: Boolean,
        updatedPosition: Int,
        duration: Int
    ): Boolean {
        val previousPosition = lastObservedPositionForHistory
        if (isPlaying && previousPosition != null) {
            val positionDifference = updatedPosition - previousPosition
            if (positionDifference in 1..MAX_POSITION_JUMP_FOR_HISTORY_MS) {
                listenedMsForCurrentSong += positionDifference
            }
        }
        lastObservedPositionForHistory = updatedPosition

        if (!isPlaying || playCountedForSongId == song.id) return false
        val songDuration = duration.takeIf { it > 0 } ?: song.duration.toInt()
        if (songDuration <= 0) return false
        val playThreshold = minOf(PLAY_COUNT_THRESHOLD_MS, (songDuration / 2).toLong())
            .coerceAtLeast(MIN_PLAY_COUNT_THRESHOLD_MS)
            .coerceAtMost(songDuration.toLong())
        if (listenedMsForCurrentSong < playThreshold) return false

        playCountedForSongId = song.id
        return true
    }

    private companion object {
        private const val PLAY_COUNT_THRESHOLD_MS = 30_000L
        private const val MIN_PLAY_COUNT_THRESHOLD_MS = 5_000L
        private const val MAX_POSITION_JUMP_FOR_HISTORY_MS = 2_000
    }
}
