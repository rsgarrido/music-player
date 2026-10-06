package io.github.rsgarrido.sazanami.external

import java.util.Locale

internal const val EXTERNAL_AUDIO_SEEK_INCREMENT_MS = 10_000L
internal const val EXTERNAL_AUDIO_VIEW_ACTION = "android.intent.action.VIEW"

internal enum class ExternalAudioFailure {
    INVALID_REQUEST,
    UNSUPPORTED_URI,
    UNSUPPORTED_AUDIO,
    INACCESSIBLE_AUDIO,
    PLAYBACK_FAILED
}

/** Plain values keep exported-intent validation and seek policy testable on the JVM. */
internal data class ExternalAudioRequestEnvelope(
    val action: String?,
    val hasUri: Boolean,
    val scheme: String?,
    val authority: String?,
    val isHierarchical: Boolean,
    val mimeType: String?
)

internal fun validateExternalAudioRequest(
    request: ExternalAudioRequestEnvelope
): ExternalAudioFailure? = when {
    request.action != EXTERNAL_AUDIO_VIEW_ACTION || !request.hasUri ->
        ExternalAudioFailure.INVALID_REQUEST
    request.scheme?.lowercase(Locale.ROOT) != "content" ||
        !request.isHierarchical || request.authority.isNullOrBlank() ->
        ExternalAudioFailure.UNSUPPORTED_URI
    !isUnspecifiedExternalAudioMime(request.mimeType) &&
        !isSupportedExternalAudioMime(request.mimeType) ->
        ExternalAudioFailure.UNSUPPORTED_AUDIO
    else -> null
}

internal fun normalizedExternalAudioMime(mimeType: String?): String? = mimeType
    ?.substringBefore(';')
    ?.trim()
    ?.lowercase(Locale.ROOT)
    ?.takeIf(String::isNotEmpty)

internal fun isSupportedExternalAudioMime(mimeType: String?): Boolean {
    val normalized = normalizedExternalAudioMime(mimeType) ?: return false
    return normalized == "application/ogg" || normalized.matches(AUDIO_MIME_PATTERN)
}

internal fun isUnspecifiedExternalAudioMime(mimeType: String?): Boolean =
    normalizedExternalAudioMime(mimeType) in setOf(null, "application/octet-stream")

/** Concrete non-audio provider types cannot be overridden by a misleading incoming audio type. */
internal fun acceptsExternalAudioSource(
    intentMimeType: String?,
    providerMimeType: String?,
    displayName: String
): Boolean {
    if (listOf(intentMimeType, providerMimeType).any { mime ->
            !isUnspecifiedExternalAudioMime(mime) && !isSupportedExternalAudioMime(mime)
        }) return false
    if (isSupportedExternalAudioMime(intentMimeType) ||
        isSupportedExternalAudioMime(providerMimeType)
    ) return true
    // Some providers omit MIME information. Only known local audio filenames get this fallback;
    // Media3 must still recognize and decode the actual bytes.
    return displayName.substringAfterLast('.', "").lowercase(Locale.ROOT) in AUDIO_EXTENSIONS
}

internal fun resolveExternalAudioDisplayName(
    providerDisplayName: String?,
    lastPathSegment: String?,
    fallback: String
): String = sequenceOf(providerDisplayName, lastPathSegment, fallback, "External audio")
    .filterNotNull()
    .map { value -> value.replace(DISPLAY_NAME_CONTROLS, "").trim().take(512) }
    .first(String::isNotBlank)

internal fun knownExternalAudioDuration(durationMs: Long): Long? =
    durationMs.takeIf { it > 0L }

internal fun canSeekExternalAudio(isSeekable: Boolean, durationMs: Long?): Boolean =
    isSeekable && durationMs != null && durationMs > 0L

internal fun externalAudioRecreationPosition(positionMs: Long, isSeekable: Boolean): Long =
    if (isSeekable) positionMs.coerceAtLeast(0L) else 0L

internal fun externalAudioSeekBack(positionMs: Long): Long =
    (positionMs.coerceAtLeast(0L) - EXTERNAL_AUDIO_SEEK_INCREMENT_MS).coerceAtLeast(0L)

internal fun externalAudioSeekForward(positionMs: Long, durationMs: Long?): Long? {
    val duration = durationMs?.takeIf { it > 0L } ?: return null
    val position = positionMs.coerceIn(0L, duration)
    return position + minOf(EXTERNAL_AUDIO_SEEK_INCREMENT_MS, duration - position)
}

internal fun externalAudioSeekPosition(fraction: Float, durationMs: Long?): Long? {
    val duration = durationMs?.takeIf { it > 0L } ?: return null
    if (!fraction.isFinite()) return null
    return (fraction.coerceIn(0f, 1f).toDouble() * duration)
        .toLong().coerceIn(0L, duration)
}

internal fun formatExternalAudioTime(positionMs: Long): String {
    val seconds = positionMs.coerceAtLeast(0L) / 1_000L
    val minutes = seconds / 60L
    return "$minutes:${(seconds % 60L).toString().padStart(2, '0')}"
}

private val AUDIO_MIME_PATTERN = Regex("audio/[a-z0-9!#$&^_.+*\\-]+")
private val DISPLAY_NAME_CONTROLS = Regex("[\\p{Cc}\\p{Cf}]")
private val AUDIO_EXTENSIONS = setOf(
    "mp3", "m4a", "aac", "wav", "wave", "flac", "ogg", "oga", "opus", "amr", "awb", "3ga"
)
