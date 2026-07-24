package com.example.cdplaya.ui.equalizer

import com.example.cdplaya.player.equalizer.EqualizerPreferencesState
import com.example.cdplaya.player.equalizer.EqualizerPresetMatch
import com.example.cdplaya.player.equalizer.EqualizerRuntimeState
import com.example.cdplaya.player.equalizer.GraphicEqualizerPresets
import com.example.cdplaya.player.equalizer.UserEqualizerPreset
import com.example.cdplaya.player.equalizer.toDspConfiguration

internal data class EqualizerScreenState(
    val durablePreferences: EqualizerPreferencesState =
        EqualizerPreferencesState(),
    val editablePreferences: EqualizerPreferencesState =
        EqualizerPreferencesState(),
    val presetMatch: EqualizerPresetMatch =
        EqualizerPresetMatch("Flat"),
    val analysis: EqualizerAnalysisResult =
        EqualizerAnalysisResult(),
    val runtimeState: EqualizerRuntimeState =
        EqualizerRuntimeState(),
    val comparisonBypassed: Boolean = false,
    val hasUncommittedPreview: Boolean = false,
    val isLoaded: Boolean = false
) {
    val presetLabel: String
        get() = presetMatch.name

    val userPresets: List<UserEqualizerPreset>
        get() = editablePreferences.userPresets
            .sortedBy { preset -> preset.name.lowercase() }

    val comparisonAvailable: Boolean
        get() = editablePreferences.enabled &&
            !editablePreferences
                .toDspConfiguration(enabledOverride = true)
                .isEffectivelyFlat

    val statusText: String
        get() = when {
            !editablePreferences.enabled -> "Off · $presetLabel"
            editablePreferences
                .toDspConfiguration(enabledOverride = true)
                .isEffectivelyFlat -> "On · Flat"
            comparisonBypassed -> "B · Exact DSP bypass"
            else -> "A · Active · $presetLabel"
        }

    val settingsSummary: String
        get() = when {
            !editablePreferences.enabled -> "Off"
            presetLabel != "Custom" -> presetLabel
            analysis.automaticHeadroom.attenuationDb > 0.0 ->
                "Custom · Auto headroom " +
                    formatEqualizerDb(
                        analysis.automaticHeadroom.attenuationDb,
                        includePlus = false
                    )
            else -> "Custom"
        }
}

internal data class EqualizerUiActions(
    val onBack: () -> Unit,
    val onEnabledChanged: (Boolean) -> Unit,
    val onPreviewBandGain: (Int, Double) -> Unit,
    val onCommitBandGain: (Int, Double) -> Unit,
    val onCancelBandGainPreview: (Int, Double) -> Unit,
    val onPreviewPreamp: (Double) -> Unit,
    val onCommitPreamp: (Double) -> Unit,
    val onCancelPreampPreview: (Double) -> Unit,
    val onAutomaticHeadroomChanged: (Boolean) -> Unit,
    val onApplyBuiltInPreset: (Int) -> Unit,
    val onApplyUserPreset: (String) -> Unit,
    val onSaveUserPreset: (String) -> Unit,
    val onRenameUserPreset: (String, String) -> Unit,
    val onDeleteUserPreset: (String) -> Unit,
    val onResetToFlat: () -> Unit,
    val onComparisonBypassedChanged: (Boolean) -> Unit
)

internal fun presetMatchFor(
    state: EqualizerPreferencesState
): EqualizerPresetMatch {
    return com.example.cdplaya.player.equalizer
        .EqualizerPresetMatcher
        .match(state)
        ?: EqualizerPresetMatch("Custom")
}

internal val builtInEqualizerPresets
    get() = GraphicEqualizerPresets.builtIns
