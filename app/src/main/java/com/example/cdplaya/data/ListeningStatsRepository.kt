package com.example.cdplaya.data

import com.example.cdplaya.data.local.AlbumListeningStatsRow
import com.example.cdplaya.data.local.ArtistListeningStatsRow
import com.example.cdplaya.data.local.ListeningEndReason
import com.example.cdplaya.data.local.ListeningOverviewRow
import com.example.cdplaya.data.local.ListeningQualificationReason
import com.example.cdplaya.data.local.ListeningSource
import com.example.cdplaya.data.local.ListeningStatsDao
import com.example.cdplaya.data.local.ListeningStatsQueries
import com.example.cdplaya.data.local.ListeningStatsQuerySpec
import com.example.cdplaya.data.local.RecentListeningEventRow
import com.example.cdplaya.data.local.TrackListeningStatsRow

class ListeningStatsRepository(
    private val dao: ListeningStatsDao
) {
    suspend fun getAllTimeOverview(
        sources: Set<ListeningSource>? = null,
        includeLegacyBaseline: Boolean = true
    ): ListeningOverview = getOverview(
        ListeningStatsFilter(
            sources = sources,
            includeLegacyBaseline = includeLegacyBaseline
        )
    )

    suspend fun getDetailedOverview(
        range: ListeningDateRange,
        sources: Set<ListeningSource>? = null
    ): ListeningOverview = getOverview(
        ListeningStatsFilter(
            dateRange = range,
            sources = sources,
            includeLegacyBaseline = false
        )
    )

    suspend fun getOverview(filter: ListeningStatsFilter): ListeningOverview =
        dao.getOverview(ListeningStatsQueries.overview(filter.toSpec())).toDomain()

    suspend fun getTopTracksByQualifiedPlays(
        limit: Int,
        filter: ListeningStatsFilter = ListeningStatsFilter()
    ): List<TrackListeningStats> = dao.getTrackStats(
        ListeningStatsQueries.tracks(
            spec = filter.toSpec(),
            orderByListeningTime = false,
            qualifiedOnly = true,
            limit = checkedLimit(limit)
        )
    ).map(TrackListeningStatsRow::toDomain)

    suspend fun getTopTracksByListeningTime(
        limit: Int,
        filter: ListeningStatsFilter = ListeningStatsFilter(includeLegacyBaseline = false)
    ): List<TrackListeningStats> = dao.getTrackStats(
        ListeningStatsQueries.tracks(
            spec = filter.toSpec(),
            orderByListeningTime = true,
            qualifiedOnly = false,
            limit = checkedLimit(limit)
        )
    ).map(TrackListeningStatsRow::toDomain)

    suspend fun getTopAlbums(
        limit: Int,
        filter: ListeningStatsFilter = ListeningStatsFilter()
    ): List<AlbumListeningStats> = dao.getAlbumStats(
        ListeningStatsQueries.albums(filter.toSpec(), checkedLimit(limit))
    ).map(AlbumListeningStatsRow::toDomain)

    suspend fun getTopArtists(
        limit: Int,
        filter: ListeningStatsFilter = ListeningStatsFilter()
    ): List<ArtistListeningStats> = dao.getArtistStats(
        ListeningStatsQueries.artists(filter.toSpec(), checkedLimit(limit))
    ).map(ArtistListeningStatsRow::toDomain)

    suspend fun getRecentlyPlayed(
        limit: Int,
        filter: ListeningStatsFilter = ListeningStatsFilter()
    ): List<RecentlyPlayedProjection> = dao.getTrackStats(
        ListeningStatsQueries.recentlyPlayed(filter.toSpec(), checkedLimit(limit))
    ).map { RecentlyPlayedProjection(it.toDomain()) }

    suspend fun getMostPlayed(
        limit: Int,
        filter: ListeningStatsFilter = ListeningStatsFilter()
    ): List<MostPlayedProjection> = dao.getTrackStats(
        ListeningStatsQueries.tracks(
            spec = filter.toSpec(),
            orderByListeningTime = false,
            qualifiedOnly = true,
            limit = checkedLimit(limit)
        )
    ).map { MostPlayedProjection(it.toDomain()) }

    suspend fun getRecentDetailedEvents(
        limit: Int,
        range: ListeningDateRange? = null,
        sources: Set<ListeningSource>? = null
    ): List<RecentListeningEvent> {
        val filter = ListeningStatsFilter(
            dateRange = range,
            sources = sources,
            includeLegacyBaseline = false
        )
        return dao.getRecentEvents(
            ListeningStatsQueries.recentEvents(filter.toSpec(), checkedLimit(limit))
        ).map(RecentListeningEventRow::toDomain)
    }

    private fun ListeningStatsFilter.toSpec() = ListeningStatsQuerySpec(
        startInclusive = dateRange?.startInclusive,
        endExclusive = dateRange?.endExclusive,
        sourceStorageValues = (sources ?: ListeningSource.entries.toSet())
            .map(ListeningSource::storageValue)
            .sorted(),
        includeLegacyBaseline = effectiveIncludeLegacy
    )

    private fun checkedLimit(limit: Int): Int {
        require(limit in 1..MAX_RESULT_LIMIT) { "Result limit must be between 1 and $MAX_RESULT_LIMIT" }
        return limit
    }

    private companion object {
        const val MAX_RESULT_LIMIT = 10_000
    }
}

private fun ListeningOverviewRow.toDomain(): ListeningOverview {
    val total = safeAdd(legacyPlayCount, detailedQualifiedPlayCount)
    return ListeningOverview(
        playCounts = ListeningPlayCountBreakdown(total, legacyPlayCount, detailedQualifiedPlayCount),
        listeningTime = ListeningTimeBreakdown(
            confirmedDetailedListeningMs = detailedListeningMs,
            legacyPlayCountWithoutKnownDuration = legacyPlayCount
        ),
        qualifiedDetailedPlayCount = detailedQualifiedPlayCount,
        naturalCompletionCount = naturalCompletionCount,
        nonQualifiedAttemptCount = nonQualifiedAttemptCount,
        detailedEventCount = detailedEventCount,
        firstDetailedEventAt = firstDetailedEventAt,
        latestDetailedEventAt = latestDetailedEventAt,
        firstKnownPlayAt = firstKnownPlayAt,
        latestKnownPlayAt = latestKnownPlayAt,
        hasLegacyBaseline = legacyIdentityCount > 0L
    )
}

private fun TrackListeningStatsRow.toDomain(): TrackListeningStats {
    val total = safeAdd(legacyPlayCount, detailedQualifiedPlayCount)
    val binding = localTrackBindingId?.let { bindingId ->
        ListeningBindingSnapshot(
            localTrackBindingId = bindingId,
            referenceKey = requireNotNull(referenceKey),
            mediaStoreId = mediaStoreId,
            volumeName = volumeName,
            contentUri = contentUri,
            relativePath = relativePath,
            displayName = displayName,
            fileSizeBytes = fileSizeBytes,
            dateModifiedEpochSeconds = dateModifiedEpochSeconds,
            missingSince = missingSince
        )
    }
    return TrackListeningStats(
        trackIdentityId = trackIdentityId,
        title = titleSnapshot,
        artist = artistSnapshot,
        album = albumSnapshot,
        albumArtist = albumArtistSnapshot,
        durationMs = durationMsSnapshot,
        binding = binding,
        playCounts = ListeningPlayCountBreakdown(total, legacyPlayCount, detailedQualifiedPlayCount),
        confirmedDetailedListeningMs = detailedListeningMs,
        detailedEventCount = detailedEventCount,
        naturalCompletionCount = naturalCompletionCount,
        nonQualifiedAttemptCount = nonQualifiedAttemptCount,
        firstKnownPlayAt = firstKnownPlayAt,
        latestKnownPlayAt = latestKnownPlayAt,
        latestDetailedEventAt = latestDetailedEventAt
    )
}

private fun AlbumListeningStatsRow.toDomain(): AlbumListeningStats {
    val total = safeAdd(legacyPlayCount, detailedQualifiedPlayCount)
    return AlbumListeningStats(
        groupingKey = groupingKey,
        album = displayAlbum,
        albumArtist = displayAlbumArtist,
        playCounts = ListeningPlayCountBreakdown(total, legacyPlayCount, detailedQualifiedPlayCount),
        confirmedDetailedListeningMs = detailedListeningMs,
        naturalCompletionCount = naturalCompletionCount,
        trackCount = trackCount,
        latestKnownPlayAt = latestKnownPlayAt
    )
}

private fun ArtistListeningStatsRow.toDomain(): ArtistListeningStats {
    val total = safeAdd(legacyPlayCount, detailedQualifiedPlayCount)
    return ArtistListeningStats(
        groupingKey = groupingKey,
        artist = displayArtist,
        playCounts = ListeningPlayCountBreakdown(total, legacyPlayCount, detailedQualifiedPlayCount),
        confirmedDetailedListeningMs = detailedListeningMs,
        naturalCompletionCount = naturalCompletionCount,
        distinctTrackCount = distinctTrackCount,
        distinctAlbumCount = distinctAlbumCount,
        latestKnownPlayAt = latestKnownPlayAt
    )
}

private fun RecentListeningEventRow.toDomain() = RecentListeningEvent(
    eventUuid = eventUuid,
    trackIdentityId = trackIdentityId,
    title = titleSnapshot,
    artist = artistSnapshot,
    album = albumSnapshot,
    source = ListeningSource.fromStorageValue(source),
    startedAt = startedAt,
    endedAt = endedAt,
    listenedMs = listenedMs,
    qualifiedAsPlay = qualifiedAsPlay,
    qualificationReason = ListeningQualificationReason.fromStorageValue(qualificationReason),
    endReason = ListeningEndReason.fromStorageValue(endReason),
    playbackSessionId = playbackSessionId
)

private fun safeAdd(left: Long, right: Long): Long =
    if (right > 0L && left > Long.MAX_VALUE - right) Long.MAX_VALUE else left + right
