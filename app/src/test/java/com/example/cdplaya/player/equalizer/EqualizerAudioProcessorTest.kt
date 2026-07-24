package com.example.cdplaya.player.equalizer

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.StreamMetadata
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import com.example.cdplaya.player.equalizer.dsp.EqualizerConfiguration
import com.example.cdplaya.player.equalizer.dsp.EqualizerFilterSpec
import com.example.cdplaya.player.equalizer.limiter.LimiterConfiguration
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EqualizerAudioProcessorTest {
    @Before
    fun resetBridgeBeforeTest() {
        EqualizerRuntimeBridge.release()
    }

    @After
    fun resetBridgeAfterTest() {
        EqualizerRuntimeBridge.release()
    }

    @Test
    fun pcm16FormatsRemainActiveAndUnchanged() {
        val formats = listOf(
            AudioFormat(32_000, 1, C.ENCODING_PCM_16BIT),
            AudioFormat(44_100, 2, C.ENCODING_PCM_16BIT),
            AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT),
            AudioFormat(96_000, 2, C.ENCODING_PCM_16BIT),
            AudioFormat(192_000, 2, C.ENCODING_PCM_16BIT),
            AudioFormat(48_000, 6, C.ENCODING_PCM_16BIT)
        )

        formats.forEach { format ->
            val processor = EqualizerAudioProcessor()

            assertEquals(format, processor.configure(format))
            assertTrue(processor.isActive)
            processor.flush(StreamMetadata.DEFAULT)
            processor.reset()
        }
    }

    @Test
    fun unsupportedOrInvalidFormatsAreRejected() {
        listOf(
            AudioFormat(48_000, 2, C.ENCODING_PCM_FLOAT),
            AudioFormat(48_000, 2, C.ENCODING_PCM_24BIT),
            AudioFormat(0, 2, C.ENCODING_PCM_16BIT),
            AudioFormat(48_000, 0, C.ENCODING_PCM_16BIT)
        ).forEach { format ->
            assertThrows(UnhandledAudioFormatException::class.java) {
                EqualizerAudioProcessor().configure(format)
            }
        }
    }

    @Test
    fun exactBypassPreservesBytesAcrossChannelsAndSequentialBuffers() {
        listOf(1, 2, 6).forEach { channelCount ->
            val processor = configuredProcessor(channelCount = channelCount)
            repeat(4) { bufferIndex ->
                val byteCount = channelCount * Short.SIZE_BYTES * 127
                val bytes = ByteArray(byteCount)
                Random(bufferIndex * 31 + channelCount).nextBytes(bytes)
                if (bufferIndex == 0) {
                    alternatingEndpoints(bytes)
                }
                val input = directBuffer(bytes)

                processor.queueInput(input)
                val output = processor.output

                assertEquals(input.limit(), input.position())
                assertEquals(bytes.size, output.remaining())
                assertTrue(output.isDirect)
                assertEquals(ByteOrder.nativeOrder(), output.order())
                assertArrayEquals(bytes, output.toByteArray())
            }
            EqualizerRuntimeBridge.publishStateForTest()
            assertTrue(EqualizerRuntimeBridge.state.value.processorConfigured)
            assertTrue(EqualizerRuntimeBridge.state.value.bypassed)
            processor.reset()
        }
    }

    @Test
    fun silenceBypassIsByteExact() {
        val bytes = ByteArray(2 * 2 * 256)
        val processor = configuredProcessor(channelCount = 2)

        processor.queueInput(directBuffer(bytes))

        assertArrayEquals(bytes, processor.output.toByteArray())
    }

    @Test
    fun emptyMedia3InputDoesNotAttemptToCopyTheSharedEmptyBuffer() {
        val processor = configuredProcessor(channelCount = 2)

        processor.queueInput(AudioProcessor.EMPTY_BUFFER)

        assertFalse(processor.output.hasRemaining())
    }

    @Test
    fun nonFrameAlignedInputIsRejectedClearly() {
        val processor = configuredProcessor(channelCount = 2)
        val input = ByteBuffer
            .allocateDirect(3)
            .order(ByteOrder.nativeOrder())
        input.put(byteArrayOf(1, 2, 3)).flip()

        assertThrows(IllegalArgumentException::class.java) {
            processor.queueInput(input)
        }
    }

    @Test
    fun preampIsAppliedOnceAndSaturatesWithoutWraparound() {
        val configuration = EqualizerConfiguration(
            enabled = true,
            preampDb = 6.020_599_913_279_624,
            filters = emptyList()
        )
        val processor = configuredProcessor(
            channelCount = 1,
            initialPlan = plan(
                version = 1L,
                configuration = configuration
            )
        )
        val input = shortBuffer(shortArrayOf(8_192, 20_000, -20_000))

        processor.queueInput(input)
        val output = processor.output

        assertEquals(16_384, output.short.toInt())
        assertEquals(Short.MAX_VALUE, output.short)
        assertEquals(Short.MIN_VALUE, output.short)
        EqualizerRuntimeBridge.publishStateForTest()
        assertEquals(
            2L,
            EqualizerRuntimeBridge.state.value.overRangeSampleCount
        )
        assertEquals(
            2L,
            EqualizerRuntimeBridge.state.value.saturatedSampleCount
        )
    }

    @Test
    fun automaticHeadroomIsIncludedInPreparedPreamp() {
        val configuration = EqualizerConfiguration(
            enabled = true,
            preampDb = 6.0,
            filters = emptyList()
        )
        val preparedPlan = plan(
            version = 2L,
            configuration = configuration,
            automaticHeadroomEnabled = true
        )
        val processor = configuredProcessor(
            channelCount = 1,
            initialPlan = preparedPlan
        )
        val inputSample = 10_000.toShort()

        processor.queueInput(shortBuffer(shortArrayOf(inputSample)))
        val actual = processor.output.short
        val expected = Pcm16SampleConversion.fromNormalizedFloat(
            Pcm16SampleConversion.toNormalizedFloat(inputSample) *
                preparedPlan.cascade.effectivePreampMultiplier.toFloat()
        )

        assertEquals(expected, actual)
        assertTrue(
            preparedPlan.automaticHeadroomResult.attenuationDb > 6.0
        )
    }

    @Test
    fun bassAndTreblePlansChangeSignalsNearTheirCenters() {
        listOf(125.0, 8_000.0).forEachIndexed { index, frequencyHz ->
            val inputSamples = sinePcm(
                frequencyHz = frequencyHz,
                sampleRateHz = 48_000,
                frameCount = 4_800
            )
            val configuration = EqualizerConfiguration(
                enabled = true,
                preampDb = 0.0,
                filters = listOf(
                    EqualizerFilterSpec.Peaking(
                        frequencyHz = frequencyHz,
                        gainDb = 6.0,
                        q = 1.41
                    )
                )
            )
            val processor = configuredProcessor(
                channelCount = 1,
                initialPlan = plan(
                    version = index + 1L,
                    configuration = configuration,
                    automaticHeadroomEnabled = true
                )
            )

            processor.queueInput(shortBuffer(inputSamples))
            val outputSamples = processor.output.toShortArray()

            assertFalse(inputSamples.contentEquals(outputSamples))
            assertEquals(inputSamples.size, outputSamples.size)
        }
    }

    @Test
    fun activeProcessingDoesNotMutateInputAndKeepsChannelsIndependent() {
        val configuration = EqualizerConfiguration(
            enabled = true,
            preampDb = 0.0,
            filters = listOf(
                EqualizerFilterSpec.Peaking(1_000.0, 6.0, 1.41)
            )
        )
        val processor = configuredProcessor(
            channelCount = 2,
            initialPlan = plan(
                version = 1L,
                configuration = configuration,
                channelCount = 2
            )
        )
        val samples = ShortArray(512)
        samples[0] = 12_000
        val input = shortBuffer(samples)
        val originalBytes = input.readOnlyBytes()

        processor.queueInput(input)
        val output = processor.output.toShortArray()

        assertArrayEquals(originalBytes, input.readOnlyBytes())
        output.indices
            .filter { sampleIndex -> sampleIndex % 2 == 1 }
            .forEach { sampleIndex ->
                assertEquals(0, output[sampleIndex].toInt())
            }
        assertEquals(0, output.size % 2)
    }

    @Test
    fun fixedSizeSteadyStateReusesScratchAndEngineCapacity() {
        val configuration = EqualizerConfiguration(
            enabled = true,
            preampDb = -1.0,
            filters = listOf(
                EqualizerFilterSpec.Peaking(1_000.0, 3.0, 1.41)
            )
        )
        val processor = configuredProcessor(
            channelCount = 2,
            initialPlan = plan(
                version = 1L,
                configuration = configuration,
                channelCount = 2
            )
        )
        val samples = ShortArray(512) { index -> (index * 17).toShort() }
        processor.queueInput(shortBuffer(samples))
        processor.output
        val warm = processor.bufferReuseSnapshot()

        repeat(100) {
            processor.queueInput(shortBuffer(samples))
            processor.output
        }
        val after = processor.bufferReuseSnapshot()

        assertEquals(warm.scratchCapacity, after.scratchCapacity)
        assertEquals(warm.inputScratchIdentity, after.inputScratchIdentity)
        assertEquals(
            warm.currentOutputScratchIdentity,
            after.currentOutputScratchIdentity
        )
        assertEquals(
            warm.pendingOutputScratchIdentity,
            after.pendingOutputScratchIdentity
        )
        assertEquals(
            warm.scratchBufferGrowthCount,
            after.scratchBufferGrowthCount
        )
        assertEquals(
            warm.outputBufferGrowthCount,
            after.outputBufferGrowthCount
        )
        assertEquals(
            warm.currentEngineCapacity,
            after.currentEngineCapacity
        )
        assertEquals(
            warm.postEqualizerScratchIdentity,
            after.postEqualizerScratchIdentity
        )
        assertEquals(
            warm.limiterOutputScratchIdentity,
            after.limiterOutputScratchIdentity
        )
    }

    @Test
    fun limiterRetainsInitialFramesAndDrainsEverySourceFrameAtEos() {
        val processor = configuredLimiterProcessor(
            channelCount = 1
        )
        val inputSamples = ShortArray(500) { index ->
            (index - 250).toShort()
        }
        val output = ArrayList<Short>()

        processor.queueInput(shortBuffer(inputSamples))
        processor.output.toShortArray().forEach(output::add)
        assertEquals(260, output.size)
        assertFalse(processor.isEnded)

        processor.queueEndOfStream()
        while (!processor.isEnded) {
            processor.output.toShortArray().forEach(output::add)
        }

        assertEquals(inputSamples.size, output.size)
        assertArrayEquals(
            inputSamples,
            output.toShortArray()
        )
    }

    @Test
    fun flushDiscardsPartiallyPrimedPreSeekAudioAndRetainsLimiter() {
        val processor = configuredLimiterProcessor(
            channelCount = 1
        )
        val stale = ShortArray(100) { 12_000 }
        processor.queueInput(shortBuffer(stale))
        assertFalse(processor.output.hasRemaining())

        processor.flush(StreamMetadata.DEFAULT)
        val fresh = ShortArray(241)
        fresh[0] = 7_000
        processor.queueInput(shortBuffer(fresh))
        val firstOutput = processor.output

        assertEquals(Short.SIZE_BYTES, firstOutput.remaining())
        assertEquals(7_000, firstOutput.short.toInt())
        EqualizerRuntimeBridge.publishStateForTest()
        assertTrue(
            EqualizerRuntimeBridge.state.value
                .limiterEffectivelyActive
        )
    }

    @Test
    fun ceilingOnlyUpdateDoesNotReprimeLimiterLatency() {
        val processor = configuredLimiterProcessor(
            channelCount = 1
        )
        processor.queueInput(shortBuffer(ShortArray(300) { 1_000 }))
        processor.output
        EqualizerRuntimeBridge.publishStateForTest()
        val initialReprimeCount =
            EqualizerRuntimeBridge.state.value.limiterReprimeCount

        installRuntimeSnapshot(
            EqualizerRuntimeBridge.requestConfiguration(
                configuration = bypassConfiguration(),
                automaticHeadroomEnabled = false,
                limiterConfiguration = LimiterConfiguration(
                    enabled = true,
                    ceilingDbfs = -3.0
                )
            ),
            channelCount = 1
        )
        processor.queueInput(shortBuffer(ShortArray(32) { 1_000 }))
        processor.output
        EqualizerRuntimeBridge.publishStateForTest()

        assertEquals(
            initialReprimeCount,
            EqualizerRuntimeBridge.state.value.limiterReprimeCount
        )
    }

    @Test
    fun disablingLimiterDrainsTailBeforeConsumingNewInput() {
        val processor = configuredLimiterProcessor(
            channelCount = 1
        )
        val old = ShortArray(300) { 2_000 }
        processor.queueInput(shortBuffer(old))
        val initialOutput = processor.output.toShortArray()
        assertEquals(60, initialOutput.size)

        installRuntimeSnapshot(
            EqualizerRuntimeBridge.requestConfiguration(
                configuration = bypassConfiguration(),
                automaticHeadroomEnabled = false,
                limiterConfiguration = LimiterConfiguration(
                    enabled = false
                )
            ),
            channelCount = 1
        )
        val newInput = shortBuffer(ShortArray(300) { 3_000 })
        processor.queueInput(newInput)
        val drainedTail = processor.output.toShortArray()

        assertEquals(240, drainedTail.size)
        assertEquals(0, newInput.position())
        assertTrue(drainedTail.all { it.toInt() == 2_000 })

        processor.queueInput(newInput)
        val newOutput = processor.output.toShortArray()
        assertEquals(300, newOutput.size)
        assertTrue(newOutput.all { it.toInt() == 3_000 })
    }

    @Test
    fun eosThenCompatibleStreamFlushHasNoDuplicateOrStaleTail() {
        val processor = configuredLimiterProcessor(
            channelCount = 1
        )
        val first = ShortArray(350) { 1_000 }
        processor.queueInput(shortBuffer(first))
        val firstOutput = ArrayList<Short>()
        processor.output.toShortArray().forEach(firstOutput::add)
        processor.queueEndOfStream()
        while (!processor.isEnded) {
            processor.output.toShortArray().forEach(firstOutput::add)
        }
        assertArrayEquals(first, firstOutput.toShortArray())

        processor.flush(StreamMetadata.DEFAULT)
        val second = ShortArray(350) { 2_000 }
        processor.queueInput(shortBuffer(second))
        val secondOutput = ArrayList<Short>()
        processor.output.toShortArray().forEach(secondOutput::add)
        processor.queueEndOfStream()
        while (!processor.isEnded) {
            processor.output.toShortArray().forEach(secondOutput::add)
        }

        assertArrayEquals(second, secondOutput.toShortArray())
    }

    @Test
    fun emptyInputIsSafeWhileLimiterIsActive() {
        val processor = configuredLimiterProcessor(
            channelCount = 2
        )

        processor.queueInput(AudioProcessor.EMPTY_BUFFER)

        assertFalse(processor.output.hasRemaining())
    }

    @Test
    fun limiterReceivesPostEqualizerSamplesAndPreventsPcmSaturation() {
        val processor = configuredLimiterProcessor(
            channelCount = 1,
            equalizerConfiguration = EqualizerConfiguration(
                enabled = true,
                preampDb = 6.0,
                filters = emptyList()
            )
        )
        val input = ShortArray(600) { 30_000 }
        val output = ArrayList<Short>()

        processor.queueInput(shortBuffer(input))
        processor.output.toShortArray().forEach(output::add)
        processor.queueEndOfStream()
        while (!processor.isEnded) {
            processor.output.toShortArray().forEach(output::add)
        }
        EqualizerRuntimeBridge.publishStateForTest()
        val runtime = EqualizerRuntimeBridge.state.value
        val maximumOutput = output.maxOf { sample ->
            kotlin.math.abs(sample.toInt())
        }
        val ceilingPcm =
            Pcm16SampleConversion.fromNormalizedFloat(
                10.0.pow(-1.0 / 20.0).toFloat()
            ).toInt()

        assertEquals(input.size, output.size)
        assertTrue(
            "maximum PCM $maximumOutput exceeded ceiling PCM $ceilingPcm",
            maximumOutput <= ceilingPcm + 1
        )
        assertTrue(runtime.overRangeSampleCount > 0L)
        assertEquals(0L, runtime.saturatedSampleCount)
        assertTrue(runtime.limiterReducedFrameCount > 0L)
    }

    @Test
    fun sampleRateChangeRebuildsLookaheadAndDropsOldFormatState() {
        val processor = configuredLimiterProcessor(
            channelCount = 1,
            sampleRateHz = 48_000
        )
        processor.queueInput(shortBuffer(ShortArray(100) { 4_000 }))
        assertFalse(processor.output.hasRemaining())

        val newFormat = AudioFormat(
            96_000,
            1,
            C.ENCODING_PCM_16BIT
        )
        processor.configure(newFormat)
        installRuntimeSnapshot(
            snapshot = EqualizerRuntimeBridge.requestedSnapshot(),
            channelCount = 1,
            sampleRateHz = 96_000
        )
        processor.flush(StreamMetadata.DEFAULT)

        processor.queueInput(shortBuffer(ShortArray(480) { 5_000 }))
        assertFalse(processor.output.hasRemaining())
        processor.queueInput(shortBuffer(shortArrayOf(6_000)))
        val firstOutput = processor.output.toShortArray()

        assertArrayEquals(shortArrayOf(5_000), firstOutput)
        EqualizerRuntimeBridge.publishStateForTest()
        assertEquals(
            480,
            EqualizerRuntimeBridge.state.value
                .limiterLookaheadFrames
        )
    }

    private fun configuredProcessor(
        channelCount: Int,
        sampleRateHz: Int = 48_000,
        initialPlan: PreparedEqualizerPlan? = null
    ): EqualizerAudioProcessor {
        val processor = EqualizerAudioProcessor()
        val format = AudioFormat(
            sampleRateHz,
            channelCount,
            C.ENCODING_PCM_16BIT
        )
        processor.configure(format)
        initialPlan?.let { preparedPlan ->
            EqualizerRuntimeBridge.installPreparedPathForTest(
                preparedPlan.createProcessingPath()
            )
        }
        processor.flush(StreamMetadata.DEFAULT)
        return processor
    }

    private fun configuredLimiterProcessor(
        channelCount: Int,
        sampleRateHz: Int = 48_000,
        equalizerConfiguration: EqualizerConfiguration =
            bypassConfiguration()
    ): EqualizerAudioProcessor {
        val snapshot =
            EqualizerRuntimeBridge.requestConfiguration(
                configuration = equalizerConfiguration,
                automaticHeadroomEnabled = false,
                limiterConfiguration = LimiterConfiguration(
                    enabled = true
                )
            )
        val processor = EqualizerAudioProcessor()
        val format = AudioFormat(
            sampleRateHz,
            channelCount,
            C.ENCODING_PCM_16BIT
        )
        processor.configure(format)
        installRuntimeSnapshot(
            snapshot = snapshot,
            channelCount = channelCount,
            sampleRateHz = sampleRateHz
        )
        processor.flush(StreamMetadata.DEFAULT)
        return processor
    }

    private fun installRuntimeSnapshot(
        snapshot: EqualizerRuntimeSnapshot,
        channelCount: Int,
        sampleRateHz: Int = 48_000
    ) {
        EqualizerRuntimeBridge.installPreparedPathForTest(
            EqualizerPlanPreparer.prepare(
                snapshot = snapshot,
                processorFormat = EqualizerProcessorFormat(
                    sampleRateHz = sampleRateHz,
                    channelCount = channelCount,
                    pcmEncoding = C.ENCODING_PCM_16BIT
                )
            ).createProcessingPath()
        )
    }

    private fun plan(
        version: Long,
        configuration: EqualizerConfiguration,
        automaticHeadroomEnabled: Boolean = false,
        channelCount: Int = 1
    ): PreparedEqualizerPlan {
        return EqualizerPlanPreparer.prepare(
            EqualizerRuntimeSnapshot(
                version = version,
                configuration = configuration,
                automaticHeadroomEnabled = automaticHeadroomEnabled
            ),
            EqualizerProcessorFormat(
                sampleRateHz = 48_000,
                channelCount = channelCount,
                pcmEncoding = C.ENCODING_PCM_16BIT
            )
        )
    }

    private fun bypassConfiguration() =
        EqualizerConfiguration(
            enabled = false,
            preampDb = 0.0,
            filters = emptyList()
        )

    private fun directBuffer(bytes: ByteArray): ByteBuffer {
        val buffer = ByteBuffer
            .allocateDirect(bytes.size)
            .order(ByteOrder.nativeOrder())
            .put(bytes)
        buffer.flip()
        return buffer
    }

    private fun shortBuffer(samples: ShortArray): ByteBuffer {
        val buffer = ByteBuffer
            .allocateDirect(samples.size * Short.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        samples.forEach(buffer::putShort)
        buffer.flip()
        return buffer
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        return ByteArray(remaining()).also(::get)
    }

    private fun ByteBuffer.toShortArray(): ShortArray {
        return ShortArray(remaining() / Short.SIZE_BYTES) {
            short
        }
    }

    private fun ByteBuffer.readOnlyBytes(): ByteArray {
        val duplicate = duplicate().order(ByteOrder.nativeOrder())
        duplicate.position(0)
        return ByteArray(duplicate.limit()).also(duplicate::get)
    }

    private fun alternatingEndpoints(bytes: ByteArray) {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder())
        var useMinimum = true
        while (buffer.remaining() >= Short.SIZE_BYTES) {
            buffer.putShort(
                if (useMinimum) Short.MIN_VALUE else Short.MAX_VALUE
            )
            useMinimum = !useMinimum
        }
    }

    private fun sinePcm(
        frequencyHz: Double,
        sampleRateHz: Int,
        frameCount: Int
    ): ShortArray {
        return ShortArray(frameCount) { frameIndex ->
            (
                sin(2.0 * PI * frequencyHz * frameIndex / sampleRateHz) *
                    8_000.0
                ).toInt().toShort()
        }
    }
}
