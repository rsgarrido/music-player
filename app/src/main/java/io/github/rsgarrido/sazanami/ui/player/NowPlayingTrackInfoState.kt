package io.github.rsgarrido.sazanami.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.player.audioquality.AudioQualityInfo
import io.github.rsgarrido.sazanami.ui.MusicOverlayState
import kotlinx.coroutines.CancellationException

/** Each opening has its own identity, including reopening the same file. */
internal class NowPlayingTrackInfoRequest(val song: Song)

@Composable
internal fun rememberNowPlayingTrackInfoOpener(
    currentSong: Song?,
    overlayState: MusicOverlayState,
    canPresent: Boolean
): (Song) -> Unit {
    val latestSong = rememberUpdatedState(currentSong)
    val latestCanPresent = rememberUpdatedState(canPresent)
    return remember(overlayState) {
        { target: Song ->
            if (latestCanPresent.value && isCurrentNowPlayingTarget(target, latestSong.value)) {
                overlayState.openTrackInformation(target)
            }
        }
    }
}

/** Keep sheet composition and extraction outside PlayerMorphHost. Playback changes do not retarget it. */
@Composable
internal fun NowPlayingTrackInfoOverlay(overlayState: MusicOverlayState, canPresent: Boolean) {
    val visible = overlayState.isTrackInformationVisible.value
    val request = overlayState.trackInfoRequest
    LaunchedEffect(visible, request, canPresent) {
        if (visible && (!canPresent || request == null)) overlayState.dismissTrackInformation()
    }
    if (visible && canPresent && request != null) {
        NowPlayingTrackInfoSheet(request, overlayState::dismissTrackInformation)
    }
}

@Composable
internal fun rememberTrackInfoAudio(
    request: NowPlayingTrackInfoRequest,
    load: suspend (Song) -> AudioQualityInfo
): AudioQualityInfo? {
    // Each request gets a separate state cell. Even a non-cooperative old load cannot fill the new cell.
    var audio by remember(request) { mutableStateOf<AudioQualityInfo?>(null) }
    LaunchedEffect(request) {
        audio = try {
            load(request.song)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }
    return audio
}
