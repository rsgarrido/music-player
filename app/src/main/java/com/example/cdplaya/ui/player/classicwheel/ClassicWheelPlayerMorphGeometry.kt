package com.example.cdplaya.ui.player.classicwheel

import androidx.compose.ui.geometry.Rect
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.ui.player.PlayerBoundsMeasurement
import com.example.cdplaya.ui.player.PlayerEndpointBounds
import com.example.cdplaya.ui.player.modern.interpolateMorphRect
import com.example.cdplaya.ui.player.modern.morphProgressWindow
import kotlin.math.abs

/** Pure geometry and ownership policy for the Classic Wheel transition. */
internal object ClassicWheelMorphSpec {
    const val ScreenRevealStart = 0.18f
    const val ScreenRevealEnd = 0.62f
    const val WheelRevealStart = 0.48f
    const val WheelRevealEnd = 0.90f
    const val MiniChromeHideStart = 0.08f
    const val MiniChromeHideEnd = 0.38f
    const val ExpandedControlsActiveAt = 0.88f
    const val MinimumDragRangePx = 48f
}

internal enum class PlayerMorphRenderer { DEFAULT, CLASSIC_WHEEL, ENDPOINT }

internal fun playerMorphRendererFor(theme: PlayerTheme): PlayerMorphRenderer = when (theme) {
    PlayerTheme.DEFAULT -> PlayerMorphRenderer.DEFAULT
    PlayerTheme.CLASSIC_WHEEL -> PlayerMorphRenderer.CLASSIC_WHEEL
    else -> PlayerMorphRenderer.ENDPOINT
}

internal data class ClassicWheelMorphGeometry(val shell: Rect)

internal fun resolveClassicWheelMorphGeometry(
    progress: Float,
    endpointBounds: PlayerEndpointBounds
): ClassicWheelMorphGeometry? {
    val mini = (endpointBounds.mini as? PlayerBoundsMeasurement.Measured)?.bounds
    val expanded = (endpointBounds.expanded as? PlayerBoundsMeasurement.Measured)?.bounds
    if (!mini.isValidClassicWheelRect() || !expanded.isValidClassicWheelRect()) return null
    return ClassicWheelMorphGeometry(interpolateMorphRect(mini!!, expanded!!, progress))
}

internal fun classicWheelMorphTravelDistance(endpointBounds: PlayerEndpointBounds): Float {
    val mini = (endpointBounds.mini as? PlayerBoundsMeasurement.Measured)?.bounds
    val expanded = (endpointBounds.expanded as? PlayerBoundsMeasurement.Measured)?.bounds
    if (!mini.isValidClassicWheelRect() || !expanded.isValidClassicWheelRect()) {
        return ClassicWheelMorphSpec.MinimumDragRangePx
    }
    return abs(mini!!.top - expanded!!.top).coerceAtLeast(ClassicWheelMorphSpec.MinimumDragRangePx)
}

internal fun classicWheelWheelReveal(progress: Float): Float = morphProgressWindow(
    progress, ClassicWheelMorphSpec.WheelRevealStart, ClassicWheelMorphSpec.WheelRevealEnd
)

internal fun classicWheelScreenReveal(progress: Float): Float = morphProgressWindow(
    progress, ClassicWheelMorphSpec.ScreenRevealStart, ClassicWheelMorphSpec.ScreenRevealEnd
)

internal fun classicWheelMiniChromeAlpha(progress: Float): Float = 1f - morphProgressWindow(
    progress, ClassicWheelMorphSpec.MiniChromeHideStart, ClassicWheelMorphSpec.MiniChromeHideEnd
)

internal fun classicWheelExpandedControlsActive(progress: Float): Boolean =
    progress >= ClassicWheelMorphSpec.ExpandedControlsActiveAt

private fun Rect?.isValidClassicWheelRect(): Boolean = this != null &&
    left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite() &&
    width > 0f && height > 0f
