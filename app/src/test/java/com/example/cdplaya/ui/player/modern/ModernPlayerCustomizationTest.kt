package com.example.cdplaya.ui.player.modern

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModernPlayerCustomizationTest {
    @Test
    fun artworkTransitionStyles_exposeStableValuesAndFriendlyLabels() {
        val expected = listOf(
            StyleExpectation(
                style = ModernArtworkTransitionStyle.SLIDE,
                storageValue = "slide",
                displayName = "Slide",
                description = "Current artwork slides naturally with the next cover."
            ),
            StyleExpectation(
                style = ModernArtworkTransitionStyle.DEPTH_SCALE,
                storageValue = "depth_scale",
                displayName = "Depth & Scale",
                description = "Covers shrink and fade slightly as they move, creating depth."
            ),
            StyleExpectation(
                style = ModernArtworkTransitionStyle.PARALLAX,
                storageValue = "parallax",
                displayName = "Parallax",
                description = "Artwork and text move at different speeds."
            ),
            StyleExpectation(
                style = ModernArtworkTransitionStyle.COVER_FLOW,
                storageValue = "cover_flow",
                displayName = "Cover Flow",
                description = "Covers tilt slightly as they move off-center."
            ),
            StyleExpectation(
                style = ModernArtworkTransitionStyle.STACK_REVEAL,
                storageValue = "stack_reveal",
                displayName = "Stack Reveal",
                description = "Current cover peels away to reveal the next cover underneath."
            )
        )

        assertEquals(expected.map { item -> item.style }, ModernArtworkTransitionStyle.values().toList())
        expected.forEach { item ->
            assertEquals(item.storageValue, item.style.storageValue)
            assertEquals(item.displayName, item.style.displayName)
            assertEquals(item.description, item.style.description)
        }
        assertTrue(ModernArtworkTransitionStyle.values().all { style ->
            style.displayName.isNotBlank() && style.description.isNotBlank()
        })
    }

    private data class StyleExpectation(
        val style: ModernArtworkTransitionStyle,
        val storageValue: String,
        val displayName: String,
        val description: String
    )
}
