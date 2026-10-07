package io.github.rsgarrido.sazanami.external

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalAudioArtworkLoaderTest {
    @Test
    fun absentArtworkAndExtractionFailuresRemainOptional() {
        assertNull(externalAudioArtworkOrNull<String> { null })
        assertNull(externalAudioArtworkOrNull<String> { throw SecurityException("No metadata access") })
        assertNull(externalAudioArtworkOrNull<String> { throw IllegalArgumentException("Malformed picture") })
        assertNull(externalAudioArtworkOrNull<String> { throw OutOfMemoryError("Decoder allocation failed") })
        assertEquals("cover", externalAudioArtworkOrNull { "cover" })
    }

    @Test
    fun cancellationIsNotConvertedIntoAnArtworkFailure() {
        val cancellation = CancellationException("Preview closed")
        val caught = runCatching {
            externalAudioArtworkOrNull<String> { throw cancellation }
        }.exceptionOrNull()
        assertSame(cancellation, caught)
    }

    @Test
    fun absentArtworkLeavesTheCurrentRequestInPlaceholderState() {
        val request = ExternalAudioArtworkRequest<String>()
        request.beginRequest(1)
        assertNull(request.artwork)
        assertTrue(request.accept(1, null))
        assertNull(request.artwork)
    }

    @Test
    fun newRequestClearsArtworkAndRejectsOldResultsIncludingOldFailures() {
        val request = ExternalAudioArtworkRequest<String>()
        request.beginRequest(1)
        assertTrue(request.accept(1, "first cover"))
        request.beginRequest(2)
        assertNull(request.artwork)
        assertFalse(request.accept(1, "late first cover"))
        assertNull(request.artwork)

        assertTrue(request.accept(2, "second cover"))
        assertFalse(request.accept(1, null))
        assertEquals("second cover", request.artwork)
    }

    @Test
    fun closingClearsReferencesAndRejectsOutstandingResults() {
        val request = ExternalAudioArtworkRequest<String>()
        request.beginRequest(1)
        request.accept(1, "cover")
        request.clear()
        assertNull(request.artwork)
        assertFalse(request.accept(1, "late cover"))
    }

    @Test
    fun encodedArtworkHasABoundedByteBudget() {
        assertFalse(acceptsExternalAudioArtworkBytes(0))
        assertFalse(acceptsExternalAudioArtworkBytes(-1))
        assertTrue(acceptsExternalAudioArtworkBytes(1))
        assertTrue(acceptsExternalAudioArtworkBytes(EXTERNAL_AUDIO_ARTWORK_MAX_BYTES))
        assertFalse(acceptsExternalAudioArtworkBytes(EXTERNAL_AUDIO_ARTWORK_MAX_BYTES + 1))
        assertFalse(acceptsExternalAudioArtworkBytes(Int.MAX_VALUE))
    }

    @Test
    fun downsamplingKeepsSmallArtworkAndBoundsLargeOrNonSquareArtwork() {
        assertEquals(1, externalAudioArtworkSampleSize(80, 60, 80))
        assertEquals(8, externalAudioArtworkSampleSize(800, 800, 80))
        assertEquals(8, externalAudioArtworkSampleSize(2048, 1024, 128))
        assertEquals(8, externalAudioArtworkSampleSize(4096, 4096, 512))
        assertEquals(128, externalAudioArtworkSampleSize(12000, 600, 80))
    }

    @Test
    fun invalidImageDimensionsDoNotReachThePixelDecoder() {
        assertNull(externalAudioArtworkSampleSize(0, 80, 80))
        assertNull(externalAudioArtworkSampleSize(80, -1, 80))
        assertNull(externalAudioArtworkSampleSize(80, 80, 0))
    }

    @Test
    fun extremeDimensionsAndTargetsCannotOverflowTheSampleCalculation() {
        listOf(1, 80, 320, 512, Int.MAX_VALUE).forEach { target ->
            val sample = checkNotNull(externalAudioArtworkSampleSize(Int.MAX_VALUE, 1, target))
            assertTrue(sample > 0 && sample and (sample - 1) == 0)
            val decodedWidth = (Int.MAX_VALUE.toLong() + sample - 1) / sample
            assertTrue(decodedWidth <= EXTERNAL_AUDIO_ARTWORK_MAX_EDGE_PX)
            assertTrue(decodedWidth <= target.coerceAtMost(512) * 2L)
        }
    }
}
