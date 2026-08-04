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

## Baseline-plus-event statistics queries

`ListeningStatsRepository` is the independent statistics/query boundary for future
history screens and reports. It is not connected to `LibraryController`, Compose,
Recently Played, or Most Played yet. All aggregation is performed by SQLite; repository
mapping only converts bounded aggregate rows into domain models.

### Counts, time, and timestamps

For a historical track identity, all-time play count is the frozen
`legacy_listening_baselines.historicalPlayCount` plus the number of detailed events whose
stored `qualifiedAsPlay` value is true. Qualification is never recalculated. Domain
models expose total, legacy, and detailed counts separately.

Confirmed listening time is the sum of `listening_events.listenedMs` for every finalized
attempt, including non-qualified attempts. Legacy counts contribute no duration because
the old aggregate rows did not record it; duration is neither inferred nor estimated.
Natural completions are detailed events whose persisted `endReason` is `NATURAL_END`.
Non-qualified attempts are detailed events whose stored `qualifiedAsPlay` value is
false.

Detailed events are classified and ordered by `startedAt`. This is the timestamp of the
recorded playback attempt, is consistent across every statistics query, and matches the
existing `(source, startedAt)`, `(qualifiedAsPlay, startedAt)`, and
`(trackIdentityId, startedAt)` indices. First/latest known qualified play is the
minimum/maximum of qualified detailed `startedAt` and the legacy baseline's first/last
known timestamp. Non-qualified attempts do not move Recently Played.

### Date ranges and source provenance

Detailed ranges are half-open epoch-millisecond intervals:
`[startInclusive, endExclusive)`. Callers resolve day, month, year, timezone, or custom
calendar boundaries before calling the repository; SQL does no device-local calendar
calculation. Ranged results are detailed-only because legacy plays cannot be allocated
to a day, month, or year.

A null source selection means all detailed sources. Any explicit detailed-source filter
applies only to events and excludes the legacy baseline even if legacy inclusion was
requested. In particular, neither a CDPlaya nor an import filter claims provenance for
legacy counts. Legacy inclusion is effective only for unfiltered all-time queries; no
`LEGACY` event source was added.

### Track, album, and artist grouping

Track statistics group strictly by `listening_track_identities.id`. MediaStore IDs,
bindings, titles, and normalized metadata never merge historical identities. Each track
row includes one deterministic preferred binding: an available binding before a missing
one, then newest `lastSeenAt`, then lowest binding ID. An identity without a binding is
still returned.

Album reporting is a grouping only. Its key is normalized album artist plus normalized
album; when album artist is absent it conservatively falls back to normalized track
artist. Blank values use explicit unknown keys. A compilation with a consistent album
artist such as `Various Artists` groups together; compilations without consistent album
artist may remain split by track artist. Artist reporting groups by normalized track
artist and never changes song-level artist metadata. Distinct track identities and
normalized album keys determine track/album counts.

When multiple display snapshots share a grouping key, SQL selects the lexicographically
smallest nonblank display value. This is stable and deterministic across query runs;
unknown groups receive `Unknown Album` or `Unknown Artist`.

### Projections and recent attempts

The replacement-capable Recently Played projection contains one row per identity with a
qualified legacy or detailed play, ordered by latest known qualified play descending and
then identity ID ascending. The Most Played projection orders by combined play count
descending, latest known qualified play descending, and identity ID ascending. Both
retain unresolved historical identities and exact binding/reference evidence; neither
is wired to production UI filtering yet.

Recent detailed events are separate from Recently Played. They include qualified and
non-qualified stops, transitions, errors, and natural completions, support source and
date filters, and order by `startedAt` descending then event row ID descending. Baseline
counts are never fabricated as events.

### SQL performance and transitional limitation

Overview, track, album, artist, projections, and recent-event results use CTE aggregation,
joins, SQL grouping, deterministic ordering, and explicit limits. They do not load the
event table into Kotlin and do not issue per-track binding queries. Existing version-9
indices cover source/date filtering, qualified/date filtering, track/date grouping, and
binding selection, so no schema version or new index is required for this layer.

The version-9 baseline is a frozen copy of history present during migration. The old
UI-side recorder continues changing `song_play_stats`, but those later writes are not
mirrored into the baseline and are deliberately not added as a third statistics source.
Until the controlled cutover session, production Recently Played and Most Played still
read `song_play_stats`, the old recorder remains active, and the existing backup format
remains unchanged.

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
Recently Played/Most Played production migration, old-recorder removal, statistics UI
and charts, backup expansion, imports and matching, ratings, Wrapped, smart playlists,
and shareable reports remain deferred.
