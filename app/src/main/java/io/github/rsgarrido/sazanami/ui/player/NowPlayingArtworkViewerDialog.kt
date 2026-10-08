package io.github.rsgarrido.sazanami.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import io.github.rsgarrido.sazanami.R

internal const val ArtworkViewerDialogTag = "now_playing_artwork_viewer"
internal const val ArtworkViewerBackdropTag = "artwork_viewer_backdrop"
internal const val ArtworkViewerImageTag = "artwork_viewer_image"
internal const val ArtworkViewerInputTag = "artwork_viewer_input"

@Composable
internal fun NowPlayingArtworkViewerDialog(
    request: NowPlayingArtworkViewerRequest,
    onDismiss: () -> Unit,
    motionPolicy: ArtworkViewerMotionPolicy? = null,
    tiltState: ArtworkViewerTiltState? = null,
    imageContent: @Composable (ImageRequest, Modifier, String, (ArtworkViewerLoadStatus) -> Unit) -> Unit = { model, modifier, description, onStatus ->
        ArtworkViewerCoilImage(model, modifier, description, onStatus)
    }
) {
    val motion = motionPolicy ?: rememberArtworkViewerMotionPolicy()
    val image = remember(request) { ArtworkViewerImageState(request) }
    val tilt = tiltState ?: remember(request) { ArtworkViewerTiltState() }
    val entrance = remember(request) { Animatable(0f) }
    var closing by remember(request) { mutableStateOf(false) }
    val latestDismiss = rememberUpdatedState(onDismiss)
    val close = { closing = true }
    val tiltEnabled = motion.tiltEnabled && image.status == ArtworkViewerLoadStatus.READY && !closing
    LaunchedEffect(request, closing, motion.animationsEnabled) {
        entrance.animateTo(if (closing) 0f else 1f, tween(if (motion.animationsEnabled) 160 else 0))
        if (closing) latestDismiss.value()
    }
    LaunchedEffect(tiltEnabled) { if (!tiltEnabled) tilt.reset() }
    val tiltSpec = if (tilt.dragging || !motion.animationsEnabled) snap<Float>() else
        spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    val rotationX by animateFloatAsState(if (tiltEnabled) tilt.rotationX else 0f, tiltSpec, label = "artworkTiltX")
    val rotationY by animateFloatAsState(if (tiltEnabled) tilt.rotationY else 0f, tiltSpec, label = "artworkTiltY")
    val pane = stringResource(R.string.player_artwork_viewer)

    Dialog(onDismissRequest = close, properties = DialogProperties(
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false
    )) {
        // The Compose scrim owns dimming so its open/close fade has one source.
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        DisposableEffect(window) {
            val original = window?.attributes?.dimAmount
            window?.setDimAmount(0f)
            onDispose { if (original != null) window?.setDimAmount(original) }
        }
        Box(Modifier.fillMaxSize().semantics { paneTitle = pane }.testTag(ArtworkViewerDialogTag)) {
            Box(Modifier.matchParentSize()
                .background(Color.Black.copy(alpha = 0.76f * entrance.value))
                .testTag(ArtworkViewerBackdropTag)
                .pointerInput(request) { detectTapGestures { close() } })
            BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
                val width = minOf((maxWidth - 32.dp).coerceAtLeast(1.dp), 840.dp)
                val height = minOf((maxHeight - 112.dp).coerceAtLeast(1.dp), 840.dp)
                val density = LocalDensity.current
                val bounds = with(density) { IntSize(width.roundToPx(), height.roundToPx()) }
                val decodeSize = artworkViewerDecodeSize(bounds)
                val context = LocalContext.current
                val model = remember(context, request, decodeSize) {
                    ImageRequest.Builder(context).data(request.artworkUri)
                        .size(decodeSize.width, decodeSize.height).scale(Scale.FIT).crossfade(false).build()
                }
                // Input is on the untransformed frame; the inner artwork alone receives perspective.
                Box(Modifier.align(Alignment.Center).size(width, height).testTag(ArtworkViewerInputTag)
                    .pointerInput(request) { detectTapGestures { /* Artwork taps stay inside the modal. */ } }
                    .pointerInput(request, tiltEnabled, bounds) {
                        if (!tiltEnabled) return@pointerInput
                        detectDragGestures(
                            onDragStart = { tilt.begin(rotationX, rotationY) },
                            onDrag = { change, delta -> change.consume(); tilt.dragBy(delta, bounds) },
                            onDragEnd = tilt::reset,
                            onDragCancel = tilt::reset
                        )
                    }, contentAlignment = Alignment.Center) {
                    val visual = Modifier.fillMaxSize().testTag(ArtworkViewerImageTag).graphicsLayer {
                        alpha = if (motion.animationsEnabled) entrance.value else 1f
                        val scale = if (motion.animationsEnabled) 0.94f + 0.06f * entrance.value else 1f
                        scaleX = scale
                        scaleY = scale
                        this.rotationX = if (motion.tiltEnabled) rotationX.coerceIn(-ArtworkViewerTiltLimit, ArtworkViewerTiltLimit) else 0f
                        this.rotationY = if (motion.tiltEnabled) rotationY.coerceIn(-ArtworkViewerTiltLimit, ArtworkViewerTiltLimit) else 0f
                        cameraDistance = artworkViewerCameraDistance(bounds)
                    }
                    imageContent(model, visual, stringResource(R.string.player_album_art_for, request.song.title)) {
                        image.update(request, it)
                    }
                    if (image.status == ArtworkViewerLoadStatus.LOADING) {
                        val loading = stringResource(R.string.player_artwork_loading)
                        CircularProgressIndicator(Modifier.size(36.dp).semantics { contentDescription = loading }, color = Color.White)
                    } else if (image.status == ArtworkViewerLoadStatus.ERROR) {
                        Text(stringResource(R.string.player_artwork_unavailable), color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                }
                IconButton(onClick = close, modifier = Modifier.align(Alignment.TopEnd).size(48.dp)) {
                    Icon(Icons.Filled.Close, stringResource(R.string.common_close), tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ArtworkViewerCoilImage(
    model: ImageRequest,
    modifier: Modifier,
    description: String,
    onStatus: (ArtworkViewerLoadStatus) -> Unit
) {
    // No retained painter, crossfade, or another song's memory-cache placeholder.
    AsyncImage(model = model, modifier = modifier, contentScale = ContentScale.Fit,
        contentDescription = description,
        onLoading = { onStatus(ArtworkViewerLoadStatus.LOADING) },
        onSuccess = { onStatus(ArtworkViewerLoadStatus.READY) },
        onError = { onStatus(ArtworkViewerLoadStatus.ERROR) })
}
