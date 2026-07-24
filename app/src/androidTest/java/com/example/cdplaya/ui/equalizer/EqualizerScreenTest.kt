package com.example.cdplaya.ui.equalizer

import androidx.compose.material3.MaterialTheme
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.cdplaya.player.equalizer.EqualizerPreferencesState
import com.example.cdplaya.player.equalizer.EqualizerRuntimeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EqualizerScreenTest {
    @get:Rule
    val composeRule =
        createAndroidComposeRule<ComponentActivity>()

    @Test
    fun screenRendersAllBandsAndProductionSemantics() {
        composeRule.setContent {
            MaterialTheme {
                EqualizerScreen(
                    state = EqualizerScreenState(
                        editablePreferences =
                            EqualizerPreferencesState(
                                enabled = true
                            ),
                        isLoaded = true
                    ),
                    actions = noOpActions()
                )
            }
        }

        listOf(
            "31 Hz",
            "62 Hz",
            "125 Hz",
            "250 Hz",
            "500 Hz",
            "1 kHz",
            "2 kHz",
            "4 kHz",
            "8 kHz",
            "16 kHz"
        ).forEach { label ->
            composeRule.onNodeWithText(
                label,
                useUnmergedTree = true
            ).assertExists()
        }
        composeRule.onNode(
            hasContentDescription(
                "Equalizer response graph",
                substring = true
            )
        ).assertExists()
        composeRule.onNodeWithText(
            "Developer equalizer verification"
        ).assertDoesNotExist()
        composeRule.onNodeWithText("Bass test")
            .assertDoesNotExist()
        composeRule.onNodeWithText("Sample-peak limiter")
            .assertExists()
    }

    @Test
    fun presetSelectorInvokesBuiltInPresetAction() {
        var appliedIndex = -1
        composeRule.setContent {
            MaterialTheme {
                EqualizerScreen(
                    state = EqualizerScreenState(
                        isLoaded = true
                    ),
                    actions = noOpActions().copy(
                        onApplyBuiltInPreset = { index ->
                            appliedIndex = index
                        }
                    )
                )
            }
        }

        composeRule.onNodeWithText(
            "Choose or manage presets"
        ).performClick()
        composeRule.onNodeWithText("Bass Lift")
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, appliedIndex)
        }
    }

    @Test
    fun enabledLimiterShowsMetersDisablesAbAndResetsCounters() {
        var resetRequested = false
        composeRule.setContent {
            MaterialTheme {
                EqualizerScreen(
                    state = EqualizerScreenState(
                        editablePreferences =
                            EqualizerPreferencesState(
                                enabled = true,
                                limiterEnabled = true
                            ).withBandGainDb(0, 4.0),
                        runtimeState = EqualizerRuntimeState(
                            limiterEffectivelyActive = true,
                            limiterPrimed = true,
                            preLimiterPeakDbfs = 0.7,
                            postLimiterPeakDbfs = -1.0,
                            currentGainReductionDb = 2.4,
                            maximumRecentGainReductionDb = 3.1,
                            overRangeSampleCount = 8,
                            saturatedSampleCount = 2
                        ),
                        isLoaded = true
                    ),
                    actions = noOpActions().copy(
                        onResetLimiterMeters = {
                            resetRequested = true
                        }
                    )
                )
            }
        }

        composeRule.onNodeWithText(
            "Disable the limiter for exact A/B comparison."
        ).assertExists()
        composeRule.onNode(
            hasContentDescription(
                "Pre-limiter peak",
                substring = true
            )
        ).assertExists()
        composeRule.onNode(
            hasContentDescription(
                "Gain reduction",
                substring = true
            )
        ).assertExists()
        composeRule.onNode(
            hasContentDescription(
                "Reset limiter meters and counters"
            )
        )
            .performScrollTo()
            .performClick()
        composeRule.runOnIdle {
            assertTrue(resetRequested)
        }
    }

    private fun noOpActions() = EqualizerUiActions(
        onBack = {},
        onEnabledChanged = {},
        onPreviewBandGain = { _, _ -> },
        onCommitBandGain = { _, _ -> },
        onCancelBandGainPreview = { _, _ -> },
        onPreviewPreamp = {},
        onCommitPreamp = {},
        onCancelPreampPreview = {},
        onAutomaticHeadroomChanged = {},
        onLimiterEnabledChanged = {},
        onPreviewLimiterCeiling = {},
        onCommitLimiterCeiling = {},
        onCancelLimiterCeilingPreview = {},
        onResetLimiterMeters = {},
        onApplyBuiltInPreset = {},
        onApplyUserPreset = {},
        onSaveUserPreset = {},
        onRenameUserPreset = { _, _ -> },
        onDeleteUserPreset = {},
        onResetToFlat = {},
        onComparisonBypassedChanged = {}
    )
}
