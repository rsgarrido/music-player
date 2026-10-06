package io.github.rsgarrido.sazanami.external

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalAudioPolicyTest {
    private val validRequest = ExternalAudioRequestEnvelope(
        action = EXTERNAL_AUDIO_VIEW_ACTION,
        hasUri = true,
        scheme = "content",
        authority = "files.example",
        isHierarchical = true,
        mimeType = "audio/ogg"
    )

    @Test
    fun acceptsViewAudioAndApplicationOggContentRequests() {
        listOf("audio/mpeg", "audio/mp4", "audio/wav", "audio/flac", "audio/opus", "audio/amr",
            "audio/*", "application/ogg", " Audio/OGG ; codecs=opus ").forEach { mime ->
            assertNull(mime, validateExternalAudioRequest(validRequest.copy(mimeType = mime)))
        }
    }

    @Test
    fun rejectsNonViewActionsAndMissingUri() {
        listOf(null, "android.intent.action.SEND", "android.intent.action.SEND_MULTIPLE",
            "android.intent.action.MAIN").forEach { action ->
            assertEquals(ExternalAudioFailure.INVALID_REQUEST,
                validateExternalAudioRequest(validRequest.copy(action = action)))
        }
        assertEquals(ExternalAudioFailure.INVALID_REQUEST,
            validateExternalAudioRequest(validRequest.copy(hasUri = false)))
    }

    @Test
    fun rejectsNetworkFileResourceAndMalformedContentUris() {
        listOf(null, "http", "https", "file", "android.resource", "data", "ftp").forEach { scheme ->
            assertEquals(scheme, ExternalAudioFailure.UNSUPPORTED_URI,
                validateExternalAudioRequest(validRequest.copy(scheme = scheme)))
        }
        assertEquals(ExternalAudioFailure.UNSUPPORTED_URI,
            validateExternalAudioRequest(validRequest.copy(authority = "")))
        assertEquals(ExternalAudioFailure.UNSUPPORTED_URI,
            validateExternalAudioRequest(validRequest.copy(isHierarchical = false)))
    }

    @Test
    fun rejectsConcreteNonAudioAndMalformedMimeTypes() {
        listOf("video/mp4", "image/jpeg", "text/plain", "application/pdf", "audio", "audio/",
            "audio/ogg/extra").forEach { mime ->
            assertFalse(mime, isSupportedExternalAudioMime(mime))
            assertEquals(mime, ExternalAudioFailure.UNSUPPORTED_AUDIO,
                validateExternalAudioRequest(validRequest.copy(mimeType = mime)))
        }
    }

    @Test
    fun unknownMimeRequiresProviderAudioTypeOrKnownAudioFilename() {
        assertNull(validateExternalAudioRequest(validRequest.copy(mimeType = null)))
        assertTrue(acceptsExternalAudioSource(null, "application/ogg", "42"))
        assertTrue(acceptsExternalAudioSource("application/octet-stream", null, "Voice note.OPUS"))
        assertTrue(acceptsExternalAudioSource(null, null, "Recording.m4a"))
        assertFalse(acceptsExternalAudioSource(null, null, "42"))
        assertFalse(acceptsExternalAudioSource(null, null, "External audio"))
        assertFalse(acceptsExternalAudioSource(null, null, "playlist.m3u8"))
        assertFalse(acceptsExternalAudioSource(null, null, "document.pdf"))
    }

    @Test
    fun concreteProviderTypeCannotBeSpoofedByIntentMimeOrFilename() {
        assertFalse(acceptsExternalAudioSource("audio/mpeg", "image/jpeg", "recording.mp3"))
        assertFalse(acceptsExternalAudioSource("text/plain", "audio/ogg", "recording.ogg"))
    }

    @Test
    fun filenamePrefersProviderThenSegmentThenLocalizedFallback() {
        assertEquals("WhatsApp note.ogg", resolveExternalAudioDisplayName(
            " WhatsApp note.ogg ", "42", "Audio externo"))
        assertEquals("voice.opus", resolveExternalAudioDisplayName(null, "voice.opus", "Audio externo"))
        assertEquals("Audio externo", resolveExternalAudioDisplayName(" ", null, "Audio externo"))
        assertEquals("Audio externo", resolveExternalAudioDisplayName("\n\u202e", " ", "Audio externo"))
        assertEquals("External audio", resolveExternalAudioDisplayName(null, null, ""))
    }

    @Test
    fun filenameRemovesControlsAndBoundsUntrustedProviderText() {
        assertEquals("voicenote.ogg", resolveExternalAudioDisplayName(
            "voice\n\u202enote.ogg", null, "External audio"))
        assertEquals(512, resolveExternalAudioDisplayName("x".repeat(10_000), null, "External audio").length)
    }

    @Test
    fun backwardSeekClampsAtZero() {
        assertEquals(0L, externalAudioSeekBack(-5L))
        assertEquals(0L, externalAudioSeekBack(0L))
        assertEquals(0L, externalAudioSeekBack(9_999L))
        assertEquals(8_000L, externalAudioSeekBack(18_000L))
    }

    @Test
    fun forwardSeekClampsToKnownDurationWithoutOverflow() {
        assertEquals(28_000L, externalAudioSeekForward(18_000L, 102_000L))
        assertEquals(102_000L, externalAudioSeekForward(100_000L, 102_000L))
        assertEquals(102_000L, externalAudioSeekForward(200_000L, 102_000L))
        assertEquals(10_000L, externalAudioSeekForward(-100L, 102_000L))
        assertEquals(Long.MAX_VALUE, externalAudioSeekForward(Long.MAX_VALUE - 5L, Long.MAX_VALUE))
    }

    @Test
    fun unknownDurationAndNonSeekableAudioDisableSeeking() {
        listOf(0L, -1L, -9223372036854775807L).forEach { duration ->
            assertNull(knownExternalAudioDuration(duration))
            assertNull(externalAudioSeekForward(18_000L, duration))
            assertNull(externalAudioSeekPosition(0.5f, duration))
        }
        assertNull(externalAudioSeekForward(18_000L, null))
        assertFalse(canSeekExternalAudio(true, null))
        assertFalse(canSeekExternalAudio(false, 102_000L))
        assertTrue(canSeekExternalAudio(true, 102_000L))
    }

    @Test
    fun recreationPreservesSeekablePositionButReopensNonSeekableStreamsAtZero() {
        assertEquals(18_000L, externalAudioRecreationPosition(18_000L, true))
        assertEquals(0L, externalAudioRecreationPosition(-1L, true))
        assertEquals(0L, externalAudioRecreationPosition(18_000L, false))
    }

    @Test
    fun sliderSeeksClampAndRejectNonFiniteInput() {
        assertEquals(51_000L, externalAudioSeekPosition(0.5f, 102_000L))
        assertEquals(0L, externalAudioSeekPosition(-1f, 102_000L))
        assertEquals(102_000L, externalAudioSeekPosition(2f, 102_000L))
        assertNull(externalAudioSeekPosition(Float.NaN, 102_000L))
        assertNull(externalAudioSeekPosition(Float.POSITIVE_INFINITY, 102_000L))
        assertNull(externalAudioSeekPosition(0.5f, null))
    }

    @Test
    fun formatsTimeWithoutIntOverflowOrNegativeValues() {
        assertEquals("0:00", formatExternalAudioTime(-1L))
        assertEquals("0:18", formatExternalAudioTime(18_999L))
        assertEquals("1:42", formatExternalAudioTime(102_000L))
        assertEquals("60:01", formatExternalAudioTime(3_601_000L))
        assertEquals("50000:00", formatExternalAudioTime(3_000_000_000L))
    }
}
