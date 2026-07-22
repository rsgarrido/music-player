package com.example.cdplaya.data

data class SongReference(
    val mediaStoreId: Long? = null,
    val volumeName: String = "",
    val contentUri: String = "",
    val relativePath: String = "",
    val displayName: String = "",
    val fileSizeBytes: Long = 0L,
    val dateModifiedEpochSeconds: Long = 0L,
    val duration: Long = 0L,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArtist: String = "",
    val legacyStableKey: String = "",
    val portableKey: String = "",
    val portableKeyVersion: Int = SongIdentity.PORTABLE_KEY_VERSION
)

fun Song.toSongReference(): SongReference {
    val identity = songIdentity()
    return SongReference(
        mediaStoreId = id.takeIf { it > 0L },
        volumeName = volumeName,
        contentUri = uri.toString(),
        relativePath = relativePath,
        displayName = displayName,
        fileSizeBytes = fileSizeBytes,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        duration = duration,
        title = title,
        artist = artist,
        album = album,
        albumArtist = albumArtist,
        legacyStableKey = identity.legacyKey,
        portableKey = identity.portableKey.orEmpty()
    )
}

fun SongReference.hasPersistedIdentity(): Boolean {
    return mediaStoreId != null || contentUri.isNotBlank() ||
        (relativePath.isNotBlank() && displayName.isNotBlank()) ||
        portableKey.isNotBlank() || legacyStableKey.isNotBlank()
}
