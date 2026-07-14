package com.example.cdplaya.ui.player.retrorack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RetroRackVisualProfileTest {
    @Test
    fun `same song identity produces same profile`() {
        val first = buildRetroRackVisualProfile(42L, "Night Drive", "Circuit Club")
        val second = buildRetroRackVisualProfile(42L, "Night Drive", "Circuit Club")

        assertEquals(first, second)
    }

    @Test
    fun `different song identity changes visual pattern`() {
        val first = buildRetroRackVisualProfile(42L, "Night Drive", "Circuit Club")
        val second = buildRetroRackVisualProfile(43L, "Morning Train", "Signal House")

        assertNotEquals(first.levels, second.levels)
        assertNotEquals(first.accent, second.accent)
        assertTrue(first.levels.all { level -> level in 0.24f..0.94f })
    }
}
