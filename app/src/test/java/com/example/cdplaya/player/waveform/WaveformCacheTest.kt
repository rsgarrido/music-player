package com.example.cdplaya.player.waveform

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WaveformCacheTest {
    @Test
    fun writeThenRead_roundTripsBinaryCache() {
        val directory = Files.createTempDirectory("waveform-cache-test").toFile()
        val key = waveformCacheKey(
            WaveformSource(1L, "/music/song.mp3", 100L, 200L)
        )
        val expected = WaveformData(listOf(0f, 0.25f, 1f), key)

        WaveformCache(directory).write(expected)
        val actual = WaveformCache(directory).read(key)

        assertEquals(expected, actual)
        directory.deleteRecursively()
    }

    @Test
    fun missingOrInvalidCache_returnsNull() {
        val directory = Files.createTempDirectory("waveform-cache-test").toFile()
        val key = waveformCacheKey(
            WaveformSource(2L, "/music/song.flac", 300L, 400L)
        )
        directory.mkdirs()
        java.io.File(directory, "$key.bin").writeText("not a waveform")

        assertNull(WaveformCache(directory).read(key))
        assertNull(WaveformCache(directory).read("unsafe-key"))
        directory.deleteRecursively()
    }
}
