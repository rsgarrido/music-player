package com.example.cdplaya.lyrics

import java.text.Normalizer
import java.util.Locale

fun normalizeFileStem(fileName: String): String {
    val trimmed = fileName.trim()
    val finalSeparator = maxOf(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('\\'))
    val name = trimmed.substring(finalSeparator + 1)
    val finalDot = name.lastIndexOf('.')
    val stem = if (finalDot > 0) name.substring(0, finalDot) else name
    return Normalizer.normalize(stem.trim(), Normalizer.Form.NFC)
        .lowercase(Locale.ROOT)
}

fun hasLrcExtension(fileName: String): Boolean {
    val trimmed = fileName.trim()
    val finalSeparator = maxOf(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('\\'))
    val name = trimmed.substring(finalSeparator + 1)
    val finalDot = name.lastIndexOf('.')
    return finalDot > 0 && name.substring(finalDot + 1).equals("lrc", ignoreCase = true)
}

fun normalizeLyricsPath(path: String?): String {
    if (path.isNullOrBlank()) return ""
    return Normalizer.normalize(path, Normalizer.Form.NFC)
        .replace('\\', '/')
        .split('/')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .joinToString("/")
        .lowercase(Locale.ROOT)
}

fun directorySuffixMatchDepth(first: String, second: String): Int {
    val firstSegments = normalizeLyricsPath(first).pathSegments()
    val secondSegments = normalizeLyricsPath(second).pathSegments()
    if (firstSegments.isEmpty() || secondSegments.isEmpty()) return 0

    val shorterSize = minOf(firstSegments.size, secondSegments.size)
    var matched = 0
    while (
        matched < shorterSize &&
        firstSegments[firstSegments.lastIndex - matched] ==
        secondSegments[secondSegments.lastIndex - matched]
    ) {
        matched++
    }
    return if (matched == shorterSize) matched else 0
}

object LocalLyricsMatcher {
    fun match(
        song: SongLyricsIdentity,
        files: List<IndexedLyricsFile>
    ): LyricsMatchResult {
        val songStem = normalizeFileStem(song.audioFileName)
        if (songStem.isEmpty()) return LyricsMatchResult.NotFound

        val candidates = files
            .asSequence()
            .filter { file -> file.normalizedStem == songStem }
            .distinctBy(IndexedLyricsFile::documentUri)
            .sortedWith(indexedLyricsFileComparator)
            .toList()
        if (candidates.isEmpty()) return LyricsMatchResult.NotFound

        val scored = candidates.map { file ->
            file to directoryScore(song, file)
        }
        val strongestScore = scored.maxOf { it.second }
        if (strongestScore > 0) {
            val strongest = scored
                .filter { it.second == strongestScore }
                .map { it.first }
            return if (strongest.size == 1) {
                LyricsMatchResult.Match(strongest.single())
            } else {
                LyricsMatchResult.Ambiguous(strongest)
            }
        }

        return if (candidates.size == 1) {
            LyricsMatchResult.Match(candidates.single())
        } else {
            LyricsMatchResult.Ambiguous(candidates)
        }
    }

    private fun directoryScore(
        song: SongLyricsIdentity,
        file: IndexedLyricsFile
    ): Int {
        val songVolume = normalizeVolumeId(song.volumeId)
        val fileVolume = normalizeVolumeId(file.rootVolumeId)
        if (songVolume != null && fileVolume != null && songVolume != fileVolume) return 0

        val relativeDepth = directorySuffixMatchDepth(
            song.relativeDirectory,
            file.relativeDirectory
        )
        val fallbackDepth = directorySuffixMatchDepth(
            song.fallbackDirectory,
            file.relativeDirectory
        )
        val depth = maxOf(relativeDepth, fallbackDepth)
        if (depth == 0) return 0

        val exactBonus = if (
            normalizeLyricsPath(song.relativeDirectory) ==
            normalizeLyricsPath(file.relativeDirectory)
        ) {
            10_000
        } else {
            0
        }
        val volumeBonus = if (songVolume != null && songVolume == fileVolume) 1_000 else 0
        return exactBonus + volumeBonus + depth
    }
}

internal val indexedLyricsFileComparator =
    compareBy<IndexedLyricsFile>(
        { normalizeLyricsPath(it.rootUri) },
        { normalizeLyricsPath(it.relativeDirectory) },
        { it.normalizedStem },
        { it.documentUri }
    )

private fun String.pathSegments(): List<String> =
    takeIf(String::isNotEmpty)?.split('/') ?: emptyList()
