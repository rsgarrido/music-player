package com.example.cdplaya.ui.player.pocketcassette

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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

internal object PocketCassetteColors {
    val silverLight = Color(0xFFE4E6E5)
    val silver = Color(0xFFB9BEC0)
    val silverMid = Color(0xFF969EA1)
    val silverDark = Color(0xFF5F686C)
    val shellInk = Color(0xFF263034)
    val blueLight = Color(0xFF6D8EAA)
    val blue = Color(0xFF456D8E)
    val blueDark = Color(0xFF294B67)
    val window = Color(0xFF080B0D)
    val windowEdge = Color(0xFF172127)
    val windowText = Color(0xFFE1E6E4)
    val windowTextMuted = Color(0xFFA5B0B0)
    val tape = Color(0xFF4B2F24)
    val reel = Color(0xFFD2D6D4)
    val reelHub = Color(0xFF6D7678)
    val button = Color(0xFF252B2E)
    val buttonTop = Color(0xFF4A5256)
    val buttonPressed = Color(0xFF15191B)
    val buttonEdge = Color(0xFF0D1012)
    val buttonIcon = Color(0xFFF0F2F1)
    val buttonActive = Color(0xFFE36E3D)
    val orange = Color(0xFFE56C36)
    val statusGreen = Color(0xFF8EBA72)
    val seam = Color(0xFF687175)
    val highlight = Color(0xBFFFFFFF)
}

internal fun Modifier.pocketCassetteShellFinish(): Modifier =
    background(
        brush = Brush.horizontalGradient(
            colorStops = arrayOf(
                0f to PocketCassetteColors.silverDark,
                0.025f to PocketCassetteColors.silverLight,
                0.22f to PocketCassetteColors.silver,
                0.52f to PocketCassetteColors.silverLight,
                0.78f to PocketCassetteColors.silverMid,
                0.975f to PocketCassetteColors.silverLight,
                1f to PocketCassetteColors.silverDark
            )
        )
    ).drawWithContent {
        drawContent()
        val hairline = 1.dp.toPx()
        var y = 2.dp.toPx()
        while (y < size.height) {
            drawLine(
                color = if ((y / hairline).toInt() % 4 == 0) {
                    Color.White.copy(alpha = 0.035f)
                } else {
                    Color.Black.copy(alpha = 0.025f)
                },
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = hairline
            )
            y += 3.dp.toPx()
        }
        drawLine(
            color = PocketCassetteColors.highlight,
            start = Offset(hairline, 0f),
            end = Offset(hairline, size.height),
            strokeWidth = hairline
        )
        drawLine(
            color = Color.Black.copy(alpha = 0.35f),
            start = Offset(size.width - hairline, 0f),
            end = Offset(size.width - hairline, size.height),
            strokeWidth = hairline
        )
    }

internal fun Modifier.pocketCassetteBluePanelFinish(radius: Dp): Modifier =
    background(
        brush = Brush.verticalGradient(
            colors = listOf(
                PocketCassetteColors.blueLight,
                PocketCassetteColors.blue,
                PocketCassetteColors.blueDark
            )
        ),
        shape = RoundedCornerShape(radius)
    ).drawWithContent {
        drawContent()
        val inset = 1.dp.toPx()
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.42f),
            topLeft = Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(
                width = size.width - inset * 2,
                height = size.height - inset * 2
            ),
            cornerRadius = CornerRadius(radius.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawLine(
            color = Color.White.copy(alpha = 0.24f),
            start = Offset(radius.toPx(), 2.dp.toPx()),
            end = Offset(size.width - radius.toPx(), 2.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }

internal fun Modifier.pocketCassetteBevel(
    radius: Dp,
    pressed: Boolean = false
): Modifier = drawWithContent {
    drawContent()
    val inset = 1.dp.toPx()
    drawRoundRect(
        color = if (pressed) PocketCassetteColors.buttonEdge else Color.White.copy(alpha = 0.25f),
        topLeft = Offset(inset, inset),
        size = androidx.compose.ui.geometry.Size(
            width = size.width - inset * 2,
            height = size.height - inset * 2
        ),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = 1.dp.toPx())
    )
    drawLine(
        color = Color.Black.copy(alpha = if (pressed) 0.18f else 0.48f),
        start = Offset(radius.toPx(), size.height - inset),
        end = Offset(size.width - radius.toPx(), size.height - inset),
        strokeWidth = 1.5.dp.toPx()
    )
}

@Composable
internal fun PocketCassetteScrew(
    modifier: Modifier = Modifier,
    size: Dp = 12.dp
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PocketCassetteColors.silverLight,
                        PocketCassetteColors.silverDark
                    )
                )
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.45f),
                style = Stroke(width = 1.dp.toPx())
            )
            drawLine(
                color = PocketCassetteColors.shellInk.copy(alpha = 0.75f),
                start = Offset(this.size.width * 0.25f, this.size.height * 0.56f),
                end = Offset(this.size.width * 0.75f, this.size.height * 0.44f),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}
