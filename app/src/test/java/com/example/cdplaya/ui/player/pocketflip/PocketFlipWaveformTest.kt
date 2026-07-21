package com.example.cdplaya.ui.player.pocketflip

import com.example.cdplaya.ui.player.buildTrackReactiveVisualizerLevels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PocketFlipWaveformTest {
    @Test
    fun unavailableWaveform_keepsDecorativeFallback() {
        assertNull(buildLevels(null))
        assertNull(buildLevels(emptyList()))
    }

    @Test
    fun realWaveform_isMappedAndClampedSafely() {
        val levels = buildLevels(listOf(Float.NaN, -1f, 0.25f, 2f))

        requireNotNull(levels)
        assertEquals(POCKET_FLIP_VISUALIZER_COLUMN_COUNT, levels.size)
        assertTrue(levels.all { level -> level.isFinite() && level in 0f..1f })
    }

    private fun buildLevels(amplitudes: List<Float>?) =
        buildTrackReactiveVisualizerLevels(
            amplitudes = amplitudes,
            currentPositionMs = 45_000L,
            durationMs = 180_000L,
            columnCount = POCKET_FLIP_VISUALIZER_COLUMN_COUNT,
            animationPhase = 0.25f,
            isPlaying = true
        )
}
