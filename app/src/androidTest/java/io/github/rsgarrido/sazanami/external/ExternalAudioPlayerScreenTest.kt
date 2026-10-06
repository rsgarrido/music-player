package io.github.rsgarrido.sazanami.external

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.ui.theme.SazanamiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExternalAudioPlayerScreenTest {
    @get:Rule
    val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun unknownDurationKeepsPlaybackAndCloseAvailableButDisablesSeeking() {
        var closeClicks = 0
        var playClicks = 0
        compose.setContent {
            SazanamiTheme {
                ExternalAudioPlayerScreen(
                    state = ExternalAudioUiState(
                        displayName = "Voice note.ogg",
                        positionMs = 18_000L,
                        isPreparing = false,
                        canPlayPause = true
                    ),
                    onClose = { closeClicks++ },
                    onPlayPause = { playClicks++ },
                    onSeek = {},
                    onSeekBack = {},
                    onSeekForward = {}
                )
            }
        }
        compose.onNodeWithText("Voice note.ogg").assertIsDisplayed()
        compose.onNodeWithText("0:18").assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.external_audio_unknown_duration)).assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(R.string.external_audio_seek)).assertIsNotEnabled()
        compose.onNodeWithContentDescription(context.getString(R.string.external_audio_rewind)).assertIsNotEnabled()
        compose.onNodeWithContentDescription(context.getString(R.string.external_audio_forward)).assertIsNotEnabled()
        compose.onNodeWithContentDescription(context.getString(R.string.external_audio_play)).performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.external_audio_close)).performClick()
        assertEquals(1, playClicks)
        assertEquals(1, closeClicks)
    }

    @Test
    fun preparedSeekableAudioShowsPauseDurationAndTenSecondControls() {
        var rewindClicks = 0
        var forwardClicks = 0
        var pauseClicks = 0
        compose.setContent {
            SazanamiTheme {
                ExternalAudioPlayerScreen(
                    state = ExternalAudioUiState(
                        displayName = "Recording.m4a",
                        positionMs = 18_000L,
                        durationMs = 102_000L,
                        isPreparing = false,
                        showPause = true,
                        canPlayPause = true,
                        canSeek = true
                    ),
                    onClose = {},
                    onPlayPause = { pauseClicks++ },
                    onSeek = {},
                    onSeekBack = { rewindClicks++ },
                    onSeekForward = { forwardClicks++ }
                )
            }
        }
        compose.onNodeWithText("1:42").assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(R.string.external_audio_seek)).assertIsEnabled()
        compose.onNodeWithContentDescription(context.getString(R.string.external_audio_rewind)).performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.external_audio_forward)).performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.external_audio_pause)).performClick()
        assertEquals(1, rewindClicks)
        assertEquals(1, forwardClicks)
        assertEquals(1, pauseClicks)
    }
}
