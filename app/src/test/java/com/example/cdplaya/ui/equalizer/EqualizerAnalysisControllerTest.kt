package com.example.cdplaya.ui.equalizer

import com.example.cdplaya.player.equalizer.EqualizerPreferencesState
import com.example.cdplaya.player.equalizer.GraphicEqualizerPresets
import com.example.cdplaya.player.equalizer.applyPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerAnalysisControllerTest {
    @Test
    fun flatResponseUsesPhaseACalculationAndStaysAtZero() {
        val result = calculate(
            EqualizerPreferencesState(enabled = true),
            48_000
        )

        assertEquals(160, result.filterResponse.size)
        assertTrue(
            result.filterResponse.all { point ->
                kotlin.math.abs(point.magnitudeDb) < 1e-9
            }
        )
        assertTrue(
            result.effectiveResponse.all { point ->
                kotlin.math.abs(point.magnitudeDb) < 1e-9
            }
        )
        assertEquals(0.0, result.predictedMaximumDb, 1e-9)
        assertEquals(
            0.0,
            result.automaticHeadroom.attenuationDb,
            1e-9
        )
    }

    @Test
    fun bassAndTreblePresetsRaiseTheirIntendedRegions() {
        val bass = calculate(
            EqualizerPreferencesState()
                .applyPreset(
                    GraphicEqualizerPresets.builtIns[1]
                ),
            48_000
        )
        val treble = calculate(
            EqualizerPreferencesState()
                .applyPreset(
                    GraphicEqualizerPresets.builtIns[2]
                ),
            48_000
        )

        assertTrue(
            bass.filterResponse.nearest(62.0).magnitudeDb >
                bass.filterResponse.nearest(8_000.0).magnitudeDb
        )
        assertTrue(
            treble.filterResponse.nearest(8_000.0).magnitudeDb >
                treble.filterResponse.nearest(62.0).magnitudeDb
        )
        assertTrue(bass.predictedMaximumDb > 0.0)
        assertTrue(
            bass.effectiveResponse.maxOf { it.magnitudeDb } <
                0.0
        )
    }

    @Test
    fun currentNyquistDeterminesIgnoredBandsWithoutLosingValues() {
        val state = EqualizerPreferencesState()
            .withBandGainDb(9, 7.0)
        val lowRate = calculate(state, 32_000)
        val standardRate = calculate(state, 44_100)

        assertTrue(9 in lowRate.ignoredBandIndices)
        assertFalse(9 in standardRate.ignoredBandIndices)
        assertEquals(7.0, state.bandGainsDb[9], 0.0)
        assertTrue(
            lowRate.effectiveResponse.last().frequencyHz <
                16_000.0
        )
        assertTrue(
            standardRate.effectiveResponse.last().frequencyHz <
                22_050.0
        )
    }

    @Test
    fun disablingHeadroomLeavesUserPreampUnattenuated() {
        val state = EqualizerPreferencesState(
            automaticHeadroomEnabled = false
        ).withBandGainDb(4, 8.0)
        val result = calculate(state, 48_000)

        assertTrue(result.predictedMaximumDb > 0.0)
        assertEquals(
            0.0,
            result.automaticHeadroom.attenuationDb,
            0.0
        )
        assertEquals(
            state.preampDb,
            result.automaticHeadroom.effectivePreampDb,
            0.0
        )
    }

    private fun calculate(
        state: EqualizerPreferencesState,
        sampleRateHz: Int
    ): EqualizerAnalysisResult {
        return EqualizerAnalysisCalculator.calculate(
            EqualizerAnalysisRequest(state, sampleRateHz)
        )
    }
}

private fun List<
    com.example.cdplaya.player.equalizer.dsp.EqualizerResponsePoint
>.nearest(frequencyHz: Double) = minBy { point ->
    kotlin.math.abs(point.frequencyHz - frequencyHz)
}
