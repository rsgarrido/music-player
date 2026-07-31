package com.example.cdplaya.ui.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerLyricsTransitionStateTest {
    @Test
    fun dragDistanceDirectlyControlsSharedProgressAndComposesLyricsEarly() {
        var composed = false
        val state = PlayerLyricsTransitionState(
            initiallyLyricsVisible = false,
            scope = CoroutineScope(Dispatchers.Unconfined),
            onCompositionVisibilityChanged = { composed = it }
        )

        state.beginOpeningDrag()
        state.dragOpeningBy(deltaY = -250f, heightPx = 1_000f)

        assertEquals(0.25f, state.progress, 0.001f)
        assertTrue(composed)
        assertTrue(state.lyricsComposed)
    }

    @Test
    fun shortOpeningDragReturnsToExpanded() {
        assertEquals(PlayerSurfaceState.EXPANDED, openingDestination(0.12f, -400f))
    }

    @Test
    fun completedOpeningDragSettlesAtLyrics() {
        assertEquals(PlayerSurfaceState.LYRICS, openingDestination(0.3f, 0f))
        assertEquals(PlayerSurfaceState.LYRICS, openingDestination(0.05f, -1_500f))
    }

    @Test
    fun downwardHeaderDragCanOnlySettleAtExpandedOrLyrics() {
        assertEquals(PlayerSurfaceState.EXPANDED, closingDestination(0.7f, 0f))
        assertEquals(PlayerSurfaceState.EXPANDED, closingDestination(0.98f, 1_300f))
        assertEquals(PlayerSurfaceState.LYRICS, closingDestination(0.95f, 200f))
    }

    @Test
    fun lyricsAreVisibleBeforeTransitionCompletesAndPlayerRemainsPresent() {
        assertTrue(lyricsVisualAlpha(0.35f) > 0f)
        assertTrue(playerVisualAlpha(0.35f) > 0f)
        assertTrue(playerVisualAlpha(1f) > 0f)
    }

    @Test
    fun settlementDurationContinuesFromCurrentProgress() {
        assertTrue(transitionDurationMillis(0.8f, 1f) <
                transitionDurationMillis(0.2f, 1f))
    }
}
