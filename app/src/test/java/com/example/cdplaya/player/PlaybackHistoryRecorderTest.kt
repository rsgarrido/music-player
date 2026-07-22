package com.example.cdplaya.player

import android.net.Uri
import com.example.cdplaya.data.Song
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class PlaybackHistoryRecorderTest {
    @Test
    fun recordsOnlyOnceAfterActualListeningThreshold() {
        val tracker = PlaybackHistoryProgressTracker()
        val song = song(1)
        tracker.shouldRecordPlay(song, true, 0, 120_000)
        repeat(14) { index ->
            assertFalse(tracker.shouldRecordPlay(song, true, (index + 1) * 2_000, 120_000))
        }
        assertTrue(tracker.shouldRecordPlay(song, true, 30_000, 120_000))
        assertFalse(tracker.shouldRecordPlay(song, true, 32_000, 120_000))
    }

    @Test
    fun seeksAndPausedProgressDoNotCountAsListening() {
        val tracker = PlaybackHistoryProgressTracker()
        val song = song(2)
        tracker.shouldRecordPlay(song, true, 0, 20_000)
        tracker.onSeek(15_000)
        assertFalse(tracker.shouldRecordPlay(song, true, 16_000, 20_000))
        assertFalse(tracker.shouldRecordPlay(song, false, 20_000, 20_000))
        assertFalse(tracker.shouldRecordPlay(song, true, 20_500, 20_000))
    }

    @Test
    fun newSongResetAllowsNewPlayAndZeroDurationNeverRecords() {
        val tracker = PlaybackHistoryProgressTracker()
        val first = song(3, duration = 10_000)
        tracker.shouldRecordPlay(first, true, 0, 10_000)
        tracker.shouldRecordPlay(first, true, 2_000, 10_000)
        tracker.shouldRecordPlay(first, true, 4_000, 10_000)
        assertTrue(tracker.shouldRecordPlay(first, true, 5_000, 10_000))

        tracker.resetForNewSong()
        val second = song(4, duration = 10_000)
        tracker.shouldRecordPlay(second, true, 0, 10_000)
        tracker.shouldRecordPlay(second, true, 2_000, 10_000)
        tracker.shouldRecordPlay(second, true, 4_000, 10_000)
        assertTrue(tracker.shouldRecordPlay(second, true, 5_000, 10_000))

        tracker.resetForNewSong()
        val invalid = song(5, duration = 0)
        tracker.shouldRecordPlay(invalid, true, 0, 0)
        assertFalse(tracker.shouldRecordPlay(invalid, true, 2_000, 0))
    }

    private fun song(id: Long, duration: Long = 120_000L) = Song(
        id, "Song $id", "Artist", "Album", 1, duration,
        mock(Uri::class.java), "/music/$id.mp3", "/music", null
    )
}
