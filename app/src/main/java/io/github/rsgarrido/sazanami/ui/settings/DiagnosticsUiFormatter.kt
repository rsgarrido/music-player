package io.github.rsgarrido.sazanami.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.player.audio.AudioCompatibilityConstraint
import io.github.rsgarrido.sazanami.player.audio.AudioOffloadRuntimeState
import io.github.rsgarrido.sazanami.player.audio.AudioOffloadStatus
import io.github.rsgarrido.sazanami.player.audio.AudioOutputUiState
import io.github.rsgarrido.sazanami.player.audio.AudioRouteCategory
import io.github.rsgarrido.sazanami.player.audio.AudioRouteInfo
import io.github.rsgarrido.sazanami.player.audio.AudioSourceFormat
import io.github.rsgarrido.sazanami.player.audio.formatSampleRate
import io.github.rsgarrido.sazanami.player.audio.friendlyCodecName
import io.github.rsgarrido.sazanami.player.equalizer.EqualizerPlanApplicationMode
import io.github.rsgarrido.sazanami.player.equalizer.EqualizerRuntimeState
import java.util.Locale

/** Only the on-screen values are localized; the copied support report keeps its stable English formatter. */
@Composable
internal fun formatAudioSourceUi(format: AudioSourceFormat?): String {
    format ?: return stringResource(R.string.diag_unknown)
    val codec = format.sampleMimeType?.let { friendlyCodecName(it, format.codecs) }
        ?: format.codecs
    val bitDepth = format.sourceBitDepth?.let {
        stringResource(R.string.diag_source_bit_depth, it)
    }
    val sampleRate = format.sampleRateHz?.let(::formatSampleRate)
    val channels = format.channelCount?.let {
        pluralStringResource(R.plurals.diag_source_channels, it, it)
    }
    val bitrate = format.bitrateBitsPerSecond?.let {
        stringResource(R.string.diag_source_bitrate, it / 1_000)
    }
    return listOfNotNull(codec, bitDepth, sampleRate, channels, bitrate)
        .joinToString(" · ")
        .ifBlank { stringResource(R.string.diag_unknown) }
}

@Composable
internal fun formatAudioRouteUi(route: AudioRouteInfo): String = stringResource(
    when (route.category) {
        AudioRouteCategory.BUILT_IN_SPEAKER -> R.string.diag_route_built_in_speaker
        AudioRouteCategory.WIRED_HEADPHONES -> R.string.diag_route_wired
        AudioRouteCategory.USB -> R.string.diag_route_usb
        AudioRouteCategory.BLUETOOTH_CLASSIC -> R.string.diag_route_bluetooth
        AudioRouteCategory.BLUETOOTH_LE -> R.string.diag_route_bluetooth_le
        AudioRouteCategory.HDMI -> R.string.diag_route_hdmi
        AudioRouteCategory.REMOTE_CAST -> R.string.diag_route_remote_cast
        AudioRouteCategory.OTHER -> R.string.diag_route_other
        AudioRouteCategory.UNKNOWN -> R.string.diag_unknown
    }
)

@Composable
internal fun formatAudioOffloadStatusUi(state: AudioOffloadRuntimeState): String = stringResource(
    when (state.status) {
        AudioOffloadStatus.DISABLED -> R.string.diag_offload_disabled
        AudioOffloadStatus.REQUESTED_NOT_ACTIVE -> R.string.diag_offload_requested_inactive
        AudioOffloadStatus.ACTIVE -> R.string.diag_offload_active
        AudioOffloadStatus.ACTIVE_SLEEPING -> R.string.diag_offload_active_sleeping
    }
)

@Composable
internal fun formatAudioCompatibilityUi(state: AudioOutputUiState): String {
    val gapless = state.isGaplessSupportRequired
    val decoded = AudioCompatibilityConstraint.EQUALIZER_REQUIRES_DECODED_PCM in
        state.offloadState.knownCompatibilityConstraints
    return stringResource(when {
        gapless && decoded -> R.string.diag_compat_gapless_equalizer
        gapless -> R.string.diag_compat_gapless
        decoded -> R.string.diag_compat_equalizer
        else -> R.string.diag_compat_basic
    })
}

@Composable
internal fun formatEqualizerStatusUi(state: EqualizerRuntimeState): String = stringResource(
    when {
        !state.requestedEnabled -> R.string.diag_eq_bypassed
        state.transitionInProgress -> R.string.diag_eq_transitioning
        state.effectivelyActive -> R.string.diag_eq_active
        else -> R.string.diag_eq_bypassed
    }
)

@Composable
internal fun formatEqualizerProcessorFormatUi(state: EqualizerRuntimeState): String {
    val sampleRate = state.sampleRateHz ?: return stringResource(R.string.diag_eq_unconfigured)
    val channels = state.channelCount ?: return stringResource(R.string.diag_eq_unconfigured)
    return stringResource(
        R.string.diag_eq_pcm_format,
        formatSampleRate(sampleRate),
        pluralStringResource(R.plurals.diag_source_channels, channels, channels)
    )
}

@Composable
internal fun formatEqualizerPlanApplicationUi(state: EqualizerRuntimeState): String =
    when (state.lastPlanApplicationMode) {
        EqualizerPlanApplicationMode.NONE -> stringResource(R.string.diag_none)
        EqualizerPlanApplicationMode.CROSSFADE -> stringResource(
            R.string.diag_eq_crossfade,
            String.format(Locale.getDefault(), "%.2f", state.lastTransitionDurationMillis),
            state.lastTransitionFrameCount,
            state.lastTransitionSampleRateHz?.let(::formatSampleRate)
                ?: stringResource(R.string.diag_eq_unknown_rate)
        )
        EqualizerPlanApplicationMode.DIRECT_AFTER_FLUSH ->
            stringResource(R.string.diag_eq_direct_after_flush)
        EqualizerPlanApplicationMode.DIRECT_BYPASS ->
            stringResource(R.string.diag_eq_direct_bypass)
    }

@Composable
internal fun formatEqualizerPlanLatencyUi(state: EqualizerRuntimeState): String {
    val preparation = state.planPreparationLatencyMillis
    val application = state.planApplicationLatencyMillis
    return when {
        preparation != null && application != null ->
            stringResource(R.string.diag_eq_latency_both, preparation, application)
        preparation != null ->
            stringResource(R.string.diag_eq_latency_prepared, preparation)
        application != null ->
            stringResource(R.string.diag_eq_latency_applied, application)
        else -> stringResource(R.string.diag_eq_latency_pending)
    }
}
