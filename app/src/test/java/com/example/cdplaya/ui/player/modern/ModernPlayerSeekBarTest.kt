package com.example.cdplaya.ui.player.modern

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModernPlayerSeekBarTest {
    @Test
    fun waveformPreviewBars_sameSeedProducesStableBars() {
        val first = generateWaveformPreviewBars("42|music/track.flac|Example")
        val second = generateWaveformPreviewBars("42|music/track.flac|Example")

        assertEquals(first, second)
        assertEquals(48, first.size)
        assertTrue(first.all { amplitude -> amplitude in 0.24f..0.96f })
    }

    @Test
    fun waveformPreviewBars_differentSeedsProduceDifferentBars() {
        val first = generateWaveformPreviewBars("song-one")
        val second = generateWaveformPreviewBars("song-two")

        assertNotEquals(first, second)
    }

    @Test
    fun segmentedFillFractions_fillsWholeAndPartialSegments() {
        assertEquals(
            listOf(1f, 1f, 0.5f, 0f),
            segmentedFillFractions(progress = 0.625f, segmentCount = 4)
        )
    }

    @Test
    fun segmentedFillFractions_clampsProgressAndHandlesEmptyCount() {
        assertEquals(listOf(0f, 0f), segmentedFillFractions(-1f, 2))
        assertEquals(listOf(1f, 1f), segmentedFillFractions(2f, 2))
        assertTrue(segmentedFillFractions(0.5f, 0).isEmpty())
    }
}
