package com.example.cdplaya.ui.player.retrorack

import androidx.compose.ui.geometry.Rect
import com.example.cdplaya.ui.player.PlayerBoundsMeasurement
import com.example.cdplaya.ui.player.PlayerEndpointBounds
import com.example.cdplaya.ui.player.modern.interpolateMorphRect
import com.example.cdplaya.ui.player.modern.morphProgressWindow
import kotlin.math.abs

/** Theme-local timing and geometry policy.  Keeping this separate prevents the rack from
 * accidentally inheriting the screen-shaped policies of the other retro devices. */
internal object RetroRackMorphSpec {
    const val deckRevealStart = .16f
    const val deckRevealEnd = .62f
    const val spectrumRevealStart = .48f
    const val spectrumRevealEnd = .78f
    const val queueRevealStart = .62f
    const val queueRevealEnd = .94f
    const val controlsRevealStart = .54f
    const val controlsRevealEnd = .88f
    const val expandedInputAt = .96f
    const val expensiveWorkAt = .52f
    const val minimumDragRangePx = 48f
}

internal data class RetroRackMorphGeometry(val shell: Rect)

internal fun resolveRetroRackMorphGeometry(
    progress: Float, endpointBounds: PlayerEndpointBounds
): RetroRackMorphGeometry? {
    val mini = (endpointBounds.mini as? PlayerBoundsMeasurement.Measured)?.bounds
    val expanded = (endpointBounds.expanded as? PlayerBoundsMeasurement.Measured)?.bounds
    if (!mini.isValidRackRect() || !expanded.isValidRackRect()) return null
    return RetroRackMorphGeometry(interpolateMorphRect(mini!!, expanded!!, progress))
}

internal fun retroRackMorphTravelDistance(endpointBounds: PlayerEndpointBounds): Float {
    val mini = (endpointBounds.mini as? PlayerBoundsMeasurement.Measured)?.bounds
    val expanded = (endpointBounds.expanded as? PlayerBoundsMeasurement.Measured)?.bounds
    if (!mini.isValidRackRect() || !expanded.isValidRackRect()) return RetroRackMorphSpec.minimumDragRangePx
    return abs(mini!!.top - expanded!!.top).coerceAtLeast(RetroRackMorphSpec.minimumDragRangePx)
}

internal fun retroRackDeckReveal(progress: Float) = morphProgressWindow(progress, RetroRackMorphSpec.deckRevealStart, RetroRackMorphSpec.deckRevealEnd)
internal fun retroRackSpectrumReveal(progress: Float) = morphProgressWindow(progress, RetroRackMorphSpec.spectrumRevealStart, RetroRackMorphSpec.spectrumRevealEnd)
internal fun retroRackQueueReveal(progress: Float) = morphProgressWindow(progress, RetroRackMorphSpec.queueRevealStart, RetroRackMorphSpec.queueRevealEnd)
internal fun retroRackControlsReveal(progress: Float) = morphProgressWindow(progress, RetroRackMorphSpec.controlsRevealStart, RetroRackMorphSpec.controlsRevealEnd)
internal fun retroRackExpandedInputEnabled(progress: Float) = progress >= RetroRackMorphSpec.expandedInputAt
internal fun shouldRunRetroRackExpandedWork(progress: Float) = progress >= RetroRackMorphSpec.expensiveWorkAt

private fun Rect?.isValidRackRect() = this != null && left.isFinite() && top.isFinite() &&
    right.isFinite() && bottom.isFinite() && width > 0f && height > 0f
