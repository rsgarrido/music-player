package com.example.cdplaya.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpandedPlayerDragStateTest {
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
}
