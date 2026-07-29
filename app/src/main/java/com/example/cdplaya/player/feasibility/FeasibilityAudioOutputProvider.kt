package com.example.cdplaya.player.feasibility

import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioOutput
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.media3.exoplayer.audio.AudioTrackAudioOutput
import androidx.media3.exoplayer.audio.ForwardingAudioOutput
import androidx.media3.exoplayer.audio.ForwardingAudioOutputProvider
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@OptIn(UnstableApi::class)
internal class FeasibilityAudioOutputProvider(
    delegate: AudioOutputProvider,
    private val bridge: BitPerfectFeasibilityRuntimeBridge =
        BitPerfectFeasibilityRuntimeBridge,
    private val onOutputReleased: (() -> Unit)? = null
) : ForwardingAudioOutputProvider(delegate) {
    private val generationCounter = AtomicInteger()
    private val released = AtomicBoolean()

    @Volatile
    private var intendedUsbDevice: AudioDeviceInfo? = null
    @Volatile
    private var latestFormatConfig: Media3FormatConfigSnapshot? = null
    @Volatile
    private var latestOutputConfig: Media3OutputConfigSnapshot? = null
    @Volatile
    private var latestAudioTrack: AudioTrack? = null
    @Volatile
    private var latestGeneration: Int? = null

    fun setIntendedUsbDevice(device: AudioDeviceInfo?) {
        intendedUsbDevice = device
    }

    fun publishRetainedCurrentFacts() {
        latestFormatConfig?.let(bridge::recordFormatConfig)
        latestOutputConfig?.let(bridge::recordOutputConfig)
        latestGeneration?.let { generation ->
            bridge.recordOutputCreated(
                generation,
                latestAudioTrack.toSnapshot(intendedUsbDevice)
            )
        }
    }

    override fun getFormatSupport(
        formatConfig: AudioOutputProvider.FormatConfig
    ): AudioOutputProvider.FormatSupport {
        recordFormatConfig(formatConfig)
        return super.getFormatSupport(formatConfig)
    }

    override fun getOutputConfig(
        formatConfig: AudioOutputProvider.FormatConfig
    ): AudioOutputProvider.OutputConfig {
        recordFormatConfig(formatConfig)
        return super.getOutputConfig(formatConfig).also {
            latestOutputConfig = it.toSnapshot()
            if (bridge.isObserving()) {
                bridge.recordOutputConfig(checkNotNull(latestOutputConfig))
            }
        }
    }

    override fun getAudioOutput(
        config: AudioOutputProvider.OutputConfig
    ): AudioOutput {
        val output = try {
            super.getAudioOutput(config)
        } catch (failure: AudioOutputProvider.InitializationException) {
            if (bridge.isObserving()) {
                bridge.recordFailure(
                    FeasibilityEventType.INITIALIZATION_FAILED
                )
            }
            throw failure
        }
        val generation = generationCounter.incrementAndGet()
        val audioTrack = (output as? AudioTrackAudioOutput)?.audioTrack
        latestAudioTrack = audioTrack
        latestGeneration = generation
        if (bridge.isObserving()) {
            bridge.recordOutputCreated(
                generation = generation,
                audioTrack = audioTrack.toSnapshot(
                    intendedUsbDevice = intendedUsbDevice
                )
            )
        }
        return ObservingAudioOutput(
            delegate = output,
            audioTrack = audioTrack,
            generation = generation
        )
    }

    override fun release() {
        if (!released.compareAndSet(false, true)) return
        try {
            super.release()
        } finally {
            if (bridge.isObserving()) {
                bridge.recordProviderReleased()
            }
        }
    }

    private fun recordFormatConfig(
        config: AudioOutputProvider.FormatConfig
    ) {
        latestFormatConfig = config.toSnapshot()
        if (bridge.isObserving()) {
            bridge.recordFormatConfig(checkNotNull(latestFormatConfig))
        }
    }

    private inner class ObservingAudioOutput(
        delegate: AudioOutput,
        private val audioTrack: AudioTrack?,
        private val generation: Int
    ) : ForwardingAudioOutput(delegate) {
        private val outputReleased = AtomicBoolean()

        override fun play() {
            super.play()
            if (bridge.isObserving()) {
                bridge.recordPlay(generation)
                publishTrackSnapshot()
            }
        }

        override fun write(
            buffer: ByteBuffer,
            encodedAccessUnitCount: Int,
            presentationTimeUs: Long
        ): Boolean {
            return try {
                super.write(
                    buffer,
                    encodedAccessUnitCount,
                    presentationTimeUs
                ).also {
                    if (bridge.isObserving()) publishTrackSnapshot()
                }
            } catch (failure: AudioOutput.WriteException) {
                if (bridge.isObserving()) {
                    bridge.recordFailure(
                        type = FeasibilityEventType.WRITE_FAILED,
                        generation = generation
                    )
                }
                throw failure
            }
        }

        override fun canReuseAudioOutput(
            currentConfig: AudioOutputProvider.OutputConfig,
            newFormat: AudioOutputProvider.FormatConfig,
            newConfig: AudioOutputProvider.OutputConfig
        ): Boolean {
            val reused = super.canReuseAudioOutput(
                currentConfig,
                newFormat,
                newConfig
            )
            if (bridge.isObserving()) {
                bridge.recordReuse(generation, reused)
            }
            return reused
        }

        override fun flush() {
            super.flush()
            if (bridge.isObserving()) bridge.recordFlush(generation)
        }

        override fun stop() {
            super.stop()
            if (bridge.isObserving()) bridge.recordStop(generation)
        }

        override fun release() {
            if (!outputReleased.compareAndSet(false, true)) return
            try {
                super.release()
            } finally {
                if (bridge.isObserving()) bridge.recordRelease(generation)
                if (latestGeneration == generation) {
                    latestGeneration = null
                    latestAudioTrack = null
                }
                onOutputReleased?.invoke()
            }
        }

        private fun publishTrackSnapshot() {
            bridge.updateAudioTrack(
                generation = generation,
                audioTrack = audioTrack.toSnapshot(
                    intendedUsbDevice = intendedUsbDevice
                )
            )
        }
    }
}

@OptIn(UnstableApi::class)
private fun AudioOutputProvider.FormatConfig.toSnapshot():
    Media3FormatConfigSnapshot = Media3FormatConfigSnapshot(
    mimeType = format.sampleMimeType,
    pcmEncoding = format.pcmEncoding.takeIf {
        it != C.ENCODING_INVALID && it != C.INDEX_UNSET
    },
    sampleRateHz = format.sampleRate.takeIf { it > 0 },
    channelCount = format.channelCount.takeIf { it > 0 },
    mediaUsage = audioAttributes.usage == C.USAGE_MEDIA,
    preferredDeviceCategory =
        preferredDevice.toFeasibilityRouteCategory(),
    audioSessionIdRequest = audioSessionId.takeIf {
        it != C.AUDIO_SESSION_ID_UNSET
    },
    preferredBufferSizeBytes = preferredBufferSize.takeIf { it > 0 },
    offloadEnabled = enableOffload,
    tunnelingEnabled = enableTunneling,
    highResolutionPcmEnabled = enableHighResolutionPcmOutput,
    playbackParametersEnabled = enablePlaybackParameters
)

@OptIn(UnstableApi::class)
private fun AudioOutputProvider.OutputConfig.toSnapshot():
    Media3OutputConfigSnapshot = Media3OutputConfigSnapshot(
    encoding = encoding.takeIf { it != C.ENCODING_INVALID },
    sampleRateHz = sampleRate.takeIf { it > 0 },
    channelMask = channelMask.takeIf {
        it != AudioFormat.CHANNEL_INVALID
    },
    bufferSizeBytes = bufferSize.takeIf { it > 0 },
    audioSessionIdRequest = audioSessionId.takeIf {
        it != C.AUDIO_SESSION_ID_UNSET
    },
    offloadEnabled = isOffload,
    tunnelingEnabled = isTunneling,
    playbackParametersEnabled = usePlaybackParameters
)

private fun AudioTrack?.toSnapshot(
    intendedUsbDevice: AudioDeviceInfo?
): AudioTrackFormatSnapshot {
    if (this == null) return AudioTrackFormatSnapshot()
    val audioFormat = format
    val routed = routedDevice
    return AudioTrackFormatSnapshot(
        encoding = audioFormat.encoding.takeIf {
            it != AudioFormat.ENCODING_INVALID
        },
        sampleRateHz = audioFormat.sampleRate.takeIf { it > 0 },
        channelMask = audioFormat.channelMask.takeIf {
            it != AudioFormat.CHANNEL_INVALID && it != 0
        },
        channelIndexMask = audioFormat.channelIndexMask.takeIf { it != 0 },
        channelCount = channelCount.takeIf { it > 0 },
        bufferSizeFrames = bufferSizeInFrames.toLong().takeIf { it >= 0 },
        audioSessionId = audioSessionId.takeIf { it > 0 },
        performanceMode = performanceMode,
        routedDeviceCategory = routed.toFeasibilityRouteCategory(),
        routeMatchesIntendedUsbDevice = intendedUsbDevice?.let {
            routed != null && routed.id == it.id
        }
    )
}

internal fun AudioDeviceInfo?.toFeasibilityRouteCategory():
    FeasibilityRouteCategory = when (this?.type) {
    AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE ->
        FeasibilityRouteCategory.BUILT_IN
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_LINE_ANALOG,
    AudioDeviceInfo.TYPE_AUX_LINE ->
        FeasibilityRouteCategory.WIRED_ANALOG
    AudioDeviceInfo.TYPE_USB_ACCESSORY,
    AudioDeviceInfo.TYPE_USB_DEVICE,
    AudioDeviceInfo.TYPE_USB_HEADSET ->
        FeasibilityRouteCategory.USB
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
    AudioDeviceInfo.TYPE_BLE_BROADCAST,
    AudioDeviceInfo.TYPE_BLE_HEADSET,
    AudioDeviceInfo.TYPE_BLE_SPEAKER ->
        FeasibilityRouteCategory.BLUETOOTH
    AudioDeviceInfo.TYPE_HDMI,
    AudioDeviceInfo.TYPE_HDMI_ARC,
    AudioDeviceInfo.TYPE_HDMI_EARC ->
        FeasibilityRouteCategory.HDMI
    null,
    AudioDeviceInfo.TYPE_UNKNOWN -> FeasibilityRouteCategory.UNKNOWN
    else -> FeasibilityRouteCategory.OTHER
}
