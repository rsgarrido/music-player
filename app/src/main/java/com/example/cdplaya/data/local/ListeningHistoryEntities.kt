package com.example.cdplaya.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "listening_track_identities",
    indices = [
        Index(value = ["normalizedArtist", "normalizedTitle", "durationMsSnapshot"]),
        Index(value = ["normalizedAlbum"]),
        Index(value = ["metadataKey"])
    ]
)
data class ListeningTrackIdentityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val titleSnapshot: String,
    val artistSnapshot: String,
    val albumSnapshot: String,
    val albumArtistSnapshot: String?,
    val durationMsSnapshot: Long?,
    val normalizedTitle: String,
    val normalizedArtist: String,
    val normalizedAlbum: String,
    val metadataKey: String?,
    val metadataKeyVersion: Int,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "local_track_bindings",
    foreignKeys = [
        ForeignKey(
            entity = ListeningTrackIdentityEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackIdentityId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["trackIdentityId"]),
        Index(value = ["referenceKey"], unique = true),
        Index(value = ["volumeName", "mediaStoreId"]),
        Index(value = ["portableKey"])
    ]
)
data class LocalTrackBindingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackIdentityId: Long,
    val referenceKey: String,
    val mediaStoreId: Long?,
    val volumeName: String?,
    val contentUri: String?,
    val relativePath: String?,
    val displayName: String?,
    val absolutePath: String?,
    val fileSizeBytes: Long?,
    val dateModifiedEpochSeconds: Long?,
    val durationMsSnapshot: Long?,
    val legacyStableKey: String?,
    val portableKey: String?,
    val portableKeyVersion: Int?,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val missingSince: Long?
)

@Entity(
    tableName = "listening_events",
    foreignKeys = [
        ForeignKey(
            entity = ListeningTrackIdentityEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackIdentityId"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = LocalTrackBindingEntity::class,
            parentColumns = ["id"],
            childColumns = ["localTrackBindingId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["eventUuid"], unique = true),
        Index(value = ["playbackSessionId"], unique = true),
        Index(value = ["source", "sourceEventKey"], unique = true),
        Index(value = ["trackIdentityId", "startedAt"]),
        Index(value = ["localTrackBindingId"]),
        Index(value = ["qualifiedAsPlay", "startedAt"]),
        Index(value = ["source", "startedAt"]),
        Index(value = ["importBatchId"])
    ]
)
data class ListeningEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventUuid: String,
    val source: ListeningSource,
    val trackIdentityId: Long,
    val localTrackBindingId: Long?,
    val playbackSessionId: String?,
    val startedAt: Long,
    val endedAt: Long,
    val listenedMs: Long,
    val trackDurationMs: Long?,
    val qualifiedAsPlay: Boolean,
    val qualificationReason: ListeningQualificationReason,
    val qualificationRuleVersion: Int,
    val endReason: ListeningEndReason,
    val sourceEventKey: String?,
    val importBatchId: Long?,
    val createdAt: Long
) {
    init {
        require(listenedMs >= 0L) { "Listening time cannot be negative" }
        require(endedAt >= startedAt) { "A finalized listening event must not end before it starts" }
    }
}

@Entity(
    tableName = "legacy_listening_baselines",
    foreignKeys = [
        ForeignKey(
            entity = ListeningTrackIdentityEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackIdentityId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["legacyReferenceKey"], unique = true)]
)
data class LegacyListeningBaselineEntity(
    @PrimaryKey
    val trackIdentityId: Long,
    val historicalPlayCount: Int,
    val firstKnownPlayedAt: Long,
    val lastKnownPlayedAt: Long,
    val legacyReferenceKey: String,
    val migratedAt: Long
)
