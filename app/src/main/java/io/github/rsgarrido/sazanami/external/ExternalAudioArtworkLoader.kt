package io.github.rsgarrido.sazanami.external

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal const val EXTERNAL_AUDIO_ARTWORK_SIZE_DP = 80
internal const val EXTERNAL_AUDIO_ARTWORK_MAX_EDGE_PX = 512
internal const val EXTERNAL_AUDIO_ARTWORK_MAX_BYTES = 8 * 1024 * 1024

/** Main-thread request ownership only; no URI, disk cache, or library identity is retained here. */
internal class ExternalAudioArtworkRequest<T> {
    private var currentRequestId: Int? = null
    var artwork: T? = null
        private set

    fun beginRequest(requestId: Int) {
        currentRequestId = requestId
        artwork = null
    }

    fun accept(requestId: Int, result: T?): Boolean {
        if (currentRequestId != requestId) return false
        artwork = result
        return true
    }

    fun clear() {
        currentRequestId = null
        artwork = null
    }
}

/** Metadata/decode failures are presentation failures; cancellation still stops obsolete work. */
internal inline fun <T> externalAudioArtworkOrNull(operation: () -> T?): T? = try {
    operation()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    null
} catch (_: OutOfMemoryError) {
    null
}

internal fun acceptsExternalAudioArtworkBytes(byteCount: Int): Boolean =
    byteCount in 1..EXTERNAL_AUDIO_ARTWORK_MAX_BYTES

/** Power-of-two sampling keeps the longest decoded edge near the UI size, and always <= 512 px. */
internal fun externalAudioArtworkSampleSize(width: Int, height: Int, targetEdgePx: Int): Int? {
    if (width <= 0 || height <= 0 || targetEdgePx <= 0) return null
    val decodeEdgeLimit = minOf(
        targetEdgePx.coerceAtMost(EXTERNAL_AUDIO_ARTWORK_MAX_EDGE_PX) * 2,
        EXTERNAL_AUDIO_ARTWORK_MAX_EDGE_PX
    )
    val longestEdge = maxOf(width, height).toLong()
    var sampleSize = 1
    while ((longestEdge + sampleSize - 1L) / sampleSize > decodeEdgeLimit) {
        sampleSize *= 2
    }
    return sampleSize
}

/** Caller must use a background dispatcher. Bounds inspection never allocates the source bitmap. */
internal fun decodeExternalAudioArtwork(bytes: ByteArray, targetEdgePx: Int): Bitmap? =
    externalAudioArtworkOrNull {
        if (!acceptsExternalAudioArtworkBytes(bytes.size)) return@externalAudioArtworkOrNull null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val sampleSize = externalAudioArtworkSampleSize(bounds.outWidth, bounds.outHeight, targetEdgePx)
            ?: return@externalAudioArtworkOrNull null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: return@externalAudioArtworkOrNull null
        if (bitmap.width > EXTERNAL_AUDIO_ARTWORK_MAX_EDGE_PX ||
            bitmap.height > EXTERNAL_AUDIO_ARTWORK_MAX_EDGE_PX
        ) {
            bitmap.recycle() // Never publish an unexpectedly unsampled decoder result.
            null
        } else bitmap
    }

internal suspend fun loadExternalAudioArtwork(context: Context, uri: Uri, targetEdgePx: Int): Bitmap? =
    withContext(Dispatchers.IO) {
        externalAudioArtworkOrNull {
            ensureActive()
            val retriever = MediaMetadataRetriever()
            val bytes = try {
                retriever.setDataSource(context.applicationContext, uri)
                retriever.embeddedPicture
            } finally {
                // Release on the same background thread even when opening/retrieval fails.
                runCatching { retriever.release() }
            }
            ensureActive()
            if (bytes == null) return@externalAudioArtworkOrNull null
            val bitmap = decodeExternalAudioArtwork(bytes, targetEdgePx)
            try {
                ensureActive()
                bitmap
            } catch (cancelled: CancellationException) {
                bitmap?.recycle() // This result has never been handed to Compose.
                throw cancelled
            }
        }
    }
