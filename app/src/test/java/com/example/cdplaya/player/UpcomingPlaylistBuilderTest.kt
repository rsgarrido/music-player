package com.example.cdplaya.player

import android.net.Uri
import com.example.cdplaya.data.Song
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class UpcomingPlaylistBuilderTest {

    @Test
    fun preservingShuffleKeepsRemainingExistingOrder() {
        val builder = UpcomingPlaylistBuilder { songs -> songs.reversed() }
        val songA = song(1)
        val songB = song(2)
        val songC = song(3)
        val songD = song(4)

        val upcoming = builder.buildUpcomingPlaylistAfterCurrent(
            startSong = songC,
            playbackSourceSongs = listOf(songA, songB, songC, songD),
            queuedSongsAfterCurrent = emptyList(),
            currentUpcomingSongs = listOf(songC, songB, songD),
            isShuffleEnabled = true,
            repeatMode = RepeatMode.ALL,
            preserveExistingShuffleOrder = true
        )

        assertEquals(listOf(songB, songD), upcoming)
    }

    @Test
    fun exhaustedPreservedShuffleCreatesShuffledRepeatAllCycle() {
        val builder = UpcomingPlaylistBuilder { songs -> songs.reversed() }
        val songA = song(1)
        val songB = song(2)
        val songC = song(3)
        val songD = song(4)

        val upcoming = builder.buildUpcomingPlaylistAfterCurrent(
            startSong = songD,
            playbackSourceSongs = listOf(songA, songB, songC, songD),
            queuedSongsAfterCurrent = emptyList(),
            currentUpcomingSongs = listOf(songC, songB, songD),
            isShuffleEnabled = true,
            repeatMode = RepeatMode.ALL,
            preserveExistingShuffleOrder = true
        )

        assertEquals(listOf(songC, songB, songA), upcoming)
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
