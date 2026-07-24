package com.example.cdplaya.player.equalizer.dsp

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min

internal data class AutomaticHeadroomResult(
    val maximumPredictedDb: Double,
    val attenuationDb: Double,
    val effectivePreampDb: Double
)

/**
 * Non-real-time full-cascade headroom analysis over a logarithmic frequency
 * grid and every valid active filter center.
 */
internal object AutomaticHeadroomCalculator {
    const val DEFAULT_SAFETY_MARGIN_DB = 0.5

    private const val GRID_POINT_COUNT = 2_048
    private const val LOWEST_FREQUENCY_HZ = 20.0
    private const val HIGHEST_FREQUENCY_HZ = 20_000.0
    private const val BELOW_NYQUIST_SCALE = 1.0 - 1e-12

    fun calculate(
        configuration: EqualizerConfiguration,
        sampleRateHz: Int,
        safetyMarginDb: Double = DEFAULT_SAFETY_MARGIN_DB
    ): AutomaticHeadroomResult {
        require(sampleRateHz > 0) {
            "sampleRateHz must be greater than 0"
        }
        require(safetyMarginDb.isFinite() && safetyMarginDb >= 0.0) {
            "safetyMarginDb must be finite and non-negative"
        }

        val nyquistHz = sampleRateHz / 2.0
        require(nyquistHz > LOWEST_FREQUENCY_HZ) {
            "sampleRateHz is too low for headroom analysis"
        }
        val highestFrequencyHz = min(
            HIGHEST_FREQUENCY_HZ,
            nyquistHz * BELOW_NYQUIST_SCALE
        )
        require(highestFrequencyHz > LOWEST_FREQUENCY_HZ) {
            "sampleRateHz does not provide a valid analysis range"
        }

        val frequenciesHz = buildFrequencyGrid(
            configuration = configuration,
            sampleRateHz = sampleRateHz,
            highestFrequencyHz = highestFrequencyHz
        )
        val response = EqualizerFrequencyResponse.calculate(
            configuration = configuration,
            sampleRateHz = sampleRateHz,
            frequenciesHz = frequenciesHz
        )
        val maximumPredictedDb = response.maxOf { point ->
            point.magnitudeDb
        }
        require(maximumPredictedDb.isFinite()) {
            "maximum predicted response must be finite"
        }

        val attenuationDb = if (
            maximumPredictedDb > EQUALIZER_DB_EPSILON
        ) {
            maximumPredictedDb + safetyMarginDb
        } else {
            0.0
        }
        val effectivePreampDb =
            configuration.preampDb - attenuationDb
        require(
            attenuationDb.isFinite() &&
                effectivePreampDb.isFinite()
        ) {
            "headroom result must be finite"
        }

        return AutomaticHeadroomResult(
            maximumPredictedDb = maximumPredictedDb,
            attenuationDb = attenuationDb,
            effectivePreampDb = effectivePreampDb
        )
    }

    private fun buildFrequencyGrid(
        configuration: EqualizerConfiguration,
        sampleRateHz: Int,
        highestFrequencyHz: Double
    ): DoubleArray {
        val frequencies = ArrayList<Double>(
            GRID_POINT_COUNT + configuration.filters.size
        )
        val logarithmicRange =
            ln(highestFrequencyHz / LOWEST_FREQUENCY_HZ)
        repeat(GRID_POINT_COUNT) { index ->
            val fraction = index.toDouble() / (GRID_POINT_COUNT - 1)
            frequencies += LOWEST_FREQUENCY_HZ *
                exp(logarithmicRange * fraction)
        }

        val nyquistHz = sampleRateHz / 2.0
        configuration.filters.forEach { filter ->
            if (
                filter.enabled &&
                !isEffectivelyZeroDb(filter.gainDb) &&
                filter.frequencyHz.isFinite() &&
                filter.frequencyHz > 0.0 &&
                filter.frequencyHz < nyquistHz
            ) {
                frequencies += filter.frequencyHz
            }
        }

        return frequencies
            .distinct()
            .sorted()
            .toDoubleArray()
    }
}
