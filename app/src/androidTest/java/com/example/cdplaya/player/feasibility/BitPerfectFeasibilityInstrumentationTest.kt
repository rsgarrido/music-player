package com.example.cdplaya.player.feasibility

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.media3.exoplayer.audio.AudioTrackAudioOutputProvider
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cdplaya.player.equalizer.EqualizerAudioProcessor
import com.example.cdplaya.player.equalizer.EqualizerRenderersFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(UnstableApi::class)
@RunWith(AndroidJUnit4::class)
class BitPerfectFeasibilityInstrumentationTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        BitPerfectFeasibilityRuntimeBridge.reset()
        BitPerfectFeasibilityRuntimeBridge.setMode(
            FeasibilityProbeMode.OBSERVE_CURRENT_PATH
        )
    }

    @After
    fun tearDown() {
        BitPerfectFeasibilityRuntimeBridge.reset()
    }

    @Test
    fun persistentProcessorPathResolvesAllRepresentativeSourcesToPcm16() {
        listOf(
            C.ENCODING_PCM_16BIT to 44_100,
            C.ENCODING_PCM_16BIT to 48_000,
            C.ENCODING_PCM_24BIT to 96_000,
            C.ENCODING_PCM_24BIT to 192_000
        ).forEach { (sourceEncoding, sampleRate) ->
            val provider = observingProvider()
            val sink = EqualizerRenderersFactory(
                context,
                EqualizerAudioProcessor(),
                provider
            ).buildAudioSinkForTest(context)
            sink.configure(
                rawFormat(sourceEncoding, sampleRate),
                0,
                null
            )
            val output =
                BitPerfectFeasibilityRuntimeBridge.state.value
                    .media3OutputConfig
            assertNotNull(output)
            assertEquals(C.ENCODING_PCM_16BIT, output?.encoding)
            assertEquals(sampleRate, output?.sampleRateHz)
            sink.release()
        }
    }

    @Test
    fun processorFreeFloatPathIsReportedSeparately() {
        val provider = observingProvider()
        val sink = DefaultAudioSink.Builder(context)
            .setAudioOutputProvider(provider)
            .setEnableFloatOutput(true)
            .build()
        sink.configure(
            rawFormat(C.ENCODING_PCM_24BIT, 96_000),
            0,
            null
        )
        val state = BitPerfectFeasibilityRuntimeBridge.state.value
        assertEquals(
            C.ENCODING_PCM_FLOAT,
            state.media3OutputConfig?.encoding
        )
        assertTrue(
            state.media3FormatConfig?.highResolutionPcmEnabled == true
        )
        sink.release()
    }

    @Test
    fun media3ProviderCreatesExactProcessorFreeAudioTracks() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val delegate =
                AudioTrackAudioOutputProvider.Builder(null).build()
            listOf(
                C.ENCODING_PCM_16BIT to 44_100,
                C.ENCODING_PCM_16BIT to 48_000,
                C.ENCODING_PCM_24BIT to 96_000,
                C.ENCODING_PCM_24BIT to 192_000
            ).forEach { (encoding, sampleRate) ->
                val format = rawFormat(encoding, sampleRate)
                val config =
                    AudioOutputProvider.FormatConfig.Builder(format)
                        .setEnableHighResolutionPcmOutput(true)
                        .build()
                val output = delegate.getOutputConfig(config)
                assertEquals(encoding, output.encoding)
                assertEquals(sampleRate, output.sampleRate)
                val audioOutput = delegate.getAudioOutput(output)
                val audioTrackFormat = audioOutput.audioTrack.format
                assertEquals(encoding, audioTrackFormat.encoding)
                assertEquals(sampleRate, audioTrackFormat.sampleRate)
                assertEquals(
                    output.channelMask,
                    audioTrackFormat.channelMask
                )
                audioOutput.release()
            }
            delegate.release()
        }
    }

    @Test
    fun api36UsbEnumerationIsSafeAndDoesNotActivateAnything() {
        val backend = AndroidUsbMixerBackend.create(context)
        val controller = UsbMixerFeasibilityController(backend)
        val match = controller.inspect(
            Media3OutputConfigSnapshot(
                encoding = C.ENCODING_PCM_16BIT,
                sampleRateHz = 44_100,
                channelMask = 12
            )
        )
        assertNotNull(match.rejectionReason)
        assertEquals(null, BitPerfectFeasibilityRuntimeBridge.state.value.setResult)
    }

    private fun observingProvider(): FeasibilityAudioOutputProvider =
        FeasibilityAudioOutputProvider(
            AudioTrackAudioOutputProvider.Builder(null).build()
        )

    private fun rawFormat(encoding: Int, sampleRate: Int): Format =
        Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_RAW)
            .setPcmEncoding(encoding)
            .setSampleRate(sampleRate)
            .setChannelCount(2)
            .build()
}
