package com.example.cdplaya.player.equalizer

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.StreamMetadata
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import com.example.cdplaya.player.equalizer.dsp.EqualizerConfiguration
import com.example.cdplaya.player.equalizer.dsp.EqualizerFilterSpec
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
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
