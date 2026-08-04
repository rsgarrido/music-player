package com.example.cdplaya.data

import com.example.cdplaya.data.local.ListeningEndReason
import com.example.cdplaya.data.local.ListeningEventEntity
import com.example.cdplaya.data.local.ListeningHistoryTypeConverters
import com.example.cdplaya.data.local.ListeningQualificationReason
import com.example.cdplaya.data.local.ListeningSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ListeningHistoryFoundationModelsTest {
    private val converters = ListeningHistoryTypeConverters()

    @Test
    fun enumConvertersPersistExplicitStableTextValues() {
        assertEquals("cdplaya", converters.listeningSourceToString(ListeningSource.CDPLAYA))
        assertEquals(
            ListeningSource.SPOTIFY_IMPORT,
            converters.stringToListeningSource("spotify_import")
        )
        assertEquals(
            "time_threshold",
            converters.qualificationReasonToString(ListeningQualificationReason.TIME_THRESHOLD)
        )
        assertEquals(
            ListeningQualificationReason.NATURAL_END,
            converters.stringToQualificationReason("natural_end")
        )
        assertEquals("transition", converters.endReasonToString(ListeningEndReason.TRANSITION))
        assertEquals(ListeningEndReason.UNKNOWN, converters.stringToEndReason("unknown"))
    }

    @Test
    fun invalidEnumStorageValueIsRejected() {
        assertThrows(IllegalStateException::class.java) {
            converters.stringToListeningSource("future_source_not_supported_by_this_build")
        }
    }

    @Test
    fun listenedTimeMayExceedDurationButCannotBeNegative() {
        val replayedSections = event(listenedMs = 240_000L, durationMs = 180_000L)
        assertEquals(240_000L, replayedSections.listenedMs)

        assertThrows(IllegalArgumentException::class.java) {
            event(listenedMs = -1L, durationMs = 180_000L)
        }
    }

    @Test
    fun finalizedEventCannotEndBeforeItStarts() {
        assertThrows(IllegalArgumentException::class.java) {
            event(listenedMs = 1L, durationMs = 180_000L, startedAt = 2_000L, endedAt = 1_999L)
        }
    }

    private fun event(
        listenedMs: Long,
        durationMs: Long?,
        startedAt: Long = 1_000L,
        endedAt: Long = 2_000L
    ) = ListeningEventEntity(
        eventUuid = "event-uuid",
        source = ListeningSource.CDPLAYA,
        trackIdentityId = 1L,
        localTrackBindingId = null,
        playbackSessionId = null,
        startedAt = startedAt,
        endedAt = endedAt,
        listenedMs = listenedMs,
        trackDurationMs = durationMs,
        qualifiedAsPlay = false,
        qualificationReason = ListeningQualificationReason.NONE,
        qualificationRuleVersion = 1,
        endReason = ListeningEndReason.UNKNOWN,
        sourceEventKey = null,
        importBatchId = null,
        createdAt = 2_000L
    )
}
