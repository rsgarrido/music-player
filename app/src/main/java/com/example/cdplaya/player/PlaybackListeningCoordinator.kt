package com.example.cdplaya.player

import com.example.cdplaya.data.NativeListeningTrack
import com.example.cdplaya.data.listening.FinalizeListeningSessionResult
import com.example.cdplaya.data.listening.FinalizedListeningEventDraft
import com.example.cdplaya.data.listening.ListeningSessionRecorder
import com.example.cdplaya.data.listening.ListeningSessionStart
import com.example.cdplaya.data.listening.MonotonicClock
import com.example.cdplaya.data.listening.WallClock
import com.example.cdplaya.data.local.ListeningEndReason

data class PlaybackCallbackTimestamp(
    val monotonicMs: Long,
    val wallClockMs: Long
)

fun interface PlaybackSessionIdGenerator {
    fun newId(): String
}

fun interface NativeListeningTrackResolution {
    suspend fun resolve(evidence: ListeningMediaItemEvidence): NativeListeningTrack
}

enum class ListeningMediaTransitionReason {
    REPEAT,
    AUTOMATIC,
    SEEK,
    PLAYLIST_CHANGED
}

/**
 * Serialized, Media3-free callback reducer. Callback timestamps are installed before recorder
 * commands, preserving elapsed time even when identity resolution suspended on Room first.
 */
class PlaybackListeningCoordinator(
    private val recorder: ListeningSessionRecorder,
    private val callbackClock: PlaybackCallbackClock,
    private val trackResolution: NativeListeningTrackResolution,
    private val sessionIdGenerator: PlaybackSessionIdGenerator,
    private val onFinalized: (FinalizedListeningEventDraft) -> Unit,
    private val onFailure: (Throwable) -> Unit = {}
) {
    private data class Attempt(val itemInstanceId: String, val playbackSessionId: String)
    private data class TransitionSignature(
        val from: String?,
        val to: String?,
        val reason: ListeningMediaTransitionReason,
        val monotonicMs: Long
    )

    private var attempt: Attempt? = null
    private var lastTransition: TransitionSignature? = null

    suspend fun onIsPlayingChanged(
        evidence: ListeningMediaItemEvidence?,
        isPlaying: Boolean,
        timestamp: PlaybackCallbackTimestamp
    ) {
        if (!isPlaying) {
            val active = attempt ?: return
            if (evidence != null && evidence.itemInstanceId != active.itemInstanceId) return
            at(timestamp) { recorder.onPlaybackSuspended(active.playbackSessionId) }
            return
        }

        val playable = evidence ?: return
        var active = attempt
        if (active != null && active.itemInstanceId != playable.itemInstanceId) {
            finalizeActive(ListeningEndReason.TRANSITION, timestamp)
            active = null
        }
        if (active == null) active = startAttempt(playable, timestamp) ?: return
        at(timestamp) { recorder.onPlaybackStarted(active.playbackSessionId) }
    }

    suspend fun onMediaItemTransition(
        evidence: ListeningMediaItemEvidence?,
        reason: ListeningMediaTransitionReason,
        isPlaying: Boolean,
        timestamp: PlaybackCallbackTimestamp
    ) {
        val active = attempt
        val signature = TransitionSignature(
            from = active?.itemInstanceId,
            to = evidence?.itemInstanceId,
            reason = reason,
            monotonicMs = timestamp.monotonicMs
        )
        if (lastTransition?.let { previous ->
                previous.to == signature.to && previous.reason == signature.reason &&
                    previous.monotonicMs == signature.monotonicMs
            } == true
        ) return
        lastTransition = signature

        val sameItem = active != null && evidence?.itemInstanceId == active.itemInstanceId
        val preservesAttempt = reason == ListeningMediaTransitionReason.PLAYLIST_CHANGED && sameItem
        if (active != null && !preservesAttempt) {
            val endReason = when (reason) {
                ListeningMediaTransitionReason.REPEAT,
                ListeningMediaTransitionReason.AUTOMATIC -> ListeningEndReason.NATURAL_END
                ListeningMediaTransitionReason.SEEK,
                ListeningMediaTransitionReason.PLAYLIST_CHANGED -> ListeningEndReason.TRANSITION
            }
            finalizeActive(endReason, timestamp)
        }

        if (isPlaying && evidence != null) {
            onIsPlayingChanged(evidence, true, timestamp)
        }
    }

    fun onPositionDiscontinuity(
        evidence: ListeningMediaItemEvidence?,
        timestamp: PlaybackCallbackTimestamp
    ) {
        val active = attempt ?: return
        if (evidence?.itemInstanceId != active.itemInstanceId) return
        at(timestamp) { recorder.onPositionDiscontinuity(active.playbackSessionId) }
    }

    fun onNaturalEnd(
        evidence: ListeningMediaItemEvidence?,
        timestamp: PlaybackCallbackTimestamp
    ) {
        val active = attempt ?: return
        if (evidence?.itemInstanceId != active.itemInstanceId) return
        finalizeActive(ListeningEndReason.NATURAL_END, timestamp)
    }

    fun onError(timestamp: PlaybackCallbackTimestamp) {
        finalizeActive(ListeningEndReason.ERROR, timestamp)
    }

    fun onStopped(timestamp: PlaybackCallbackTimestamp) {
        finalizeActive(ListeningEndReason.STOPPED, timestamp)
    }

    fun onServiceDestroyed(timestamp: PlaybackCallbackTimestamp) {
        finalizeActive(ListeningEndReason.STOPPED, timestamp)
    }

    private suspend fun startAttempt(
        evidence: ListeningMediaItemEvidence,
        timestamp: PlaybackCallbackTimestamp
    ): Attempt? {
        val resolved = try {
            trackResolution.resolve(evidence)
        } catch (error: Throwable) {
            onFailure(error)
            return null
        }
        val sessionId = sessionIdGenerator.newId()
        at(timestamp) {
            recorder.startSession(
                ListeningSessionStart(
                    playbackSessionId = sessionId,
                    trackIdentityId = resolved.trackIdentityId,
                    localTrackBindingId = resolved.localTrackBindingId,
                    trackDurationMs = evidence.reference.duration.takeIf { it > 0L }
                )
            )
        }
        return Attempt(evidence.itemInstanceId, sessionId).also { attempt = it }
    }

    private fun finalizeActive(reason: ListeningEndReason, timestamp: PlaybackCallbackTimestamp) {
        val active = attempt ?: return
        attempt = null
        val result = at(timestamp) { recorder.finalizeSession(active.playbackSessionId, reason) }
        if (result is FinalizeListeningSessionResult.Finalized) {
            runCatching { onFinalized(result.draft) }.onFailure(onFailure)
        }
    }

    private inline fun <T> at(timestamp: PlaybackCallbackTimestamp, block: () -> T): T {
        callbackClock.set(timestamp)
        return block()
    }
}

class PlaybackCallbackClock(
    initial: PlaybackCallbackTimestamp = PlaybackCallbackTimestamp(0L, 0L)
) : MonotonicClock, WallClock {
    private var current = initial

    fun set(timestamp: PlaybackCallbackTimestamp) {
        current = timestamp
    }

    override fun elapsedRealtimeMs(): Long = current.monotonicMs
    override fun currentTimeMillis(): Long = current.wallClockMs
}
