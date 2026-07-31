package com.example.cdplaya.ui.player.retrorack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** A transparent root plus a physically interpolated rack shell. */
@Composable
internal fun RetroRackPlayerMorph(
    progress: Float,
    geometry: RetroRackMorphGeometry?,
    content: @Composable (deckReveal: Float, spectrumReveal: Float, queueReveal: Float, controlsReveal: Float, inputEnabled: Boolean) -> Unit
) {
    val density = LocalDensity.current
    val p = progress.coerceIn(0f, 1f)
    Box(Modifier.fillMaxSize()) {
        geometry?.shell?.let { shell ->
            Box(Modifier.offset { IntOffset(shell.left.roundToInt(), shell.top.roundToInt()) }
                .size(with(density) { shell.width.toDp() }, with(density) { shell.height.toDp() })
                .clip(androidx.compose.foundation.shape.RoundedCornerShape((8f * (1f - p)).dp))
                .background(RackBackground))
        }
        Box(Modifier.fillMaxSize().clipRackShell(geometry, p)) {
            content(retroRackDeckReveal(p), retroRackSpectrumReveal(p), retroRackQueueReveal(p), retroRackControlsReveal(p), retroRackExpandedInputEnabled(p))
        }
    }
}

private fun Modifier.clipRackShell(geometry: RetroRackMorphGeometry?, progress: Float) = drawWithContent {
    val shell = geometry?.shell ?: return@drawWithContent
    val radius = 8.dp.toPx() * (1f - progress)
    val path = Path().apply { addRoundRect(RoundRect(shell, CornerRadius(radius, radius))) }
    clipPath(path) { this@drawWithContent.drawContent() }
}
