package com.example.cdplaya.player

import android.net.Uri
import com.example.cdplaya.data.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock

class PlaybackNavigationHistoryTest {

    @Test
    fun previousAndNextFollowActualPlaybackPath() {
        val history = PlaybackNavigationHistory()
        val songA = song(1)
        val songC = song(3)
        val songB = song(2)

        history.addPreviousSong(songA)
        history.addPreviousSong(songC)

        assertEquals(songC, history.popPreviousSongAndPushCurrent(songB))
        assertEquals(songA, history.popPreviousSongAndPushCurrent(songC))
        assertEquals(songC, history.popNextSong())
        assertEquals(songB, history.popNextSong())
        assertNull(history.popNextSong())
    }

    @Test
    fun consecutiveDuplicatePreviousSongsAreIgnored() {
        val history = PlaybackNavigationHistory()
        val songA = song(1)

        history.addPreviousSong(songA)
        history.addPreviousSong(songA)

        assertEquals(listOf(songA.id), history.getPreviousSongIds())
    }

    @Test
    fun clearAllRemovesBackAndForwardHistoryForNewContext() {
        val history = PlaybackNavigationHistory()
        val songA = song(1)
        val songB = song(2)

        history.addPreviousSong(songA)
        history.popPreviousSongAndPushCurrent(songB)
        history.clearAll()

        assertEquals(emptyList<Long>(), history.getPreviousSongIds())
        assertEquals(emptyList<Long>(), history.getNextSongIds())
    }

    private fun song(id: Long): Song {
        return Song(
            id = id,
            title = "Song $id",
            artist = "Artist",
            album = "Album",
            trackNumber = id.toInt(),
            duration = 180_000,
            uri = mock(Uri::class.java),
            filePath = "/music/$id.mp3",
            folderPath = "/music",
            albumArtUri = null
        )
    }
}
