package io.github.rsgarrido.sazanami.external

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

/** Local encoded images exercise our decode policy; no file manager or metadata provider needed. */
@RunWith(AndroidJUnit4::class)
class ExternalAudioArtworkInstrumentedTest {
    @Test
    fun largeArtworkIsDecodedAtASampledResolutionWithoutAFullSizeCopy() {
        val bytes = encodeArtwork(width = 2048, height = 1024)
        val decoded = decodeExternalAudioArtwork(bytes, targetEdgePx = 128)
        assertNotNull(decoded)
        val bitmap = checkNotNull(decoded)
        try {
            assertEquals(256, bitmap.width)
            assertEquals(128, bitmap.height)
            assertTrue(bitmap.byteCount <= EXTERNAL_AUDIO_ARTWORK_MAX_EDGE_PX *
                EXTERNAL_AUDIO_ARTWORK_MAX_EDGE_PX * 4)
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun smallArtworkPreservesItsAspectRatioWithoutUpscaling() {
        val decoded = decodeExternalAudioArtwork(encodeArtwork(60, 40), targetEdgePx = 80)
        assertNotNull(decoded)
        val bitmap = checkNotNull(decoded)
        try {
            assertEquals(60, bitmap.width)
            assertEquals(40, bitmap.height)
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun emptyMalformedOrOversizedEncodedPicturesReturnNoArtwork() {
        assertNull(decodeExternalAudioArtwork(byteArrayOf(), 80))
        assertNull(decodeExternalAudioArtwork("not an image".toByteArray(), 80))
        assertNull(decodeExternalAudioArtwork(ByteArray(EXTERNAL_AUDIO_ARTWORK_MAX_BYTES + 1), 80))
    }

    private fun encodeArtwork(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        try {
            bitmap.eraseColor(Color.BLUE)
            return ByteArrayOutputStream().use { output ->
                assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output))
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }
}
