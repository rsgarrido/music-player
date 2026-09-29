package io.github.rsgarrido.sazanami.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class LightPaletteContrastTest {
    @Test
    fun coreTextAndAccentRolesRemainReadable() {
        assertTrue(contrast(SazanamiLightOnSurface, SazanamiLightBackground) >= 7f)
        assertTrue(contrast(SazanamiLightOnSurfaceVariant, SazanamiLightSurfaceHigh) >= 4.5f)
        assertTrue(contrast(SazanamiLightPrimary, SazanamiLightSurfaceLow) >= 4.5f)
        assertTrue(contrast(Color.White, SazanamiLightPrimary) >= 4.5f)
        assertTrue(contrast(SazanamiLightOnSecondaryContainer, SazanamiLightSecondaryContainer) >= 4.5f)
    }

    private fun contrast(first: Color, second: Color): Float {
        val lighter = maxOf(first.luminance(), second.luminance())
        val darker = minOf(first.luminance(), second.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
