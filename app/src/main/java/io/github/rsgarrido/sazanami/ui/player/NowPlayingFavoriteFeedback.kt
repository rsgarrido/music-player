package io.github.rsgarrido.sazanami.ui.player

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalResources
import io.github.rsgarrido.sazanami.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow

/** Screen-owned FIFO: action callbacks carry only this queue, never snackbar dependencies. */
internal class NowPlayingFavoriteFeedbackQueue {
    private val pending = Channel<NowPlayingFavoriteFeedback>(Channel.UNLIMITED)
    val events = pending.receiveAsFlow()

    fun emit(feedback: NowPlayingFavoriteFeedback) {
        pending.trySend(feedback)
    }

    fun close() {
        pending.close()
    }
}

/** Compose outside PlayerMorphHost so message presentation is not captured by its content. */
@Composable
internal fun NowPlayingFavoriteFeedbackEffect(
    queue: NowPlayingFavoriteFeedbackQueue,
    snackbarHostState: SnackbarHostState
) {
    val resources by rememberUpdatedState(LocalResources.current)
    DisposableEffect(queue) {
        onDispose { queue.close() }
    }
    LaunchedEffect(queue, snackbarHostState) {
        queue.events.collect { feedback ->
            val messageRes = when (feedback) {
                NowPlayingFavoriteFeedback.ADDED_TO_FAVORITES -> R.string.player_added_to_favorites
                NowPlayingFavoriteFeedback.REMOVED_FROM_FAVORITES -> R.string.player_removed_from_favorites
            }
            snackbarHostState.showSnackbar(
                message = resources.getString(messageRes),
                duration = SnackbarDuration.Short,
                withDismissAction = true
            )
        }
    }
}
