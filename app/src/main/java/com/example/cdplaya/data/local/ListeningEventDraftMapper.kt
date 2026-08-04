package com.example.cdplaya.data.local

import com.example.cdplaya.data.listening.FinalizedListeningEventDraft

/** Room boundary kept separate from the pure listening-session recorder. */
fun FinalizedListeningEventDraft.toEntity(): ListeningEventEntity = ListeningEventEntity(
    eventUuid = eventUuid,
    source = source,
    trackIdentityId = trackIdentityId,
    localTrackBindingId = localTrackBindingId,
    playbackSessionId = playbackSessionId,
    startedAt = startedAt,
    endedAt = endedAt,
    listenedMs = listenedMs,
    trackDurationMs = trackDurationMs,
    qualifiedAsPlay = qualifiedAsPlay,
    qualificationReason = qualificationReason,
    qualificationRuleVersion = qualificationRuleVersion,
    endReason = endReason,
    sourceEventKey = sourceEventKey,
    importBatchId = importBatchId,
    createdAt = createdAt
)
