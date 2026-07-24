package com.example.cdplaya.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.player.audio.AudioOffloadPreference
import com.example.cdplaya.player.replaygain.ReplayGainMode
import com.example.cdplaya.ui.player.modern.ModernArtworkTransitionStyle
import com.example.cdplaya.ui.player.modern.ModernSeekbarStyle
import com.example.cdplaya.ui.player.theme.PlayerThemeTokens
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EqualizerSettingsNavigationTest {
    @get:Rule
    val composeRule =
        createAndroidComposeRule<ComponentActivity>()

    @Test
    fun playbackSettingsShowsSummaryAndOpensEqualizer() {
        var opened = false
        composeRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    totalSongCount = 1,
                    availableFolderCount = 1,
                    selectedFolderCount = 0,
                    onBackClick = {},
                    onLibraryFoldersClick = {},
                    onExportBackupClick = {},
                    onRestoreBackupClick = {},
                    onDiagnosticsClick = {},
                    equalizerSummary = "Bass Lift",
                    onEqualizerClick = { opened = true },
                    isSleepTimerActive = false,
                    sleepTimerDisplayText = "",
                    onSleepTimerClick = {},
                    selectedPlayerTheme = PlayerTheme.DEFAULT,
                    selectedPlayerThemeTokens =
                        PlayerThemeTokens(
                            shellColor = Color.Black,
                            accentColor = Color.Blue,
                            displayBackgroundColor =
                                Color.Black,
                            displayTextColor = Color.White
                        ),
                    onPlayerThemeSelected = {},
                    onUpdatePlayerThemeTokenOverride =
                        { _, _, _ -> },
                    onResetPlayerThemeTokenOverrides = {},
                    selectedModernArtworkTransitionStyle =
                        ModernArtworkTransitionStyle.SLIDE,
                    onModernArtworkTransitionStyleSelected = {},
                    selectedModernSeekbarStyle =
                        ModernSeekbarStyle.CLASSIC_BAR,
                    onModernSeekbarStyleSelected = {},
                    selectedReplayGainMode =
                        ReplayGainMode.OFF,
                    onReplayGainModeSelected = {},
                    selectedAudioOffloadPreference =
                        AudioOffloadPreference.DISABLED,
                    onAudioOffloadPreferenceSelected = {}
                )
            }
        }

        composeRule.onNodeWithText("Bass Lift")
            .assertExists()
        composeRule.onNodeWithText("Equalizer")
            .performClick()
        composeRule.runOnIdle {
            assertTrue(opened)
        }
    }
}
