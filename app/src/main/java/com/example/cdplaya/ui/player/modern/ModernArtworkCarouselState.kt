package com.example.cdplaya.ui.player.modern

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

internal const val MODERN_ARTWORK_SWIPE_DISTANCE_THRESHOLD_FRACTION = 0.25f
internal const val MODERN_ARTWORK_SWIPE_VELOCITY_THRESHOLD_PX_PER_SECOND = 900f

internal enum class ModernArtworkSwipeResult {
    PREVIOUS,
    NEXT,
    CANCELLED
}

@Stable
internal class ModernArtworkCarouselState(
    private val coroutineScope: CoroutineScope,
    private val onPrevious: () -> Unit,
    private val onNext: () -> Unit
) {
    var offsetX by mutableFloatStateOf(0f)
        private set

    var artworkWidthPx by mutableFloatStateOf(1f)
        private set

    private var settleJob: Job? = null

    val dragProgress: Float
        get() = (abs(offsetX) / artworkWidthPx).coerceIn(0f, 1f)

    fun updateArtworkWidth(widthPx: Int) {
        if (widthPx > 0) {
            artworkWidthPx = widthPx.toFloat()
        }
    }

    fun startDrag() {
        settleJob?.cancel()
    }

    fun dragBy(deltaX: Float) {
        offsetX = (offsetX + deltaX).coerceIn(-artworkWidthPx, artworkWidthPx)
    }

    fun settle(velocityX: Float) {
        settleJob?.cancel()

        val result = resolveModernArtworkSwipe(
            offsetX = offsetX,
            artworkWidthPx = artworkWidthPx,
            velocityX = velocityX
        )
        val targetOffset = when (result) {
            ModernArtworkSwipeResult.PREVIOUS -> artworkWidthPx
            ModernArtworkSwipeResult.NEXT -> -artworkWidthPx
            ModernArtworkSwipeResult.CANCELLED -> 0f
        }
        val durationMillis = if (result == ModernArtworkSwipeResult.CANCELLED) {
            180
        } else {
            130
        }

        settleJob = coroutineScope.launch {
            animateOffsetTo(targetOffset, durationMillis)

            when (result) {
                ModernArtworkSwipeResult.PREVIOUS -> onPrevious()
                ModernArtworkSwipeResult.NEXT -> onNext()
                ModernArtworkSwipeResult.CANCELLED -> Unit
            }

            offsetX = 0f
        }
    }

    fun resetForSongChange() {
        settleJob?.cancel()
        offsetX = 0f
    }

    private suspend fun animateOffsetTo(targetOffset: Float, durationMillis: Int) {
        Animatable(offsetX).animateTo(
            targetValue = targetOffset,
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = FastOutSlowInEasing
            )
        ) {
            offsetX = value
        }
    }
}

@Composable
internal fun rememberModernArtworkCarouselState(
    onPrevious: () -> Unit,
    onNext: () -> Unit
): ModernArtworkCarouselState {
    val currentOnPrevious by rememberUpdatedState(onPrevious)
    val currentOnNext by rememberUpdatedState(onNext)
    val coroutineScope = rememberCoroutineScope()

    return remember(coroutineScope) {
        ModernArtworkCarouselState(
            coroutineScope = coroutineScope,
            onPrevious = { currentOnPrevious() },
            onNext = { currentOnNext() }
        )
    }
}

internal fun resolveModernArtworkSwipe(
    offsetX: Float,
    artworkWidthPx: Float,
    velocityX: Float
): ModernArtworkSwipeResult {
    if (artworkWidthPx <= 0f) {
        return ModernArtworkSwipeResult.CANCELLED
    }

    if (abs(velocityX) >= MODERN_ARTWORK_SWIPE_VELOCITY_THRESHOLD_PX_PER_SECOND) {
        return if (velocityX > 0f) {
            ModernArtworkSwipeResult.PREVIOUS
        } else {
            ModernArtworkSwipeResult.NEXT
        }
    }

    val distanceThreshold =
        artworkWidthPx * MODERN_ARTWORK_SWIPE_DISTANCE_THRESHOLD_FRACTION
    return when {
        offsetX >= distanceThreshold -> ModernArtworkSwipeResult.PREVIOUS
        offsetX <= -distanceThreshold -> ModernArtworkSwipeResult.NEXT
        else -> ModernArtworkSwipeResult.CANCELLED
    }
}
