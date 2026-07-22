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

    @Test
    fun explicitQueuePrecedesNormalUpcomingContext() {
        val builder = UpcomingPlaylistBuilder()
        val songs = (1L..5L).map(::song)

        val upcoming = builder.buildUpcomingPlaylistAfterCurrent(
            startSong = songs[1],
            playbackSourceSongs = songs.take(4),
            queuedSongsAfterCurrent = listOf(songs[4], songs[0]),
            currentUpcomingSongs = emptyList(),
            isShuffleEnabled = false,
            repeatMode = RepeatMode.OFF,
            preserveExistingShuffleOrder = false
        )

        assertEquals(listOf(songs[4], songs[0], songs[2], songs[3]), upcoming)
    }

    @Test
    fun repeatOffUsesRemainingContextOnceAndRepeatAllWrapsOrder() {
        val builder = UpcomingPlaylistBuilder()
        val songs = (1L..4L).map(::song)

        val repeatOff = builder.buildUpcomingPlaylistAfterCurrent(
            songs[2], songs, emptyList(), emptyList(), false, RepeatMode.OFF, false
        )
        val repeatAll = builder.buildUpcomingPlaylistAfterCurrent(
            songs[2], songs, emptyList(), emptyList(), false, RepeatMode.ALL, false
        )

        assertEquals(listOf(songs[3], songs[0], songs[1]), repeatOff)
        assertEquals(repeatOff, repeatAll)
    }

    @Test
    fun exhaustedMissingContextPreservesExistingUpcoming() {
        val builder = UpcomingPlaylistBuilder()
        val current = song(99)
        val existing = listOf(song(2), song(3))

        val upcoming = builder.buildUpcomingPlaylistAfterCurrent(
            current, listOf(song(1)), emptyList(), existing, false, RepeatMode.OFF, true
        )

        assertEquals(existing, upcoming)
    }

    @Test
    fun duplicateQueueEntriesArePreservedAndExcludedFromContextById() {
        val builder = UpcomingPlaylistBuilder()
        val queued = song(2)

        val upcoming = builder.buildUpcomingPlaylistAfterCurrent(
            startSong = song(1),
            playbackSourceSongs = listOf(song(1), song(2), song(2), song(3)),
            queuedSongsAfterCurrent = listOf(queued, queued),
            currentUpcomingSongs = emptyList(),
            isShuffleEnabled = false,
            repeatMode = RepeatMode.OFF,
            preserveExistingShuffleOrder = false
        )

        assertEquals(listOf(2L, 2L, 3L), upcoming.map { it.id })
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
