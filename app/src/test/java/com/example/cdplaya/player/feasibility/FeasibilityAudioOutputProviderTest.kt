package com.example.cdplaya.player.feasibility

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.audio.AudioOutput
import androidx.media3.exoplayer.audio.AudioOutputProvider
import java.nio.ByteBuffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class FeasibilityAudioOutputProviderTest {
    private lateinit var delegate: AudioOutputProvider
    private lateinit var output: AudioOutput
    private lateinit var provider: FeasibilityAudioOutputProvider
    private lateinit var formatConfig: AudioOutputProvider.FormatConfig
    private lateinit var outputConfig: AudioOutputProvider.OutputConfig

    @Before
    fun setUp() {
        BitPerfectFeasibilityRuntimeBridge.reset()
        delegate = mock(AudioOutputProvider::class.java)
        output = mock(AudioOutput::class.java)
        formatConfig = AudioOutputProvider.FormatConfig.Builder(
            Format.Builder()
                .setSampleMimeType(MimeTypes.AUDIO_RAW)
                .setPcmEncoding(C.ENCODING_PCM_16BIT)
                .setSampleRate(44_100)
                .setChannelCount(2)
                .build()
        ).build()
        outputConfig = AudioOutputProvider.OutputConfig.Builder()
            .setEncoding(C.ENCODING_PCM_16BIT)
            .setSampleRate(44_100)
            .setChannelMask(12)
            .setBufferSize(4096)
            .build()
        doReturn(
            AudioOutputProvider.FormatSupport.Builder()
                .setFormatSupportLevel(
                    AudioOutputProvider.FORMAT_SUPPORTED_DIRECTLY
                )
                .build()
        ).`when`(delegate).getFormatSupport(formatConfig)
        doReturn(outputConfig)
            .`when`(delegate).getOutputConfig(formatConfig)
        doReturn(output)
            .`when`(delegate)
            .getAudioOutput(any(AudioOutputProvider.OutputConfig::class.java))
        provider = FeasibilityAudioOutputProvider(delegate)
    }

    @After
    fun tearDown() {
        BitPerfectFeasibilityRuntimeBridge.reset()
    }

    @Test
    fun offModeDelegatesWithoutPublishingFacts() {
        provider.getFormatSupport(formatConfig)
        provider.getOutputConfig(formatConfig)
        provider.getAudioOutput(outputConfig)
        verify(delegate).getFormatSupport(formatConfig)
        verify(delegate).getOutputConfig(formatConfig)
        assertNull(
            BitPerfectFeasibilityRuntimeBridge.state.value.media3OutputConfig
        )
    }

    @Test
    fun observeModeDoesNotModifyConfigAndPublishesCreationOnce() {
        BitPerfectFeasibilityRuntimeBridge.setMode(
            FeasibilityProbeMode.OBSERVE_CURRENT_PATH
        )
        provider.getFormatSupport(formatConfig)
        provider.getOutputConfig(formatConfig)
        provider.getAudioOutput(outputConfig)
        verify(delegate).getFormatSupport(formatConfig)
        verify(delegate).getOutputConfig(formatConfig)
        assertEquals(
            1,
            BitPerfectFeasibilityRuntimeBridge.state.value
                .outputCreationCount
        )
    }

    @Test
    fun identicalFormatAndOutputSnapshotsAreDeduplicated() {
        BitPerfectFeasibilityRuntimeBridge.setMode(
            FeasibilityProbeMode.OBSERVE_CURRENT_PATH
        )
        provider.getOutputConfig(formatConfig)
        val eventCount =
            BitPerfectFeasibilityRuntimeBridge.state.value.events.size
        provider.getOutputConfig(formatConfig)
        assertEquals(
            eventCount,
            BitPerfectFeasibilityRuntimeBridge.state.value.events.size
        )
    }

    @Test
    fun flushAndReleaseAreObservedAndReleaseIsIdempotent() {
        BitPerfectFeasibilityRuntimeBridge.setMode(
            FeasibilityProbeMode.OBSERVE_CURRENT_PATH
        )
        val wrapped = provider.getAudioOutput(outputConfig)
        wrapped.flush()
        wrapped.release()
        wrapped.release()
        verify(output).flush()
        verify(output, times(1)).release()
        val state = BitPerfectFeasibilityRuntimeBridge.state.value
        assertEquals(1, state.outputFlushCount)
        assertEquals(1, state.outputReleaseCount)
        assertNull(state.currentOutputGeneration)
        assertNull(state.audioTrackFormat)
    }

    @Test
    fun writeFailureRemainsDistinguishable() {
        BitPerfectFeasibilityRuntimeBridge.setMode(
            FeasibilityProbeMode.OBSERVE_CURRENT_PATH
        )
        val failure = mock(AudioOutput.WriteException::class.java)
        doThrow(failure).`when`(output).write(
            any(ByteBuffer::class.java),
            org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyLong()
        )
        val wrapped = provider.getAudioOutput(outputConfig)
        try {
            wrapped.write(
                ByteBuffer.allocateDirect(4),
                1,
                0
            )
        } catch (_: AudioOutput.WriteException) {
            // Expected.
        }
        assertEquals(
            FeasibilityEventType.WRITE_FAILED,
            BitPerfectFeasibilityRuntimeBridge.state.value.events.last().type
        )
    }

    @Test
    fun providerReleaseIsIdempotent() {
        provider.release()
        provider.release()
        verify(delegate, times(1)).release()
    }
}
