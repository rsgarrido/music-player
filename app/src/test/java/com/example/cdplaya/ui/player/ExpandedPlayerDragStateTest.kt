package com.example.cdplaya.ui.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpandedPlayerDragStateTest {
    @Test
    fun lyricsTransitionCanExplicitlyResetExpandedPlayerOffset() {
        val state = ExpandedPlayerDragState(
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
            onCollapse = {}
        )
        state.updateContainerHeight(1_000)
        state.dragBy(-240f)

        state.resetToExpanded()

        assertEquals(0f, state.offsetY, 0f)
    }

    @Test
    fun distancePastThresholdCollapses() {
        assertTrue(
            shouldCollapseExpandedPlayer(
                offsetY = 261f,
                containerHeightPx = 1_000f,
                velocityY = 0f
            )
        )
    }

    @Test
    fun downwardVelocityCollapsesBeforeDistanceThreshold() {
        assertTrue(
            shouldCollapseExpandedPlayer(
                offsetY = 80f,
                containerHeightPx = 1_000f,
                velocityY = ExpandedPlayerCollapseVelocityPxPerSecond
            )
        )
    }

    @Test
    fun shortSlowDragSnapsBack() {
        assertFalse(
            shouldCollapseExpandedPlayer(
                offsetY = 120f,
                containerHeightPx = 1_000f,
                velocityY = 500f
            )
        )
    }

    @Test
    fun upwardFlingNeverTriggersVelocityCollapse() {
        assertFalse(
            shouldCollapseExpandedPlayer(
                offsetY = 80f,
                containerHeightPx = 1_000f,
                velocityY = -2_000f
            )
        )
    }

    @Test
    fun deliberateUpwardDistanceOpensLyrics() {
        assertTrue(
            shouldOpenLyrics(
                offsetY = -181f,
                containerHeightPx = 1_000f,
                velocityY = 0f
            )
        )
    }

    @Test
    fun fastUpwardFlingOpensLyricsBeforeDistanceThreshold() {
        assertTrue(
            shouldOpenLyrics(
                offsetY = -40f,
                containerHeightPx = 1_000f,
                velocityY = ExpandedPlayerLyricsVelocityPxPerSecond
            )
        )
    }

    @Test
    fun shortUpwardMovementDoesNotOpenLyrics() {
        assertFalse(
            shouldOpenLyrics(
                offsetY = -100f,
                containerHeightPx = 1_000f,
                velocityY = -500f
            )
        )
    }
}
