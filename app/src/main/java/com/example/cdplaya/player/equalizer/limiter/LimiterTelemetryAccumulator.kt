package com.example.cdplaya.player.equalizer.limiter

import kotlin.math.abs
import kotlin.math.max

/**
 * Audio-thread-owned accumulator. Counts are updated as ordinary fields and
 * are published as one immutable snapshot per processor call.
 */
internal class LimiterTelemetryAccumulator {
    private var callPrePeak = 0.0
    private var callPostPeak = 0.0
    private var callMaximumReductionDb = 0.0
    private var currentGainReductionDb = 0.0
    private var overRangeSampleCount = 0L
    private var saturatedSampleCount = 0L
    private var limiterActiveFrameCount = 0L
    private var limiterReducedFrameCount = 0L

    fun beginProcessingCall() {
        callPrePeak = 0.0
        callPostPeak = 0.0
        callMaximumReductionDb = 0.0
    }

    fun observePreLimiterSample(sample: Float) {
        val magnitude = abs(sample.toDouble())
        callPrePeak = max(callPrePeak, magnitude)
        if (magnitude > 1.0) {
            overRangeSampleCount++
        }
    }

    fun observePostLimiterSample(sample: Float) {
        val magnitude = abs(sample.toDouble())
        callPostPeak = max(callPostPeak, magnitude)
    }

    fun observeLimiterFrame(linearGain: Double) {
        val reductionDb = LimiterMath.gainReductionDb(linearGain)
        limiterActiveFrameCount++
        if (reductionDb > REDUCTION_EPSILON_DB) {
            limiterReducedFrameCount++
        }
        currentGainReductionDb = reductionDb
        callMaximumReductionDb =
            max(callMaximumReductionDb, reductionDb)
    }

    fun observeLimiterInactive() {
        currentGainReductionDb = 0.0
    }

    fun observeSaturatedSample() {
        saturatedSampleCount++
    }

    fun snapshot(): LimiterMeterSnapshot = LimiterMeterSnapshot(
        preLimiterPeakDbfs =
            LimiterMath.linearToDbfs(callPrePeak),
        postLimiterPeakDbfs =
            LimiterMath.linearToDbfs(callPostPeak),
        currentGainReductionDb = currentGainReductionDb,
        maximumGainReductionDb = callMaximumReductionDb,
        overRangeSampleCount = overRangeSampleCount,
        saturatedSampleCount = saturatedSampleCount,
        limiterActiveFrameCount = limiterActiveFrameCount,
        limiterReducedFrameCount = limiterReducedFrameCount
    )

    fun reset() {
        callPrePeak = 0.0
        callPostPeak = 0.0
        callMaximumReductionDb = 0.0
        currentGainReductionDb = 0.0
        overRangeSampleCount = 0L
        saturatedSampleCount = 0L
        limiterActiveFrameCount = 0L
        limiterReducedFrameCount = 0L
    }

    private companion object {
        const val REDUCTION_EPSILON_DB = 1.0e-6
    }
}
