package com.example.cdplaya.player.equalizer.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomaticHeadroomCalculatorTest {
    @Test
    fun flatZeroDbRequiresNoAttenuation() {
        val result = AutomaticHeadroomCalculator.calculate(
            flatConfiguration(),
            sampleRateHz = 48_000
        )

        assertEquals(0.0, result.maximumPredictedDb, 0.0)
        assertEquals(0.0, result.attenuationDb, 0.0)
        assertEquals(0.0, result.effectivePreampDb, 0.0)
    }

    @Test
    fun negativeOnlyConfigurationRequiresNoAttenuation() {
        val configuration = EqualizerConfiguration(
            enabled = true,
            preampDb = -2.0,
            filters = listOf(
                EqualizerFilterSpec.Peaking(1_000.0, -6.0, 1.0),
                EqualizerFilterSpec.HighShelf(6_000.0, -3.0, 1.0)
            )
        )

        val result = AutomaticHeadroomCalculator.calculate(
            configuration,
            sampleRateHz = 48_000
        )

        assertTrue(result.maximumPredictedDb <= 0.0)
        assertEquals(0.0, result.attenuationDb, 0.0)
        assertEquals(-2.0, result.effectivePreampDb, 0.0)
    }

    @Test
    fun positivePreampIsIncludedAndReceivesSafetyMargin() {
        val configuration = EqualizerConfiguration(
            enabled = true,
            preampDb = 3.0,
            filters = emptyList()
        )

        val result = AutomaticHeadroomCalculator.calculate(
            configuration,
            sampleRateHz = 48_000
        )

        assertEquals(3.0, result.maximumPredictedDb, COEFFICIENT_TOLERANCE)
        assertEquals(3.5, result.attenuationDb, COEFFICIENT_TOLERANCE)
        assertEquals(-0.5, result.effectivePreampDb, COEFFICIENT_TOLERANCE)
    }

    @Test
    fun boostedBandProducesAttenuation() {
        val result = AutomaticHeadroomCalculator.calculate(
            EqualizerConfiguration(
                enabled = true,
                preampDb = 0.0,
                filters = listOf(
                    EqualizerFilterSpec.Peaking(1_000.0, 6.0, 1.41)
                )
            ),
            sampleRateHz = 48_000
        )

        assertEquals(6.0, result.maximumPredictedDb, RESPONSE_DB_TOLERANCE)
        assertEquals(6.5, result.attenuationDb, RESPONSE_DB_TOLERANCE)
    }

    @Test
    fun overlappingAdjacentBoostsExceedLargestIndividualGain() {
        val configuration = EqualizerConfiguration(
            enabled = true,
            preampDb = 0.0,
            filters = listOf(
                EqualizerFilterSpec.Peaking(1_000.0, 6.0, 1.41),
                EqualizerFilterSpec.Peaking(1_250.0, 6.0, 1.41)
            )
        )

        val result = AutomaticHeadroomCalculator.calculate(
            configuration,
            sampleRateHz = 48_000
        )

        assertTrue(result.maximumPredictedDb > 6.0)
        assertTrue(result.attenuationDb > 6.5)
    }

    @Test
    fun negativeUserPreampCanEliminateRequiredAttenuation() {
        val configuration = EqualizerConfiguration(
            enabled = true,
            preampDb = -12.0,
            filters = listOf(
                EqualizerFilterSpec.Peaking(1_000.0, 6.0, 1.0)
            )
        )

        val result = AutomaticHeadroomCalculator.calculate(
            configuration,
            sampleRateHz = 48_000
        )

        assertTrue(result.maximumPredictedDb < 0.0)
        assertEquals(0.0, result.attenuationDb, 0.0)
        assertEquals(-12.0, result.effectivePreampDb, 0.0)
    }

    @Test
    fun safetyMarginIsAppliedOnlyToPositivePredictedResponse() {
        val nonPositive = AutomaticHeadroomCalculator.calculate(
            EqualizerConfiguration(true, -1.0, emptyList()),
            sampleRateHz = 48_000,
            safetyMarginDb = 0.75
        )
        val positive = AutomaticHeadroomCalculator.calculate(
            EqualizerConfiguration(true, 1.0, emptyList()),
            sampleRateHz = 48_000,
            safetyMarginDb = 0.75
        )

        assertEquals(0.0, nonPositive.attenuationDb, 0.0)
        assertEquals(1.75, positive.attenuationDb, COEFFICIENT_TOLERANCE)
    }

    @Test
    fun applyingResultLeavesAnalyzedPeakAtNegativeSafetyMargin() {
        val safetyMarginDb = 0.7
        val result = AutomaticHeadroomCalculator.calculate(
            EqualizerConfiguration(
                enabled = true,
                preampDb = 2.0,
                filters = listOf(
                    EqualizerFilterSpec.LowShelf(150.0, 5.0, 0.8),
                    EqualizerFilterSpec.Peaking(900.0, 7.0, 2.0),
                    EqualizerFilterSpec.Peaking(1_200.0, 4.0, 1.0)
                )
            ),
            sampleRateHz = 48_000,
            safetyMarginDb = safetyMarginDb
        )

        assertEquals(
            -safetyMarginDb,
            result.maximumPredictedDb - result.attenuationDb,
            COEFFICIENT_TOLERANCE
        )
        assertTrue(
            result.maximumPredictedDb - result.attenuationDb <=
                -safetyMarginDb + COEFFICIENT_TOLERANCE
        )
    }

    @Test
    fun resultsAreFiniteAcrossSampleRateMatrix() {
        val configuration = EqualizerConfiguration(
            enabled = true,
            preampDb = 1.0,
            filters = listOf(
                EqualizerFilterSpec.Peaking(1_000.0, 6.0, 1.41)
            )
        )

        TEST_SAMPLE_RATES.forEach { sampleRateHz ->
            val result = AutomaticHeadroomCalculator.calculate(
                configuration,
                sampleRateHz
            )
            assertTrue(
                listOf(
                    result.maximumPredictedDb,
                    result.attenuationDb,
                    result.effectivePreampDb
                ).all(Double::isFinite)
            )
        }
    }

    @Test
    fun invalidSafetyMarginAndSampleRateAreRejected() {
        listOf(
            -0.1,
            Double.NaN,
            Double.POSITIVE_INFINITY
        ).forEach { safetyMarginDb ->
            assertThrows(IllegalArgumentException::class.java) {
                AutomaticHeadroomCalculator.calculate(
                    flatConfiguration(),
                    sampleRateHz = 48_000,
                    safetyMarginDb = safetyMarginDb
                )
            }
        }
        listOf(0, 40).forEach { sampleRateHz ->
            assertThrows(IllegalArgumentException::class.java) {
                AutomaticHeadroomCalculator.calculate(
                    flatConfiguration(),
                    sampleRateHz
                )
            }
        }
    }
}
