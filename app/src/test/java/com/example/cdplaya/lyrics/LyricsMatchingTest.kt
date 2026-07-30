package com.example.cdplaya.lyrics

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsMatchingTest {
    @Test
    fun filenameNormalizationHandlesExtensionsWhitespaceAndMultipleDots() {
        assertEquals("thrill", normalizeFileStem("Thrill.flac"))
        assertEquals("thrill", normalizeFileStem("Thrill.LRC"))
        assertEquals("song.live", normalizeFileStem(" Song.live.flac "))
        assertTrue(hasLrcExtension("Track.LrC"))
    }

    @Test
    fun filenameNormalizationUsesNfcAndLocaleIndependentCase() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals(normalizeFileStem("Cafe\u0301.FLAC"), normalizeFileStem("Café.lrc"))
            assertEquals("idol", normalizeFileStem("IDOL.FLAC"))
            assertEquals("アイドル", normalizeFileStem("アイドル.flac"))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun filenameNormalizationPreservesMeaningfulPunctuation() {
        assertNotEquals(normalizeFileStem("Song-live.flac"), normalizeFileStem("Song live.lrc"))
        assertEquals("01 - thrill", normalizeFileStem("01 - Thrill.flac"))
    }

    @Test
    fun pathNormalizationHandlesSlashesSeparatorsUnicodeAndEmptyPaths() {
        assertEquals("music/band/album", normalizeLyricsPath("\\Music//BAND\\Album/"))
        assertEquals("音楽/バンド", normalizeLyricsPath("/音楽//バンド/"))
        assertEquals("", normalizeLyricsPath(null))
        assertEquals("", normalizeLyricsPath("///"))
    }

    @Test
    fun pathSuffixRequiresCompleteMatchingDirectorySegments() {
        assertEquals(
            2,
            directorySuffixMatchDepth("Music/BAND-MAID/New Beginning", "BAND-MAID/New Beginning")
        )
        assertEquals(0, directorySuffixMatchDepth("Music/My Album", "Music/Album"))
        assertEquals(0, directorySuffixMatchDepth("", "Music/Album"))
    }

    @Test
    fun exactSiblingAndExactRelativeDirectoryMatch() {
        val result = LocalLyricsMatcher.match(
            song("Thrill.flac", "Music/Band/Album"),
            listOf(file("Thrill.lrc", "Music/Band/Album"))
        )

        assertTrue(result is LyricsMatchResult.Match)
    }

    @Test
    fun validDirectorySuffixMatchWins() {
        val preferred = file("Thrill.lrc", "BAND-MAID/New Beginning", uri = "content://preferred")
        val other = file("Thrill.lrc", "Other Album", uri = "content://other")

        assertEquals(
            preferred,
            (LocalLyricsMatcher.match(
                song("Thrill.flac", "Music/BAND-MAID/New Beginning"),
                listOf(other, preferred)
            ) as LyricsMatchResult.Match).file
        )
    }

    @Test
    fun uniqueExactStemFallsBackWithoutDirectoryMatch() {
        val only = file("Track.LRC", "Unrelated")

        assertEquals(
            only,
            (LocalLyricsMatcher.match(song("track.flac", ""), listOf(only))
                as LyricsMatchResult.Match).file
        )
    }

    @Test
    fun sameFilenameInTwoAlbumsIsAmbiguousWithoutDirectoryEvidence() {
        val result = LocalLyricsMatcher.match(
            song("Intro.flac", ""),
            listOf(file("Intro.lrc", "Album A", "content://a"), file("Intro.lrc", "Album B", "content://b"))
        )

        assertTrue(result is LyricsMatchResult.Ambiguous)
    }

    @Test
    fun volumeIdentityDistinguishesInternalStorageFromSdCard() {
        val internal = file(
            "Intro.lrc",
            "Music/Album",
            uri = "content://internal",
            volume = "primary"
        )
        val sdCard = file(
            "Intro.lrc",
            "Music/Album",
            uri = "content://sd",
            volume = "1234-5678"
        )

        val result = LocalLyricsMatcher.match(
            song("Intro.flac", "Music/Album", volume = "external_primary"),
            listOf(sdCard, internal)
        ) as LyricsMatchResult.Match

        assertEquals(internal, result.file)
    }

    @Test
    fun uniquelyStrongestPathCandidateWins() {
        val shallow = file("Track.lrc", "Album", "content://shallow")
        val deep = file("Track.lrc", "Artist/Album", "content://deep")

        val result = LocalLyricsMatcher.match(
            song("Track.flac", "Music/Artist/Album"),
            listOf(shallow, deep)
        ) as LyricsMatchResult.Match

        assertEquals(deep, result.file)
    }

    @Test
    fun equalStrongestCandidatesRemainAmbiguous() {
        val result = LocalLyricsMatcher.match(
            song("Track.flac", "Music/Album"),
            listOf(
                file("Track.lrc", "Album", "content://a"),
                file("Track.lrc", "Album", "content://b")
            )
        )

        assertTrue(result is LyricsMatchResult.Ambiguous)
    }

    @Test
    fun noMatchingStemAndNoTitleFallback() {
        assertEquals(
            LyricsMatchResult.NotFound,
            LocalLyricsMatcher.match(
                song("01 - Thrill.flac", "Music/Album"),
                listOf(file("Thrill.lrc", "Music/Album"))
            )
        )
    }

    @Test
    fun japaneseFilenameMatchesAndResultIsIndependentOfInputOrder() {
        val preferred = file("アイドル.LRC", "音楽/アルバム", "content://preferred")
        val other = file("アイドル.lrc", "別", "content://other")
        val identity = song("アイドル.flac", "Music/音楽/アルバム")

        val first = LocalLyricsMatcher.match(identity, listOf(preferred, other))
        val second = LocalLyricsMatcher.match(identity, listOf(other, preferred))

        assertEquals(first, second)
        assertEquals(preferred, (first as LyricsMatchResult.Match).file)
    }

    private fun song(
        name: String,
        relativeDirectory: String,
        volume: String? = null
    ) = SongLyricsIdentity(
        audioFileName = name,
        relativeDirectory = relativeDirectory,
        fallbackDirectory = "",
        volumeId = volume
    )

    private fun file(
        name: String,
        directory: String,
        uri: String = "content://lyrics/$name",
        volume: String? = null
    ) = IndexedLyricsFile(
        documentUri = uri,
        rootUri = "content://root",
        displayName = name,
        normalizedStem = normalizeFileStem(name),
        relativeDirectory = directory,
        rootVolumeId = volume
    )
}
