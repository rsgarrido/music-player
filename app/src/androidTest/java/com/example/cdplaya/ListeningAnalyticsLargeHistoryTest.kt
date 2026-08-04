package com.example.cdplaya

import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cdplaya.data.AnalyticsBucketBoundary
import com.example.cdplaya.data.AnalyticsBucketGranularity
import com.example.cdplaya.data.AnalyticsRangeSelection
import com.example.cdplaya.data.AnalyticsZoneIdProvider
import com.example.cdplaya.data.ListeningAnalyticsBucketBuilder
import com.example.cdplaya.data.ListeningAnalyticsRangeResolver
import com.example.cdplaya.data.ListeningStatsRepository
import com.example.cdplaya.data.local.AppDatabase
import com.example.cdplaya.data.local.ListeningEndReason
import com.example.cdplaya.data.local.ListeningEventEntity
import com.example.cdplaya.data.local.ListeningQualificationReason
import com.example.cdplaya.data.local.ListeningSource
import com.example.cdplaya.data.local.ListeningStatsQueries
import com.example.cdplaya.data.local.ListeningTrackIdentityEntity
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListeningAnalyticsLargeHistoryTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun tenThousandEventsAggregateExactlyThroughBoundedRealQuery() = runBlocking {
        val id = database.listeningTrackIdentityDao().insert(
            ListeningTrackIdentityEntity(
                titleSnapshot = "Bulk",
                artistSnapshot = "Scale",
                albumSnapshot = "History",
                albumArtistSnapshot = "Scale",
                durationMsSnapshot = 100L,
                normalizedTitle = "bulk",
                normalizedArtist = "scale",
                normalizedAlbum = "history",
                metadataKey = null,
                metadataKeyVersion = 1,
                createdAt = 1L,
                updatedAt = 1L
            )
        )
        val base = Instant.parse("1500-01-01T00:00:00Z").toEpochMilli()
        val events = (0 until 10_000).map { index ->
            ListeningEventEntity(
                eventUuid = "analytics-bulk-$index",
                source = ListeningSource.CDPLAYA,
                trackIdentityId = id,
                localTrackBindingId = null,
                playbackSessionId = null,
                startedAt = base + index,
                endedAt = base + index + 10L,
                listenedMs = 10L,
                trackDurationMs = 100L,
                qualifiedAsPlay = index % 2 == 0,
                qualificationReason = if (index % 2 == 0) ListeningQualificationReason.TIME_THRESHOLD else ListeningQualificationReason.NONE,
                qualificationRuleVersion = 1,
                endReason = if (index % 4 == 0) ListeningEndReason.NATURAL_END else ListeningEndReason.STOPPED,
                sourceEventKey = null,
                importBatchId = null,
                createdAt = base + index + 10L
            )
        }
        events.chunked(1_000).forEach { database.listeningEventDao().insert(it) }
        val resolver = ListeningAnalyticsRangeResolver(
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC")),
            AnalyticsZoneIdProvider { ZoneId.of("UTC") }
        )
        val selection = AnalyticsRangeSelection.Custom(LocalDate.of(1200, 1, 1), LocalDate.of(2026, 12, 31))
        val resolved = resolver.resolve(selection)
        val boundaries = ListeningAnalyticsBucketBuilder.build(resolved)
        assertTrue(boundaries.size <= 400)

        val snapshot = ListeningStatsRepository(database).getAnalyticsSnapshot(resolved)
        assertEquals(10_000L, snapshot.trend.sumOf { it.totalAttemptCount })
        assertEquals(5_000L, snapshot.trend.sumOf { it.qualifiedPlayCount })
        assertEquals(2_500L, snapshot.trend.sumOf { it.naturalCompletionCount })
        assertEquals(100_000L, snapshot.trend.sumOf { it.listenedMs })
        assertEquals(boundaries.size, snapshot.trend.size)
        assertTrue(snapshot.trend.any { it.totalAttemptCount == 0L })
    }

    @Test
    fun existingCompositeIndexesCoverSourceDateAndQualifiedDatePlans() {
        val sourcePlan = queryPlan(
            "SELECT id FROM listening_events WHERE source = 'cdplaya' AND startedAt >= 100 AND startedAt < 200"
        )
        val qualifiedPlan = queryPlan(
            "SELECT id FROM listening_events WHERE qualifiedAsPlay = 1 AND startedAt >= 100 AND startedAt < 200"
        )
        val trackPlan = queryPlan(
            "SELECT id FROM listening_events WHERE trackIdentityId = 1 AND startedAt >= 100 AND startedAt < 200"
        )
        val bucketJoinPlan = queryPlan(
            "WITH buckets(startInclusive, endExclusive) AS (VALUES (100, 200)) " +
                "SELECT COUNT(e.id) FROM buckets b LEFT JOIN listening_events e " +
                "ON e.source = 'cdplaya' AND e.startedAt >= b.startInclusive AND e.startedAt < b.endExclusive"
        )
        assertTrue(sourcePlan.contains("index_listening_events_source_startedAt"))
        assertTrue(qualifiedPlan.contains("index_listening_events_qualifiedAsPlay_startedAt"))
        assertTrue(trackPlan.contains("index_listening_events_trackIdentityId_startedAt"))
        assertTrue(bucketJoinPlan.contains("index_listening_events_source_startedAt"))
    }

    @Test
    fun maximumFourHundredBucketQueryStaysWithinSqliteBindLimit() = runBlocking {
        val boundaries = (0 until ListeningAnalyticsBucketBuilder.MAX_BUCKET_COUNT).map { index ->
            AnalyticsBucketBoundary(
                index = index,
                startInclusive = index.toLong(),
                endExclusive = index.toLong() + 1L,
                localStart = Instant.ofEpochMilli(index.toLong()).atZone(ZoneId.of("UTC")),
                granularity = AnalyticsBucketGranularity.DAY
            )
        }
        val rows = database.listeningStatsDao().getTrendBuckets(
            ListeningStatsQueries.trend(
                boundaries,
                ListeningSource.entries.map(ListeningSource::storageValue)
            )
        )
        assertEquals(400, rows.size)
        assertTrue(rows.all { it.totalAttemptCount == 0L && it.listenedMs == 0L })
    }

    private fun queryPlan(sql: String): String = database.query(SimpleSQLiteQuery("EXPLAIN QUERY PLAN $sql")).use { cursor ->
        buildList {
            val column = cursor.getColumnIndexOrThrow("detail")
            while (cursor.moveToNext()) add(cursor.getString(column))
        }.joinToString()
    }
}
