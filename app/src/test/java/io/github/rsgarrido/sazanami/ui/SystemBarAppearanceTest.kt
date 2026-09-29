package io.github.rsgarrido.sazanami.ui

import io.github.rsgarrido.sazanami.data.preferences.AppAppearance
import io.github.rsgarrido.sazanami.data.PlayerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemBarAppearanceTest {
    @Test
    fun lightShellUsesDarkIcons() {
        assertTrue(shouldUseDarkSystemBarIcons(false, SystemBarSurface.SHELL))
    }

    @Test
    fun darkShellUsesLightIcons() {
        assertFalse(shouldUseDarkSystemBarIcons(true, SystemBarSurface.SHELL))
    }

    @Test
    fun systemAppearanceTracksOsChangesInShell() {
        assertTrue(
            shouldUseDarkSystemBarIcons(
                AppAppearance.SYSTEM.isDark(systemIsDark = false),
                SystemBarSurface.SHELL
            )
        )
        assertFalse(
            shouldUseDarkSystemBarIcons(
                AppAppearance.SYSTEM.isDark(systemIsDark = true),
                SystemBarSurface.SHELL
            )
        )
    }

    @Test
    fun modernPlayerAndLyricsUseLightIconsOverLightShell() {
        assertFalse(shouldUseDarkSystemBarIcons(false, SystemBarSurface.MODERN_PLAYER))
        assertFalse(shouldUseDarkSystemBarIcons(false, SystemBarSurface.LYRICS))
    }

    @Test
    fun returningToShellRestoresDarkIcons() {
        assertFalse(shouldUseDarkSystemBarIcons(false, SystemBarSurface.LYRICS))
        assertTrue(shouldUseDarkSystemBarIcons(false, SystemBarSurface.SHELL))
    }

    @Test
    fun expandedPlayerSurfaceOverridesShellOnlyWhenVisible() {
        assertEquals(
            SystemBarSurface.MODERN_PLAYER,
            playerSystemBarSurface(PlayerTheme.DEFAULT, true, true, false)
        )
        assertEquals(
            SystemBarSurface.SHELL,
            playerSystemBarSurface(PlayerTheme.DEFAULT, false, true, false)
        )
        assertEquals(
            SystemBarSurface.SHELL,
            playerSystemBarSurface(PlayerTheme.DEFAULT, true, false, false)
        )
        assertEquals(
            SystemBarSurface.SHELL,
            playerSystemBarSurface(PlayerTheme.POCKET_DISC, false, true, false)
        )
    }

    @Test
    fun lyricsOverridesRetroImmersivePlayerWhenBarsReappear() {
        assertEquals(
            SystemBarSurface.SHELL,
            playerSystemBarSurface(PlayerTheme.POCKET_DISC, true, true, false)
        )
        assertEquals(
            SystemBarSurface.LYRICS,
            playerSystemBarSurface(PlayerTheme.POCKET_DISC, true, true, true)
        )
    }
}
