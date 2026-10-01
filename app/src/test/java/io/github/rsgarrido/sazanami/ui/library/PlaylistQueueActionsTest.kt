package io.github.rsgarrido.sazanami.ui.library

import io.github.rsgarrido.sazanami.data.Playlist
import io.github.rsgarrido.sazanami.R
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistQueueActionsTest {
    @Test
    fun everySharedPlaylistQueueActionDispatchesTheOriginalPlaylist() {
        val playlist = Playlist(4, "Road Trip", 3)
        val received = mutableListOf<Pair<String, Playlist>>()
        val actions = playlistQueueActions(playlist, LibraryQueueUiEnvironment(
            onPlayPlaylistNext = { received += "next" to it },
            onAddPlaylistToAnotherQueue = { received += "another" to it },
            onPlayPlaylistInNewQueue = { received += "new" to it }
        ), onAddToQueue = { received += "queue" to it }, resolveString = Int::toString)
        actions.forEach { it.onClick() }
        assertEquals(listOf("next", "queue", "another", "new"), received.map { it.first })
        received.forEach { assertEquals(playlist, it.second) }
    }

    @Test
    fun playNextIsPresentAndUsesTheSharedPlaylistQueueCallback() {
        val playlist = Playlist(playlistId = 4L, name = "Road Trip", songCount = 3)
        var playedNext: Playlist? = null
        val actions = playlistQueueActions(
            playlist = playlist,
            queueUi = LibraryQueueUiEnvironment(
                onPlayPlaylistNext = { playedNext = it }
            ),
            onAddToQueue = {},
            resolveString = Int::toString
        )

        assertEquals(
            listOf(R.string.playlist_play_next, R.string.playlist_add_to_queue, R.string.playlist_add_to_another_queue, R.string.playlist_play_in_new_queue).map(Int::toString),
            actions.map { action -> action.label }
        )
        actions.first().onClick()
        assertEquals(playlist, playedNext)
    }
}
