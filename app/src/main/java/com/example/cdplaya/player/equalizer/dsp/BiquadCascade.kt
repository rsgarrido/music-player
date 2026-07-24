package com.example.cdplaya.player.equalizer.dsp

/**
 * Direct Form I cascade with independent history for every channel and section.
 *
 * Coefficients and the four state values x1, x2, y1, and y2 are stored in
 * primitive arrays. State index `channel * sectionCount + section` identifies
 * one channel/section pair.
 */
internal class BiquadCascade(
    coefficients: List<BiquadCoefficients>,
    private val channelCount: Int
) {
    private val sectionCount = coefficients.size
    private val b0 = DoubleArray(sectionCount)
    private val b1 = DoubleArray(sectionCount)
    private val b2 = DoubleArray(sectionCount)
    private val a1 = DoubleArray(sectionCount)
    private val a2 = DoubleArray(sectionCount)

    private val x1: DoubleArray
    private val x2: DoubleArray
    private val y1: DoubleArray
    private val y2: DoubleArray

    init {
        require(channelCount > 0) {
            "channelCount must be greater than 0"
        }
        val stateValueCount = channelCount.toLong() * sectionCount
        require(stateValueCount <= Int.MAX_VALUE) {
            "channel and section count require too much filter state"
        }

        coefficients.forEachIndexed { sectionIndex, coefficient ->
            b0[sectionIndex] = coefficient.b0
            b1[sectionIndex] = coefficient.b1
            b2[sectionIndex] = coefficient.b2
            a1[sectionIndex] = coefficient.a1
            a2[sectionIndex] = coefficient.a2
        }

        val stateSize = stateValueCount.toInt()
        x1 = DoubleArray(stateSize)
        x2 = DoubleArray(stateSize)
        y1 = DoubleArray(stateSize)
        y2 = DoubleArray(stateSize)
    }

    fun processSample(
        inputSample: Double,
        channelIndex: Int
    ): Double {
        var sectionInput = inputSample
        var sectionIndex = 0
        var stateIndex = channelIndex * sectionCount

        while (sectionIndex < sectionCount) {
            val sectionOutput =
                b0[sectionIndex] * sectionInput +
                    b1[sectionIndex] * x1[stateIndex] +
                    b2[sectionIndex] * x2[stateIndex] -
                    a1[sectionIndex] * y1[stateIndex] -
                    a2[sectionIndex] * y2[stateIndex]

            x2[stateIndex] = x1[stateIndex]
            x1[stateIndex] = sectionInput
            y2[stateIndex] = y1[stateIndex]
            y1[stateIndex] = sectionOutput

            sectionInput = sectionOutput
            sectionIndex++
            stateIndex++
        }

        return sectionInput
    }

    fun reset() {
        x1.fill(0.0)
        x2.fill(0.0)
        y1.fill(0.0)
        y2.fill(0.0)
    }
}

