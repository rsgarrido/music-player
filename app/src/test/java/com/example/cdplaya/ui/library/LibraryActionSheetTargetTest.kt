package com.example.cdplaya.ui.library

import android.net.Uri
import com.example.cdplaya.data.Song
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class LibraryActionSheetTargetTest {
    @Test
    fun songTargetPreservesExistingActionsAndCallbacks() {
        val song = testSong()
        val invokedActions = mutableListOf<String>()

        val target = songActionSheetTarget(
            song = song,
            wasRecentlyAdded = false,
            isFavorite = false,
            onPlayNextClick = { invokedActions += "play_next" },
            onAddToQueueClick = { invokedActions += "queue" },
            onToggleFavoriteClick = { invokedActions += "favorite" },
            onAddToPlaylistClick = { invokedActions += "playlist" },
            onEditSongTagsClick = { invokedActions += "edit" }
        )

        assertEquals(
            listOf(
                "Play next",
                "Add to queue",
                "Add to favorites",
                "Add to playlist",
                "Edit tags"
            ),
            target.actions.map { action -> action.label }
        )

        target.actions.forEach { action -> action.onClick() }
        assertEquals(
            listOf("play_next", "queue", "favorite", "playlist", "edit"),
            invokedActions
        )
    }

    @Test
    fun collectionTargetsPreserveExistingActionOrder() {
        val song = testSong()
        val noOp: (String, List<Song>) -> Unit = { _, _ -> }

        val albumTarget = albumActionSheetTarget(
            albumTitle = "Album",
            subtitle = "Artist • 1 song",
            artworkUri = null,
            albumSongs = listOf(song),
            onPlayClick = noOp,
            onShuffleClick = noOp,
            onPlayNextClick = noOp,
            onAddToQueueClick = noOp,
            onAddToPlaylistClick = noOp
        )
        val artistTarget = artistActionSheetTarget(
            artistName = "Artist",
            subtitle = "1 song",
            artworkUri = null,
            artistSongs = listOf(song),
            onPlayClick = noOp,
            onShuffleClick = noOp,
            onPlayNextClick = noOp,
            onAddToQueueClick = noOp,
            onAddToPlaylistClick = noOp
        )
        val expected = listOf("Play", "Shuffle", "Play next", "Add to queue", "Add to playlist")

        assertEquals(expected, albumTarget.actions.map { action -> action.label })
        assertEquals(expected, artistTarget.actions.map { action -> action.label })
    }

    private fun testSong(): Song {
        return Song(
            id = 1L,
            title = "Song",
            artist = "Artist",
            album = "Album",
            trackNumber = 1,
            duration = 120_000L,
            uri = mock(Uri::class.java),
            filePath = "/music/song.flac",
            folderPath = "/music",
            albumArtUri = null
        )
    }
}
