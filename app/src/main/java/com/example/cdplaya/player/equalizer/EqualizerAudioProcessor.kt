package com.example.cdplaya.player.equalizer

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.StreamMetadata
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

@OptIn(UnstableApi::class)
internal class EqualizerAudioProcessor(
    private val runtimeBridge: EqualizerRuntimeBridge =
        EqualizerRuntimeBridge,
    transitionDurationMillis: Int =
        EqualizerTransitionState.DEFAULT_DURATION_MILLIS
) : BaseAudioProcessor() {
    private val transitionState =
        EqualizerTransitionState(transitionDurationMillis)

    private var currentFormat: EqualizerProcessorFormat? = null
    private var currentPath: PreparedEqualizerProcessingPath? = null
    private var pendingPath: PreparedEqualizerProcessingPath? = null

    private var inputScratch = EMPTY_FLOAT_ARRAY
    private var currentOutputScratch = EMPTY_FLOAT_ARRAY
    private var pendingOutputScratch = EMPTY_FLOAT_ARRAY
    private var scratchCapacity = 0
    private var scratchBufferGrowthCount = 0
    private var outputCapacity = 0
    private var outputBufferGrowthCount = 0
    private var lastTransitionFrameCount = 0

    override fun onConfigure(
        inputAudioFormat: AudioFormat
    ): AudioFormat {
        if (
            inputAudioFormat.encoding != C.ENCODING_PCM_16BIT ||
            inputAudioFormat.sampleRate <= 0 ||
            inputAudioFormat.channelCount <= 0
        ) {
            throw UnhandledAudioFormatException(
                "Equalizer requires positive-rate PCM16 audio:",
                inputAudioFormat
            )
        }

        val format = EqualizerProcessorFormat(
            sampleRateHz = inputAudioFormat.sampleRate,
            channelCount = inputAudioFormat.channelCount,
            pcmEncoding = inputAudioFormat.encoding
        )
        runtimeBridge.publishProcessorFormat(format)
        runtimeBridge.publishProcessorConfigured(
            configured = true,
            bypassed = true
        )
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val format = checkNotNull(currentFormat) {
            "Equalizer processor must be flushed after configuration"
        }
        require(inputBuffer.isDirect) {
            "PCM input buffer must be direct"
        }
        require(inputBuffer.order() == ByteOrder.nativeOrder()) {
            "PCM input buffer must use native byte order"
        }

        val inputByteCount = inputBuffer.remaining()
        val frameSizeBytes = format.channelCount * Short.SIZE_BYTES
        require(inputByteCount % frameSizeBytes == 0) {
            "PCM16 input must contain complete audio frames"
        }
        if (inputByteCount == 0) {
            return
        }
        val frameCount = inputByteCount / frameSizeBytes
        val sampleCount = frameCount * format.channelCount
        considerLatestPreparedPath(format)

        val outputBuffer = replaceOutputBuffer(inputByteCount)
        if (outputBuffer.capacity() > outputCapacity) {
            outputCapacity = outputBuffer.capacity()
            outputBufferGrowthCount++
        }

        if (isExactBypass()) {
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        ensureScratchCapacity(sampleCount)
        Pcm16SampleConversion.decode(
            input = inputBuffer,
            output = inputScratch,
            sampleCount = sampleCount
        )

        val activeCurrentPath = currentPath
        if (activeCurrentPath?.bypassed == false) {
            activeCurrentPath.process(
                input = inputScratch,
                output = currentOutputScratch,
                frameCount = frameCount
            )
        }
        val activePendingPath = pendingPath
        if (activePendingPath?.bypassed == false) {
            activePendingPath.process(
                input = inputScratch,
                output = pendingOutputScratch,
                frameCount = frameCount
            )
        }

        if (transitionState.isActive) {
            encodeTransition(
                outputBuffer = outputBuffer,
                frameCount = frameCount,
                channelCount = format.channelCount
            )
        } else {
            val processedSamples = if (
                activeCurrentPath?.bypassed == false
            ) {
                currentOutputScratch
            } else {
                inputScratch
            }
            Pcm16SampleConversion.encode(
                input = processedSamples,
                output = outputBuffer,
                sampleCount = sampleCount
            )
        }
        outputBuffer.flip()
    }

    override fun onFlush(streamMetadata: StreamMetadata) {
        val format = EqualizerProcessorFormat(
            sampleRateHz = inputAudioFormat.sampleRate,
            channelCount = inputAudioFormat.channelCount,
            pcmEncoding = inputAudioFormat.encoding
        )
        currentFormat = format
        currentPath?.reset()
        pendingPath?.reset()
        transitionState.cancel()
        pendingPath = null

        currentPath = runtimeBridge.latestCompatiblePath(format)
        currentPath?.reset()
        runtimeBridge.publishAppliedPlan(
            plan = currentPath?.plan,
            applicationMode =
                EqualizerPlanApplicationMode.DIRECT_AFTER_FLUSH
        )
        runtimeBridge.publishTransitionInProgress(false)
        runtimeBridge.publishProcessorConfigured(
            configured = true,
            bypassed = currentPath?.bypassed != false
        )
    }

    override fun onReset() {
        currentPath?.reset()
        pendingPath?.reset()
        currentPath = null
        pendingPath = null
        currentFormat = null
        transitionState.cancel()
        inputScratch = EMPTY_FLOAT_ARRAY
        currentOutputScratch = EMPTY_FLOAT_ARRAY
        pendingOutputScratch = EMPTY_FLOAT_ARRAY
        scratchCapacity = 0
        outputCapacity = 0
        lastTransitionFrameCount = 0
        runtimeBridge.publishProcessorFormat(null)
        runtimeBridge.clearProcessorTelemetry()
    }

    internal fun bufferReuseSnapshot(): EqualizerBufferReuseSnapshot {
        return EqualizerBufferReuseSnapshot(
            scratchCapacity = scratchCapacity,
            inputScratchIdentity = System.identityHashCode(inputScratch),
            currentOutputScratchIdentity =
                System.identityHashCode(currentOutputScratch),
            pendingOutputScratchIdentity =
                System.identityHashCode(pendingOutputScratch),
            scratchBufferGrowthCount = scratchBufferGrowthCount,
            outputCapacity = outputCapacity,
            outputBufferGrowthCount = outputBufferGrowthCount,
            currentEngineCapacity = currentPath?.capacitySnapshot(),
            pendingEngineCapacity = pendingPath?.capacitySnapshot()
        )
    }

    internal fun transitionFrameCount(): Int {
        return lastTransitionFrameCount
    }

    private fun considerLatestPreparedPath(
        format: EqualizerProcessorFormat
    ) {
        if (transitionState.isActive) return
        val latest = runtimeBridge.latestCompatiblePath(format) ?: return
        val currentVersion =
            currentPath?.plan?.sourceSnapshotVersion ?: -1L
        if (latest.plan.sourceSnapshotVersion <= currentVersion) return

        if (
            currentPath?.bypassed != false &&
            latest.bypassed
        ) {
            currentPath = latest
            currentPath?.reset()
            runtimeBridge.publishAppliedPlan(
                plan = latest.plan,
                applicationMode =
                    EqualizerPlanApplicationMode.DIRECT_BYPASS
            )
            runtimeBridge.publishProcessorConfigured(
                configured = true,
                bypassed = true
            )
            return
        }

        latest.reset()
        pendingPath = latest
        transitionState.start(format.sampleRateHz)
        lastTransitionFrameCount = transitionState.totalFrameCount
        runtimeBridge.publishTransitionStarted(
            totalFrameCount = transitionState.totalFrameCount,
            sampleRateHz = format.sampleRateHz
        )
    }

    private fun isExactBypass(): Boolean {
        return !transitionState.isActive &&
            currentPath?.bypassed != false
    }

    private fun ensureScratchCapacity(requiredSampleCount: Int) {
        if (requiredSampleCount <= scratchCapacity) return
        inputScratch = FloatArray(requiredSampleCount)
        currentOutputScratch = FloatArray(requiredSampleCount)
        pendingOutputScratch = FloatArray(requiredSampleCount)
        scratchCapacity = requiredSampleCount
        scratchBufferGrowthCount++
        runtimeBridge.publishScratchBufferGrowthCount(
            scratchBufferGrowthCount
        )
    }

    private fun encodeTransition(
        outputBuffer: ByteBuffer,
        frameCount: Int,
        channelCount: Int
    ) {
        val oldPathUsesDsp = currentPath?.bypassed == false
        val newPathUsesDsp = pendingPath?.bypassed == false
        var transitionCompleted = false
        var frameIndex = 0

        while (frameIndex < frameCount) {
            if (!transitionCompleted) {
                val progress = transitionState.progressForNextFrame()
                val oldWeight = 1.0 - progress
                var channelIndex = 0
                while (channelIndex < channelCount) {
                    val sampleIndex =
                        frameIndex * channelCount + channelIndex
                    val oldOutput = if (oldPathUsesDsp) {
                        currentOutputScratch[sampleIndex]
                    } else {
                        inputScratch[sampleIndex]
                    }
                    val newOutput = if (newPathUsesDsp) {
                        pendingOutputScratch[sampleIndex]
                    } else {
                        inputScratch[sampleIndex]
                    }
                    val mixedOutput =
                        oldOutput * oldWeight + newOutput * progress
                    outputBuffer.putShort(
                        Pcm16SampleConversion.fromNormalizedFloat(
                            mixedOutput.toFloat()
                        )
                    )
                    channelIndex++
                }
                transitionState.advanceFrame()
                transitionCompleted = !transitionState.isActive
            } else {
                var channelIndex = 0
                while (channelIndex < channelCount) {
                    val sampleIndex =
                        frameIndex * channelCount + channelIndex
                    val newOutput = if (newPathUsesDsp) {
                        pendingOutputScratch[sampleIndex]
                    } else {
                        inputScratch[sampleIndex]
                    }
                    outputBuffer.putShort(
                        Pcm16SampleConversion.fromNormalizedFloat(newOutput)
                    )
                    channelIndex++
                }
            }
            frameIndex++
        }

        if (transitionCompleted) {
            currentPath = pendingPath
            pendingPath = null
            transitionState.cancel()
            runtimeBridge.publishAppliedPlan(
                plan = currentPath?.plan,
                applicationMode =
                    EqualizerPlanApplicationMode.CROSSFADE
            )
            runtimeBridge.publishTransitionInProgress(false)
            runtimeBridge.publishProcessorConfigured(
                configured = true,
                bypassed = currentPath?.bypassed != false
            )
        }
    }

    companion object {
        private val EMPTY_FLOAT_ARRAY = FloatArray(0)
    }
}

internal data class EqualizerBufferReuseSnapshot(
    val scratchCapacity: Int,
    val inputScratchIdentity: Int,
    val currentOutputScratchIdentity: Int,
    val pendingOutputScratchIdentity: Int,
    val scratchBufferGrowthCount: Int,
    val outputCapacity: Int,
    val outputBufferGrowthCount: Int,
    val currentEngineCapacity:
        com.example.cdplaya.player.equalizer.dsp.EqualizerEngineCapacity?,
    val pendingEngineCapacity:
        com.example.cdplaya.player.equalizer.dsp.EqualizerEngineCapacity?
)
