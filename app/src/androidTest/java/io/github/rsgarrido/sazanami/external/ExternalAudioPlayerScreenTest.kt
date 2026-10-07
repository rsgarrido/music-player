package io.github.rsgarrido.sazanami.external

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.media3.common.Player
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
    fun brandingAndArtworkPlaceholderKeepTheSameLayoutWhenArtworkAppearsOrDisappears() {
        val filename = "「僕は...」 - Atarayo.flac"
        val screenState = mutableStateOf(ExternalAudioUiState(
            displayName = filename,
            positionMs = 18_000L,
            durationMs = 102_000L,
            isPreparing = false,
            showPause = true,
            canPlayPause = true,
            canSeek = true
        ))
        compose.setContent {
            SazanamiTheme {
                ExternalAudioPlayerScreen(
                    state = screenState.value,
                    onClose = {},
                    onPlayPause = {},
                    onSeek = {},
                    onSeekBack = {},
                    onSeekForward = {}
                )
            }
        }
        val placeholderDescription = context.getString(R.string.external_audio_artwork_placeholder)
        val embeddedDescription = context.getString(R.string.external_audio_embedded_artwork)
        val seekDescription = context.getString(R.string.external_audio_seek)
        val pauseDescription = context.getString(R.string.external_audio_pause)
        compose.onNodeWithContentDescription(context.getString(R.string.external_audio_app_icon))
            .assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.app_name)).assertIsDisplayed()
        compose.onNodeWithContentDescription(placeholderDescription).assertIsDisplayed()
        val initialArtworkBounds = compose.onNodeWithTag(EXTERNAL_AUDIO_ARTWORK_TAG)
            .fetchSemanticsNode().boundsInRoot
        val initialFilenameBounds = compose.onNodeWithText(filename).fetchSemanticsNode().boundsInRoot
        val initialSeekBounds = compose.onNodeWithContentDescription(seekDescription)
            .fetchSemanticsNode().boundsInRoot
        val initialPauseBounds = compose.onNodeWithContentDescription(pauseDescription)
            .fetchSemanticsNode().boundsInRoot
        val initialTimeBounds = compose.onNodeWithText("0:18").fetchSemanticsNode().boundsInRoot
        val initialDurationBounds = compose.onNodeWithText("1:42").fetchSemanticsNode().boundsInRoot
        assertEquals(initialArtworkBounds.width, initialArtworkBounds.height, 0f)

        // A rectangular image also stays within the same square crop; no provider is involved.
        val bitmap = Bitmap.createBitmap(120, 60, Bitmap.Config.ARGB_8888)
        compose.runOnIdle { screenState.value = screenState.value.copy(artwork = bitmap) }
        compose.onNodeWithContentDescription(placeholderDescription).assertDoesNotExist()
        compose.onNodeWithContentDescription(embeddedDescription).assertIsDisplayed()
        assertEquals(initialArtworkBounds, compose.onNodeWithTag(EXTERNAL_AUDIO_ARTWORK_TAG)
            .fetchSemanticsNode().boundsInRoot)
        assertEquals(initialFilenameBounds, compose.onNodeWithText(filename).fetchSemanticsNode().boundsInRoot)
        assertEquals(initialSeekBounds, compose.onNodeWithContentDescription(seekDescription)
            .fetchSemanticsNode().boundsInRoot)
        assertEquals(initialPauseBounds, compose.onNodeWithContentDescription(pauseDescription)
            .fetchSemanticsNode().boundsInRoot)
        assertEquals(initialTimeBounds, compose.onNodeWithText("0:18").fetchSemanticsNode().boundsInRoot)
        assertEquals(initialDurationBounds, compose.onNodeWithText("1:42").fetchSemanticsNode().boundsInRoot)

        compose.runOnIdle { screenState.value = screenState.value.copy(artwork = null) }
        compose.onNodeWithContentDescription(placeholderDescription).assertIsDisplayed()
        compose.onNodeWithContentDescription(embeddedDescription).assertDoesNotExist()
        assertEquals(initialArtworkBounds, compose.onNodeWithTag(EXTERNAL_AUDIO_ARTWORK_TAG)
            .fetchSemanticsNode().boundsInRoot)
        assertEquals(initialFilenameBounds, compose.onNodeWithText(filename).fetchSemanticsNode().boundsInRoot)
        assertEquals(initialSeekBounds, compose.onNodeWithContentDescription(seekDescription)
            .fetchSemanticsNode().boundsInRoot)
        assertEquals(initialPauseBounds, compose.onNodeWithContentDescription(pauseDescription)
            .fetchSemanticsNode().boundsInRoot)
        // Compose owns image rendering until teardown; deliberately do not recycle its bitmap.
    }

    @Test
    fun postReadySeekBufferingKeepsPreparationHiddenAndSeekControlsStationary() {
        val session = ExternalAudioPlayerSession()
        session.beginRequest()
        val screenState = mutableStateOf(ExternalAudioUiState(
            displayName = "Recording.m4a",
            durationMs = 102_000L,
            isPreparing = session.isInitiallyPreparing,
            showPause = true,
            canPlayPause = true,
            canSeek = true
        ))
        compose.setContent {
            SazanamiTheme {
                ExternalAudioPlayerScreen(
                    state = screenState.value,
                    onClose = {},
                    onPlayPause = {},
                    onSeek = {},
                    onSeekBack = {},
                    onSeekForward = {}
                )
            }
        }
        val preparingText = context.getString(R.string.external_audio_preparing)
        val seekDescription = context.getString(R.string.external_audio_seek)
        val pauseDescription = context.getString(R.string.external_audio_pause)
        compose.onNodeWithText(preparingText).assertIsDisplayed()

        compose.runOnIdle {
            session.recordPlaybackState(Player.STATE_READY)
            screenState.value = screenState.value.copy(isPreparing = session.isInitiallyPreparing)
        }
        compose.onNodeWithText(preparingText).assertDoesNotExist()
        val readySeekBounds = compose.onNodeWithContentDescription(seekDescription)
            .fetchSemanticsNode().boundsInRoot
        val readyPauseBounds = compose.onNodeWithContentDescription(pauseDescription)
            .fetchSemanticsNode().boundsInRoot

        compose.runOnIdle {
            session.recordPlaybackState(Player.STATE_BUFFERING)
            screenState.value = screenState.value.copy(
                isPreparing = session.isInitiallyPreparing,
                positionMs = 10_000L
            )
        }
        compose.onNodeWithText(preparingText).assertDoesNotExist()
        assertEquals(readySeekBounds, compose.onNodeWithContentDescription(seekDescription)
            .fetchSemanticsNode().boundsInRoot)
        assertEquals(readyPauseBounds, compose.onNodeWithContentDescription(pauseDescription)
            .fetchSemanticsNode().boundsInRoot)

        compose.runOnIdle {
            session.recordPlaybackState(Player.STATE_READY)
            screenState.value = screenState.value.copy(
                isPreparing = session.isInitiallyPreparing,
                positionMs = 10_250L
            )
        }
        compose.onNodeWithText(preparingText).assertDoesNotExist()
        assertEquals(readySeekBounds, compose.onNodeWithContentDescription(seekDescription)
            .fetchSemanticsNode().boundsInRoot)
    }

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
