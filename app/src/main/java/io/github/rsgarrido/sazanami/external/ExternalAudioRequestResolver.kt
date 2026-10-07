package io.github.rsgarrido.sazanami.external

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.CancellationSignal
import android.provider.OpenableColumns

internal data class ExternalAudioRequest(val uri: Uri, val displayName: String)

internal class ExternalAudioRequestException(val failure: ExternalAudioFailure) : Exception()

internal fun validateExternalAudioIntent(intent: Intent): ExternalAudioFailure? {
    val uri = intent.data
    return validateExternalAudioRequest(
        ExternalAudioRequestEnvelope(
            action = intent.action,
            hasUri = uri != null,
            scheme = uri?.scheme,
            authority = uri?.authority,
            isHierarchical = uri?.isHierarchical == true,
            mimeType = intent.type
        )
    )
}

/** Blocking provider calls belong on Dispatchers.IO, never the Activity's main thread. */
internal fun resolveExternalAudioRequest(
    resolver: ContentResolver,
    intent: Intent,
    fallbackName: String,
    cancellationSignal: CancellationSignal
): ExternalAudioRequest {
    validateExternalAudioIntent(intent)?.let { throw ExternalAudioRequestException(it) }
    val uri = intent.data ?: throw ExternalAudioRequestException(ExternalAudioFailure.INVALID_REQUEST)
    cancellationSignal.throwIfCanceled()

    // The sender's Activity grant is sufficient. No library/storage permission or persisted grant.
    try {
        val descriptor = resolver.openAssetFileDescriptor(uri, "r", cancellationSignal)
            ?: throw ExternalAudioRequestException(ExternalAudioFailure.INACCESSIBLE_AUDIO)
        descriptor.use { /* Probe access, without reading or copying the audio. */ }
    } catch (error: ExternalAudioRequestException) {
        throw error
    } catch (error: android.os.OperationCanceledException) {
        throw error
    } catch (_: Exception) {
        throw ExternalAudioRequestException(ExternalAudioFailure.INACCESSIBLE_AUDIO)
    }

    val providerName = try {
        resolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null, cancellationSignal
        )?.use { cursor ->
            val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (column >= 0 && cursor.moveToFirst() && !cursor.isNull(column)) {
                cursor.getString(column)
            } else null
        }
    } catch (error: android.os.OperationCanceledException) {
        throw error
    } catch (_: Exception) {
        null // Filename queries are optional; access and decoding are checked independently.
    }
    val displayName = resolveExternalAudioDisplayName(providerName, uri.lastPathSegment, fallbackName)
    cancellationSignal.throwIfCanceled()
    val providerMime = try {
        resolver.getType(uri)
    } catch (_: Exception) {
        null
    }
    if (!acceptsExternalAudioSource(intent.type, providerMime, displayName)) {
        throw ExternalAudioRequestException(ExternalAudioFailure.UNSUPPORTED_AUDIO)
    }
    cancellationSignal.throwIfCanceled()
    return ExternalAudioRequest(uri, displayName)
}
