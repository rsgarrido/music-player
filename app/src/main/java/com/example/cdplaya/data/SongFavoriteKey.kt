package com.example.cdplaya.data

import java.security.MessageDigest

fun Song.favoriteKey(): String {
    val rawKey = listOf(
        title.normalizedForFavoriteKey(),
        artist.normalizedForFavoriteKey(),
        album.normalizedForFavoriteKey(),
        duration.toString()
    ).joinToString("|")

    return rawKey.sha256()
}

private fun String.normalizedForFavoriteKey(): String {
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