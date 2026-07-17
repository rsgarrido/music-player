package com.example.cdplaya.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

internal const val ExpandedPlayerCollapseThresholdFraction = 0.26f
internal const val ExpandedPlayerCollapseVelocityPxPerSecond = 1_400f
private const val HorizontalSwipeThresholdPx = 120f

@Stable
class ExpandedPlayerDragState internal constructor(
    private val coroutineScope: CoroutineScope,
    private val onCollapse: () -> Unit
) {
    var offsetY by mutableFloatStateOf(0f)
        private set

    private var containerHeightPx by mutableFloatStateOf(1f)
    private var settleJob: Job? = null

    val progress: Float
        get() = (offsetY / (containerHeightPx * 0.46f)).coerceIn(0f, 1f)

    fun updateContainerHeight(heightPx: Int) {
        if (heightPx > 0) {
            containerHeightPx = heightPx.toFloat()
        }
    }

    fun startDrag() {
        settleJob?.cancel()
    }

    fun dragBy(deltaY: Float) {
        offsetY = (offsetY + deltaY).coerceIn(0f, containerHeightPx)
    }

    fun settle(velocityY: Float) {
        settleJob?.cancel()

        if (shouldCollapseExpandedPlayer(
                offsetY = offsetY,
                containerHeightPx = containerHeightPx,
                velocityY = velocityY
            )
        ) {
            settleJob = coroutineScope.launch {
                animateOffsetTo(
                    targetOffset = max(offsetY, containerHeightPx * 0.42f),
                    durationMillis = 110
                )
                onCollapse()
                offsetY = 0f
            }
        } else {
            settleJob = coroutineScope.launch {
                animateOffsetTo(
                    targetOffset = 0f,
                    durationMillis = 180
                )
            }
        }
    }

    private suspend fun animateOffsetTo(
        targetOffset: Float,
        durationMillis: Int
    ) {
        Animatable(offsetY).animateTo(
            targetValue = targetOffset,
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = FastOutSlowInEasing
            )
        ) {
            offsetY = value
        }
    }
}

@Composable
fun rememberExpandedPlayerDragState(
    onCollapse: () -> Unit
): ExpandedPlayerDragState {
    val currentOnCollapse by rememberUpdatedState(onCollapse)
    val coroutineScope = rememberCoroutineScope()

    return remember(coroutineScope) {
        ExpandedPlayerDragState(
            coroutineScope = coroutineScope,
            onCollapse = { currentOnCollapse() }
        )
    }
}

internal fun shouldCollapseExpandedPlayer(
    offsetY: Float,
    containerHeightPx: Float,
    velocityY: Float
): Boolean {
    val distanceThreshold = containerHeightPx * ExpandedPlayerCollapseThresholdFraction
    return offsetY >= distanceThreshold ||
            velocityY >= ExpandedPlayerCollapseVelocityPxPerSecond
}

fun Modifier.expandedPlayerHorizontalSwipeGestures(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
): Modifier {
    return pointerInput(onSwipeLeft, onSwipeRight) {
        var totalDragX = 0f

        detectHorizontalDragGestures(
            onDragStart = {
                totalDragX = 0f
            },
            onHorizontalDrag = { change, dragAmount ->
                totalDragX += dragAmount
                change.consume()
            },
            onDragEnd = {
                when {
                    totalDragX <= -HorizontalSwipeThresholdPx -> onSwipeLeft()
                    totalDragX >= HorizontalSwipeThresholdPx -> onSwipeRight()
                }
                totalDragX = 0f
            },
            onDragCancel = {
                totalDragX = 0f
            }
        )
    }
}
