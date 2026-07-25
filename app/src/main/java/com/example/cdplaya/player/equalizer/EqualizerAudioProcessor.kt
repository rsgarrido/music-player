package com.example.cdplaya.player.equalizer

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.StreamMetadata
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.cdplaya.player.equalizer.limiter.LimiterPreparedConfiguration
import com.example.cdplaya.player.equalizer.limiter.LimiterTelemetryAccumulator
import com.example.cdplaya.player.equalizer.limiter.LookaheadLimiterEngine
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.min

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
    private var postEqualizerScratch = EMPTY_FLOAT_ARRAY
    private var limiterOutputScratch = EMPTY_FLOAT_ARRAY
    private var scratchCapacity = 0
    private var scratchBufferGrowthCount = 0
    private var outputCapacity = 0
    private var outputBufferGrowthCount = 0
    private var lastTransitionFrameCount = 0
    private val limiterTelemetry =
        LimiterTelemetryAccumulator()
    private var limiterEngine: LookaheadLimiterEngine? = null
    private var appliedLimiterConfiguration:
        LimiterPreparedConfiguration? = null
    private var pendingLimiterDisable = false
    private var endOfStreamDraining = false
    private var limiterReprimeCount = 0
    private var observedMeterResetVersion = 0L

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
        val frameCount = inputByteCount / frameSizeBytes
        val sampleCount = frameCount * format.channelCount
        applyPendingMeterReset()
        considerLatestPreparedConfiguration(format)
        if (runtimeBridge.isLimiterPreparationPending(format)) {
            return
        }

        if (pendingLimiterDisable) {
            val activeLimiter = limiterEngine
            if (activeLimiter != null && !activeLimiter.isDrained) {
                drainLimiterBeforeDisable(
                    requestedFrameCapacity =
                        frameCount.coerceAtLeast(1),
                    format = format
                )
                return
            }
            completeLimiterDisable()
        }

        if (inputByteCount == 0) return
        val outputBuffer = outputBuffer(inputByteCount)
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

        val postEqualizerSamples = if (transitionState.isActive) {
            mixTransition(
                frameCount = frameCount,
                channelCount = format.channelCount
            )
        } else {
            if (
                activeCurrentPath?.bypassed == false
            ) {
                currentOutputScratch
            } else {
                inputScratch
            }
        }

        limiterTelemetry.beginProcessingCall()
        val activeLimiter = limiterEngine
        val outputSamples: FloatArray
        val outputFrameCount: Int
        if (activeLimiter != null) {
            outputFrameCount = activeLimiter.process(
                input = postEqualizerSamples,
                inputOffset = 0,
                frameCount = frameCount,
                output = limiterOutputScratch,
                outputOffset = 0
            )
            outputSamples = limiterOutputScratch
        } else {
            observeUnboundedProcessorSamples(
                samples = postEqualizerSamples,
                sampleCount = sampleCount
            )
            outputFrameCount = frameCount
            outputSamples = postEqualizerSamples
        }
        encodeWithTelemetry(
            input = outputSamples,
            output = outputBuffer,
            sampleCount = outputFrameCount * format.channelCount
        )
        outputBuffer.flip()
        publishLimiterTelemetry()
    }

    override fun onQueueEndOfStream() {
        // Media3 1.9.1 drains an AudioProcessor after queueEndOfStream()
        // before the next stream is flushed through this processor. At this
        // extension point there are therefore no next-stream samples available
        // to fill the old stream's lookahead window. We preserve every source
        // tail frame here, then onFlush() deliberately re-primes the next
        // stream; perfectly continuous lookahead across that boundary is not
        // representable without moving the limiter to a different layer.
        val engine = limiterEngine
        if (engine != null && !engine.isDrained) {
            endOfStreamDraining = true
            if (!hasPendingOutput()) {
                fillEndOfStreamDrainOutput()
            }
        }
    }

    override fun getOutput(): ByteBuffer {
        val available = super.getOutput()
        if (available.hasRemaining()) return available
        if (
            endOfStreamDraining &&
            limiterEngine?.isDrained == false
        ) {
            fillEndOfStreamDrainOutput()
            return super.getOutput()
        }
        if (limiterEngine?.isDrained != false) {
            endOfStreamDraining = false
        }
        return available
    }

    override fun isEnded(): Boolean {
        return super.isEnded() &&
            !endOfStreamDraining &&
            limiterEngine?.isDrained != false
    }

    private fun observeUnboundedProcessorSamples(
        samples: FloatArray,
        sampleCount: Int
    ) {
        var sampleIndex = 0
        while (sampleIndex < sampleCount) {
            val sample = samples[sampleIndex]
            limiterTelemetry.observePreLimiterSample(sample)
            limiterTelemetry.observePostLimiterSample(sample)
            sampleIndex++
        }
        limiterTelemetry.observeLimiterInactive()
    }

    private fun encodeWithTelemetry(
        input: FloatArray,
        output: ByteBuffer,
        sampleCount: Int
    ) {
        var sampleIndex = 0
        while (sampleIndex < sampleCount) {
            val sample = input[sampleIndex]
            if (abs(sample) > 1.0f) {
                limiterTelemetry.observeSaturatedSample()
            }
            output.putShort(
                Pcm16SampleConversion.fromNormalizedFloat(sample)
            )
            sampleIndex++
        }
    }

    private fun outputBuffer(requiredBytes: Int): ByteBuffer {
        val output = replaceOutputBuffer(requiredBytes)
        if (output.capacity() > outputCapacity) {
            outputCapacity = output.capacity()
            outputBufferGrowthCount++
        }
        return output
    }

    private fun fillEndOfStreamDrainOutput() {
        val format = currentFormat ?: return
        val engine = limiterEngine ?: return
        if (engine.isDrained) {
            endOfStreamDraining = false
            return
        }
        val frameCount = min(
            engine.pendingFrameCount,
            DRAIN_CHUNK_FRAME_COUNT
        )
        ensureScratchCapacity(frameCount * format.channelCount)
        limiterTelemetry.beginProcessingCall()
        val producedFrames = engine.drain(
            output = limiterOutputScratch,
            outputOffset = 0,
            maximumFrameCount = frameCount
        )
        val output = outputBuffer(
            producedFrames * format.channelCount * Short.SIZE_BYTES
        )
        encodeWithTelemetry(
            input = limiterOutputScratch,
            output = output,
            sampleCount = producedFrames * format.channelCount
        )
        output.flip()
        publishLimiterTelemetry()
        if (engine.isDrained) {
            endOfStreamDraining = false
            publishLimiterProcessorState()
        }
    }

    private fun drainLimiterBeforeDisable(
        requestedFrameCapacity: Int,
        format: EqualizerProcessorFormat
    ) {
        val engine = checkNotNull(limiterEngine)
        val frameCount = min(
            engine.pendingFrameCount,
            requestedFrameCapacity
        )
        ensureScratchCapacity(frameCount * format.channelCount)
        limiterTelemetry.beginProcessingCall()
        val producedFrames = engine.drain(
            output = limiterOutputScratch,
            outputOffset = 0,
            maximumFrameCount = frameCount
        )
        val output = outputBuffer(
            producedFrames * format.channelCount * Short.SIZE_BYTES
        )
        encodeWithTelemetry(
            input = limiterOutputScratch,
            output = output,
            sampleCount = producedFrames * format.channelCount
        )
        output.flip()
        publishLimiterTelemetry()
        if (engine.isDrained) {
            completeLimiterDisable()
        }
    }

    private fun publishLimiterTelemetry() {
        runtimeBridge.publishLimiterMeterSnapshot(
            limiterTelemetry.snapshot()
        )
        publishLimiterProcessorState()
    }

    private fun publishLimiterProcessorState() {
        runtimeBridge.publishLimiterProcessorState(
            effectivelyActive = limiterEngine != null,
            primed = limiterEngine?.isPrimed == true,
            reprimeCount = limiterReprimeCount
        )
    }

    private fun applyPendingMeterReset() {
        val resetVersion =
            runtimeBridge.limiterMeterResetVersion()
        if (resetVersion == observedMeterResetVersion) return
        observedMeterResetVersion = resetVersion
        limiterTelemetry.reset()
        runtimeBridge.publishLimiterMeterSnapshot(
            limiterTelemetry.snapshot()
        )
    }

    private fun considerLatestPreparedConfiguration(
        format: EqualizerProcessorFormat
    ) {
        considerLatestLimiterConfiguration(format)
        considerLatestPreparedPath(format)
    }

    private fun considerLatestLimiterConfiguration(
        format: EqualizerProcessorFormat
    ) {
        val latest =
            runtimeBridge.latestCompatibleLimiterConfiguration(format)
                ?: return
        val applied = appliedLimiterConfiguration
        if (
            applied != null &&
            latest.configurationVersion <=
            applied.configurationVersion
        ) {
            return
        }

        if (latest.enabled) {
            pendingLimiterDisable = false
            val activeEngine = limiterEngine
            if (activeEngine == null) {
                limiterEngine = LookaheadLimiterEngine(
                    preparedConfiguration = latest,
                    telemetry = limiterTelemetry
                )
                limiterReprimeCount++
            } else {
                activeEngine.updateCeiling(latest)
            }
            appliedLimiterConfiguration = latest
            publishLimiterProcessorState()
        } else if (limiterEngine != null) {
            pendingLimiterDisable = true
            appliedLimiterConfiguration = latest
        } else {
            appliedLimiterConfiguration = latest
            publishLimiterProcessorState()
        }
    }

    private fun completeLimiterDisable() {
        limiterEngine?.reset()
        limiterEngine = null
        pendingLimiterDisable = false
        endOfStreamDraining = false
        limiterTelemetry.observeLimiterInactive()
        publishLimiterProcessorState()
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
        limiterEngine?.reset()
        limiterEngine = null
        appliedLimiterConfiguration = null
        pendingLimiterDisable = false
        endOfStreamDraining = false
        considerLatestLimiterConfiguration(format)
        publishLimiterProcessorState()
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
        postEqualizerScratch = EMPTY_FLOAT_ARRAY
        limiterOutputScratch = EMPTY_FLOAT_ARRAY
        scratchCapacity = 0
        outputCapacity = 0
        lastTransitionFrameCount = 0
        limiterEngine?.reset()
        limiterEngine = null
        appliedLimiterConfiguration = null
        pendingLimiterDisable = false
        endOfStreamDraining = false
        limiterReprimeCount = 0
        limiterTelemetry.reset()
        observedMeterResetVersion = 0L
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
            postEqualizerScratchIdentity =
                System.identityHashCode(postEqualizerScratch),
            limiterOutputScratchIdentity =
                System.identityHashCode(limiterOutputScratch),
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
            currentPath?.bypassed != false &&
            limiterEngine == null &&
            !pendingLimiterDisable
    }

    private fun ensureScratchCapacity(requiredSampleCount: Int) {
        if (requiredSampleCount <= scratchCapacity) return
        inputScratch = FloatArray(requiredSampleCount)
        currentOutputScratch = FloatArray(requiredSampleCount)
        pendingOutputScratch = FloatArray(requiredSampleCount)
        postEqualizerScratch = FloatArray(requiredSampleCount)
        limiterOutputScratch = FloatArray(requiredSampleCount)
        scratchCapacity = requiredSampleCount
        scratchBufferGrowthCount++
        runtimeBridge.publishScratchBufferGrowthCount(
            scratchBufferGrowthCount
        )
    }

    private fun mixTransition(
        frameCount: Int,
        channelCount: Int
    ): FloatArray {
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
                    postEqualizerScratch[sampleIndex] =
                        mixedOutput.toFloat()
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
                    postEqualizerScratch[sampleIndex] = newOutput
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
        return postEqualizerScratch
    }

    companion object {
        private val EMPTY_FLOAT_ARRAY = FloatArray(0)
        private const val DRAIN_CHUNK_FRAME_COUNT = 1_024
    }
}

internal data class EqualizerBufferReuseSnapshot(
    val scratchCapacity: Int,
    val inputScratchIdentity: Int,
    val currentOutputScratchIdentity: Int,
    val pendingOutputScratchIdentity: Int,
    val postEqualizerScratchIdentity: Int,
    val limiterOutputScratchIdentity: Int,
    val scratchBufferGrowthCount: Int,
    val outputCapacity: Int,
    val outputBufferGrowthCount: Int,
    val currentEngineCapacity:
        com.example.cdplaya.player.equalizer.dsp.EqualizerEngineCapacity?,
    val pendingEngineCapacity:
        com.example.cdplaya.player.equalizer.dsp.EqualizerEngineCapacity?
)
