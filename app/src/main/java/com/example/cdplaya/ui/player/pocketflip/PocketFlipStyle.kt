package com.example.cdplaya.ui.player.pocketflip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal object PocketFlipColors {
    val shellTop = Color(0xFFB3414C)
    val shell = Color(0xFF982E3B)
    val shellBottom = Color(0xFF76212D)
    val shellHighlight = Color(0x66FFD9DC)
    val shellShadow = Color(0x660F0709)
    val shellText = Color(0xFFF5C7CB)
    val bezel = Color(0xFF17171B)
    val bezelText = Color(0xFFC7C9C4)
    val display = Color(0xFF263029)
    val artworkWell = Color(0xFF111713)
    val screenText = Color(0xFFE0E7D8)
    val screenTextMuted = Color(0xFF9CA99A)
    val screenAccent = Color(0xFFA5C980)
    val seekInactive = Color(0xFF4A544B)
    val statusOn = Color(0xFF8DD663)
    val statusIdle = Color(0xFF715257)
    val hinge = Color(0xFF6B1E29)
    val hingeCap = Color(0xFF321318)
    val button = Color(0xFF2A2B31)
    val buttonPressed = Color(0xFF18191D)
    val buttonShadow = Color(0xFF541923)
    val buttonCenter = Color(0xFF202126)
    val buttonHighlight = Color(0xFF4B4D55)
    val buttonIcon = Color(0xFFDADADF)
    val buttonActive = Color(0xFFB6C880)
    val buttonActiveIcon = Color(0xFF20231D)
    val action = Color(0xFF4B1A2E)
    val actionActive = Color(0xFF6D203C)
    val actionPressed = Color(0xFF35101F)
    val actionIcon = Color(0xFFF7DFE8)
    val utility = Color(0xFF70232F)
    val utilityPressed = Color(0xFF521923)
    val utilityIcon = Color(0xFFF7D8DB)
    val speaker = Color(0xFF48151D)
    val speakerHighlight = Color(0xFFBD5260)
    val controlWell = Color(0xFF741F2B)
    val controlGroove = Color(0xFF5B1822)
    val engravedText = Color(0xFFC66C76)
    val screw = Color(0xFF7D2732)
    val screwSlot = Color(0xFF48131B)
}

internal fun Modifier.pocketFlipRoundButtonFinish(isPressed: Boolean): Modifier =
    drawWithContent {
        drawContent()
        val inset = 1.dp.toPx()
        drawCircle(
            color = if (isPressed) Color(0xFF210B14) else Color(0xFF7D3651),
            radius = size.minDimension / 2f - inset,
            center = center,
            style = Stroke(width = if (isPressed) 1.dp.toPx() else 2.dp.toPx())
        )
    }

internal fun Modifier.pocketFlipUtilitySwitchFinish(isPressed: Boolean): Modifier =
    drawWithContent {
        drawContent()
        val inset = 1.dp.toPx()
        drawRoundRect(
            color = if (isPressed) Color(0xFF310E16) else Color(0xFF9D4250),
            topLeft = Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(
                width = size.width - inset * 2f,
                height = size.height - inset * 2f
            ),
            cornerRadius = CornerRadius(size.height / 2f),
            style = Stroke(width = 1.dp.toPx())
        )
    }

internal fun Modifier.pocketFlipShellFinish(): Modifier =
    background(
        brush = Brush.verticalGradient(
            colors = listOf(
                PocketFlipColors.shellTop,
                PocketFlipColors.shell,
                PocketFlipColors.shellBottom
            )
        )
    ).drawWithContent {
        drawContent()
        val edge = 1.dp.toPx()
        drawLine(
            color = PocketFlipColors.shellHighlight,
            start = Offset(0f, edge),
            end = Offset(size.width, edge),
            strokeWidth = edge
        )
        drawLine(
            color = PocketFlipColors.shellShadow,
            start = Offset(0f, size.height - edge),
            end = Offset(size.width, size.height - edge),
            strokeWidth = edge
        )
    }

internal fun Modifier.pocketFlipBezelFinish(cornerRadius: Dp): Modifier =
    background(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF24242A), PocketFlipColors.bezel)
        )
    ).drawWithContent {
        drawContent()
        drawRoundRect(
            color = Color(0xFF08080A),
            cornerRadius = CornerRadius(cornerRadius.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
        drawLine(
            color = Color(0xFF46464D),
            start = Offset(cornerRadius.toPx(), 2.dp.toPx()),
            end = Offset(size.width - cornerRadius.toPx(), 2.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }

internal fun Modifier.pocketFlipScreenFinish(): Modifier = drawWithContent {
    drawContent()
    val scanlineGap = 4.dp.toPx()
    var y = scanlineGap
    while (y < size.height) {
        drawLine(
            color = Color.Black.copy(alpha = 0.045f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += scanlineGap
    }
}

@Composable
internal fun PocketFlipHinge(compact: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 18.dp else 22.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF8C2B37), PocketFlipColors.hinge, Color(0xFF4F1720))
                ),
                RoundedCornerShape(50)
            )
            .padding(horizontal = if (compact) 8.dp else 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PocketFlipHingeCap(compact = compact)
            PocketFlipHingeCap(compact = compact)
        }
    }
}

@Composable
private fun PocketFlipHingeCap(compact: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.24f)
            .height(if (compact) 8.dp else 10.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF4D2027), PocketFlipColors.hingeCap)
                ),
                CircleShape
            )
    )
}
