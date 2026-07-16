package com.example.cdplaya.ui.player.theme

import androidx.compose.ui.graphics.Color
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.ui.player.classicwheel.ClassicWheelDefaultTokens
import com.example.cdplaya.ui.player.pocketcassette.PocketCassetteDefaultTokens
import com.example.cdplaya.ui.player.pocketflip.PocketFlipDefaultTokens
import com.example.cdplaya.ui.player.retrorack.RetroRackDefaultTokens

internal val DefaultPlayerThemeTokens = PlayerThemeTokens(
    shellColor = Color(0xFFFFFBFE),
    accentColor = Color(0xFF6650A4),
    displayBackgroundColor = Color(0xFFFFFBFE),
    displayTextColor = Color(0xFF1C1B1F),
    secondaryAccentColor = Color(0xFF625B71)
)

internal fun PlayerTheme.defaultTokens(): PlayerThemeTokens = when (this) {
    PlayerTheme.DEFAULT -> DefaultPlayerThemeTokens
    PlayerTheme.CLASSIC_WHEEL -> ClassicWheelDefaultTokens
    PlayerTheme.RETRO_RACK -> RetroRackDefaultTokens
    PlayerTheme.POCKET_FLIP -> PocketFlipDefaultTokens
    PlayerTheme.POCKET_CASSETTE -> PocketCassetteDefaultTokens
}
