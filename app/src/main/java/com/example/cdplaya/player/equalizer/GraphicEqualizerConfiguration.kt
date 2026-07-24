package com.example.cdplaya.player.equalizer

import com.example.cdplaya.player.equalizer.dsp.EqualizerConfiguration
import com.example.cdplaya.player.equalizer.dsp.EqualizerFilterSpec
import com.example.cdplaya.player.equalizer.dsp.GraphicEqualizerDefaults

internal fun EqualizerPreferencesState.toDspConfiguration(
    enabledOverride: Boolean = enabled
): EqualizerConfiguration {
    return EqualizerConfiguration(
        enabled = enabledOverride,
        preampDb = preampDb,
        filters = GraphicEqualizerDefaults.frequenciesHz.mapIndexed {
                index,
                frequencyHz ->
            EqualizerFilterSpec.Peaking(
                frequencyHz = frequencyHz,
                gainDb = bandGainsDb[index],
                q = GraphicEqualizerDefaults.Q
            )
        }
    )
}

internal fun EqualizerPreferencesState.applyPreset(
    preset: BuiltInEqualizerPreset
): EqualizerPreferencesState = withCurve(
    preampDb = preset.preampDb,
    automaticHeadroomEnabled =
        preset.automaticHeadroomEnabled,
    bandGainsDb = preset.bandGainsDb
)

internal fun EqualizerPreferencesState.applyPreset(
    preset: UserEqualizerPreset
): EqualizerPreferencesState = withCurve(
    preampDb = preset.preampDb,
    automaticHeadroomEnabled =
        preset.automaticHeadroomEnabled,
    bandGainsDb = preset.bandGainsDb
)
