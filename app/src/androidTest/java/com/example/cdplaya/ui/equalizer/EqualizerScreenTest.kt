package com.example.cdplaya.ui.equalizer

import androidx.compose.material3.MaterialTheme
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.cdplaya.player.equalizer.EqualizerPreferencesState
import com.example.cdplaya.player.equalizer.EqualizerMode
import com.example.cdplaya.player.equalizer.EqualizerRuntimeState
import com.example.cdplaya.player.equalizer.parametric.ParametricEqualizerState
import com.example.cdplaya.player.equalizer.parametric.ParametricFilter
import com.example.cdplaya.player.equalizer.parametric.ParametricFilterFactory
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
    fun parametricModeRendersAccessibleFiltersMarkersAndFocusedEditor() {
        val peak = ParametricFilter.Peaking(
            id = "peak",
            enabled = true,
            frequencyHz = 1_000.0,
            gainDb = 4.0,
            q = 1.25
        )
        composeRule.setContent {
            MaterialTheme {
                EqualizerScreen(
                    state = EqualizerScreenState(
                        editablePreferences =
                            EqualizerPreferencesState(
                                enabled = true,
                                mode = EqualizerMode.PARAMETRIC,
                                parametricState =
                                    ParametricEqualizerState(
                                        filters = listOf(peak)
                                    )
                            ),
                        selectedParametricFilterId = peak.id,
                        isLoaded = true
                    ),
                    actions = noOpActions()
                )
            }
        }

        composeRule.onNodeWithText("Add Filter").assertExists()
        composeRule.onNode(
            hasContentDescription(
                "Filter 1, peaking, enabled",
                substring = true
            )
        ).assertExists()
        composeRule.onNode(
            hasContentDescription(
                "Filter marker 1",
                substring = true
            )
        ).assertExists()
        composeRule.onNode(
            hasContentDescription("Edit filter 1")
        ).performScrollTo().performClick()
        composeRule.onNodeWithText("Frequency (Hz)")
            .assertExists()
        composeRule.onNodeWithText("Gain (dB)")
            .assertExists()
        composeRule.onNodeWithText("Q").assertExists()
        composeRule.onNodeWithText("Shelf slope S")
            .assertDoesNotExist()
    }

    @Test
    fun maximumTenParametricFiltersDisablesAddAction() {
        composeRule.setContent {
            MaterialTheme {
                EqualizerScreen(
                    state = EqualizerScreenState(
                        editablePreferences =
                            EqualizerPreferencesState(
                                mode = EqualizerMode.PARAMETRIC,
                                parametricState =
                                    ParametricEqualizerState(
                                        filters = List(10) { index ->
                                            ParametricFilterFactory.default(
                                                id = "filter-$index"
                                            )
                                        }
                                    )
                            ),
                        isLoaded = true
                    ),
                    actions = noOpActions()
                )
            }
        }

        composeRule.onNodeWithText("Add Filter")
            .assertIsNotEnabled()
        composeRule.onNodeWithText(
            "Maximum of ten filters reached."
        ).assertExists()
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
        onModeChanged = {},
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
        onSelectParametricFilter = {},
        onAddParametricFilter = {},
        onPreviewParametricFilter = {},
        onCommitParametricFilter = {},
        onCancelParametricFilterPreview = {},
        onMoveParametricFilter = { _, _ -> },
        onDeleteParametricFilter = {},
        onApplyParametricFlatPreset = {},
        onApplyParametricUserPreset = {},
        onSaveParametricUserPreset = {},
        onRenameParametricUserPreset = { _, _ -> },
        onDeleteParametricUserPreset = {},
        onResetToFlat = {},
        onComparisonBypassedChanged = {}
    )
}
