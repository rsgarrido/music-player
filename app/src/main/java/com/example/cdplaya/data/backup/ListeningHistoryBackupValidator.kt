package com.example.cdplaya.data.backup

import com.example.cdplaya.data.local.ListeningEndReason
import com.example.cdplaya.data.local.ListeningQualificationReason
import com.example.cdplaya.data.local.ListeningSource

object ListeningHistoryBackupValidator {
    fun validate(history: BackupListeningHistoryV2): BackupListeningHistoryV2 {
        require(history.formatVersion == BackupListeningHistoryV2.CURRENT_FORMAT_VERSION) {
            "Unsupported listening-history backup format version ${history.formatVersion}."
        }

        val identities = history.identities.associateByUnique(
            BackupListeningTrackIdentity::backupIdentityId,
            "backup identity ID"
        )
        require(identities.keys.none { it <= 0L }) {
            "Listening-history backup identity IDs must be positive."
        }
        history.identities.forEach { identity ->
            require(identity.metadataKeyVersion > 0) {
                "Listening-history metadata-key version is invalid."
            }
            require(identity.durationMsSnapshot == null || identity.durationMsSnapshot >= 0L) {
                "Listening-history identity duration is invalid."
            }
            require(identity.updatedAt >= identity.createdAt) {
                "Listening-history identity timestamps are invalid."
            }
        }

        val bindings = history.bindings.associateByUnique(
            BackupLocalTrackBinding::backupBindingId,
            "backup binding ID"
        )
        require(bindings.keys.none { it <= 0L }) {
            "Listening-history backup binding IDs must be positive."
        }
        require(history.bindings.map { it.referenceKey }.distinct().size == history.bindings.size) {
            "Listening-history binding reference keys must be unique."
        }
        history.bindings.forEach { binding ->
            require(binding.trackIdentityBackupId in identities) {
                "Listening-history binding references a missing identity."
            }
            require(binding.referenceKey.isNotBlank()) {
                "Listening-history binding reference key is invalid."
            }
            require(binding.lastSeenAt >= binding.firstSeenAt) {
                "Listening-history binding timestamps are invalid."
            }
            require(binding.fileSizeBytes == null || binding.fileSizeBytes >= 0L) {
                "Listening-history binding file size is invalid."
            }
            require(binding.durationMsSnapshot == null || binding.durationMsSnapshot >= 0L) {
                "Listening-history binding duration is invalid."
            }
        }

        require(
            history.baselines.map { it.trackIdentityBackupId }.distinct().size ==
                history.baselines.size
        ) { "Listening-history baselines must reference unique identities." }
        require(
            history.baselines.map { it.legacyReferenceKey }.distinct().size ==
                history.baselines.size
        ) { "Listening-history baseline reference keys must be unique." }
        history.baselines.forEach { baseline ->
            require(baseline.trackIdentityBackupId in identities) {
                "Listening-history baseline references a missing identity."
            }
            require(baseline.historicalPlayCount > 0) {
                "Listening-history baseline play count is invalid."
            }
            require(baseline.lastKnownPlayedAt >= baseline.firstKnownPlayedAt) {
                "Listening-history baseline timestamps are invalid."
            }
            require(baseline.legacyReferenceKey.isNotBlank()) {
                "Listening-history baseline reference key is invalid."
            }
        }

        require(history.events.map { it.eventUuid }.distinct().size == history.events.size) {
            "Listening-history event UUIDs must be unique."
        }
        val sessionIds = history.events.mapNotNull { it.playbackSessionId }
        require(sessionIds.distinct().size == sessionIds.size) {
            "Listening-history playback-session IDs must be unique."
        }
        val sourceEventKeys = history.events.mapNotNull { event ->
            event.sourceEventKey?.let { event.source to it }
        }
        require(sourceEventKeys.distinct().size == sourceEventKeys.size) {
            "Listening-history source-event keys must be unique within each source."
        }
        history.events.forEach { event ->
            require(event.eventUuid.isNotBlank()) {
                "Listening-history event UUID is invalid."
            }
            require(event.trackIdentityBackupId in identities) {
                "Listening-history event references a missing identity."
            }
            event.localTrackBindingBackupId?.let { bindingId ->
                val binding = bindings[bindingId]
                    ?: throw IllegalArgumentException(
                        "Listening-history event references a missing binding."
                    )
                require(binding.trackIdentityBackupId == event.trackIdentityBackupId) {
                    "Listening-history event binding belongs to a different identity."
                }
            }
            require(event.listenedMs >= 0L) {
                "Listening-history event listening time is invalid."
            }
            require(event.endedAt >= event.startedAt) {
                "Listening-history event timestamps are invalid."
            }
            require(event.trackDurationMs == null || event.trackDurationMs >= 0L) {
                "Listening-history event duration is invalid."
            }
            require(event.qualificationRuleVersion > 0) {
                "Listening-history qualification-rule representation is unsupported."
            }
            require(event.playbackSessionId == null || event.playbackSessionId.isNotBlank()) {
                "Listening-history playback-session ID is invalid."
            }
            require(event.sourceEventKey == null || event.sourceEventKey.isNotBlank()) {
                "Listening-history source-event key is invalid."
            }
            parseEnum("source", event.source, ListeningSource::fromStorageValue)
            parseEnum(
                "qualification reason",
                event.qualificationReason,
                ListeningQualificationReason::fromStorageValue
            )
            parseEnum("end reason", event.endReason, ListeningEndReason::fromStorageValue)
        }

        val expectedSummary = history.recordsSummary()
        require(history.summary == expectedSummary) {
            "Listening-history backup summary does not match its records."
        }
        return history
    }
}

internal fun BackupListeningHistoryV2.recordsSummary() = BackupListeningHistorySummary(
    identityCount = identities.size.toLong(),
    bindingCount = bindings.size.toLong(),
    baselineCount = baselines.size.toLong(),
    eventCount = events.size.toLong(),
    qualifiedEventCount = events.count { it.qualifiedAsPlay }.toLong(),
    nonQualifiedEventCount = events.count { !it.qualifiedAsPlay }.toLong(),
    earliestDetailedEventAt = events.minOfOrNull { it.startedAt },
    latestDetailedEventAt = events.maxOfOrNull { it.startedAt }
)

private inline fun <T, K> List<T>.associateByUnique(
    keySelector: (T) -> K,
    label: String
): Map<K, T> {
    val result = associateBy(keySelector)
    require(result.size == size) { "Listening-history $label values must be unique." }
    return result
}

private inline fun <T> parseEnum(
    label: String,
    value: String,
    parser: (String) -> T
) {
    try {
        parser(value)
    } catch (_: IllegalStateException) {
        throw IllegalArgumentException("Listening-history $label value is unsupported.")
    }
}
