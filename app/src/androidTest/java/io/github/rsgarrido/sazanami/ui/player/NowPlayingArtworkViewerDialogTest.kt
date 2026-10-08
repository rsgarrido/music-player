package io.github.rsgarrido.sazanami.ui.player

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import coil.request.ImageRequest
import coil.size.Scale
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.ui.MusicOverlayState
import io.github.rsgarrido.sazanami.ui.MusicOverlayDestination
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class NowPlayingArtworkViewerDialogTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()
    private var playerClicks = 0
    private var playerBacks = 0
    private val visible = mutableStateOf(true)
    private val status = mutableStateOf(ArtworkViewerLoadStatus.READY)
    private val tilt = ArtworkViewerTiltState()

    private fun content(motion: ArtworkViewerMotionPolicy = ArtworkViewerMotionPolicy(true, false)) {
        val target = song(1)
        val request = NowPlayingArtworkViewerRequest(target, requireNotNull(target.albumArtUri))
        composeRule.setContent {
            MaterialTheme {
                BackHandler { playerBacks++ }
                Box(Modifier.fillMaxSize().clickable { playerClicks++ })
                if (visible.value) NowPlayingArtworkViewerDialog(request, { visible.value = false }, motion, tilt,
                    imageContent = { _, modifier, description, onStatus ->
                        Box(modifier.background(Color.Blue).semantics { contentDescription = description })
                        SideEffect { onStatus(status.value) }
                    })
            }
        }
    }

    @Test fun closeDismissesOnlyTheViewerAndExposesLocalizedPaneAndArtwork() {
        content()
        val pane = composeRule.onNodeWithTag(ArtworkViewerDialogTag).fetchSemanticsNode()
        assertEquals(composeRule.activity.getString(R.string.player_artwork_viewer), pane.config[SemanticsProperties.PaneTitle])
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.player_album_art_for, "Song 1")).assertExists()
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.common_close)).performClick()
        composeRule.onNodeWithTag(ArtworkViewerDialogTag).assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(0, playerClicks); assertEquals(0, playerBacks) }
    }

    @Test fun backClosesBeforeTheUnderlyingPlayerBackHandler() {
        content()
        Espresso.pressBack()
        composeRule.onNodeWithTag(ArtworkViewerDialogTag).assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(0, playerBacks) }
    }

    @Test fun backdropClosesWithoutClickingTheUnderlyingPlayer() {
        content()
        composeRule.onNodeWithTag(ArtworkViewerBackdropTag).performTouchInput { click(Offset(5f, 5f)) }
        composeRule.onNodeWithTag(ArtworkViewerDialogTag).assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(0, playerClicks) }
    }

    @Test fun artworkTapsAndTiltDoNotDismissAndReleaseSpringsToNeutral() {
        content()
        composeRule.onNodeWithTag(ArtworkViewerInputTag).performTouchInput { click(center) }
        composeRule.onNodeWithTag(ArtworkViewerDialogTag).assertExists()
        composeRule.onNodeWithTag(ArtworkViewerInputTag).performTouchInput {
            down(center)
            moveBy(Offset(150f, 100f))
        }
        composeRule.runOnIdle {
            assertTrue(tilt.dragging)
            assertTrue(kotlin.math.abs(tilt.rotationX) <= 7f)
            assertTrue(kotlin.math.abs(tilt.rotationY) <= 7f)
        }
        composeRule.onNodeWithTag(ArtworkViewerInputTag).performTouchInput { up() }
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.runOnIdle { assertFalse(tilt.dragging); assertEquals(0f, tilt.rotationX, 0f); assertEquals(0f, tilt.rotationY, 0f) }
        composeRule.onNodeWithTag(ArtworkViewerDialogTag).assertExists()
        composeRule.runOnIdle { assertEquals(0, playerClicks) }
        composeRule.onNodeWithTag(ArtworkViewerInputTag).performTouchInput { down(center); moveBy(Offset(-100f, 100f)) }
        composeRule.onNodeWithTag(ArtworkViewerInputTag).performTouchInput { cancel() }
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.runOnIdle { assertFalse(tilt.dragging); assertEquals(0f, tilt.rotationX, 0f); assertEquals(0f, tilt.rotationY, 0f) }
        composeRule.onNodeWithTag(ArtworkViewerDialogTag).assertExists()
    }

    @Test fun closeKeepsTheModalMountedUntilItsAnimationCompletes() {
        content()
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.common_close)).performClick()
        composeRule.onNodeWithTag(ArtworkViewerDialogTag).assertExists()
        composeRule.onNodeWithTag(ArtworkViewerBackdropTag).performTouchInput { click(Offset(5f, 5f)) }
        composeRule.runOnIdle { assertEquals(0, playerClicks) }
        composeRule.mainClock.advanceTimeBy(200)
        composeRule.mainClock.autoAdvance = true
        composeRule.onNodeWithTag(ArtworkViewerDialogTag).assertDoesNotExist()
    }

    @Test fun failureShowsLocalizedErrorDisablesTiltAndKeepsCloseWorking() {
        status.value = ArtworkViewerLoadStatus.ERROR
        content()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.player_artwork_unavailable)).assertExists()
        composeRule.onNodeWithTag(ArtworkViewerInputTag).performTouchInput { swipe(center, center + Offset(100f, 100f)) }
        composeRule.runOnIdle { assertFalse(tilt.dragging); assertEquals(0f, tilt.rotationY, 0f) }
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.common_close)).performClick()
        composeRule.onNodeWithTag(ArtworkViewerDialogTag).assertDoesNotExist()
    }

    @Test fun reducedMotionAndTouchExplorationLeaveArtworkNeutral() {
        content(ArtworkViewerMotionPolicy(false, true))
        composeRule.onNodeWithTag(ArtworkViewerInputTag).performTouchInput { swipe(center, center + Offset(100f, 100f)) }
        composeRule.runOnIdle { assertFalse(tilt.dragging); assertEquals(0f, tilt.rotationX, 0f) }
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.common_close)).performClick()
        composeRule.onNodeWithTag(ArtworkViewerDialogTag).assertDoesNotExist()
    }

    @Test fun reopeningUsesCapturedUriAndOldImageCompletionCannotFillTheNewViewer() {
        val first = song(1)
        val second = song(2)
        val request = mutableStateOf(NowPlayingArtworkViewerRequest(first, requireNotNull(first.albumArtUri)))
        val callbacks = mutableMapOf<Uri, (ArtworkViewerLoadStatus) -> Unit>()
        var model: ImageRequest? = null
        composeRule.setContent {
            MaterialTheme {
                NowPlayingArtworkViewerDialog(request.value, {}, ArtworkViewerMotionPolicy(false, false),
                    imageContent = { value, modifier, description, onStatus ->
                        Box(modifier.semantics { contentDescription = description })
                        SideEffect { model = value; callbacks[value.data as Uri] = onStatus }
                    })
            }
        }
        composeRule.runOnIdle {
            assertSame(first.albumArtUri, model?.data)
            assertEquals(Scale.FIT, model?.scale)
            request.value = NowPlayingArtworkViewerRequest(second, requireNotNull(second.albumArtUri))
        }
        composeRule.runOnIdle { callbacks.getValue(requireNotNull(first.albumArtUri))(ArtworkViewerLoadStatus.ERROR) }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.player_artwork_unavailable)).assertDoesNotExist()
        composeRule.runOnIdle { assertSame(second.albumArtUri, model?.data); callbacks.getValue(requireNotNull(second.albumArtUri))(ArtworkViewerLoadStatus.ERROR) }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.player_artwork_unavailable)).assertExists()
    }

    @Test fun sharedOpenerIsStableRejectsStaleTargetsAndLeavesAnOpenRequestPinned() {
        val first = song(1)
        val current = mutableStateOf(first)
        val available = mutableStateOf(true)
        lateinit var overlay: MusicOverlayState
        lateinit var open: (Song) -> Unit
        composeRule.setContent {
            val scope = rememberCoroutineScope()
            overlay = remember { MusicOverlayState(PlayerMorphState(PlayerPresentation.Expanded, scope), mutableStateOf(null), mutableStateOf(null)) }
            val lyrics = rememberPlayerLyricsTransitionState(false) {}
            open = rememberNowPlayingArtworkViewerOpener(current.value, overlay, lyrics, available.value)
        }
        lateinit var initial: (Song) -> Unit
        composeRule.runOnIdle { initial = open; open(first); current.value = song(2) }
        composeRule.runOnIdle {
            assertSame(initial, open)
            assertSame(first, overlay.artworkViewerRequest?.song)
            overlay.dismissArtworkViewer()
            open(first)
            assertNull(overlay.artworkViewerRequest)
            open(current.value.copy(albumArtUri = null))
            assertNull(overlay.artworkViewerRequest)
            available.value = false
        }
        composeRule.runOnIdle { open(current.value); assertNull(overlay.artworkViewerRequest) }
    }

    @Test fun restoredDestinationWithoutCapturedArtworkIsDismissed() {
        lateinit var overlay: MusicOverlayState
        composeRule.setContent {
            val scope = rememberCoroutineScope()
            overlay = remember { MusicOverlayState(PlayerMorphState(PlayerPresentation.Expanded, scope),
                mutableStateOf(null), mutableStateOf(MusicOverlayDestination.ARTWORK_VIEWER)) }
            NowPlayingArtworkViewerOverlay(overlay, true)
        }
        composeRule.runOnIdle { assertFalse(overlay.isArtworkViewerVisible.value); assertNull(overlay.artworkViewerRequest) }
        composeRule.onNodeWithTag(ArtworkViewerDialogTag).assertDoesNotExist()
    }

    private fun song(id: Long) = Song(id, "Song $id", "Artist", "Album", 1, 120_000,
        Uri.parse("content://media/$id"), "/music/$id.flac", "/music", Uri.parse("content://art/$id"))
}
