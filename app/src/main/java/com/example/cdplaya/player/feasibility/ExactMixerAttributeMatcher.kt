package com.example.cdplaya.player.feasibility

data class ExactMixerAttributeMatch(
    val selected: MixerAttributeSnapshot? = null,
    val rejectionReason: FeasibilityRejectionReason =
        FeasibilityRejectionReason.NONE
) {
    val isMatch: Boolean
        get() = selected != null &&
            rejectionReason == FeasibilityRejectionReason.NONE
}
object ExactMixerAttributeMatcher {
    fun select(
        output: Media3OutputConfigSnapshot?,
        candidates: List<MixerAttributeSnapshot>
    ): ExactMixerAttributeMatch {
        val safeCandidates = candidates.toList()
        val encoding = output?.encoding
        val sampleRate = output?.sampleRateHz
        val channelMask = output?.channelMask
        if (encoding == null || sampleRate == null || channelMask == null) {
            return ExactMixerAttributeMatch(
                rejectionReason =
                    FeasibilityRejectionReason.OUTPUT_FORMAT_UNKNOWN
            )
        }
        if (output.offloadEnabled) {
            return ExactMixerAttributeMatch(
                rejectionReason = FeasibilityRejectionReason.OFFLOAD_OUTPUT
            )
        }
        if (output.tunnelingEnabled) {
            return ExactMixerAttributeMatch(
                rejectionReason = FeasibilityRejectionReason.TUNNELED_OUTPUT
            )
        }

        val bitPerfect = safeCandidates
            .withIndex()
            .filter {
                it.value.mixerBehavior ==
                    FeasibilityMixerBehavior.BIT_PERFECT
            }
        if (bitPerfect.isEmpty()) {
            return ExactMixerAttributeMatch(
                rejectionReason =
                    FeasibilityRejectionReason.NO_BIT_PERFECT_ATTRIBUTE
            )
        }
        val encodingMatches = bitPerfect.filter {
            it.value.encoding == encoding
        }
        if (encodingMatches.isEmpty()) {
            return ExactMixerAttributeMatch(
                rejectionReason = FeasibilityRejectionReason.ENCODING_MISMATCH
            )
        }
        val sampleRateMatches = encodingMatches.filter {
            it.value.sampleRateHz == sampleRate
        }
        if (sampleRateMatches.isEmpty()) {
            return ExactMixerAttributeMatch(
                rejectionReason =
                    FeasibilityRejectionReason.SAMPLE_RATE_MISMATCH
            )
        }
        val exactMatches = sampleRateMatches.filter {
            it.value.channelMask == channelMask
        }
        if (exactMatches.isEmpty()) {
            return ExactMixerAttributeMatch(
                rejectionReason =
                    FeasibilityRejectionReason.CHANNEL_MASK_MISMATCH
            )
        }

        // Platform order is retained. This is stable, explicit, and does not
        // introduce a "nearest" format policy.
        return ExactMixerAttributeMatch(selected = exactMatches.first().value)
    }
}
