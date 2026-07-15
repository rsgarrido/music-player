package com.example.cdplaya.ui.player.pocketflip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
    val bezelTextMuted = Color(0xFF777A76)
    val display = Color(0xFF263029)
    val lcdBand = Color(0xFF172019)
    val lcdTint = Color(0x058FC479)
    val lcdGrid = Color(0x0D0A100C)
    val lcdScanline = Color(0x16040805)
    val artworkWell = Color(0xFF111713)
    val screenText = Color(0xFFE0E7D8)
    val screenTextMuted = Color(0xFF9CA99A)
    val screenAccent = Color(0xFFA5C980)
    val seekInactive = Color(0xFF4A544B)
    val seekHousing = Color(0xFF080A09)
    val seekThumb = Color(0xFFC7C9C4)
    val seekThumbHighlight = Color(0xFFF0F1EC)
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
    val buttonActiveIcon = Color(0xFFA6CE87)
    val modeGlow = Color(0x293E7C52)
    val modeLamp = Color(0xFF82C66B)
    val action = Color(0xFF4B1A2E)
    val actionActive = Color(0xFF6D203C)
    val actionPressed = Color(0xFF35101F)
    val actionIcon = Color(0xFFF7DFE8)
    val utility = Color(0xFF321318)
    val utilityPressed = Color(0xFF1E0A0E)
    val utilityLabel = Color(0xFFF0B1B8)
    val utilityEdge = Color(0xFF9E4B56)
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

internal fun Modifier.pocketFlipActionPlateFinish(): Modifier =
    drawWithContent {
        drawContent()
        val inset = 1.dp.toPx()
        drawRoundRect(
            color = PocketFlipColors.controlGroove,
            topLeft = Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(
                width = size.width - inset * 2f,
                height = size.height - inset * 2f
            ),
            cornerRadius = CornerRadius(size.height / 2f),
            style = Stroke(width = 2.dp.toPx())
        )
        drawLine(
            color = PocketFlipColors.shellHighlight.copy(alpha = 0.18f),
            start = Offset(size.height * 0.32f, 2.dp.toPx()),
            end = Offset(size.width - size.height * 0.32f, 2.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }

internal fun Modifier.pocketFlipUtilitySwitchFinish(isPressed: Boolean): Modifier =
    drawWithContent {
        drawContent()
        val inset = 1.dp.toPx()
        drawRoundRect(
            color = if (isPressed) PocketFlipColors.utilityPressed else PocketFlipColors.utilityEdge,
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

internal fun Modifier.pocketFlipLcdFrameFinish(cornerRadius: Dp): Modifier =
    drawWithContent {
        drawContent()
        val outerInset = 1.dp.toPx()
        drawRoundRect(
            color = Color(0xFF050706),
            topLeft = Offset(outerInset, outerInset),
            size = androidx.compose.ui.geometry.Size(
                width = size.width - outerInset * 2f,
                height = size.height - outerInset * 2f
            ),
            cornerRadius = CornerRadius(cornerRadius.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
        drawLine(
            color = Color(0xFF566159),
            start = Offset(cornerRadius.toPx(), 3.dp.toPx()),
            end = Offset(size.width - cornerRadius.toPx(), 3.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }

internal fun Modifier.pocketFlipArtworkFrameFinish(): Modifier =
    drawWithContent {
        drawContent()
        val inset = 1.dp.toPx()
        drawRoundRect(
            color = Color(0xFF667065),
            topLeft = Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(
                width = size.width - inset * 2f,
                height = size.height - inset * 2f
            ),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(width = 1.dp.toPx())
        )
    }

@Composable
internal fun PocketFlipHinge(compact: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 20.dp else 24.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PocketFlipHingeSegment(
            modifier = Modifier.weight(0.22f),
            compact = compact
        )
        PocketFlipHingeSegment(
            modifier = Modifier.weight(0.56f),
            compact = compact,
            centerGroove = true
        )
        PocketFlipHingeSegment(
            modifier = Modifier.weight(0.22f),
            compact = compact
        )
    }
}

@Composable
private fun PocketFlipHingeSegment(
    compact: Boolean,
    modifier: Modifier = Modifier,
    centerGroove: Boolean = false
) {
    Box(
        modifier = modifier
            .height(if (compact) 16.dp else 20.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFB13B47),
                        PocketFlipColors.hinge,
                        PocketFlipColors.hingeCap
                    )
                ),
                RoundedCornerShape(50)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (centerGroove) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(if (compact) 11.dp else 14.dp)
                    .background(PocketFlipColors.hingeCap, CircleShape)
            )
        }
    }
}
