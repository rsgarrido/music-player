package com.example.cdplaya.data.backup

import com.example.cdplaya.data.SongIdentity
import com.example.cdplaya.data.SongReference
import java.security.MessageDigest

fun BackupSongReference.toSongReference(): SongReference = SongReference(
    relativePath = relativePath,
    displayName = displayName,
    fileSizeBytes = fileSizeBytes,
    duration = duration,
    title = title,
    artist = artist,
    album = album,
    albumArtist = albumArtist,
    legacyStableKey = legacyStableKey,
    portableKey = portableKey,
    portableKeyVersion = portableKeyVersion
)

fun SongReference.toBackupSongReference(): BackupSongReference = BackupSongReference(
    relativePath = relativePath.takeUnless { it.isAbsolutePathLike() }.orEmpty(),
    displayName = displayName,
    fileSizeBytes = fileSizeBytes,
    duration = duration,
    title = title,
    artist = artist,
    album = album,
    albumArtist = albumArtist,
    legacyStableKey = legacyStableKey,
    portableKey = portableKey,
    portableKeyVersion = portableKeyVersion.takeIf { it > 0 }
        ?: SongIdentity.PORTABLE_KEY_VERSION
)

internal fun String.isAbsolutePathLike(): Boolean {
    val normalized = replace('\\', '/')
    return normalized.startsWith('/') || Regex("^[A-Za-z]:/").containsMatchIn(normalized)
}

internal fun BackupSongReference.restoredReferenceKey(): String {
    val raw = listOf(
        relativePath,
        displayName,
        fileSizeBytes.toString(),
        portableKey,
        legacyStableKey
    ).joinToString("|")
    val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
    return "backup:v1:" + digest.joinToString("") { byte -> "%02x".format(byte) }
}
