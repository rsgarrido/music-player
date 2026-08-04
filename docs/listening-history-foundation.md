# Native listening-history persistence foundation

Database version 9 adds durable historical track identities, local-track bindings,
finalized listening events, and exact baselines for the aggregate history that existed
before event storage. The current Recently Played and Most Played features continue to
read `song_play_stats`; playback does not write detailed events yet.

Migration 8→9 creates one identity, one local binding, and one legacy baseline for every
existing `song_play_stats` row. It deliberately creates no synthetic listening events,
because the old aggregate count does not contain individual timestamps, listened
durations, completion state, or qualification evidence.

The manual JSON backup format remains unchanged in this release. A later, explicitly
versioned backup format must add `listening_track_identities`, `local_track_bindings`,
`legacy_listening_baselines`, and `listening_events`. That later design must account for
large event histories instead of silently adding them to the current JSON payload.

## Pure listening-session recorder (qualification rule v1)

`ListeningSessionRecorder` is a pure Kotlin state machine for one native CDPlaya
playback attempt. It is not connected to production playback yet. In particular,
`PlaybackService`, Media3 callbacks, the existing aggregate `PlaybackHistoryRecorder`,
Room writes, UI, and Android lifecycle components do not call it.

The recorder is constructed with a monotonic clock, a wall clock, and an event UUID
generator. Its command API is:

* `startSession(request)` creates an inactive session with zero listened time.
* `onPlaybackStarted(playbackSessionId)` opens an actively-playing segment.
* `onPlaybackSuspended(playbackSessionId)` closes that segment for either pause or
  buffering.
* `onPositionDiscontinuity(playbackSessionId)` closes and immediately reopens a segment
  if playback is active. It neither accepts nor infers a media position.
* `snapshot()` returns immutable current state.
* `finalizeSession(playbackSessionId, endReason)` closes an active segment and returns
  one immutable `FinalizedListeningEventDraft`.

Every callback command carries the playback session ID. A delayed command for an older
session is rejected instead of mutating the current session.

### Actual listening time

Only elapsed monotonic time during explicitly active-playing segments is accumulated.
Wall-clock time is used for the persisted `startedAt`, `endedAt`, and `createdAt`
timestamps only, so civil-time changes cannot alter `listenedMs`. Snapshot time includes
the elapsed portion of an open segment without closing that segment.

Pause and buffering have identical recorder semantics: both close the active segment,
and time remains stopped until another explicit playback-started command. A seek closes
the current segment exactly once, preserves all accumulated time, and reopens at the
same monotonic instant when still playing. Forward seeks cannot add listening evidence;
backward seeks never subtract evidence, and listening to replayed sections can make
`listenedMs` exceed the duration snapshot.

If a faulty monotonic clock moves backward, readings are clamped to the recorder's last
observed high-water mark. The rollback interval therefore contributes zero time; time
starts accumulating again only after the clock passes that high-water mark. Elapsed
subtraction and accumulation saturate at `Long.MAX_VALUE` rather than overflowing.

If the wall clock moves before the session start, final `endedAt` and `createdAt` are
clamped to `startedAt` so the Room v9 finalized-event constraints remain valid.

### Qualification rule v1

The time threshold is:

```text
min(durationMs / 2 + durationMs % 2, 240_000 ms)
```

This is an overflow-safe integer ceiling of half the duration, capped at four minutes.
Listening equal to the threshold qualifies; one millisecond below it does not. There is
no minimum duration. Null, zero, and negative durations cannot qualify by elapsed time.

A proven `NATURAL_END` always qualifies, including before the time threshold and when
duration is invalid or unknown. Stops, errors, transitions, and unknown endings do not
provide natural-completion evidence. Time qualification is sticky once reached, while a
later natural ending upgrades its reason from `TIME_THRESHOLD` to `NATURAL_END`. The
draft stores both the reason and `qualificationRuleVersion = 1`; persistence does not
need to recalculate them later.

### Idempotency and session boundaries

Duplicate play and suspend commands do not reopen, close, or count a segment twice.
Repeated discontinuities at the same monotonic instant add zero time. Starting an
identical request for the currently active playback session is idempotent. A same-ID
request with different identity, binding, or duration data is rejected as conflicting,
and a different session cannot replace an unfinalized one.

Finalizing without an active session produces no synthetic event. Successful
finalization clears the active session so another can start, while the most recently
finalized session ID is retained to reject both its duplicate finalization and a delayed
duplicate start. A genuinely new session therefore needs a new playback session ID.
UUID generation occurs only for successful finalization. Native drafts always use
source `CDPLAYA` and leave import fields null. The separate `toEntity()` mapper is the
Room boundary; the recorder itself neither constructs nor inserts Room entities.

### Deferred integration

A later service-adapter milestone must decide how Media3's `isPlaying`, discontinuity,
completion, transition, error, stop, queue, repeat, notification, and Bluetooth signals
map to these stable commands and end reasons. That adapter must explicitly finalize the
old session before starting a new one. Active-session persistence, checkpoints,
process-death recovery, statistics queries and UI, backup expansion, and imports also
remain deferred.
