package com.example.cdplaya.data.backup

import androidx.room.withTransaction
import com.example.cdplaya.data.local.AppDatabase
import com.example.cdplaya.data.local.LegacyListeningBaselineEntity
import com.example.cdplaya.data.local.ListeningEndReason
import com.example.cdplaya.data.local.ListeningEventEntity
import com.example.cdplaya.data.local.ListeningQualificationReason
import com.example.cdplaya.data.local.ListeningSource
import com.example.cdplaya.data.local.ListeningTrackIdentityEntity
import com.example.cdplaya.data.local.LocalTrackBindingEntity

class ListeningHistoryBackupRepository(
    private val database: AppDatabase
) {
    suspend fun export(): BackupListeningHistoryV2 = database.withTransaction {
        val identities = database.listeningTrackIdentityDao().getAll().map { entity ->
            BackupListeningTrackIdentity(
                backupIdentityId = entity.id,
                titleSnapshot = entity.titleSnapshot,
                artistSnapshot = entity.artistSnapshot,
                albumSnapshot = entity.albumSnapshot,
                albumArtistSnapshot = entity.albumArtistSnapshot,
                durationMsSnapshot = entity.durationMsSnapshot,
                normalizedTitle = entity.normalizedTitle,
                normalizedArtist = entity.normalizedArtist,
                normalizedAlbum = entity.normalizedAlbum,
                metadataKey = entity.metadataKey,
                metadataKeyVersion = entity.metadataKeyVersion,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
        val bindings = database.localTrackBindingDao().getAllForBackup().map { entity ->
            BackupLocalTrackBinding(
                backupBindingId = entity.id,
                trackIdentityBackupId = entity.trackIdentityId,
                referenceKey = entity.referenceKey,
                mediaStoreId = entity.mediaStoreId,
                volumeName = entity.volumeName,
                contentUri = entity.contentUri,
                relativePath = entity.relativePath,
                displayName = entity.displayName,
                absolutePath = entity.absolutePath,
                fileSizeBytes = entity.fileSizeBytes,
                dateModifiedEpochSeconds = entity.dateModifiedEpochSeconds,
                durationMsSnapshot = entity.durationMsSnapshot,
                legacyStableKey = entity.legacyStableKey,
                portableKey = entity.portableKey,
                portableKeyVersion = entity.portableKeyVersion,
                firstSeenAt = entity.firstSeenAt,
                lastSeenAt = entity.lastSeenAt,
                missingSince = entity.missingSince
            )
        }
        val baselines = database.legacyListeningBaselineDao().getAllForBackup().map { entity ->
            BackupLegacyListeningBaseline(
                trackIdentityBackupId = entity.trackIdentityId,
                historicalPlayCount = entity.historicalPlayCount,
                firstKnownPlayedAt = entity.firstKnownPlayedAt,
                lastKnownPlayedAt = entity.lastKnownPlayedAt,
                legacyReferenceKey = entity.legacyReferenceKey,
                migratedAt = entity.migratedAt
            )
        }
        val events = buildList {
            var offset = 0
            do {
                val page = database.listeningEventDao().getBackupPage(EVENT_PAGE_SIZE, offset)
                page.mapTo(this) { entity -> entity.toBackup() }
                offset += page.size
            } while (page.size == EVENT_PAGE_SIZE)
        }
        BackupListeningHistoryV2(
            identities = identities,
            bindings = bindings,
            baselines = baselines,
            events = events
        ).let { history -> history.copy(summary = history.recordsSummary()) }
    }

    suspend fun restore(history: BackupListeningHistoryV2) {
        val validated = ListeningHistoryBackupValidator.validate(history)
        database.withTransaction {
            restoreValidatedWithinTransaction(validated)
        }
    }

    suspend fun restoreValidatedWithinTransaction(history: BackupListeningHistoryV2) {
        database.listeningEventDao().deleteAll()
        database.legacyListeningBaselineDao().deleteAll()
        database.localTrackBindingDao().deleteAll()
        database.listeningTrackIdentityDao().deleteAll()

        val identityIds = HashMap<Long, Long>(history.identities.size)
        history.identities.forEach { backup ->
            val restoredId = database.listeningTrackIdentityDao().insert(
                ListeningTrackIdentityEntity(
                    titleSnapshot = backup.titleSnapshot,
                    artistSnapshot = backup.artistSnapshot,
                    albumSnapshot = backup.albumSnapshot,
                    albumArtistSnapshot = backup.albumArtistSnapshot,
                    durationMsSnapshot = backup.durationMsSnapshot,
                    normalizedTitle = backup.normalizedTitle,
                    normalizedArtist = backup.normalizedArtist,
                    normalizedAlbum = backup.normalizedAlbum,
                    metadataKey = backup.metadataKey,
                    metadataKeyVersion = backup.metadataKeyVersion,
                    createdAt = backup.createdAt,
                    updatedAt = backup.updatedAt
                )
            )
            identityIds[backup.backupIdentityId] = restoredId
        }

        val bindingIds = HashMap<Long, Long>(history.bindings.size)
        history.bindings.forEach { backup ->
            val restoredId = database.localTrackBindingDao().insert(
                LocalTrackBindingEntity(
                    trackIdentityId = identityIds.getValue(backup.trackIdentityBackupId),
                    referenceKey = backup.referenceKey,
                    mediaStoreId = backup.mediaStoreId,
                    volumeName = backup.volumeName,
                    contentUri = backup.contentUri,
                    relativePath = backup.relativePath,
                    displayName = backup.displayName,
                    absolutePath = backup.absolutePath,
                    fileSizeBytes = backup.fileSizeBytes,
                    dateModifiedEpochSeconds = backup.dateModifiedEpochSeconds,
                    durationMsSnapshot = backup.durationMsSnapshot,
                    legacyStableKey = backup.legacyStableKey,
                    portableKey = backup.portableKey,
                    portableKeyVersion = backup.portableKeyVersion,
                    firstSeenAt = backup.firstSeenAt,
                    lastSeenAt = backup.lastSeenAt,
                    missingSince = backup.missingSince
                )
            )
            bindingIds[backup.backupBindingId] = restoredId
        }

        history.baselines.chunked(RESTORE_BATCH_SIZE).forEach { batch ->
            database.legacyListeningBaselineDao().insert(batch.map { backup ->
                LegacyListeningBaselineEntity(
                    trackIdentityId = identityIds.getValue(backup.trackIdentityBackupId),
                    historicalPlayCount = backup.historicalPlayCount,
                    firstKnownPlayedAt = backup.firstKnownPlayedAt,
                    lastKnownPlayedAt = backup.lastKnownPlayedAt,
                    legacyReferenceKey = backup.legacyReferenceKey,
                    migratedAt = backup.migratedAt
                )
            })
        }
        history.events.chunked(RESTORE_BATCH_SIZE).forEach { batch ->
            database.listeningEventDao().insert(batch.map { backup ->
                ListeningEventEntity(
                    eventUuid = backup.eventUuid,
                    source = ListeningSource.fromStorageValue(backup.source),
                    trackIdentityId = identityIds.getValue(backup.trackIdentityBackupId),
                    localTrackBindingId = backup.localTrackBindingBackupId?.let(bindingIds::getValue),
                    playbackSessionId = backup.playbackSessionId,
                    startedAt = backup.startedAt,
                    endedAt = backup.endedAt,
                    listenedMs = backup.listenedMs,
                    trackDurationMs = backup.trackDurationMs,
                    qualifiedAsPlay = backup.qualifiedAsPlay,
                    qualificationReason = ListeningQualificationReason.fromStorageValue(
                        backup.qualificationReason
                    ),
                    qualificationRuleVersion = backup.qualificationRuleVersion,
                    endReason = ListeningEndReason.fromStorageValue(backup.endReason),
                    sourceEventKey = backup.sourceEventKey,
                    importBatchId = backup.importBatchId,
                    createdAt = backup.createdAt
                )
            })
        }
    }

    companion object {
        const val EVENT_PAGE_SIZE = 1_000
        const val RESTORE_BATCH_SIZE = 500
    }
}

private fun ListeningEventEntity.toBackup() = BackupListeningEvent(
    eventUuid = eventUuid,
    source = source.storageValue,
    trackIdentityBackupId = trackIdentityId,
    localTrackBindingBackupId = localTrackBindingId,
    playbackSessionId = playbackSessionId,
    startedAt = startedAt,
    endedAt = endedAt,
    listenedMs = listenedMs,
    trackDurationMs = trackDurationMs,
    qualifiedAsPlay = qualifiedAsPlay,
    qualificationReason = qualificationReason.storageValue,
    qualificationRuleVersion = qualificationRuleVersion,
    endReason = endReason.storageValue,
    sourceEventKey = sourceEventKey,
    importBatchId = importBatchId,
    createdAt = createdAt
)
