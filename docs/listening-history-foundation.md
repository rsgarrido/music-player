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
