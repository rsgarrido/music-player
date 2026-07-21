package com.example.cdplaya.ui.player.modern

import org.junit.Assert.assertEquals
import org.junit.Test

class ModernArtworkCarouselStateTest {

    @Test
    fun leftDragPastDistanceThresholdMovesNext() {
        assertEquals(
            ModernArtworkSwipeResult.NEXT,
            resolveModernArtworkSwipe(
                offsetX = -251f,
                artworkWidthPx = 1_000f,
                velocityX = 0f
            )
        )
    }

    @Test
    fun rightDragPastDistanceThresholdMovesPrevious() {
        assertEquals(
            ModernArtworkSwipeResult.PREVIOUS,
            resolveModernArtworkSwipe(
                offsetX = 250f,
                artworkWidthPx = 1_000f,
                velocityX = 0f
            )
        )
    }

    @Test
    fun shortDragCancels() {
        assertEquals(
            ModernArtworkSwipeResult.CANCELLED,
            resolveModernArtworkSwipe(
                offsetX = -249f,
                artworkWidthPx = 1_000f,
                velocityX = 899f
            )
        )
    }

    @Test
    fun fastFlingUsesVelocityDirection() {
        assertEquals(
            ModernArtworkSwipeResult.PREVIOUS,
            resolveModernArtworkSwipe(
                offsetX = -100f,
                artworkWidthPx = 1_000f,
                velocityX = 901f
            )
        )
        assertEquals(
            ModernArtworkSwipeResult.NEXT,
            resolveModernArtworkSwipe(
                offsetX = 100f,
                artworkWidthPx = 1_000f,
                velocityX = -901f
            )
        )
    }
}
