package com.example.cdplaya.ui.player.classicwheel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.cdplaya.ui.player.theme.PlayerThemeTokens
import kotlin.math.roundToInt

/**
 * Transition-owned device shell. Endpoint content remains the existing Classic Wheel
 * implementation, so playback and wheel state are never duplicated.
 */
@Composable
internal fun ClassicWheelPlayerMorph(
    progress: Float,
    geometry: ClassicWheelMorphGeometry?,
    tokens: PlayerThemeTokens,
    content: @Composable (screenAlpha: Float, wheelAlpha: Float, controlsActive: Boolean) -> Unit
) {
    val safeProgress = progress.coerceIn(0f, 1f)
    val density = LocalDensity.current
    Box(Modifier.fillMaxSize()) {
        if (geometry != null) {
            val shell = geometry.shell
            val radius = (18f * (1f - safeProgress)).dp
            Box(
                Modifier
                    .offset { IntOffset(shell.left.roundToInt(), shell.top.roundToInt()) }
                    .size(
                        with(density) { shell.width.coerceAtLeast(1f).toDp() },
                        with(density) { shell.height.coerceAtLeast(1f).toDp() }
                    )
                    .clip(RoundedCornerShape(radius))
                    .background(tokens.shellColor)
            )
        }
        content(
            classicWheelScreenReveal(safeProgress),
            classicWheelWheelReveal(safeProgress),
            classicWheelExpandedControlsActive(safeProgress)
        )
    }
}
