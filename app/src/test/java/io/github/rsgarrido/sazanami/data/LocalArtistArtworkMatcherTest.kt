package io.github.rsgarrido.sazanami.data

import android.net.Uri
import android.content.Context
import android.content.ContentResolver
import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class LocalArtistArtworkMatcherTest {
    @Test fun supportedNamesAreExactAndCaseInsensitive() {
        assertEquals(0, artistArtworkFilenamePriority("ARTIST.JPG"))
        assertEquals(1, artistArtworkFilenamePriority("artist.jpeg"))
        assertEquals(2, artistArtworkFilenamePriority("Artist.PnG"))
        assertEquals(null, artistArtworkFilenamePriority("artist.webp"))
        assertEquals(null, artistArtworkFilenamePriority("my-artist.jpg"))
        assertTrue(isLikelyAlbumCoverFile("COVER.JPG"))
    }

    @Test fun oneDirectoryUsesDeterministicFilenamePriority() {
        val png = candidate("The Warning", "png")
        val jpeg = candidate("The Warning", "jpeg")
        val jpg = candidate("The Warning", "jpg")
        var selected: Pair<Int, LocalArtworkCandidate>? = null
        selected = preferArtistCandidate(selected, 2 to png)
        selected = preferArtistCandidate(selected, 1 to jpeg)
        selected = preferArtistCandidate(selected, 0 to jpg)
        assertEquals(jpg, requireNotNull(selected).second)
        assertEquals(jpg, preferArtistCandidate(selected, 2 to png).second)
    }

    @Test fun artistAlbumAndDirectTracksResolveToOneArtist() {
        val image = candidate("The Warning", "jpg")
        val songs = listOf(
            song("The Warning", "Music/The Warning/ERROR/"),
            song("The Warning", "Music/The Warning/Queen of the Murder Scene/"),
            song("The Warning", "Music/The Warning/")
        )
        assertEquals(image, match("Music", listOf(image), songs)[artistIdentity("The Warning").key])
    }

    @Test fun compilationDoesNotLeakOntoTrackArtistsOrAlbumArtist() {
        val image = candidate("Various Artists", "jpg")
        val songs = listOf(
            song("Artist A", "Music/Various Artists/Compilation/", "Various Artists"),
            song("Artist B", "Music/Various Artists/Compilation/", "Various Artists")
        )
        assertTrue(match("Music", listOf(image), songs).isEmpty())
    }

    @Test fun fullFeaturedIdentityIsNotReducedToDirectoryArtist() {
        val image = candidate("Artist", "jpg")
        assertTrue(match("Music", listOf(image),
            listOf(song("Artist feat. Guest", "Music/Artist/Album/"))).isEmpty())
    }

    @Test fun mixedArtistDescendantsRefuseTheCandidate() {
        val image = candidate("Artist", "jpg")
        assertTrue(match("Music", listOf(image), listOf(
            song("Artist", "Music/Artist/Album/"),
            song("Guest", "Music/Artist/Album/")
        )).isEmpty())
    }

    @Test fun twoDistinctDirectoriesAreAmbiguousButDuplicateUriIsNot() {
        val first = candidate("The Warning", "one")
        val second = candidate("Other Music/The Warning", "two")
        val songs = listOf(
            song("The Warning", "Music/The Warning/Album/"),
            song("The Warning", "Music/Other Music/The Warning/Album/")
        )
        assertTrue(match("Music", listOf(first, second), songs).isEmpty())
        assertEquals(first, match("Music", listOf(first, first), songs)[artistIdentity("The Warning").key])
    }

    @Test fun volumeAndFullPathMustMatch() {
        val image = candidate("The Warning", "jpg")
        assertTrue(match("Music", listOf(image),
            listOf(song("The Warning", "Elsewhere/The Warning/Album/"))).isEmpty())
        assertTrue(match("Music", listOf(image),
            listOf(song("The Warning", "Music/The Warning/Album/", volume = "1234-5678"))).isEmpty())
        assertTrue(match("Music", listOf(image),
            listOf(song("The Warning", "Music/The Warning/Album/", volume = ""))).isEmpty())
        assertFalse(match("Music", listOf(image),
            listOf(song("The Warning", "Music/The Warning/Album/"))).isEmpty())
    }

    @Test fun sameUriReceivesStableDigestUntilItsBytesChange() {
        val context = mock(Context::class.java)
        val resolver = mock(ContentResolver::class.java)
        val uri = mock(Uri::class.java)
        `when`(context.contentResolver).thenReturn(resolver)
        `when`(resolver.openInputStream(uri))
            .thenAnswer { ByteArrayInputStream("first".toByteArray()) }
            .thenAnswer { ByteArrayInputStream("first".toByteArray()) }
            .thenAnswer { ByteArrayInputStream("second".toByteArray()) }
        val first = hashArtistImage(context, uri)
        assertEquals(first, hashArtistImage(context, uri))
        assertTrue(first != hashArtistImage(context, uri))
    }

    @Test fun newScanReplacesAddedRenamedAndDeletedCandidates() {
        val songs = listOf(song("Artist", "Music/Artist/Album/"))
        val jpg = candidate("Artist", "jpg")
        val png = candidate("Artist", "png")
        val key = artistIdentity("Artist").key

        assertTrue(match("Music", emptyList(), songs).isEmpty())
        assertEquals(jpg, match("Music", listOf(jpg), songs)[key])
        assertEquals(png, match("Music", listOf(png), songs)[key])
        assertTrue(match("Music", emptyList(), songs).isEmpty())
    }

    private fun match(root: String, candidates: List<LocalArtworkCandidate>, songs: List<Song>) =
        matchLocalArtistArtwork("primary", root, candidates, songs)

    private fun candidate(directory: String, suffix: String): LocalArtworkCandidate {
        val uri = mock(Uri::class.java)
        doReturn("content://artist/$suffix").`when`(uri).toString()
        return LocalArtworkCandidate(directory, uri, null, null)
    }

    private fun song(
        artist: String, relativePath: String, albumArtist: String = "", volume: String = "external_primary"
    ) = Song(
        id = 1, title = "Track", artist = artist, album = "Album", trackNumber = 1,
        duration = 1000, uri = mock(Uri::class.java), filePath = "/storage/emulated/0/$relativePath/track.flac",
        folderPath = "/storage/emulated/0/$relativePath", albumArtUri = null,
        albumArtist = albumArtist, volumeName = volume, relativePath = relativePath
    )
}
