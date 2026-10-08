package io.github.rsgarrido.sazanami.ui.player

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.roundToInt

internal const val ArtworkViewerTiltLimit = 16f
internal const val ArtworkViewerTiltMaxDragFraction = 0.375f
private const val ArtworkViewerTiltDegreesPerDimension = ArtworkViewerTiltLimit / ArtworkViewerTiltMaxDragFraction
internal enum class ArtworkViewerLoadStatus { LOADING, READY, ERROR }

internal class ArtworkViewerImageState(private val request: NowPlayingArtworkViewerRequest) {
    var status by mutableStateOf(ArtworkViewerLoadStatus.LOADING)
        private set
    var intrinsicSize by mutableStateOf(IntSize.Zero)
        private set
    fun update(source: NowPlayingArtworkViewerRequest, next: ArtworkViewerLoadStatus) {
        if (source === request) status = next
    }
    fun resolvedSize(source: NowPlayingArtworkViewerRequest, size: IntSize) {
        if (source === request) intrinsicSize = size
    }
}

internal class ArtworkViewerTiltState {
    var rotationX by mutableFloatStateOf(0f)
        private set
    var rotationY by mutableFloatStateOf(0f)
        private set
    var dragging by mutableStateOf(false)
        private set

    fun begin(currentX: Float, currentY: Float) {
        rotationX = currentX.coerceIn(-ArtworkViewerTiltLimit, ArtworkViewerTiltLimit)
        rotationY = currentY.coerceIn(-ArtworkViewerTiltLimit, ArtworkViewerTiltLimit)
        dragging = true
    }
    fun dragBy(delta: Offset, size: IntSize) {
        if (!dragging || size.width <= 0 || size.height <= 0) return
        rotationX = (rotationX - delta.y / size.height * ArtworkViewerTiltDegreesPerDimension).coerceIn(-ArtworkViewerTiltLimit, ArtworkViewerTiltLimit)
        rotationY = (rotationY + delta.x / size.width * ArtworkViewerTiltDegreesPerDimension).coerceIn(-ArtworkViewerTiltLimit, ArtworkViewerTiltLimit)
    }
    fun reset() {
        dragging = false
        rotationX = 0f
        rotationY = 0f
    }
}

/** Bound decoded pixels without changing the requested bounds' aspect ratio. */
internal fun artworkViewerDecodeSize(bounds: IntSize): IntSize {
    val width = bounds.width.coerceAtLeast(1)
    val height = bounds.height.coerceAtLeast(1)
    val factor = minOf(1f, 2048f / max(width, height))
    return IntSize((width * factor).roundToInt().coerceAtLeast(1), (height * factor).roundToInt().coerceAtLeast(1))
}

internal fun artworkViewerCameraDistance(bounds: IntSize): Float = max(bounds.width, bounds.height).coerceAtLeast(1) * 1.5f

/** Normalize tilt against the painted Fit image, excluding the frame's empty letterboxing. */
internal fun artworkViewerTiltBounds(frame: IntSize, intrinsic: IntSize): IntSize {
    if (intrinsic.width <= 0 || intrinsic.height <= 0) return frame
    val scale = minOf(frame.width.toFloat() / intrinsic.width, frame.height.toFloat() / intrinsic.height)
    return IntSize((intrinsic.width * scale).roundToInt().coerceAtLeast(1),
        (intrinsic.height * scale).roundToInt().coerceAtLeast(1))
}

internal data class ArtworkViewerMotionPolicy(val animationsEnabled: Boolean, val touchExploration: Boolean) {
    val tiltEnabled get() = animationsEnabled && !touchExploration
}

@Composable
internal fun rememberArtworkViewerMotionPolicy(): ArtworkViewerMotionPolicy {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val manager = remember(context) { context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager }
    fun animationsEnabled() = runCatching {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }.getOrDefault(true)
    var animations by remember(resolver) { mutableStateOf(animationsEnabled()) }
    var exploration by remember(manager) { mutableStateOf(manager?.isTouchExplorationEnabled == true) }
    DisposableEffect(resolver, manager) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { animations = animationsEnabled() }
        }
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { exploration = it }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        manager?.addTouchExplorationStateChangeListener(listener)
        onDispose {
            resolver.unregisterContentObserver(observer)
            manager?.removeTouchExplorationStateChangeListener(listener)
        }
    }
    return ArtworkViewerMotionPolicy(animations, exploration)
}
