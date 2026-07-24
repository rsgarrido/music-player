package com.example.cdplaya.player.equalizer

import com.example.cdplaya.player.equalizer.dsp.EqualizerConfiguration
import com.example.cdplaya.player.equalizer.dsp.EqualizerFilterSpec

internal object DebugEqualizerConfigurations {
    fun requestBypass() {
        EqualizerRuntimeBridge.requestConfiguration(
            configuration = disabledConfiguration(),
            automaticHeadroomEnabled = false
        )
    }

    fun requestBassTest() {
        EqualizerRuntimeBridge.requestConfiguration(
            configuration = EqualizerConfiguration(
                enabled = true,
                preampDb = 0.0,
                filters = listOf(
                    EqualizerFilterSpec.Peaking(
                        frequencyHz = 125.0,
                        gainDb = 6.0,
                        q = 1.41
                    )
                )
            ),
            automaticHeadroomEnabled = true
        )
    }

    fun requestTrebleTest() {
        EqualizerRuntimeBridge.requestConfiguration(
            configuration = EqualizerConfiguration(
                enabled = true,
                preampDb = 0.0,
                filters = listOf(
                    EqualizerFilterSpec.Peaking(
                        frequencyHz = 8_000.0,
                        gainDb = 6.0,
                        q = 1.41
                    )
                )
            ),
            automaticHeadroomEnabled = true
        )
    }

    fun requestPreampTest() {
        EqualizerRuntimeBridge.requestConfiguration(
            configuration = EqualizerConfiguration(
                enabled = true,
                preampDb = -6.0,
                filters = emptyList()
            ),
            automaticHeadroomEnabled = false
        )
    }

    fun reset() {
        requestBypass()
    }

    private fun disabledConfiguration(): EqualizerConfiguration {
        return EqualizerConfiguration(
            enabled = false,
            preampDb = 0.0,
            filters = emptyList()
        )
    }
}
