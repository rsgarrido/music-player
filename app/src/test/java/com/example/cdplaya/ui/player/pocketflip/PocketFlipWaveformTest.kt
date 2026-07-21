package com.example.cdplaya.ui.player.pocketflip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PocketFlipWaveformTest {
    @Test
    fun unavailableWaveform_keepsDecorativeFallback() {
        assertNull(mapPocketFlipWaveformLevels(null))
        assertNull(mapPocketFlipWaveformLevels(emptyList()))
    }

    @Test
    fun realWaveform_isMappedAndClampedSafely() {
        val levels = mapPocketFlipWaveformLevels(
            amplitudes = listOf(Float.NaN, -1f, 0.25f, 2f),
            barCount = 12
        )

        requireNotNull(levels)
        assertEquals(12, levels.size)
        assertTrue(levels.all { level -> level.isFinite() && level in 0f..1f })
    }
}
