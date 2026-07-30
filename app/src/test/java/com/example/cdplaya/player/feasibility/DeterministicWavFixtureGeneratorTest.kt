package com.example.cdplaya.player.feasibility

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import kotlin.math.roundToInt
import kotlin.math.sin
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeterministicWavFixtureGeneratorTest {
    @Test
    fun representativeFixturesAreSmallDeterministicAndSelfDescribing() {
        val formats = listOf(
            WavFixtureFormat(16, 44_100),
            WavFixtureFormat(16, 48_000),
            WavFixtureFormat(24, 96_000),
            WavFixtureFormat(24, 192_000)
        )
        formats.forEach { format ->
            val first = generateWavFixture(format)
            val second = generateWavFixture(format)
            assertArrayEquals(first, second)
            assertTrue(first.size < 80_000)
            assertEquals("RIFF", first.copyOfRange(0, 4).decodeToString())
            assertEquals("WAVE", first.copyOfRange(8, 12).decodeToString())
            assertEquals(
                sha256(first),
                sha256(second)
            )
            println(
                "fixture-${format.bitsPerSample}-${format.sampleRateHz}.wav " +
                    sha256(first)
            )
        }
    }
}

private data class WavFixtureFormat(
    val bitsPerSample: Int,
    val sampleRateHz: Int
)

/**
 * Generates 40 ms of a low-amplitude 997 Hz sine with an initial impulse.
 * The generator lives in test sources, so no fixture bytes enter production APKs.
 */
private fun generateWavFixture(format: WavFixtureFormat): ByteArray {
    val channels = 2
    val bytesPerSample = format.bitsPerSample / 8
    val frameCount = format.sampleRateHz / 25
    val dataSize = frameCount * channels * bytesPerSample
    val output = ByteBuffer.allocate(44 + dataSize)
        .order(ByteOrder.LITTLE_ENDIAN)
    output.put("RIFF".encodeToByteArray())
    output.putInt(36 + dataSize)
    output.put("WAVEfmt ".encodeToByteArray())
    output.putInt(16)
    output.putShort(1)
    output.putShort(channels.toShort())
    output.putInt(format.sampleRateHz)
    output.putInt(format.sampleRateHz * channels * bytesPerSample)
    output.putShort((channels * bytesPerSample).toShort())
    output.putShort(format.bitsPerSample.toShort())
    output.put("data".encodeToByteArray())
    output.putInt(dataSize)
    repeat(frameCount) { frame ->
        val sine = if (frame == 0) {
            0.125
        } else {
            sin(2.0 * Math.PI * 997.0 * frame / format.sampleRateHz) *
                0.0625
        }
        repeat(channels) {
            if (format.bitsPerSample == 16) {
                output.putShort((sine * Short.MAX_VALUE).roundToInt().toShort())
            } else {
                val value = (sine * 8_388_607).roundToInt()
                output.put((value and 0xff).toByte())
                output.put((value shr 8 and 0xff).toByte())
                output.put((value shr 16 and 0xff).toByte())
            }
        }
    }
    return output.array()
}

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
