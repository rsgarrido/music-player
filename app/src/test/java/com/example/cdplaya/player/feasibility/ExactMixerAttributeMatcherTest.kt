package com.example.cdplaya.player.feasibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExactMixerAttributeMatcherTest {
    private val output = Media3OutputConfigSnapshot(
        encoding = 21,
        sampleRateHz = 96_000,
        channelMask = 12
    )
    private val exact = MixerAttributeSnapshot(
        encoding = 21,
        sampleRateHz = 96_000,
        channelMask = 12,
        mixerBehavior = FeasibilityMixerBehavior.BIT_PERFECT
    )

    @Test
    fun exactFormatSucceeds() {
        assertEquals(
            exact,
            ExactMixerAttributeMatcher.select(output, listOf(exact)).selected
        )
    }

    @Test
    fun nearestSampleRateFails() {
        val result = ExactMixerAttributeMatcher.select(
            output,
            listOf(exact.copy(sampleRateHz = 192_000))
        )
        assertEquals(
            FeasibilityRejectionReason.SAMPLE_RATE_MISMATCH,
            result.rejectionReason
        )
    }

    @Test
    fun encodingMismatchFails() {
        val result = ExactMixerAttributeMatcher.select(
            output,
            listOf(exact.copy(encoding = 22))
        )
        assertEquals(
            FeasibilityRejectionReason.ENCODING_MISMATCH,
            result.rejectionReason
        )
    }

    @Test
    fun channelMaskMismatchFails() {
        val result = ExactMixerAttributeMatcher.select(
            output,
            listOf(exact.copy(channelMask = 4))
        )
        assertEquals(
            FeasibilityRejectionReason.CHANNEL_MASK_MISMATCH,
            result.rejectionReason
        )
    }

    @Test
    fun defaultMixerBehaviorIsRejected() {
        val result = ExactMixerAttributeMatcher.select(
            output,
            listOf(
                exact.copy(
                    mixerBehavior = FeasibilityMixerBehavior.DEFAULT
                )
            )
        )
        assertEquals(
            FeasibilityRejectionReason.NO_BIT_PERFECT_ATTRIBUTE,
            result.rejectionReason
        )
    }

    @Test
    fun unknownOutputIsRejected() {
        val result = ExactMixerAttributeMatcher.select(
            output.copy(encoding = null),
            listOf(exact)
        )
        assertEquals(
            FeasibilityRejectionReason.OUTPUT_FORMAT_UNKNOWN,
            result.rejectionReason
        )
    }

    @Test
    fun packed24Pcm32AndFloatRemainDistinct() {
        val packed24 = exact.copy(encoding = 21)
        val pcm32 = exact.copy(encoding = 22)
        val float = exact.copy(encoding = 4)
        assertTrue(
            ExactMixerAttributeMatcher.select(
                output.copy(encoding = 21),
                listOf(packed24)
            ).isMatch
        )
        assertFalse(
            ExactMixerAttributeMatcher.select(
                output.copy(encoding = 21),
                listOf(pcm32, float)
            ).isMatch
        )
    }

    @Test
    fun multipleMatchesRetainDeterministicPlatformOrder() {
        val first = exact
        val second = exact.copy()
        assertEquals(
            first,
            ExactMixerAttributeMatcher.select(
                output,
                listOf(first, second)
            ).selected
        )
    }

    @Test
    fun callerMutationCannotChangeCompletedSelection() {
        val candidates = mutableListOf(exact)
        val result = ExactMixerAttributeMatcher.select(output, candidates)
        candidates.clear()
        assertEquals(exact, result.selected)
        assertNull(result.rejectionReason.takeUnless {
            it == FeasibilityRejectionReason.NONE
        })
    }
}
