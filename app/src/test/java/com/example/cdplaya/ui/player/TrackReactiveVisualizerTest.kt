package com.example.cdplaya.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackReactiveVisualizerTest {
    @Test
    fun outputChangesWithPlaybackPosition() {
        val amplitudes = List(64) { index -> index / 63f }

        val early = buildLevels(amplitudes, positionMs = 10_000L, phase = 0.3f)
        val late = buildLevels(amplitudes, positionMs = 90_000L, phase = 0.3f)

        assertNotEquals(early, late)
    }

    @Test
    fun playingOutputChangesWithAnimationPhase() {
        val amplitudes = List(64) { index -> (index % 11) / 10f }

        val first = buildLevels(amplitudes, phase = 0.1f, isPlaying = true)
        val second = buildLevels(amplitudes, phase = 0.6f, isPlaying = true)

        assertNotEquals(first, second)
    }

    @Test
    fun pausedOutputIsStableAcrossAnimationPhases() {
        val amplitudes = List(64) { index -> (index % 7) / 6f }

        val first = buildLevels(amplitudes, phase = 0.1f, isPlaying = false)
        val second = buildLevels(amplitudes, phase = 0.8f, isPlaying = false)

        assertEquals(first, second)
    }

    @Test
    fun invalidInputsReturnFallbackSignal() {
        assertNull(buildLevels(emptyList()))
        assertNull(buildLevels(listOf(0.5f), durationMs = 0L))
        assertNull(buildLevels(listOf(0.5f), columnCount = 0))
    }

    private fun buildLevels(
        amplitudes: List<Float>,
        positionMs: Long = 50_000L,
        durationMs: Long = 100_000L,
        columnCount: Int = 18,
        phase: Float = 0f,
        isPlaying: Boolean = true
    ) = buildTrackReactiveVisualizerLevels(
        amplitudes = amplitudes,
        currentPositionMs = positionMs,
        durationMs = durationMs,
        columnCount = columnCount,
        animationPhase = phase,
        isPlaying = isPlaying
    )
}
