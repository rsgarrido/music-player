package com.example.cdplaya.data

data class ResolvedProductionListeningHistory(
    val recentlyPlayed: List<Song>,
    val mostPlayed: List<Song>
)

/** Maps identity projections to one immutable, folder-filtered library snapshot. */
object ListeningHistoryProjectionResolver {
    fun resolve(
        projections: ProductionListeningHistoryProjections,
        index: SongReferenceIndex,
        visibleMembershipKeys: Set<String>
    ): ResolvedProductionListeningHistory = ResolvedProductionListeningHistory(
        recentlyPlayed = projections.recentlyPlayed.mapNotNull { projection ->
            resolveTrack(projection.track, index, visibleMembershipKeys)
        },
        mostPlayed = projections.mostPlayed.mapNotNull { projection ->
            resolveTrack(projection.track, index, visibleMembershipKeys)
        }
    )

    private fun resolveTrack(
        track: TrackListeningStats,
        index: SongReferenceIndex,
        visibleMembershipKeys: Set<String>
    ): Song? {
        val bindings = track.knownBindings.ifEmpty { listOfNotNull(track.binding) }
        val preferred = bindings.firstOrNull() ?: return null
        when (val resolution = index.resolve(preferred.toReference(track))) {
            is SongReferenceResolution.Resolved -> return resolution.song
                .takeIf { it.membershipKey() in visibleMembershipKeys }
            is SongReferenceResolution.Ambiguous -> return null
            SongReferenceResolution.NotFound -> Unit
        }

        var resolvedSong: Song? = null
        for (binding in bindings.drop(1)) {
            when (val resolution = index.resolve(binding.toReference(track))) {
                is SongReferenceResolution.Resolved -> {
                    val previous = resolvedSong
                    if (previous != null && previous != resolution.song) return null
                    resolvedSong = resolution.song
                }
                is SongReferenceResolution.Ambiguous -> return null
                SongReferenceResolution.NotFound -> Unit
            }
        }
        return resolvedSong?.takeIf { it.membershipKey() in visibleMembershipKeys }
    }

    private fun ListeningBindingSnapshot.toReference(track: TrackListeningStats) = SongReference(
        mediaStoreId = mediaStoreId,
        volumeName = volumeName.orEmpty(),
        contentUri = contentUri.orEmpty(),
        relativePath = relativePath.orEmpty(),
        displayName = displayName.orEmpty(),
        fileSizeBytes = fileSizeBytes ?: 0L,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds ?: 0L,
        duration = durationMs ?: track.durationMs ?: 0L,
        title = track.title,
        artist = track.artist,
        album = track.album,
        albumArtist = track.albumArtist.orEmpty(),
        legacyStableKey = legacyStableKey.orEmpty(),
        portableKey = portableKey.orEmpty(),
        portableKeyVersion = portableKeyVersion ?: SongIdentity.PORTABLE_KEY_VERSION
    )
}
