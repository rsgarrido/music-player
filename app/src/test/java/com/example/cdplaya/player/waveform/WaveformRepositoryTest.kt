package com.example.cdplaya.player.waveform

import android.net.Uri
import com.example.cdplaya.data.Song
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock

class WaveformRepositoryTest {
    @Test
    fun load_analyzesOnceThenUsesCache() = runBlocking {
        val directory = Files.createTempDirectory("waveform-repository-test").toFile()
        var analysisCount = 0
        val source = WaveformSource(7L, "/music/example.flac", 10L, 20L)
        val repository = repository(
            directory = directory,
            sourceResolver = WaveformSourceResolver { source },
            analyzer = WaveformAnalyzer { _, sourceKey, barCount ->
                analysisCount++
                WaveformData(List(barCount) { 0.5f }, sourceKey)
            }
        )

        val first = repository.load(song())
        val second = repository.load(song())

        assertEquals(1, analysisCount)
        assertEquals(first, second)
        assertEquals(WaveformRepository.DEFAULT_ANALYZED_BAR_COUNT, first?.amplitudes?.size)
        directory.deleteRecursively()
        Unit
    }

    @Test
    fun load_fileMetadataChangeInvalidatesCache() = runBlocking {
        val directory = Files.createTempDirectory("waveform-repository-test").toFile()
        var analysisCount = 0
        var lastModified = 10L
        val repository = repository(
            directory = directory,
            sourceResolver = WaveformSourceResolver {
                WaveformSource(7L, "/music/example.flac", lastModified, 20L)
            },
            analyzer = WaveformAnalyzer { _, sourceKey, _ ->
                analysisCount++
                WaveformData(listOf(1f), sourceKey)
            }
        )

        repository.load(song())
        lastModified++
        repository.load(song())

        assertEquals(2, analysisCount)
        directory.deleteRecursively()
        Unit
    }

    @Test
    fun load_analysisFailureIsNotCachedAndReturnsNull() = runBlocking {
        val directory = Files.createTempDirectory("waveform-repository-test").toFile()
        var analysisCount = 0
        val repository = repository(
            directory = directory,
            sourceResolver = WaveformSourceResolver {
                WaveformSource(7L, "/music/unsupported.xyz", 10L, 20L)
            },
            analyzer = WaveformAnalyzer { _, _, _ ->
                analysisCount++
                null
            }
        )

        assertNull(repository.load(song()))
        assertNull(repository.load(song()))

        assertEquals(2, analysisCount)
        directory.deleteRecursively()
        Unit
    }

    private fun repository(
        directory: java.io.File,
        sourceResolver: WaveformSourceResolver,
        analyzer: WaveformAnalyzer
    ) = WaveformRepository(
        cache = WaveformCache(directory),
        analyzer = analyzer,
        sourceResolver = sourceResolver,
        ioDispatcher = Dispatchers.Unconfined
    )

    private fun song() = Song(
        id = 7L,
        title = "Example",
        artist = "Artist",
        album = "Album",
        trackNumber = 1,
        duration = 180_000L,
        uri = mock(Uri::class.java),
        filePath = "/music/example.flac",
        folderPath = "/music",
        albumArtUri = null
    )
}
