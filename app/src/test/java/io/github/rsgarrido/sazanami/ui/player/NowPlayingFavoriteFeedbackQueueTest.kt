package io.github.rsgarrido.sazanami.ui.player

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class NowPlayingFavoriteFeedbackQueueTest {
    @Test
    fun pendingFeedbackKeepsOrderAndRepeatedMessagesUntilTheOwnerConsumesIt() = runBlocking {
        val queue = NowPlayingFavoriteFeedbackQueue()
        val requested = listOf(
            NowPlayingFavoriteFeedback.ADDED_TO_FAVORITES,
            NowPlayingFavoriteFeedback.REMOVED_FROM_FAVORITES,
            NowPlayingFavoriteFeedback.ADDED_TO_FAVORITES,
            NowPlayingFavoriteFeedback.ADDED_TO_FAVORITES
        )
        requested.forEach(queue::emit)
        try {
            assertEquals(requested, queue.events.take(requested.size).toList())
        } finally {
            queue.close()
        }
    }

    @Test
    fun disposedQueueIgnoresLateCallbacksAndTerminatesConsumption() = runBlocking {
        val queue = NowPlayingFavoriteFeedbackQueue()
        queue.close()
        queue.emit(NowPlayingFavoriteFeedback.ADDED_TO_FAVORITES)
        assertEquals(emptyList<NowPlayingFavoriteFeedback>(), queue.events.toList())
    }
}
