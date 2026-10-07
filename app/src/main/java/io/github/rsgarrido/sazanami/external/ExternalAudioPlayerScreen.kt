package io.github.rsgarrido.sazanami.external

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.ui.AppShellIcons
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

internal const val EXTERNAL_AUDIO_ARTWORK_TAG = "external_audio_artwork"

internal data class ExternalAudioUiState(
    val requestId: Int = 0,
    val displayName: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long? = null,
    val isPreparing: Boolean = true,
    val showPause: Boolean = false,
    val canPlayPause: Boolean = false,
    val canSeek: Boolean = false,
    val artwork: Bitmap? = null
)

@Composable
internal fun ExternalAudioPlayerScreen(
    state: ExternalAudioUiState,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit
) {
    var draggedFraction by remember(state.requestId) { mutableStateOf<Float?>(null) }
    LaunchedEffect(state.canSeek) {
        if (!state.canSeek) draggedFraction = null
    }
    val duration = state.durationMs
    val positionFraction = if (duration != null && duration > 0L) {
        (state.positionMs.toDouble() / duration).toFloat().coerceIn(0f, 1f)
    } else 0f
    val shownPosition = draggedFraction?.let { externalAudioSeekPosition(it, duration) }
        ?: state.positionMs
    val seekDescription = stringResource(R.string.external_audio_seek)
    val context = LocalContext.current
    val iconSizePx = with(LocalDensity.current) { 32.dp.roundToPx() }
    val appIconRequest = remember(context, iconSizePx) {
        ImageRequest.Builder(context)
            .data(R.drawable.sazanami_icon_foreground)
            .size(iconSizePx)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .build()
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.85f).dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Existing launcher foreground, decoded asynchronously at header size.
                    AsyncImage(
                        model = appIconRequest,
                        contentDescription = stringResource(R.string.external_audio_app_icon),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, stringResource(R.string.external_audio_close))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(EXTERNAL_AUDIO_ARTWORK_SIZE_DP.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .testTag(EXTERNAL_AUDIO_ARTWORK_TAG),
                    contentAlignment = Alignment.Center
                ) {
                    val artwork = state.artwork
                    if (artwork != null) {
                        Image(
                            bitmap = remember(artwork) { artwork.asImageBitmap() },
                            contentDescription = stringResource(R.string.external_audio_embedded_artwork),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = AppShellIcons.MusicNote,
                            contentDescription = stringResource(R.string.external_audio_artwork_placeholder),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Text(
                    text = state.displayName.ifBlank { stringResource(R.string.external_audio_title) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            if (state.isPreparing) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(
                        stringResource(R.string.external_audio_preparing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column {
                Slider(
                    value = draggedFraction ?: positionFraction,
                    onValueChange = { draggedFraction = it },
                    onValueChangeFinished = {
                        draggedFraction?.let(onSeek)
                        draggedFraction = null
                    },
                    enabled = state.canSeek,
                    modifier = Modifier.semantics { contentDescription = seekDescription }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatExternalAudioTime(shownPosition), style = MaterialTheme.typography.bodySmall)
                    Text(
                        duration?.let(::formatExternalAudioTime)
                            ?: stringResource(R.string.external_audio_unknown_duration),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSeekBack, enabled = state.canSeek) {
                    Icon(Icons.Filled.Replay10, stringResource(R.string.external_audio_rewind))
                }
                FilledIconButton(
                    onClick = onPlayPause,
                    enabled = state.canPlayPause,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        if (state.showPause) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        stringResource(
                            if (state.showPause) R.string.external_audio_pause else R.string.external_audio_play
                        ),
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = onSeekForward, enabled = state.canSeek) {
                    Icon(Icons.Filled.Forward10, stringResource(R.string.external_audio_forward))
                }
            }
        }
    }
}
