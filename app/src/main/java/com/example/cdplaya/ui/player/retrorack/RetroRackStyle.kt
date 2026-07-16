package com.example.cdplaya.ui.player.retrorack

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import com.example.cdplaya.ui.player.theme.PlayerThemeTokens
import com.example.cdplaya.ui.player.theme.darken
import com.example.cdplaya.ui.player.theme.lighten

internal fun Modifier.rackBevel(pressed: Boolean = false): Modifier = drawWithContent {
    drawContent()
    val stroke = 1f
    val topLeft = if (pressed) RackShadow else RackHighlight
    val bottomRight = if (pressed) RackHighlight else RackShadow
    drawLine(topLeft, Offset.Zero, Offset(size.width, 0f), stroke)
    drawLine(topLeft, Offset.Zero, Offset(0f, size.height), stroke)
    drawLine(bottomRight, Offset(0f, size.height), Offset(size.width, size.height), stroke)
    drawLine(bottomRight, Offset(size.width, 0f), Offset(size.width, size.height), stroke)
}

internal val RetroRackDefaultTokens = PlayerThemeTokens(
    shellColor = Color(0xFF202329),
    accentColor = Color(0xFF75F05F),
    displayBackgroundColor = Color(0xFF040705),
    displayTextColor = Color(0xFFD2D5D9),
    secondaryAccentColor = Color(0xFF6FA75F)
)

internal class RetroRackPalette private constructor(tokens: PlayerThemeTokens) {
    private val shell = tokens.shellColor
    private val accent = tokens.accentColor
    private val activeAccent = tokens.secondaryAccentColor ?: accent.darken(0.30f)

    val rackBackground = shell.darken(0.628f)
    val panelDark = shell
    val panelHeader = shell.lighten(0.199f)
    val panelHeaderEnd = shell
    val displayBlack = tokens.displayBackgroundColor
    val lcdGreen = accent
    val lcdGreenDim = accent.darken(0.289f)
    val controlSilver = tokens.displayTextColor
    val buttonFace = shell.lighten(0.143f)
    val buttonPressed = shell.lighten(0.044f)
    val activeButton = activeAccent
    val selectedRow = accent.darken(0.770f)
    val rackHighlight = shell.lighten(0.535f)
    val rackShadow = shell.darken(0.822f)
    val inactiveTrack = shell.lighten(0.076f)

    companion object {
        fun from(tokens: PlayerThemeTokens): RetroRackPalette = RetroRackPalette(tokens)
    }
}

internal val RetroRackDefaultPalette = RetroRackPalette.from(RetroRackDefaultTokens)

internal val RackBackground = RetroRackDefaultPalette.rackBackground
internal val PanelDark = RetroRackDefaultPalette.panelDark
internal val PanelHeader = RetroRackDefaultPalette.panelHeader
internal val PanelHeaderEnd = RetroRackDefaultPalette.panelHeaderEnd
internal val DisplayBlack = RetroRackDefaultPalette.displayBlack
internal val LcdGreen = RetroRackDefaultPalette.lcdGreen
internal val LcdGreenDim = RetroRackDefaultPalette.lcdGreenDim
internal val ControlSilver = RetroRackDefaultPalette.controlSilver
internal val ButtonFace = RetroRackDefaultPalette.buttonFace
internal val ButtonPressed = RetroRackDefaultPalette.buttonPressed
internal val ActiveButton = RetroRackDefaultPalette.activeButton
internal val SelectedRow = RetroRackDefaultPalette.selectedRow
internal val RackHighlight = RetroRackDefaultPalette.rackHighlight
internal val RackShadow = RetroRackDefaultPalette.rackShadow
internal val InactiveTrack = RetroRackDefaultPalette.inactiveTrack
