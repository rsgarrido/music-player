package com.example.cdplaya.ui.equalizer

import com.example.cdplaya.player.equalizer.EqualizerPreferencesState
import com.example.cdplaya.player.equalizer.GraphicEqualizerPresets
import com.example.cdplaya.player.equalizer.applyPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerScreenStateTest {
    @Test
    fun settingsSummaryUsesOffPresetAndCustomTruthfully() {
        val off = EqualizerScreenState(
            editablePreferences = EqualizerPreferencesState()
        )
        val bass = EqualizerPreferencesState(enabled = true)
            .applyPreset(
                GraphicEqualizerPresets.builtIns[1]
            )
        val custom = bass.withBandGainDb(0, 3.0)

        assertEquals("Off", off.settingsSummary)
        assertEquals(
            "Bass Lift",
            EqualizerScreenState(
                editablePreferences = bass,
                presetMatch = presetMatchFor(bass)
            ).settingsSummary
        )
        assertEquals(
            "Custom",
            EqualizerScreenState(
                editablePreferences = custom,
                presetMatch = presetMatchFor(custom)
            ).settingsSummary
        )
    }

    @Test
    fun comparisonRequiresEnabledNonFlatCurve() {
        val offActive = EqualizerPreferencesState()
            .withBandGainDb(0, 4.0)
        val onFlat = EqualizerPreferencesState(enabled = true)
        val onActive = offActive.withEnabled(true)

        assertFalse(
            EqualizerScreenState(
                editablePreferences = offActive
            ).comparisonAvailable
        )
        assertFalse(
            EqualizerScreenState(
                editablePreferences = onFlat
            ).comparisonAvailable
        )
        assertTrue(
            EqualizerScreenState(
                editablePreferences = onActive
            ).comparisonAvailable
        )
    }
}
