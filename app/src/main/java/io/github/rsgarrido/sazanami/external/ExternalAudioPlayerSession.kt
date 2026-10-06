package io.github.rsgarrido.sazanami.external

import androidx.media3.common.Player

/** Owns only the preview player. Clearing ownership first makes all dismissal paths idempotent. */
internal class ExternalAudioPlayerSession {
    var player: Player? = null
        private set

    fun replace(newPlayer: Player) {
        if (player === newPlayer) return
        close()
        player = newPlayer
    }

    fun close() {
        val closing = player ?: return
        player = null
        // Even a failed stop must not skip releasing the decoder and audio focus.
        runCatching { closing.stop() }
        runCatching { closing.release() }
    }
}
