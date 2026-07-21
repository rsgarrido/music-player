package com.example.cdplaya.ui.player.retrorack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RetroRackWaveformTest {
    @Test
    fun unavailableWaveform_keepsDecorativeFallback() {
        assertNull(mapRetroRackWaveformLevels(null, barCount = 18))
        assertNull(mapRetroRackWaveformLevels(emptyList(), barCount = 18))
    }

    @Test
    fun realWaveform_isMappedAndClampedSafely() {
        val levels = mapRetroRackWaveformLevels(
            amplitudes = listOf(Float.POSITIVE_INFINITY, -0.5f, 0.5f, 1.5f),
            barCount = 18
        )

        requireNotNull(levels)
        assertEquals(18, levels.size)
        assertTrue(levels.all { level -> level.isFinite() && level in 0f..1f })
    }
}
