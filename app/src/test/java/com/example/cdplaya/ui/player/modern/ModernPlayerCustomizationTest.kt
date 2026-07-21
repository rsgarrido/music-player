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

    @Test
    fun seekbarStyles_exposeStableValuesAndFriendlyLabels() {
        val expected = listOf(
            SeekbarStyleExpectation(
                style = ModernSeekbarStyle.CLASSIC_BAR,
                storageValue = "classic_bar",
                displayName = "Classic Bar",
                description = "The current simple progress bar."
            ),
            SeekbarStyleExpectation(
                style = ModernSeekbarStyle.SLIM_LINE,
                storageValue = "slim_line",
                displayName = "Slim Line",
                description = "A minimal thin progress line."
            ),
            SeekbarStyleExpectation(
                style = ModernSeekbarStyle.THICK_CAPSULE,
                storageValue = "thick_capsule",
                displayName = "Thick Capsule",
                description = "A larger rounded progress control."
            ),
            SeekbarStyleExpectation(
                style = ModernSeekbarStyle.SEGMENTED,
                storageValue = "segmented",
                displayName = "Segmented",
                description = "Small separated blocks that fill with progress."
            ),
            SeekbarStyleExpectation(
                style = ModernSeekbarStyle.WAVEFORM_PREVIEW,
                storageValue = "waveform_preview",
                displayName = "Waveform Preview",
                description = "Uses analyzed track shape when available, with a generated preview while loading or unsupported."
            ),
            SeekbarStyleExpectation(
                style = ModernSeekbarStyle.WAVEFORM_PEAKS,
                storageValue = "waveform_peaks",
                displayName = "Waveform Peaks",
                description = "Mirrors analyzed track peaks when available, with a generated preview as fallback."
            ),
            SeekbarStyleExpectation(
                style = ModernSeekbarStyle.WAVEFORM_GLOW,
                storageValue = "waveform_glow",
                displayName = "Waveform Glow",
                description = "Adds a soft glow to analyzed track shape, with a generated preview as fallback."
            )
        )

        assertEquals(expected.map { item -> item.style }, ModernSeekbarStyle.values().toList())
        expected.forEach { item ->
            assertEquals(item.storageValue, item.style.storageValue)
            assertEquals(item.displayName, item.style.displayName)
            assertEquals(item.description, item.style.description)
        }
        assertTrue(
            ModernSeekbarStyle.values()
                .filter { style -> style.name.startsWith("WAVEFORM") }
                .all { style -> style.usesWaveformData }
        )
        assertTrue(
            ModernSeekbarStyle.values()
                .filterNot { style -> style.name.startsWith("WAVEFORM") }
                .none { style -> style.usesWaveformData }
        )
    }

    private data class StyleExpectation(
        val style: ModernArtworkTransitionStyle,
        val storageValue: String,
        val displayName: String,
        val description: String
    )

    private data class SeekbarStyleExpectation(
        val style: ModernSeekbarStyle,
        val storageValue: String,
        val displayName: String,
        val description: String
    )
}
