package com.example.cdplaya.player.equalizer.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Designs normalized W3C/RBJ Audio EQ Cookbook biquads.
 *
 * Peaking sections use Q. Low- and high-shelf sections use the Cookbook shelf
 * slope S, whose valid range here is `(0, 1]`.
 */
internal object BiquadDesigner {

    fun design(
        filter: EqualizerFilterSpec,
        sampleRateHz: Int
    ): BiquadCoefficients {
        validateCommonParameters(
            frequencyHz = filter.frequencyHz,
            gainDb = filter.gainDb,
            sampleRateHz = sampleRateHz
        )

        return when (filter) {
            is EqualizerFilterSpec.Peaking -> designPeaking(
                frequencyHz = filter.frequencyHz,
                gainDb = filter.gainDb,
                q = filter.q,
                sampleRateHz = sampleRateHz
            )

            is EqualizerFilterSpec.LowShelf -> designLowShelf(
                frequencyHz = filter.frequencyHz,
                gainDb = filter.gainDb,
                slope = filter.slope,
                sampleRateHz = sampleRateHz
            )

            is EqualizerFilterSpec.HighShelf -> designHighShelf(
                frequencyHz = filter.frequencyHz,
                gainDb = filter.gainDb,
                slope = filter.slope,
                sampleRateHz = sampleRateHz
            )
        }
    }

    fun designPeaking(
        frequencyHz: Double,
        gainDb: Double,
        q: Double,
        sampleRateHz: Int
    ): BiquadCoefficients {
        validateCommonParameters(frequencyHz, gainDb, sampleRateHz)
        require(q.isFinite() && q > 0.0) {
            "q must be finite and greater than 0"
        }
        if (isEffectivelyZeroDb(gainDb)) return BiquadCoefficients.UNITY

        val terms = designTerms(frequencyHz, gainDb, sampleRateHz)
        val alpha = terms.sinOmega / (2.0 * q)
        return normalize(
            b0 = 1.0 + alpha * terms.amplitude,
            b1 = -2.0 * terms.cosOmega,
            b2 = 1.0 - alpha * terms.amplitude,
            a0 = 1.0 + alpha / terms.amplitude,
            a1 = -2.0 * terms.cosOmega,
            a2 = 1.0 - alpha / terms.amplitude
        )
    }

    fun designLowShelf(
        frequencyHz: Double,
        gainDb: Double,
        slope: Double,
        sampleRateHz: Int
    ): BiquadCoefficients {
        validateCommonParameters(frequencyHz, gainDb, sampleRateHz)
        validateShelfSlope(slope)
        if (isEffectivelyZeroDb(gainDb)) return BiquadCoefficients.UNITY

        val terms = designTerms(frequencyHz, gainDb, sampleRateHz)
        val alpha = shelfAlpha(
            amplitude = terms.amplitude,
            sinOmega = terms.sinOmega,
            slope = slope
        )
        val twoSquareRootAmplitudeAlpha =
            2.0 * sqrt(terms.amplitude) * alpha
        val amplitudePlusOne = terms.amplitude + 1.0
        val amplitudeMinusOne = terms.amplitude - 1.0

        return normalize(
            b0 = terms.amplitude * (
                amplitudePlusOne -
                    amplitudeMinusOne * terms.cosOmega +
                    twoSquareRootAmplitudeAlpha
                ),
            b1 = 2.0 * terms.amplitude * (
                amplitudeMinusOne -
                    amplitudePlusOne * terms.cosOmega
                ),
            b2 = terms.amplitude * (
                amplitudePlusOne -
                    amplitudeMinusOne * terms.cosOmega -
                    twoSquareRootAmplitudeAlpha
                ),
            a0 = amplitudePlusOne +
                amplitudeMinusOne * terms.cosOmega +
                twoSquareRootAmplitudeAlpha,
            a1 = -2.0 * (
                amplitudeMinusOne +
                    amplitudePlusOne * terms.cosOmega
                ),
            a2 = amplitudePlusOne +
                amplitudeMinusOne * terms.cosOmega -
                twoSquareRootAmplitudeAlpha
        )
    }

    fun designHighShelf(
        frequencyHz: Double,
        gainDb: Double,
        slope: Double,
        sampleRateHz: Int
    ): BiquadCoefficients {
        validateCommonParameters(frequencyHz, gainDb, sampleRateHz)
        validateShelfSlope(slope)
        if (isEffectivelyZeroDb(gainDb)) return BiquadCoefficients.UNITY

        val terms = designTerms(frequencyHz, gainDb, sampleRateHz)
        val alpha = shelfAlpha(
            amplitude = terms.amplitude,
            sinOmega = terms.sinOmega,
            slope = slope
        )
        val twoSquareRootAmplitudeAlpha =
            2.0 * sqrt(terms.amplitude) * alpha
        val amplitudePlusOne = terms.amplitude + 1.0
        val amplitudeMinusOne = terms.amplitude - 1.0

        return normalize(
            b0 = terms.amplitude * (
                amplitudePlusOne +
                    amplitudeMinusOne * terms.cosOmega +
                    twoSquareRootAmplitudeAlpha
                ),
            b1 = -2.0 * terms.amplitude * (
                amplitudeMinusOne +
                    amplitudePlusOne * terms.cosOmega
                ),
            b2 = terms.amplitude * (
                amplitudePlusOne +
                    amplitudeMinusOne * terms.cosOmega -
                    twoSquareRootAmplitudeAlpha
                ),
            a0 = amplitudePlusOne -
                amplitudeMinusOne * terms.cosOmega +
                twoSquareRootAmplitudeAlpha,
            a1 = 2.0 * (
                amplitudeMinusOne -
                    amplitudePlusOne * terms.cosOmega
                ),
            a2 = amplitudePlusOne -
                amplitudeMinusOne * terms.cosOmega -
                twoSquareRootAmplitudeAlpha
        )
    }

    private fun validateCommonParameters(
        frequencyHz: Double,
        gainDb: Double,
        sampleRateHz: Int
    ) {
        require(sampleRateHz > 0) {
            "sampleRateHz must be greater than 0"
        }
        require(
            isEqualizerFrequencySupported(
                frequencyHz = frequencyHz,
                sampleRateHz = sampleRateHz
            )
        ) {
            "frequencyHz must be finite, greater than 0, and below Nyquist"
        }
        require(gainDb.isFinite()) {
            "gainDb must be finite"
        }
    }

    private fun validateShelfSlope(slope: Double) {
        require(slope.isFinite() && slope > 0.0 && slope <= 1.0) {
            "slope must be finite and in the range (0, 1]"
        }
    }

    private fun designTerms(
        frequencyHz: Double,
        gainDb: Double,
        sampleRateHz: Int
    ): DesignTerms {
        val amplitude = 10.0.pow(gainDb / 40.0)
        require(amplitude.isFinite() && amplitude > 0.0) {
            "gainDb produces an invalid amplitude"
        }
        val omega = 2.0 * PI * frequencyHz / sampleRateHz
        return DesignTerms(
            amplitude = amplitude,
            sinOmega = sin(omega),
            cosOmega = cos(omega)
        )
    }

    private fun shelfAlpha(
        amplitude: Double,
        sinOmega: Double,
        slope: Double
    ): Double {
        return sinOmega / 2.0 * sqrt(
            (amplitude + 1.0 / amplitude) * (1.0 / slope - 1.0) + 2.0
        )
    }

    private fun normalize(
        b0: Double,
        b1: Double,
        b2: Double,
        a0: Double,
        a1: Double,
        a2: Double
    ): BiquadCoefficients {
        require(a0.isFinite() && a0 != 0.0) {
            "Designed a0 coefficient must be finite and non-zero"
        }
        return BiquadCoefficients(
            b0 = b0 / a0,
            b1 = b1 / a0,
            b2 = b2 / a0,
            a1 = a1 / a0,
            a2 = a2 / a0
        )
    }

    private data class DesignTerms(
        val amplitude: Double,
        val sinOmega: Double,
        val cosOmega: Double
    )
}
