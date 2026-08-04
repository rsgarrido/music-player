package com.example.cdplaya.data.local

import androidx.room.Dao
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery

data class ListeningOverviewRow(
    val legacyPlayCount: Long,
    val detailedQualifiedPlayCount: Long,
    val detailedListeningMs: Long,
    val naturalCompletionCount: Long,
    val nonQualifiedAttemptCount: Long,
    val detailedEventCount: Long,
    val firstDetailedEventAt: Long?,
    val latestDetailedEventAt: Long?,
    val firstKnownPlayAt: Long?,
    val latestKnownPlayAt: Long?,
    val legacyIdentityCount: Long
)

data class TrackListeningStatsRow(
    val trackIdentityId: Long,
    val titleSnapshot: String,
    val artistSnapshot: String,
    val albumSnapshot: String,
    val albumArtistSnapshot: String?,
    val durationMsSnapshot: Long?,
    val localTrackBindingId: Long?,
    val referenceKey: String?,
    val mediaStoreId: Long?,
    val volumeName: String?,
    val contentUri: String?,
    val relativePath: String?,
    val displayName: String?,
    val fileSizeBytes: Long?,
    val dateModifiedEpochSeconds: Long?,
    val missingSince: Long?,
    val legacyPlayCount: Long,
    val detailedQualifiedPlayCount: Long,
    val detailedListeningMs: Long,
    val detailedEventCount: Long,
    val naturalCompletionCount: Long,
    val nonQualifiedAttemptCount: Long,
    val firstKnownPlayAt: Long?,
    val latestKnownPlayAt: Long?,
    val latestDetailedEventAt: Long?
)

data class AlbumListeningStatsRow(
    val groupingKey: String,
    val displayAlbum: String,
    val displayAlbumArtist: String,
    val legacyPlayCount: Long,
    val detailedQualifiedPlayCount: Long,
    val detailedListeningMs: Long,
    val naturalCompletionCount: Long,
    val trackCount: Long,
    val latestKnownPlayAt: Long?
)

data class ArtistListeningStatsRow(
    val groupingKey: String,
    val displayArtist: String,
    val legacyPlayCount: Long,
    val detailedQualifiedPlayCount: Long,
    val detailedListeningMs: Long,
    val naturalCompletionCount: Long,
    val distinctTrackCount: Long,
    val distinctAlbumCount: Long,
    val latestKnownPlayAt: Long?
)

data class RecentListeningEventRow(
    val eventUuid: String,
    val trackIdentityId: Long,
    val titleSnapshot: String,
    val artistSnapshot: String,
    val albumSnapshot: String,
    val source: String,
    val startedAt: Long,
    val endedAt: Long,
    val listenedMs: Long,
    val qualifiedAsPlay: Boolean,
    val qualificationReason: String,
    val endReason: String,
    val playbackSessionId: String?
)

@Dao
interface ListeningStatsDao {
    @RawQuery
    suspend fun getOverview(query: SupportSQLiteQuery): ListeningOverviewRow

    @RawQuery
    suspend fun getTrackStats(query: SupportSQLiteQuery): List<TrackListeningStatsRow>

    @RawQuery
    suspend fun getAlbumStats(query: SupportSQLiteQuery): List<AlbumListeningStatsRow>

    @RawQuery
    suspend fun getArtistStats(query: SupportSQLiteQuery): List<ArtistListeningStatsRow>

    @RawQuery
    suspend fun getRecentEvents(query: SupportSQLiteQuery): List<RecentListeningEventRow>
}
