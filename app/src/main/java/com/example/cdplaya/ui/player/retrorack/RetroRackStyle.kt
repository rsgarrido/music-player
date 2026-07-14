package com.example.cdplaya.ui.player.retrorack

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent

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

internal val RackBackground = Color(0xFF0B0D10)
internal val PanelDark = Color(0xFF202329)
internal val PanelHeader = Color(0xFF4A4E57)
internal val PanelHeaderEnd = Color(0xFF202329)
internal val DisplayBlack = Color(0xFF040705)
internal val LcdGreen = Color(0xFF75F05F)
internal val LcdGreenDim = Color(0xFF51A94A)
internal val ControlSilver = Color(0xFFD2D5D9)
internal val ButtonFace = Color(0xFF3D424B)
internal val ButtonPressed = Color(0xFF292D33)
internal val ActiveButton = Color(0xFF6FA75F)
internal val SelectedRow = Color(0xFF19351E)
internal val RackHighlight = Color(0xFF9298A2)
internal val RackShadow = Color(0xFF050608)
