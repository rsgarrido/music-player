package io.github.rsgarrido.sazanami.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.rsgarrido.sazanami.data.PlayerTheme
import io.github.rsgarrido.sazanami.ui.player.theme.PlayerThemeTokens
import io.github.rsgarrido.sazanami.ui.player.theme.defaultTokens
import io.github.rsgarrido.sazanami.ui.theme.SazanamiAccent
import io.github.rsgarrido.sazanami.ui.theme.SazanamiLightBackground
import io.github.rsgarrido.sazanami.ui.theme.SazanamiLightPrimary
import io.github.rsgarrido.sazanami.ui.theme.SazanamiLightOnSurface
import io.github.rsgarrido.sazanami.ui.theme.SazanamiLightSurfaceLow
import io.github.rsgarrido.sazanami.ui.theme.SazanamiLightSurfaceHigh
import io.github.rsgarrido.sazanami.ui.theme.SazanamiLightSurfaceHighest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppShellThemeAccentTest {
    @Test
    fun everyPlayerThemeResolvesAnOpaqueShellAccent() {
        PlayerTheme.entries.forEach { playerTheme ->
            val accent = resolveAppShellAccent(
                playerTheme = playerTheme,
                tokens = playerTheme.defaultTokens()
            )

            assertNotEquals(Color.Unspecified, accent)
            assertEquals(1f, accent.alpha, 0f)
        }
    }

    @Test
    fun defaultThemeKeepsCurrentSazanamiAccent() {
        val unrelatedTokens = tokens(accent = Color.Cyan)

        assertEquals(
            SazanamiAccent,
            resolveAppShellAccent(PlayerTheme.DEFAULT, unrelatedTokens)
        )
    }

    @Test
    fun tokenOverrideChangesRetroShellAccent() {
        val customAccent = Color(0xFF62E6FF)

        assertEquals(
            customAccent,
            resolveAppShellAccent(
                playerTheme = PlayerTheme.POCKET_FLIP,
                tokens = tokens(accent = customAccent)
            )
        )
    }

    @Test
    fun cassetteUsesItsActiveControlAccent() {
        val panelAccent = Color(0xFF5B7EA0)
        val activeAccent = Color(0xFFFF9D63)

        assertEquals(
            activeAccent,
            resolveAppShellAccent(
                playerTheme = PlayerTheme.POCKET_CASSETTE,
                tokens = tokens(
                    accent = panelAccent,
                    secondaryAccent = activeAccent
                )
            )
        )
    }

    @Test
    fun missingTokensFallBackToCurrentAccent() {
        PlayerTheme.entries.forEach { playerTheme ->
            assertEquals(
                SazanamiAccent,
                resolveAppShellAccent(playerTheme, tokens = null)
            )
        }
    }

    @Test
    fun darkCustomAccentIsLiftedForShellReadability() {
        val darkAccent = Color(0xFF101820)
        val resolved = resolveAppShellAccent(
            playerTheme = PlayerTheme.RETRO_RACK,
            tokens = tokens(accent = darkAccent)
        )

        assertTrue(resolved.luminance() > darkAccent.luminance())
    }

    @Test
    fun brightRetroAccentDarkensForLightShellSurfaces() {
        val brightAccent = Color(0xFFFFD980)
        val surfaces = listOf(SazanamiLightBackground, SazanamiLightSurfaceHigh)
        val resolved = resolveAppShellAccent(
            playerTheme = PlayerTheme.RETRO_RACK,
            tokens = tokens(accent = brightAccent),
            surfaces = surfaces,
            foreground = SazanamiLightOnSurface
        )

        assertTrue(resolved.luminance() < brightAccent.luminance())
        surfaces.forEach { surface ->
            val lighter = maxOf(resolved.luminance(), surface.luminance())
            val darker = minOf(resolved.luminance(), surface.luminance())
            assertTrue((lighter + 0.05f) / (darker + 0.05f) >= 4.5f)
        }
    }

    @Test
    fun everyPlayerThemeHasReadableLightShellAndChartAccents() {
        val surfaces = listOf(
            SazanamiLightBackground,
            SazanamiLightSurfaceLow,
            SazanamiLightSurfaceHigh,
            SazanamiLightSurfaceHighest
        )
        PlayerTheme.entries.forEach { playerTheme ->
            val shellAccent = resolveAppShellAccent(
                playerTheme = playerTheme,
                tokens = playerTheme.defaultTokens(),
                fallbackAccent = SazanamiLightPrimary,
                surfaces = surfaces,
                foreground = SazanamiLightOnSurface
            )
            val chartAccent = resolveAppShellAccent(
                playerTheme = playerTheme,
                tokens = playerTheme.defaultTokens(),
                fallbackAccent = SazanamiAccent,
                surfaces = listOf(SazanamiLightSurfaceLow),
                foreground = SazanamiLightOnSurface,
                minimumContrast = ChartAccentMinimumContrast
            )
            if (playerTheme == PlayerTheme.DEFAULT) {
                assertEquals(SazanamiLightPrimary, shellAccent)
            }
            surfaces.forEach { surface ->
                assertTrue("$playerTheme shell accent", contrast(shellAccent, surface) >= 4.5f)
            }
            assertTrue(
                "$playerTheme chart accent",
                contrast(chartAccent, SazanamiLightSurfaceLow) >= 3f
            )
        }
    }

    @Test
    fun lightRetroAccentsKeepTheirDistinctColorFamilies() {
        val surfaces = listOf(SazanamiLightBackground, SazanamiLightSurfaceLow)
        fun accent(theme: PlayerTheme) = resolveAppShellAccent(
            playerTheme = theme,
            tokens = theme.defaultTokens(),
            fallbackAccent = SazanamiLightPrimary,
            surfaces = surfaces,
            foreground = SazanamiLightOnSurface
        )

        val rack = accent(PlayerTheme.RETRO_RACK)
        val disc = accent(PlayerTheme.POCKET_DISC)
        val cassette = accent(PlayerTheme.POCKET_CASSETTE)
        assertTrue(rack.green > rack.red && rack.green > rack.blue)
        assertTrue(disc.blue > disc.red && disc.blue > disc.green)
        assertTrue(cassette.red > cassette.blue)
    }

    private fun contrast(first: Color, second: Color): Float {
        val lighter = maxOf(first.luminance(), second.luminance())
        val darker = minOf(first.luminance(), second.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    private fun tokens(
        accent: Color,
        secondaryAccent: Color? = null
    ) = PlayerThemeTokens(
        shellColor = Color.DarkGray,
        accentColor = accent,
        displayBackgroundColor = Color.Black,
        displayTextColor = Color.White,
        secondaryAccentColor = secondaryAccent
    )
}
