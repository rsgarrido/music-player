# Native listening-history persistence foundation

Database version 9 adds durable historical track identities, local-track bindings,
finalized listening events, and exact baselines for the aggregate history that existed
before event storage. The current Recently Played and Most Played features continue to
read `song_play_stats`. Native playback now also writes detailed finalized attempts to
`listening_events` from the authoritative `PlaybackService` player.

Migration 8→9 creates one identity, one local binding, and one legacy baseline for every
existing `song_play_stats` row. It deliberately creates no synthetic listening events,
because the old aggregate count does not contain individual timestamps, listened
durations, completion state, or qualification evidence.

The manual JSON backup format remains unchanged in this release. A later, explicitly
versioned backup format must add `listening_track_identities`, `local_track_bindings`,
`legacy_listening_baselines`, and `listening_events`. That later design must account for
large event histories instead of silently adding them to the current JSON payload.

## Service-owned native recording

`PlaybackServiceListeningAdapter` is attached directly to the ExoPlayer owned by
`PlaybackService`, so Compose, notification, Bluetooth/headset, MediaSession, Android
Auto, automatic playlist, and repeat-one commands converge on the same callback stream.
The adapter timestamps callbacks immediately, processes them through one coroutine
consumer, and performs Room writes independently on an IO scope. A slow identity lookup
does not lose initial playback time because the captured monotonic callback time is
installed before each pure-recorder command.

Playable `MediaItem`s carry centralized, validated extras containing a unique item
instance ID, an exact durable local reference key, and immutable `SongReference`
evidence. The service resolves an existing local binding only by the exact reference
key. Otherwise it transactionally creates a new historical identity and binding;
normalized metadata is a snapshot/index only and never a merge key. Missing or malformed
extras cause that item to be skipped safely rather than guessed from its title.

Sessions are created only on confirmed `isPlaying == true`, not on preload. Automatic
and repeat transitions finalize the old attempt as `NATURAL_END`; repeat-one immediately
gets a new playback session ID even though the item instance is unchanged. User/direct
and current-item playlist replacement transitions use `TRANSITION`. Same-current-item
queue edits do not end a session. `STATE_ENDED`, player error, `STATE_IDLE`/stop, and
graceful service destruction map to natural end, error, and stopped finalization as
appropriate. Only within-item Media3 seek and seek-adjustment discontinuities are sent
to the recorder; automatic transition discontinuities are ignored.

Finalized drafts are inserted with Room conflict-ignore semantics. Event UUID and
playback-session unique indexes make callback duplication idempotent. Insert failures
are logged without track metadata and never crash or block playback. Native events keep
source `CDPLAYA`, null import fields, and never update `song_play_stats`.

This is intentionally transitional: `PlaybackHistoryProgressTracker` and the
`PlaybackController` aggregate recorder remain active, so `song_play_stats` still uses
the previous UI-side threshold behavior while `listening_events` uses qualification rule
v1. Recently Played and Most Played remain on the legacy aggregate queries until the
later parity/projection session.

Active sessions are in memory only. Graceful service destruction is finalized and its
outstanding insert is drained asynchronously, but abrupt process death can lose the
unfinished attempt. There are no periodic checkpoints or process-death recovery yet.

## Pure listening-session recorder (qualification rule v1)

`ListeningSessionRecorder` remains a pure Kotlin state machine for one native CDPlaya
playback attempt. Android, Media3, Room, UI, and lifecycle dependencies remain outside
its contract in the service adapter and repository boundaries.

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

### Deferred work

Active-session persistence, periodic checkpoints, process-death recovery, combined
baseline-plus-event statistics queries, Recently Played/Most Played migration,
statistics UI, backup expansion, imports and matching, ratings, Wrapped, and smart
playlists remain deferred.
