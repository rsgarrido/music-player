package com.example.cdplaya.data

import java.security.MessageDigest

fun Song.stableKey(): String {
    val rawKey = listOf(
        title.normalizedForStableKey(),
        artist.normalizedForStableKey(),
        album.normalizedForStableKey(),
        duration.toString()
    ).joinToString("|")

    return rawKey.sha256()
}

fun Song.favoriteKey(): String {
    return stableKey()
}

private fun String.normalizedForStableKey(): String {
    return trim()
        .lowercase()
        .replace(Regex("\\s+"), " ")
}

private fun String.sha256(): String {
    val bytes = MessageDigest
        .getInstance("SHA-256")
        .digest(toByteArray())

    return bytes.joinToString("") { byte ->
        "%02x".format(byte)
    }
}