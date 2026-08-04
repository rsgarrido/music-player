package com.example.cdplaya.ui.state

import com.example.cdplaya.data.AlbumListeningStats
import com.example.cdplaya.data.AnalyticsRangeSelection
import com.example.cdplaya.data.ArtistListeningStats
import com.example.cdplaya.data.ListeningAnalyticsCoverage
import com.example.cdplaya.data.ListeningOverview
import com.example.cdplaya.data.ListeningTrendBucket
import com.example.cdplaya.data.ListeningTrendMetric
import com.example.cdplaya.data.ResolvedAnalyticsRange
import com.example.cdplaya.data.TrackListeningStats

enum class ListeningAnalyticsErrorKind {
    RANGE_RESOLUTION,
    SNAPSHOT_LOAD
}

data class ListeningAnalyticsError(
    val kind: ListeningAnalyticsErrorKind,
    val retryable: Boolean,
    val cause: Throwable
)

data class ListeningAnalyticsUiState(
    val selectedRange: AnalyticsRangeSelection = AnalyticsRangeSelection.Default,
    val resolvedRange: ResolvedAnalyticsRange? = null,
    val overview: ListeningOverview? = null,
    val trend: List<ListeningTrendBucket> = emptyList(),
    val topTracks: List<TrackListeningStats> = emptyList(),
    val topAlbums: List<AlbumListeningStats> = emptyList(),
    val topArtists: List<ArtistListeningStats> = emptyList(),
    val coverage: ListeningAnalyticsCoverage? = null,
    val trendMetric: ListeningTrendMetric = ListeningTrendMetric.RECORDED_LISTENING_TIME,
    val isActive: Boolean = false,
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: ListeningAnalyticsError? = null
)
