package com.example.cdplaya.player.feasibility

import java.util.Collections

enum class FeasibilityProbeMode {
    OFF,
    OBSERVE_CURRENT_PATH,
    EXPERIMENTAL_USB_EXACT_PATH
}

enum class FeasibilityRouteCategory {
    BUILT_IN,
    WIRED_ANALOG,
    USB,
    BLUETOOTH,
    HDMI,
    REMOTE,
    OTHER,
    UNKNOWN
}

enum class FeasibilityMixerBehavior {
    DEFAULT,
    BIT_PERFECT,
    UNKNOWN
}

enum class FeasibilityCleanupResult {
    NOT_REQUIRED,
    PENDING,
    CLEARED_AND_CONFIRMED,
    CLEAR_FAILED,
    CLEAR_NOT_CONFIRMED
}

enum class FeasibilityRejectionReason {
    NONE,
    DEBUG_BUILD_REQUIRED,
    API_TOO_OLD,
    NON_USB_DEVICE,
    MULTIPLE_USB_DEVICES,
    NO_BIT_PERFECT_ATTRIBUTE,
    ENCODING_MISMATCH,
    SAMPLE_RATE_MISMATCH,
    CHANNEL_MASK_MISMATCH,
    OUTPUT_FORMAT_UNKNOWN,
    OFFLOAD_OUTPUT,
    TUNNELED_OUTPUT,
    NON_UNITY_PLAYBACK_SPEED,
    ACTIVE_EQUALIZER,
    ACTIVE_LIMITER,
    ACTIVE_REPLAY_GAIN,
    ROUTE_NOT_CONFIRMED,
    SET_FAILED,
    PREFERRED_ATTRIBUTE_NOT_CONFIRMED
}

enum class FeasibilityEventType {
    MODE_CHANGED,
    FORMAT_CONFIG_REQUESTED,
    OUTPUT_CONFIG_RESOLVED,
    OUTPUT_CREATED,
    OUTPUT_REUSED,
    OUTPUT_NOT_REUSED,
    OUTPUT_PLAY,
    OUTPUT_FLUSHED,
    OUTPUT_STOPPED,
    OUTPUT_RELEASED,
    PROVIDER_RELEASED,
    INITIALIZATION_FAILED,
    WRITE_FAILED,
    USB_CAPABILITY_QUERIED,
    STALE_ATTRIBUTE_FOUND,
    STALE_ATTRIBUTE_CLEARED,
    ACTIVATION_REJECTED,
    PREFERRED_ATTRIBUTE_SET,
    PREFERRED_ATTRIBUTE_CONFIRMED,
    PREFERRED_ATTRIBUTE_CALLBACK,
    ROUTE_CONFIRMED,
    CLEANUP_STARTED,
    CLEANUP_FINISHED
}

data class FeasibilityAudioFormatSnapshot(
    val encoding: Int? = null,
    val sampleRateHz: Int? = null,
    val channelCount: Int? = null,
    val channelMask: Int? = null
)

data class Media3FormatConfigSnapshot(
    val mimeType: String? = null,
    val pcmEncoding: Int? = null,
    val sampleRateHz: Int? = null,
    val channelCount: Int? = null,
    val mediaUsage: Boolean? = null,
    val preferredDeviceCategory: FeasibilityRouteCategory =
        FeasibilityRouteCategory.UNKNOWN,
    val audioSessionIdRequest: Int? = null,
    val preferredBufferSizeBytes: Int? = null,
    val offloadEnabled: Boolean = false,
    val tunnelingEnabled: Boolean = false,
    val highResolutionPcmEnabled: Boolean = false,
    val playbackParametersEnabled: Boolean = false
)

data class Media3OutputConfigSnapshot(
    val encoding: Int? = null,
    val sampleRateHz: Int? = null,
    val channelMask: Int? = null,
    val bufferSizeBytes: Int? = null,
    val audioSessionIdRequest: Int? = null,
    val offloadEnabled: Boolean = false,
    val tunnelingEnabled: Boolean = false,
    val playbackParametersEnabled: Boolean = false
)

data class AudioTrackFormatSnapshot(
    val encoding: Int? = null,
    val sampleRateHz: Int? = null,
    val channelMask: Int? = null,
    val channelIndexMask: Int? = null,
    val channelCount: Int? = null,
    val bufferSizeFrames: Long? = null,
    val audioSessionId: Int? = null,
    val performanceMode: Int? = null,
    val routedDeviceCategory: FeasibilityRouteCategory =
        FeasibilityRouteCategory.UNKNOWN,
    val routeMatchesIntendedUsbDevice: Boolean? = null
)

data class MixerAttributeSnapshot(
    val encoding: Int? = null,
    val sampleRateHz: Int? = null,
    val channelMask: Int? = null,
    val mixerBehavior: FeasibilityMixerBehavior =
        FeasibilityMixerBehavior.UNKNOWN
)

data class FeasibilityEvent(
    val sequence: Long,
    val type: FeasibilityEventType,
    val generation: Int? = null,
    val reason: FeasibilityRejectionReason? = null
)

/**
 * Framework-free, process-local evidence. Lists are copied on construction and exposed read-only.
 */
class BitPerfectFeasibilitySnapshot(
    val probeMode: FeasibilityProbeMode = FeasibilityProbeMode.OFF,
    val processorInputFormat: FeasibilityAudioFormatSnapshot? = null,
    val processorOutputFormat: FeasibilityAudioFormatSnapshot? = null,
    val processorReceivedBufferCount: Long = 0,
    val media3FormatConfig: Media3FormatConfigSnapshot? = null,
    val media3OutputConfig: Media3OutputConfigSnapshot? = null,
    val audioTrackFormat: AudioTrackFormatSnapshot? = null,
    supportedMixerAttributes: List<MixerAttributeSnapshot> = emptyList(),
    val selectedMixerAttribute: MixerAttributeSnapshot? = null,
    val setResult: Boolean? = null,
    val confirmedPreferredAttribute: MixerAttributeSnapshot? = null,
    val listenerCallbackObserved: Boolean? = null,
    val routeConfirmed: Boolean? = null,
    val exactFormatConfirmed: Boolean? = null,
    val cleanupResult: FeasibilityCleanupResult =
        FeasibilityCleanupResult.NOT_REQUIRED,
    val rejectionReason: FeasibilityRejectionReason? = null,
    val outputCreationCount: Int = 0,
    val outputReuseCount: Int = 0,
    val outputFlushCount: Int = 0,
    val outputStopCount: Int = 0,
    val outputReleaseCount: Int = 0,
    val currentOutputGeneration: Int? = null,
    events: List<FeasibilityEvent> = emptyList()
) {
    val supportedMixerAttributes: List<MixerAttributeSnapshot> =
        immutableCopy(supportedMixerAttributes)
    val events: List<FeasibilityEvent> = immutableCopy(events)

    fun copy(
        probeMode: FeasibilityProbeMode = this.probeMode,
        processorInputFormat: FeasibilityAudioFormatSnapshot? = this.processorInputFormat,
        processorOutputFormat: FeasibilityAudioFormatSnapshot? = this.processorOutputFormat,
        processorReceivedBufferCount: Long = this.processorReceivedBufferCount,
        media3FormatConfig: Media3FormatConfigSnapshot? = this.media3FormatConfig,
        media3OutputConfig: Media3OutputConfigSnapshot? = this.media3OutputConfig,
        audioTrackFormat: AudioTrackFormatSnapshot? = this.audioTrackFormat,
        supportedMixerAttributes: List<MixerAttributeSnapshot> =
            this.supportedMixerAttributes,
        selectedMixerAttribute: MixerAttributeSnapshot? = this.selectedMixerAttribute,
        setResult: Boolean? = this.setResult,
        confirmedPreferredAttribute: MixerAttributeSnapshot? =
            this.confirmedPreferredAttribute,
        listenerCallbackObserved: Boolean? = this.listenerCallbackObserved,
        routeConfirmed: Boolean? = this.routeConfirmed,
        exactFormatConfirmed: Boolean? = this.exactFormatConfirmed,
        cleanupResult: FeasibilityCleanupResult = this.cleanupResult,
        rejectionReason: FeasibilityRejectionReason? = this.rejectionReason,
        outputCreationCount: Int = this.outputCreationCount,
        outputReuseCount: Int = this.outputReuseCount,
        outputFlushCount: Int = this.outputFlushCount,
        outputStopCount: Int = this.outputStopCount,
        outputReleaseCount: Int = this.outputReleaseCount,
        currentOutputGeneration: Int? = this.currentOutputGeneration,
        events: List<FeasibilityEvent> = this.events
    ): BitPerfectFeasibilitySnapshot = BitPerfectFeasibilitySnapshot(
        probeMode = probeMode,
        processorInputFormat = processorInputFormat,
        processorOutputFormat = processorOutputFormat,
        processorReceivedBufferCount = processorReceivedBufferCount,
        media3FormatConfig = media3FormatConfig,
        media3OutputConfig = media3OutputConfig,
        audioTrackFormat = audioTrackFormat,
        supportedMixerAttributes = supportedMixerAttributes,
        selectedMixerAttribute = selectedMixerAttribute,
        setResult = setResult,
        confirmedPreferredAttribute = confirmedPreferredAttribute,
        listenerCallbackObserved = listenerCallbackObserved,
        routeConfirmed = routeConfirmed,
        exactFormatConfirmed = exactFormatConfirmed,
        cleanupResult = cleanupResult,
        rejectionReason = rejectionReason,
        outputCreationCount = outputCreationCount,
        outputReuseCount = outputReuseCount,
        outputFlushCount = outputFlushCount,
        outputStopCount = outputStopCount,
        outputReleaseCount = outputReleaseCount,
        currentOutputGeneration = currentOutputGeneration,
        events = events
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BitPerfectFeasibilitySnapshot) return false
        return probeMode == other.probeMode &&
            processorInputFormat == other.processorInputFormat &&
            processorOutputFormat == other.processorOutputFormat &&
            processorReceivedBufferCount == other.processorReceivedBufferCount &&
            media3FormatConfig == other.media3FormatConfig &&
            media3OutputConfig == other.media3OutputConfig &&
            audioTrackFormat == other.audioTrackFormat &&
            supportedMixerAttributes == other.supportedMixerAttributes &&
            selectedMixerAttribute == other.selectedMixerAttribute &&
            setResult == other.setResult &&
            confirmedPreferredAttribute == other.confirmedPreferredAttribute &&
            listenerCallbackObserved == other.listenerCallbackObserved &&
            routeConfirmed == other.routeConfirmed &&
            exactFormatConfirmed == other.exactFormatConfirmed &&
            cleanupResult == other.cleanupResult &&
            rejectionReason == other.rejectionReason &&
            outputCreationCount == other.outputCreationCount &&
            outputReuseCount == other.outputReuseCount &&
            outputFlushCount == other.outputFlushCount &&
            outputStopCount == other.outputStopCount &&
            outputReleaseCount == other.outputReleaseCount &&
            currentOutputGeneration == other.currentOutputGeneration &&
            events == other.events
    }

    override fun hashCode(): Int {
        var result = probeMode.hashCode()
        result = 31 * result + (processorInputFormat?.hashCode() ?: 0)
        result = 31 * result + (processorOutputFormat?.hashCode() ?: 0)
        result = 31 * result + processorReceivedBufferCount.hashCode()
        result = 31 * result + (media3FormatConfig?.hashCode() ?: 0)
        result = 31 * result + (media3OutputConfig?.hashCode() ?: 0)
        result = 31 * result + (audioTrackFormat?.hashCode() ?: 0)
        result = 31 * result + supportedMixerAttributes.hashCode()
        result = 31 * result + (selectedMixerAttribute?.hashCode() ?: 0)
        result = 31 * result + (setResult?.hashCode() ?: 0)
        result = 31 * result + (confirmedPreferredAttribute?.hashCode() ?: 0)
        result = 31 * result + (listenerCallbackObserved?.hashCode() ?: 0)
        result = 31 * result + (routeConfirmed?.hashCode() ?: 0)
        result = 31 * result + (exactFormatConfirmed?.hashCode() ?: 0)
        result = 31 * result + cleanupResult.hashCode()
        result = 31 * result + (rejectionReason?.hashCode() ?: 0)
        result = 31 * result + outputCreationCount
        result = 31 * result + outputReuseCount
        result = 31 * result + outputFlushCount
        result = 31 * result + outputStopCount
        result = 31 * result + outputReleaseCount
        result = 31 * result + (currentOutputGeneration ?: 0)
        return 31 * result + events.hashCode()
    }
}

private fun <T> immutableCopy(values: List<T>): List<T> =
    Collections.unmodifiableList(ArrayList(values))
