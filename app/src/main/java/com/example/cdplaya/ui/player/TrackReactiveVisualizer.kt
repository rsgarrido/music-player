package com.example.cdplaya.ui.player

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

internal fun buildTrackReactiveVisualizerLevels(
    amplitudes: List<Float>?,
    currentPositionMs: Long,
    durationMs: Long,
    columnCount: Int,
    animationPhase: Float,
    isPlaying: Boolean
): List<Float>? {
    if (amplitudes.isNullOrEmpty() || durationMs <= 0L || columnCount <= 0) {
        return null
    }

    val lastIndex = amplitudes.lastIndex.toFloat()
    val progress = (currentPositionMs.toDouble() / durationMs.toDouble())
        .coerceIn(0.0, 1.0)
        .toFloat()
    val centerIndex = progress * lastIndex
    val windowRadius = minOf(
        lastIndex,
        maxOf(columnCount * 0.55f, amplitudes.size * 0.08f)
    )
    val normalizedPhase = if (animationPhase.isFinite()) {
        animationPhase - floor(animationPhase)
    } else {
        0f
    }
    val phaseRadians = normalizedPhase * (2f * PI.toFloat())

    return List(columnCount) { columnIndex ->
        val columnProgress = if (columnCount == 1) {
            0f
        } else {
            columnIndex.toFloat() / (columnCount - 1)
        }
        val centeredColumn = columnProgress * 2f - 1f
        val sourceDrift = if (isPlaying) {
            sin(phaseRadians + columnIndex * 0.43f) * minOf(1.5f, windowRadius * 0.12f)
        } else {
            0f
        }
        val sourceIndex = centerIndex + centeredColumn * windowRadius + sourceDrift
        val localAmplitude = (
                sampleWaveform(amplitudes, sourceIndex - 1f) * 0.2f +
                        sampleWaveform(amplitudes, sourceIndex) * 0.6f +
                        sampleWaveform(amplitudes, sourceIndex + 1f) * 0.2f
                ).coerceIn(0f, 1f)
        val baseLevel = if (isPlaying) {
            0.14f + localAmplitude * 0.74f
        } else {
            0.12f + localAmplitude * 0.62f
        }
        val movement = if (isPlaying) {
            sin(
                phaseRadians + columnIndex * 0.79f +
                        localAmplitude * PI.toFloat()
            ) * (0.055f + localAmplitude * 0.11f)
        } else {
            0f
        }

        (baseLevel + movement).coerceIn(0.08f, 0.98f)
    }
}

private fun sampleWaveform(
    amplitudes: List<Float>,
    sourceIndex: Float
): Float {
    val clampedIndex = sourceIndex.coerceIn(0f, amplitudes.lastIndex.toFloat())
    val lowerIndex = floor(clampedIndex).toInt()
    val upperIndex = (lowerIndex + 1).coerceAtMost(amplitudes.lastIndex)
    val fraction = clampedIndex - lowerIndex
    return safeAmplitude(amplitudes[lowerIndex]) * (1f - fraction) +
            safeAmplitude(amplitudes[upperIndex]) * fraction
}

private fun safeAmplitude(amplitude: Float): Float =
    if (amplitude.isFinite()) amplitude.coerceIn(0f, 1f) else 0f
