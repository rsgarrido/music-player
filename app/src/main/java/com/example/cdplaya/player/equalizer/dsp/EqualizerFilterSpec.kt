package com.example.cdplaya.player.equalizer.dsp

/**
 * Immutable parameters for one equalizer section.
 *
 * Peaking filters use quality factor [Peaking.q]. Shelf filters use the RBJ
 * shelf-slope parameter [LowShelf.slope] or [HighShelf.slope].
 */
internal sealed interface EqualizerFilterSpec {
    val frequencyHz: Double
    val gainDb: Double
    val enabled: Boolean

    data class Peaking(
        override val frequencyHz: Double,
        override val gainDb: Double,
        val q: Double,
        override val enabled: Boolean = true
    ) : EqualizerFilterSpec

    data class LowShelf(
        override val frequencyHz: Double,
        override val gainDb: Double,
        val slope: Double = 1.0,
        override val enabled: Boolean = true
    ) : EqualizerFilterSpec

    data class HighShelf(
        override val frequencyHz: Double,
        override val gainDb: Double,
        val slope: Double = 1.0,
        override val enabled: Boolean = true
    ) : EqualizerFilterSpec
}
