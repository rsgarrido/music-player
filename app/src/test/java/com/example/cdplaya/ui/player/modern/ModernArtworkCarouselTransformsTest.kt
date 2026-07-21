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
    fun slide_metadataKeepsNaturalTranslationWithoutStyleEffects() {
        val metadata = modernMetadataPageTransform(
            style = ModernArtworkTransitionStyle.SLIDE,
            gestureOffset = -0.5f,
            restingOffset = 0f,
            isCurrent = true
        )

        assertEquals(-0.5f, metadata.translationMultiplier, FLOAT_TOLERANCE)
        assertEquals(1f, metadata.scale, FLOAT_TOLERANCE)
        assertEquals(0f, metadata.rotationY, FLOAT_TOLERANCE)
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
        assertEquals(0.80f, outgoing.scale, FLOAT_TOLERANCE)
        assertEquals(0.80f, incomingAtStart.scale, FLOAT_TOLERANCE)
        assertEquals(1f, incomingAtCenter.scale, FLOAT_TOLERANCE)
        assertEquals(0f, incomingAtStart.alpha, FLOAT_TOLERANCE)
        assertEquals(1f, incomingAtCenter.alpha, FLOAT_TOLERANCE)
        assertTrue(outgoing.alpha < centered.alpha)
    }

    @Test
    fun depthScale_metadataScalesAndFadesAwayFromCenter() {
        val centered = modernMetadataPageTransform(
            style = ModernArtworkTransitionStyle.DEPTH_SCALE,
            gestureOffset = 0f,
            restingOffset = 0f,
            isCurrent = true
        )
        val outgoing = modernMetadataPageTransform(
            style = ModernArtworkTransitionStyle.DEPTH_SCALE,
            gestureOffset = -0.75f,
            restingOffset = 0f,
            isCurrent = true
        )
        val incoming = modernMetadataPageTransform(
            style = ModernArtworkTransitionStyle.DEPTH_SCALE,
            gestureOffset = -0.25f,
            restingOffset = 1f,
            isCurrent = false
        )

        assertTrue(outgoing.scale < centered.scale)
        assertTrue(outgoing.alpha < centered.alpha)
        assertTrue(incoming.scale < centered.scale)
        assertTrue(incoming.alpha > 0f)
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
        assertEquals(22f, COVER_FLOW_MAX_ROTATION_DEGREES, FLOAT_TOLERANCE)
        assertEquals(
            COVER_FLOW_MAX_ROTATION_DEGREES,
            abs(draggedLeft.rotationY),
            FLOAT_TOLERANCE
        )
        assertEquals(-draggedLeft.rotationY, draggedRight.rotationY, FLOAT_TOLERANCE)
    }

    @Test
    fun coverFlow_metadataRotatesLessThanArtworkAndScalesAwayFromCenter() {
        val artwork = modernArtworkPageTransform(
            style = ModernArtworkTransitionStyle.COVER_FLOW,
            gestureOffset = -0.75f,
            restingOffset = 0f,
            isCurrent = true
        )
        val metadata = modernMetadataPageTransform(
            style = ModernArtworkTransitionStyle.COVER_FLOW,
            gestureOffset = -0.75f,
            restingOffset = 0f,
            isCurrent = true
        )

        assertTrue(abs(metadata.rotationY) > 0f)
        assertTrue(abs(metadata.rotationY) < abs(artwork.rotationY))
        assertEquals(
            artwork.rotationY * COVER_FLOW_METADATA_ROTATION_MULTIPLIER,
            metadata.rotationY,
            FLOAT_TOLERANCE
        )
        assertTrue(metadata.scale < 1f)
        assertTrue(metadata.alpha < 1f)
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
        assertEquals(0.42f, PARALLAX_METADATA_TRANSLATION_MULTIPLIER, FLOAT_TOLERANCE)
        assertTrue(
            abs(metadata.translationMultiplier) < abs(artwork.translationMultiplier)
        )
    }

    @Test
    fun depthScaleAndCoverFlow_centeredNeighborsAreFullyInvisible() {
        listOf(
            ModernArtworkTransitionStyle.DEPTH_SCALE,
            ModernArtworkTransitionStyle.COVER_FLOW
        ).forEach { style ->
            listOf(-1f, 1f).forEach { restingOffset ->
                val artwork = modernArtworkPageTransform(
                    style = style,
                    gestureOffset = 0f,
                    restingOffset = restingOffset,
                    isCurrent = false
                )
                val metadata = modernMetadataPageTransform(
                    style = style,
                    gestureOffset = 0f,
                    restingOffset = restingOffset,
                    isCurrent = false
                )

                assertEquals(restingOffset, artwork.translationMultiplier, FLOAT_TOLERANCE)
                assertEquals(0f, artwork.alpha, FLOAT_TOLERANCE)
                assertEquals(restingOffset, metadata.translationMultiplier, FLOAT_TOLERANCE)
                assertEquals(0f, metadata.alpha, FLOAT_TOLERANCE)
            }
        }
    }

    @Test
    fun depthScaleAndCoverFlow_neighborsRevealAfterDragStarts() {
        listOf(
            ModernArtworkTransitionStyle.DEPTH_SCALE,
            ModernArtworkTransitionStyle.COVER_FLOW
        ).forEach { style ->
            val artwork = modernArtworkPageTransform(
                style = style,
                gestureOffset = -0.1f,
                restingOffset = 1f,
                isCurrent = false
            )

            assertTrue(artwork.alpha > 0f)
        }
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
