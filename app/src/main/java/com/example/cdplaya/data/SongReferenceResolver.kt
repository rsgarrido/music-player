package com.example.cdplaya.data

sealed interface SongReferenceResolution {
    data class Resolved(
        val song: Song,
        val matchType: SongReferenceMatchType
    ) : SongReferenceResolution

    data class Ambiguous(
        val candidates: List<Song>,
        val matchType: SongReferenceMatchType
    ) : SongReferenceResolution

    data object NotFound : SongReferenceResolution
}

enum class SongReferenceMatchType {
    LOCAL,
    SOURCE,
    FILE_SIGNATURE,
    PORTABLE,
    LEGACY
}

/** Resolves the first matching confidence tier, but never chooses among equal-tier candidates. */
object SongReferenceResolver {
    fun resolve(
        reference: SongReference,
        songs: Collection<Song>
    ): SongReferenceResolution {
        if (songs.isEmpty()) return SongReferenceResolution.NotFound

        val tiers = listOfNotNull(
            localMatches(reference, songs),
            sourceMatches(reference, songs),
            signatureMatches(reference, songs),
            portableMatches(reference, songs),
            legacyMatches(reference, songs)
        )

        tiers.forEach { (matchType, candidates) ->
            if (candidates.isNotEmpty()) {
                val deterministicCandidates = candidates.sortedWith(songResolutionOrder)
                return if (deterministicCandidates.size == 1) {
                    SongReferenceResolution.Resolved(deterministicCandidates.single(), matchType)
                } else {
                    SongReferenceResolution.Ambiguous(deterministicCandidates, matchType)
                }
            }
        }
        return SongReferenceResolution.NotFound
    }

    private fun localMatches(reference: SongReference, songs: Collection<Song>) =
        SongReferenceMatchType.LOCAL to songs.filter { song ->
            val sameVolumeAndId = reference.mediaStoreId != null &&
                reference.volumeName.isNotBlank() &&
                song.id == reference.mediaStoreId &&
                song.volumeName.equals(reference.volumeName, ignoreCase = true)
            val sameContentUri = reference.contentUri.isNotBlank() &&
                song.uri.toString() == reference.contentUri
            sameVolumeAndId || sameContentUri
        }

    private fun sourceMatches(reference: SongReference, songs: Collection<Song>) =
        if (reference.relativePath.isBlank() || reference.displayName.isBlank()) null else {
            val expectedPath = reference.relativePath.identityPathNormalized()
            val expectedName = reference.displayName.identityNormalized()
            SongReferenceMatchType.SOURCE to songs.filter { song ->
                val volumeMatches = reference.volumeName.isBlank() ||
                    song.volumeName.equals(reference.volumeName, ignoreCase = true)
                volumeMatches && song.relativePath.identityPathNormalized() == expectedPath &&
                    song.displayName.identityNormalized() == expectedName
            }
        }

    private fun signatureMatches(reference: SongReference, songs: Collection<Song>) =
        if (
            reference.displayName.isBlank() || reference.fileSizeBytes <= 0L ||
            reference.duration <= 0L
        ) null else {
            SongReferenceMatchType.FILE_SIGNATURE to songs.filter { song ->
                song.displayName.identityNormalized() == reference.displayName.identityNormalized() &&
                    song.fileSizeBytes == reference.fileSizeBytes &&
                    durationsMatch(song.duration, reference.duration)
            }
        }

    private fun portableMatches(reference: SongReference, songs: Collection<Song>) =
        reference.effectivePortableKey()?.let { portableKey ->
            SongReferenceMatchType.PORTABLE to songs.filter { song ->
                song.songIdentity().portableKey == portableKey
            }
        }

    private fun legacyMatches(reference: SongReference, songs: Collection<Song>) =
        reference.legacyStableKey.takeIf { it.isNotBlank() }?.let { legacyKey ->
            SongReferenceMatchType.LEGACY to songs.filter { song -> song.stableKey() == legacyKey }
        }

    private fun SongReference.effectivePortableKey(): String? {
        if (portableKeyVersion != SongIdentity.PORTABLE_KEY_VERSION) return null
        return portableKey.takeIf { it.isNotBlank() }
            ?: portableMetadataKey(title, artist, album, duration)
    }

    private fun durationsMatch(first: Long, second: Long): Boolean {
        return kotlin.math.abs(first - second) <= DURATION_TOLERANCE_MILLIS
    }

    private val songResolutionOrder = compareBy<Song>(
        { it.volumeName.identityNormalized() },
        { it.relativePath.identityPathNormalized() },
        { it.displayName.identityNormalized() },
        { it.id }
    )

    private const val DURATION_TOLERANCE_MILLIS = 2_000L
}
