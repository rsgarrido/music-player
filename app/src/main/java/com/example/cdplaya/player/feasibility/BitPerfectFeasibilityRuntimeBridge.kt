package com.example.cdplaya.player.feasibility

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal object BitPerfectFeasibilityRuntimeBridge {
    private const val MAX_EVENTS = 96

    private val _state = MutableStateFlow(BitPerfectFeasibilitySnapshot())
    val state: StateFlow<BitPerfectFeasibilitySnapshot> = _state.asStateFlow()

    @Volatile
    private var controller: Controller? = null
    private var eventSequence = 0L

    interface Controller {
        fun observeCurrentOutput()
        fun runExactUsbProbe()
        fun stopAndClearProbe()
    }

    fun attachController(controller: Controller) {
        this.controller = controller
    }

    fun detachController(controller: Controller) {
        if (this.controller === controller) {
            this.controller = null
        }
    }

    fun requestObserveCurrentOutput(): Boolean =
        controller?.let {
            it.observeCurrentOutput()
            true
        } ?: false

    fun requestExactUsbProbe(): Boolean =
        controller?.let {
            it.runExactUsbProbe()
            true
        } ?: false

    fun requestStopAndClear(): Boolean =
        controller?.let {
            it.stopAndClearProbe()
            true
        } ?: false

    fun reset() {
        eventSequence = 0
        _state.value = BitPerfectFeasibilitySnapshot()
    }

    fun isObserving(): Boolean =
        _state.value.probeMode != FeasibilityProbeMode.OFF

    fun setMode(mode: FeasibilityProbeMode) {
        mutate(
            eventType = FeasibilityEventType.MODE_CHANGED
        ) { current ->
            current.copy(
                probeMode = mode,
                rejectionReason = null,
                cleanupResult = if (
                    mode == FeasibilityProbeMode.EXPERIMENTAL_USB_EXACT_PATH
                ) {
                    FeasibilityCleanupResult.PENDING
                } else {
                    current.cleanupResult
                }
            )
        }
    }

    fun recordFormatConfig(snapshot: Media3FormatConfigSnapshot) {
        if (_state.value.media3FormatConfig == snapshot) return
        mutate(FeasibilityEventType.FORMAT_CONFIG_REQUESTED) {
            it.copy(media3FormatConfig = snapshot)
        }
    }

    fun recordOutputConfig(snapshot: Media3OutputConfigSnapshot) {
        if (_state.value.media3OutputConfig == snapshot) return
        mutate(FeasibilityEventType.OUTPUT_CONFIG_RESOLVED) {
            it.copy(media3OutputConfig = snapshot)
        }
    }

    fun recordProcessorConfigured(
        input: FeasibilityAudioFormatSnapshot,
        output: FeasibilityAudioFormatSnapshot
    ) {
        if (!isObserving()) return
        _state.update {
            it.copy(
                processorInputFormat = input,
                processorOutputFormat = output
            )
        }
    }

    fun recordProcessorBuffer() {
        if (!isObserving()) return
        _state.update {
            it.copy(
                processorReceivedBufferCount =
                    it.processorReceivedBufferCount + 1
            )
        }
    }

    fun recordOutputCreated(
        generation: Int,
        audioTrack: AudioTrackFormatSnapshot
    ) {
        val current = _state.value
        if (
            current.currentOutputGeneration == generation &&
            current.audioTrackFormat == audioTrack
        ) {
            return
        }
        mutate(FeasibilityEventType.OUTPUT_CREATED, generation) {
            it.copy(
                audioTrackFormat = audioTrack,
                exactFormatConfirmed =
                    exactTrackFormatConfirmed(it, audioTrack),
                outputCreationCount = it.outputCreationCount + 1,
                currentOutputGeneration = generation
            )
        }
    }

    fun updateAudioTrack(
        generation: Int,
        audioTrack: AudioTrackFormatSnapshot
    ) {
        _state.update {
            if (it.currentOutputGeneration == generation) {
                it.copy(
                    audioTrackFormat = audioTrack,
                    routeConfirmed =
                        audioTrack.routeMatchesIntendedUsbDevice,
                    exactFormatConfirmed =
                        exactTrackFormatConfirmed(it, audioTrack)
                )
            } else {
                it
            }
        }
    }

    fun recordReuse(generation: Int, reused: Boolean) {
        mutate(
            if (reused) {
                FeasibilityEventType.OUTPUT_REUSED
            } else {
                FeasibilityEventType.OUTPUT_NOT_REUSED
            },
            generation
        ) {
            if (reused) {
                it.copy(outputReuseCount = it.outputReuseCount + 1)
            } else {
                it
            }
        }
    }

    fun recordPlay(generation: Int) {
        mutate(FeasibilityEventType.OUTPUT_PLAY, generation) { it }
    }

    fun recordFlush(generation: Int) {
        mutate(FeasibilityEventType.OUTPUT_FLUSHED, generation) {
            it.copy(outputFlushCount = it.outputFlushCount + 1)
        }
    }

    fun recordStop(generation: Int) {
        mutate(FeasibilityEventType.OUTPUT_STOPPED, generation) {
            it.copy(outputStopCount = it.outputStopCount + 1)
        }
    }

    fun recordRelease(generation: Int) {
        mutate(FeasibilityEventType.OUTPUT_RELEASED, generation) {
            val releasedCurrent = it.currentOutputGeneration == generation
            it.copy(
                outputReleaseCount = it.outputReleaseCount + 1,
                currentOutputGeneration = if (releasedCurrent) {
                    null
                } else {
                    it.currentOutputGeneration
                },
                audioTrackFormat = if (releasedCurrent) {
                    null
                } else {
                    it.audioTrackFormat
                }
            )
        }
    }

    fun recordProviderReleased() {
        mutate(FeasibilityEventType.PROVIDER_RELEASED) {
            it.copy(
                currentOutputGeneration = null,
                audioTrackFormat = null
            )
        }
    }

    fun recordFailure(
        type: FeasibilityEventType,
        generation: Int? = null
    ) {
        mutate(type, generation) { it }
    }

    fun recordMixerCapabilities(
        attributes: List<MixerAttributeSnapshot>,
        rejectionReason: FeasibilityRejectionReason? = null
    ) {
        mutate(
            FeasibilityEventType.USB_CAPABILITY_QUERIED,
            reason = rejectionReason
        ) {
            it.copy(
                supportedMixerAttributes = attributes,
                rejectionReason = rejectionReason
            )
        }
    }

    fun recordActivationRejected(reason: FeasibilityRejectionReason) {
        mutate(FeasibilityEventType.ACTIVATION_REJECTED, reason = reason) {
            it.copy(
                rejectionReason = reason,
                setResult = false,
                exactFormatConfirmed = false
            )
        }
    }

    fun recordPreferredSet(
        selected: MixerAttributeSnapshot,
        result: Boolean
    ) {
        mutate(FeasibilityEventType.PREFERRED_ATTRIBUTE_SET) {
            it.copy(
                selectedMixerAttribute = selected,
                setResult = result
            )
        }
    }

    fun recordPreferredConfirmation(
        confirmed: MixerAttributeSnapshot?
    ) {
        mutate(FeasibilityEventType.PREFERRED_ATTRIBUTE_CONFIRMED) {
            it.copy(
                confirmedPreferredAttribute = confirmed,
                exactFormatConfirmed = null
            )
        }
    }

    fun recordPreferredCallback(
        attribute: MixerAttributeSnapshot?
    ) {
        mutate(FeasibilityEventType.PREFERRED_ATTRIBUTE_CALLBACK) {
            it.copy(
                listenerCallbackObserved = true,
                confirmedPreferredAttribute = attribute
                    ?: it.confirmedPreferredAttribute
            )
        }
    }

    fun recordCleanup(
        result: FeasibilityCleanupResult,
        stale: Boolean = false
    ) {
        mutate(
            if (stale) {
                FeasibilityEventType.STALE_ATTRIBUTE_CLEARED
            } else {
                FeasibilityEventType.CLEANUP_FINISHED
            }
        ) {
            it.copy(
                cleanupResult = result,
                selectedMixerAttribute = null,
                setResult = null,
                confirmedPreferredAttribute = null,
                listenerCallbackObserved = null,
                routeConfirmed = null,
                exactFormatConfirmed = null
            )
        }
    }

    private inline fun mutate(
        eventType: FeasibilityEventType,
        generation: Int? = null,
        reason: FeasibilityRejectionReason? = null,
        crossinline transform:
            (BitPerfectFeasibilitySnapshot) ->
                BitPerfectFeasibilitySnapshot
    ) {
        _state.update { current ->
            eventSequence++
            val event = FeasibilityEvent(
                sequence = eventSequence,
                type = eventType,
                generation = generation,
                reason = reason
            )
            transform(current).copy(
                events = (current.events + event).takeLast(MAX_EVENTS)
            )
        }
    }

    private fun exactTrackFormatConfirmed(
        snapshot: BitPerfectFeasibilitySnapshot,
        audioTrack: AudioTrackFormatSnapshot
    ): Boolean? {
        val selected = snapshot.selectedMixerAttribute ?: return null
        if (snapshot.confirmedPreferredAttribute != selected) return null
        val output = snapshot.media3OutputConfig ?: return null
        return audioTrack.encoding == selected.encoding &&
            audioTrack.sampleRateHz == selected.sampleRateHz &&
            audioTrack.channelMask == selected.channelMask &&
            output.encoding == selected.encoding &&
            output.sampleRateHz == selected.sampleRateHz &&
            output.channelMask == selected.channelMask
    }
}
