package com.example.cdplaya.ui.player.modern

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ModernArtworkCarouselTransformsTest {
    @Test
    fun slide_centeredArtworkKeepsFullScaleAndNaturalTranslation() {
        val centered = modernArtworkPageTransform(
            style = ModernArtworkTransitionStyle.SLIDE,
            gestureOffset = 0f,
            restingOffset = 0f,
            isCurrent = true
        )
        val dragged = modernArtworkPageTransform(
            style = ModernArtworkTransitionStyle.SLIDE,
            gestureOffset = -0.5f,
            restingOffset = 0f,
            isCurrent = true
        )

        assertEquals(1f, centered.scale, FLOAT_TOLERANCE)
        assertEquals(0f, centered.translationMultiplier, FLOAT_TOLERANCE)
        assertEquals(-0.5f, dragged.translationMultiplier, FLOAT_TOLERANCE)
        assertEquals(0f, dragged.rotationY, FLOAT_TOLERANCE)
    }

    @Test
    fun depthScale_reducesScaleAwayFromCenterAndGrowsIncomingPage() {
        val centered = modernArtworkPageTransform(
            style = ModernArtworkTransitionStyle.DEPTH_SCALE,
            gestureOffset = 0f,
            restingOffset = 0f,
            isCurrent = true
        )
        val outgoing = modernArtworkPageTransform(
            style = ModernArtworkTransitionStyle.DEPTH_SCALE,
            gestureOffset = -1f,
            restingOffset = 0f,
            isCurrent = true
        )
        val incomingAtStart = modernArtworkPageTransform(
            style = ModernArtworkTransitionStyle.DEPTH_SCALE,
            gestureOffset = 0f,
            restingOffset = 1f,
            isCurrent = false
        )
        val incomingAtCenter = modernArtworkPageTransform(
            style = ModernArtworkTransitionStyle.DEPTH_SCALE,
            gestureOffset = -1f,
            restingOffset = 1f,
            isCurrent = false
        )

        assertEquals(1f, centered.scale, FLOAT_TOLERANCE)
        assertEquals(0.85f, outgoing.scale, FLOAT_TOLERANCE)
        assertEquals(0.85f, incomingAtStart.scale, FLOAT_TOLERANCE)
        assertEquals(1f, incomingAtCenter.scale, FLOAT_TOLERANCE)
        assertTrue(outgoing.alpha < centered.alpha)
    }

    @Test
    fun coverFlow_rotationIsZeroAtCenterAndChangesDirectionAcrossCenter() {
        val centered = modernArtworkPageTransform(
            style = ModernArtworkTransitionStyle.COVER_FLOW,
            gestureOffset = 0f,
            restingOffset = 0f,
            isCurrent = true
        )
        val draggedLeft = modernArtworkPageTransform(
            style = ModernArtworkTransitionStyle.COVER_FLOW,
            gestureOffset = -1f,
            restingOffset = 0f,
            isCurrent = true
        )
        val draggedRight = modernArtworkPageTransform(
            style = ModernArtworkTransitionStyle.COVER_FLOW,
            gestureOffset = 1f,
            restingOffset = 0f,
            isCurrent = true
        )

        assertEquals(0f, centered.rotationY, FLOAT_TOLERANCE)
        assertEquals(
            COVER_FLOW_MAX_ROTATION_DEGREES,
            abs(draggedLeft.rotationY),
            FLOAT_TOLERANCE
        )
        assertEquals(-draggedLeft.rotationY, draggedRight.rotationY, FLOAT_TOLERANCE)
    }

    @Test
    fun parallax_currentMetadataMovesSlowerThanArtwork() {
        val gestureOffset = -0.5f
        val artwork = modernArtworkPageTransform(
            style = ModernArtworkTransitionStyle.PARALLAX,
            gestureOffset = gestureOffset,
            restingOffset = 0f,
            isCurrent = true
        )
        val metadata = modernMetadataPageTransform(
            style = ModernArtworkTransitionStyle.PARALLAX,
            gestureOffset = gestureOffset,
            restingOffset = 0f,
            isCurrent = true
        )

        assertEquals(
            gestureOffset * PARALLAX_METADATA_TRANSLATION_MULTIPLIER,
            metadata.translationMultiplier,
            FLOAT_TOLERANCE
        )
        assertTrue(
            abs(metadata.translationMultiplier) < abs(artwork.translationMultiplier)
        )
    }

    @Test
    fun stackReveal_keepsActiveNeighborUnderStackAndInactiveNeighborOffscreen() {
        val activeNext = modernArtworkPageTransform(
            style = ModernArtworkTransitionStyle.STACK_REVEAL,
            gestureOffset = -0.5f,
            restingOffset = 1f,
            isCurrent = false
        )
        val inactivePrevious = modernArtworkPageTransform(
            style = ModernArtworkTransitionStyle.STACK_REVEAL,
            gestureOffset = -0.5f,
            restingOffset = -1f,
            isCurrent = false
        )
        val slideNext = modernArtworkPageTransform(
            style = ModernArtworkTransitionStyle.SLIDE,
            gestureOffset = -0.5f,
            restingOffset = 1f,
            isCurrent = false
        )

        assertTrue(
            abs(activeNext.translationMultiplier) < abs(slideNext.translationMultiplier)
        )
        assertEquals(-1f, inactivePrevious.translationMultiplier, FLOAT_TOLERANCE)
        assertEquals(0f, inactivePrevious.alpha, FLOAT_TOLERANCE)
        assertEquals(1f, activeNext.alpha, FLOAT_TOLERANCE)
    }

    @Test
    fun stackReveal_supportsPreviousDirectionSymmetrically() {
        val previous = modernArtworkPageTransform(
            style = ModernArtworkTransitionStyle.STACK_REVEAL,
            gestureOffset = 0.5f,
            restingOffset = -1f,
            isCurrent = false
        )
        val next = modernArtworkPageTransform(
            style = ModernArtworkTransitionStyle.STACK_REVEAL,
            gestureOffset = -0.5f,
            restingOffset = 1f,
            isCurrent = false
        )

        assertEquals(-next.translationMultiplier, previous.translationMultiplier, FLOAT_TOLERANCE)
        assertEquals(next.scale, previous.scale, FLOAT_TOLERANCE)
        assertEquals(next.alpha, previous.alpha, FLOAT_TOLERANCE)
    }

    private companion object {
        const val FLOAT_TOLERANCE = 0.0001f
    }
}
