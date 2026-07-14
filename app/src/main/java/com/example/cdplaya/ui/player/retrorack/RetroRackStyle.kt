package com.example.cdplaya.ui.player.retrorack

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent

internal fun Modifier.rackBevel(): Modifier = drawWithContent {
    drawContent()
    val stroke = 1f
    drawLine(RackHighlight, Offset.Zero, Offset(size.width, 0f), stroke)
    drawLine(RackHighlight, Offset.Zero, Offset(0f, size.height), stroke)
    drawLine(RackShadow, Offset(0f, size.height), Offset(size.width, size.height), stroke)
    drawLine(RackShadow, Offset(size.width, 0f), Offset(size.width, size.height), stroke)
}

internal val RackBackground = Color(0xFF0B0D10)
internal val PanelDark = Color(0xFF25282E)
internal val PanelHeader = Color(0xFF343841)
internal val PanelHeaderEnd = Color(0xFF1D2026)
internal val DisplayBlack = Color(0xFF040705)
internal val LcdGreen = Color(0xFF75F05F)
internal val LcdGreenDim = Color(0xFF51A94A)
internal val MeterAmber = Color(0xFFE0C04A)
internal val ControlSilver = Color(0xFFD2D5D9)
internal val ButtonFace = Color(0xFF484D56)
internal val ActiveButton = Color(0xFF8ACD74)
internal val SelectedRow = Color(0xFF78D866)
internal val RackHighlight = Color(0xFF7B818C)
internal val RackShadow = Color(0xFF050608)
