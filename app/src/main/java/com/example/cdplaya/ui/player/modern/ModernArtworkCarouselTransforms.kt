package com.example.cdplaya.ui.player.modern

import kotlin.math.abs

internal data class ModernArtworkPageTransform(
    val translationMultiplier: Float,
    val scale: Float,
    val alpha: Float,
    val rotationY: Float = 0f
)

internal data class ModernMetadataPageTransform(
    val translationMultiplier: Float,
    val scale: Float = 1f,
    val alpha: Float
)

internal fun modernArtworkPageTransform(
    style: ModernArtworkTransitionStyle,
    gestureOffset: Float,
    restingOffset: Float,
    isCurrent: Boolean
): ModernArtworkPageTransform {
    val progress = abs(gestureOffset).coerceIn(0f, 1f)
    val pageOffset = gestureOffset + restingOffset
    val distanceFromCenter = abs(pageOffset).coerceIn(0f, 1f)

    return when (style) {
        ModernArtworkTransitionStyle.SLIDE,
        ModernArtworkTransitionStyle.PARALLAX -> slideArtworkTransform(
            pageOffset = pageOffset,
            progress = progress,
            isCurrent = isCurrent
        )

        ModernArtworkTransitionStyle.DEPTH_SCALE -> ModernArtworkPageTransform(
            translationMultiplier = pageOffset,
            scale = 1f - DEPTH_SCALE_REDUCTION * distanceFromCenter,
            alpha = 1f - DEPTH_ALPHA_REDUCTION * distanceFromCenter
        )

        ModernArtworkTransitionStyle.COVER_FLOW -> ModernArtworkPageTransform(
            translationMultiplier = pageOffset,
            scale = 1f - COVER_FLOW_SCALE_REDUCTION * distanceFromCenter,
            alpha = 1f - COVER_FLOW_ALPHA_REDUCTION * distanceFromCenter,
            rotationY = -pageOffset.coerceIn(-1f, 1f) * COVER_FLOW_MAX_ROTATION_DEGREES
        )

        ModernArtworkTransitionStyle.STACK_REVEAL -> stackRevealArtworkTransform(
            gestureOffset = gestureOffset,
            restingOffset = restingOffset,
            pageOffset = pageOffset,
            progress = progress,
            isCurrent = isCurrent
        )
    }
}

internal fun modernMetadataPageTransform(
    style: ModernArtworkTransitionStyle,
    gestureOffset: Float,
    restingOffset: Float,
    isCurrent: Boolean
): ModernMetadataPageTransform {
    val progress = abs(gestureOffset).coerceIn(0f, 1f)
    val pageOffset = gestureOffset + restingOffset
    val distanceFromCenter = abs(pageOffset).coerceIn(0f, 1f)

    return when (style) {
        ModernArtworkTransitionStyle.SLIDE -> ModernMetadataPageTransform(
            translationMultiplier = pageOffset,
            alpha = slidePageAlpha(progress, isCurrent)
        )

        ModernArtworkTransitionStyle.DEPTH_SCALE -> ModernMetadataPageTransform(
            translationMultiplier = pageOffset,
            scale = 1f - DEPTH_METADATA_SCALE_REDUCTION * distanceFromCenter,
            alpha = 1f - DEPTH_METADATA_ALPHA_REDUCTION * distanceFromCenter
        )

        ModernArtworkTransitionStyle.PARALLAX -> ModernMetadataPageTransform(
            translationMultiplier = if (isCurrent) {
                gestureOffset * PARALLAX_METADATA_TRANSLATION_MULTIPLIER
            } else {
                pageOffset
            },
            alpha = if (isCurrent) 1f - progress else progress
        )

        ModernArtworkTransitionStyle.COVER_FLOW -> ModernMetadataPageTransform(
            translationMultiplier = pageOffset,
            alpha = 1f - COVER_FLOW_METADATA_ALPHA_REDUCTION * distanceFromCenter
        )

        ModernArtworkTransitionStyle.STACK_REVEAL -> {
            val isActiveNeighbor = isActiveStackNeighbor(
                gestureOffset = gestureOffset,
                restingOffset = restingOffset
            )
            when {
                isCurrent -> ModernMetadataPageTransform(
                    translationMultiplier = gestureOffset,
                    scale = 1f - STACK_CURRENT_METADATA_SCALE_REDUCTION * progress,
                    alpha = 1f - progress
                )

                isActiveNeighbor -> ModernMetadataPageTransform(
                    translationMultiplier = restingOffset *
                        STACK_UNDERLYING_TRANSLATION_MULTIPLIER * (1f - progress),
                    scale = STACK_UNDERLYING_METADATA_START_SCALE +
                        (1f - STACK_UNDERLYING_METADATA_START_SCALE) * progress,
                    alpha = progress
                )

                else -> ModernMetadataPageTransform(
                    translationMultiplier = restingOffset,
                    alpha = 0f
                )
            }
        }
    }
}

private fun slideArtworkTransform(
    pageOffset: Float,
    progress: Float,
    isCurrent: Boolean
): ModernArtworkPageTransform {
    return ModernArtworkPageTransform(
        translationMultiplier = pageOffset,
        scale = if (isCurrent) {
            1f - progress * SLIDE_CURRENT_SCALE_REDUCTION
        } else {
            SLIDE_NEIGHBOR_START_SCALE + progress * SLIDE_NEIGHBOR_SCALE_INCREASE
        },
        alpha = slidePageAlpha(progress, isCurrent)
    )
}

private fun stackRevealArtworkTransform(
    gestureOffset: Float,
    restingOffset: Float,
    pageOffset: Float,
    progress: Float,
    isCurrent: Boolean
): ModernArtworkPageTransform {
    if (isCurrent) {
        return ModernArtworkPageTransform(
            translationMultiplier = pageOffset,
            scale = 1f - STACK_CURRENT_SCALE_REDUCTION * progress,
            alpha = 1f - STACK_CURRENT_ALPHA_REDUCTION * progress
        )
    }

    if (!isActiveStackNeighbor(gestureOffset, restingOffset)) {
        return ModernArtworkPageTransform(
            translationMultiplier = restingOffset,
            scale = STACK_UNDERLYING_START_SCALE,
            alpha = 0f
        )
    }

    return ModernArtworkPageTransform(
        translationMultiplier = restingOffset *
            STACK_UNDERLYING_TRANSLATION_MULTIPLIER * (1f - progress),
        scale = STACK_UNDERLYING_START_SCALE +
            (1f - STACK_UNDERLYING_START_SCALE) * progress,
        alpha = 1f
    )
}

private fun isActiveStackNeighbor(
    gestureOffset: Float,
    restingOffset: Float
): Boolean {
    return when {
        gestureOffset < 0f -> restingOffset > 0f
        gestureOffset > 0f -> restingOffset < 0f
        else -> false
    }
}

private fun slidePageAlpha(progress: Float, isCurrent: Boolean): Float {
    return if (isCurrent) {
        1f - progress * SLIDE_CURRENT_ALPHA_REDUCTION
    } else {
        (progress * SLIDE_NEIGHBOR_ALPHA_MULTIPLIER).coerceAtMost(
            SLIDE_NEIGHBOR_MAX_ALPHA
        )
    }
}

internal const val PARALLAX_METADATA_TRANSLATION_MULTIPLIER = 0.64f
internal const val COVER_FLOW_MAX_ROTATION_DEGREES = 14f
internal const val COVER_FLOW_CAMERA_DISTANCE_MULTIPLIER = 12f

private const val SLIDE_CURRENT_SCALE_REDUCTION = 0.035f
private const val SLIDE_NEIGHBOR_START_SCALE = 0.94f
private const val SLIDE_NEIGHBOR_SCALE_INCREASE = 0.04f
private const val SLIDE_CURRENT_ALPHA_REDUCTION = 0.08f
private const val SLIDE_NEIGHBOR_ALPHA_MULTIPLIER = 1.8f
private const val SLIDE_NEIGHBOR_MAX_ALPHA = 0.9f

private const val DEPTH_SCALE_REDUCTION = 0.15f
private const val DEPTH_ALPHA_REDUCTION = 0.18f
private const val DEPTH_METADATA_SCALE_REDUCTION = 0.04f
private const val DEPTH_METADATA_ALPHA_REDUCTION = 0.28f

private const val COVER_FLOW_SCALE_REDUCTION = 0.04f
private const val COVER_FLOW_ALPHA_REDUCTION = 0.12f
private const val COVER_FLOW_METADATA_ALPHA_REDUCTION = 0.18f

private const val STACK_CURRENT_SCALE_REDUCTION = 0.08f
private const val STACK_CURRENT_ALPHA_REDUCTION = 0.18f
private const val STACK_CURRENT_METADATA_SCALE_REDUCTION = 0.03f
private const val STACK_UNDERLYING_TRANSLATION_MULTIPLIER = 0.06f
private const val STACK_UNDERLYING_START_SCALE = 0.94f
private const val STACK_UNDERLYING_METADATA_START_SCALE = 0.98f
