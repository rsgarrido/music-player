package io.github.rsgarrido.sazanami.ui.player.modern

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModernOptionLabelsTest {
    @Test
    fun everyChoiceHasItsOwnResourceWithoutChangingStorageIdentity() {
        val groups = listOf(
            ModernArtworkTransitionStyle.entries.map { it.labelRes },
            ModernSeekbarStyle.entries.map { it.labelRes },
            ModernWaveformSize.entries.map { it.labelRes },
            ModernWaveformDensity.entries.map { it.labelRes },
            ModernSeekbarColorMode.entries.map { it.labelRes },
            ModernBackgroundStyle.entries.map { it.labelRes },
            ModernBlurStrength.entries.map { it.labelRes },
            ModernDimmingStrength.entries.map { it.labelRes },
            ModernArtworkShape.entries.map { it.labelRes },
            ModernArtworkSize.entries.map { it.labelRes },
            ModernArtworkFit.entries.map { it.labelRes },
            ModernArtworkShadow.entries.map { it.labelRes },
            ModernControlStyle.entries.map { it.labelRes },
            ModernControlSize.entries.map { it.labelRes },
            ModernControlAccent.entries.map { it.labelRes },
            ModernLayoutDensity.entries.map { it.labelRes },
            ModernMetadataAlignment.entries.map { it.labelRes },
        )
        groups.forEach { ids ->
            assertTrue(ids.all { it != 0 })
            assertEquals(ids.size, ids.distinct().size)
        }
        assertEquals("depth_scale", ModernArtworkTransitionStyle.DEPTH_SCALE.storageValue)
        assertEquals("album_derived", ModernControlAccent.ALBUM_DERIVED.storageValue)
    }
}
