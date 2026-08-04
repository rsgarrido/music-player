# Native listening-history persistence foundation

Database version 9 adds durable historical track identities, local-track bindings,
finalized listening events, and exact baselines for the aggregate history that existed
before event storage. Production Recently Played and Most Played now read the
baseline-plus-qualified-event projections. Native playback writes detailed finalized
attempts to `listening_events` from the authoritative `PlaybackService` player.

Migration 8→9 creates one identity, one local binding, and one legacy baseline for every
existing `song_play_stats` row. It deliberately creates no synthetic listening events,
because the old aggregate count does not contain individual timestamps, listened
durations, completion state, or qualification evidence.

Manual JSON backup schema 7 contains the complete canonical version-9 history. Its
independently versioned `canonicalListeningHistory` section has format version 1 and
stores identities, bindings, baselines, finalized events, and a count/time-boundary
summary. Database-generated identity and binding IDs are backup-local references only;
restore inserts new rows and remaps every foreign-key reference. Stored normalization,
event UUIDs, enum storage strings, session/source provenance, qualification facts and
rule versions are preserved rather than recalculated.

Schema 6 and older backups remain readable. Each old aggregate history row becomes its
own identity, exact-evidence binding, and legacy baseline, even when metadata is
identical. Counts and first/last timestamps are preserved and no detailed events or
listening durations are fabricated.

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

The old `PlaybackController` aggregate recorder and its UI-side progress qualification
tracker have been removed. Playback-position observation remains for player progress and
checkpointing, but it no longer decides listening qualification or writes history.
`PlaybackService` is the only owner of new native listening-event semantics, covering UI,
notification, Bluetooth/headset, MediaSession, automatic transitions, and repeat-one.

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

The production Recently Played projection contains one row per identity with a
qualified legacy or detailed play, ordered by latest known qualified play descending and
then identity ID ascending. The Most Played projection orders by combined play count
descending, latest known qualified play descending, and identity ID ascending. Both
retain unresolved historical identities and exact binding/reference evidence.

## Production cutover and reactive library mapping

`ListeningStatsRepository.observeProductionHistory()` observes only
`listening_events`, `legacy_listening_baselines`, `listening_track_identities`, and
`local_track_bindings`. Room invalidation is conflated, and each refresh reads Recently
Played, Most Played, and all binding evidence inside one database transaction. A newer
invalidation cancels an older refresh before it can publish a stale snapshot. Collection
is owned by the long-lived `LibraryController` scope; cancelling that scope removes the
Room observer, and no repository-owned or global scope is created.

The controller combines that database snapshot with its immutable
`SongReferenceIndex` and visible membership-key snapshot. Mapping runs off the main
thread, publishes both lists in one UI-state update, and is cancelled/restarted when
either history or the current library changes. Consequently database inserts, rescans,
and folder-selection changes all use the same resolution path without polling or a
service-to-UI refresh callback.

Resolution preserves projection order and omits unresolved or ambiguous identities. It
tries the deterministic preferred exact binding first, then other known bindings for the
same identity. Resolver confidence tiers may use local ID/URI, source path, file
signature, portable key, and legacy key evidence stored by those bindings; there is no
full-library title-only or fuzzy match. A preferred exact match wins. If fallback
evidence is ambiguous or resolves to conflicting current songs, the identity is omitted.
Distinct identity IDs are never merged even when their metadata snapshots are identical.
The index includes the reference library while the visible membership set enforces the
current folder selection, so hiding a folder removes only the playable row. History is
not deleted, and a later rescan or folder re-inclusion can resolve it again.

The projection model exposes all known local bindings for production resolution. The
database query still exposes one deterministic preferred binding on ordinary statistics
rows for compatibility with the Session 4 API.

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

The version-9 baseline is a frozen copy of history present during migration. Production
totals combine only that frozen baseline with qualified detailed events. Current
`song_play_stats` values are not copied into baselines or events and are not a third
statistics source. No heuristic reconciliation is performed. A development device used
while both branch-only recorders were temporarily active may therefore show small count
or ordering differences after cutover; those aggregate test writes are intentionally
ignored because folding them in could double count events and mix qualification rules.

`song_play_stats` remains at database version 9 for migration verification, old-backup
compatibility, legacy-reference maintenance, and development rollback inspection.
Playback no longer mutates it, production history screens never read it, and schema-7
export does not use it. A new schema-7 restore clears this noncanonical compatibility
table. A migrated schema-6 restore may also restore its aggregate rows for legacy code,
but canonical totals come only from the separately converted baselines and events, so
the compatibility rows cannot double-count Recently Played or Most Played.

## Manual backup and restore schema 7

Canonical export reads identities by ID, bindings by identity then binding ID,
baselines by identity, and events by start time then event row ID. All four reads occur
in one Room read transaction, so finalized playback inserted during export is wholly
before or after the captured history snapshot. Event cursors are read in pages of 1,000.
The JSON encoder writes directly to the destination stream and uses compact JSON, so it
does not allocate a second full serialized string. The in-memory `AppBackup` model still
retains the collected records while serialization runs; database cursor and JSON-string
memory are bounded, but the current serializer is not a record-at-a-time parser. Import
batch IDs remain opaque nullable event provenance because no canonical import-batch
table exists yet. The existing UI reports completion rather than per-page progress.

Before restore mutates data, schema version, history format version, summary counts,
references, ownership, durations, timestamps, play counts, enum strings, and every
database uniqueness key are validated. Favorites, playlists, the compatibility table,
and the four canonical history tables are then replaced inside one Room transaction.
Canonical deletion order is events, baselines, bindings, identities; insertion order is
identities, bindings, baselines, events. Identity and binding maps translate backup-local
IDs to restored IDs, and baseline/event inserts use batches of 500. Any validation or
database failure leaves all database-backed categories at their prior state. Preferences
remain in their existing DataStore boundary and are applied after the Room commit.

Room invalidation from the committed transaction refreshes production Recently Played
and Most Played without an app restart, rescan, tab change, or artificial playback
event. An active in-memory playback attempt is intentionally not serialized, stopped,
or rewritten. If it finalizes after restore, the service inserts exactly one new event
after the restored snapshot.

Backups remain local to the user-selected document destination and add no upload or
network behavior. Schema 7 may contain track metadata, timestamps, listening behavior,
and local-reference evidence including paths and content URIs. Backup code does not log
the JSON, metadata, paths, or event history; structural errors use privacy-safe messages.

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

Active-session persistence, periodic checkpoints, process-death recovery, statistics UI
and charts, Spotify/Last.fm imports and matching, ratings, Wrapped, smart playlists,
cloud synchronization, and shareable reports remain deferred.
