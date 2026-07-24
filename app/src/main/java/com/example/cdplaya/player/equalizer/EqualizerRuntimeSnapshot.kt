package com.example.cdplaya.player.equalizer

import com.example.cdplaya.player.equalizer.dsp.EqualizerConfiguration

internal data class EqualizerRuntimeSnapshot(
    val version: Long,
    val configuration: EqualizerConfiguration,
    val automaticHeadroomEnabled: Boolean
) {
    init {
        require(version >= 0L) {
            "version must be non-negative"
        }
    }

    companion object {
        val DEFAULT = EqualizerRuntimeSnapshot(
            version = 0L,
            configuration = EqualizerConfiguration(
                enabled = false,
                preampDb = 0.0,
                filters = emptyList()
            ),
            automaticHeadroomEnabled = false
        )
    }
}

