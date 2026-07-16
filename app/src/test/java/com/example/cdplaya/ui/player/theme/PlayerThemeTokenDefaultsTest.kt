package com.example.cdplaya.ui.player.theme

import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.ui.player.classicwheel.ClassicWheelDefaultTokens
import com.example.cdplaya.ui.player.pocketcassette.PocketCassetteDefaultTokens
import com.example.cdplaya.ui.player.pocketflip.PocketFlipDefaultTokens
import com.example.cdplaya.ui.player.retrorack.RetroRackDefaultTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PlayerThemeTokenDefaultsTest {
    @Test
    fun everyPlayerTheme_hasDefaultTokens() {
        PlayerTheme.values().forEach { theme ->
            assertNotNull(theme.defaultTokens())
        }
    }

    @Test
    fun defaultTokens_returnsThemeSpecificDefaults() {
        assertEquals(DefaultPlayerThemeTokens, PlayerTheme.DEFAULT.defaultTokens())
        assertEquals(ClassicWheelDefaultTokens, PlayerTheme.CLASSIC_WHEEL.defaultTokens())
        assertEquals(RetroRackDefaultTokens, PlayerTheme.RETRO_RACK.defaultTokens())
        assertEquals(PocketFlipDefaultTokens, PlayerTheme.POCKET_FLIP.defaultTokens())
        assertEquals(PocketCassetteDefaultTokens, PlayerTheme.POCKET_CASSETTE.defaultTokens())
    }
}
