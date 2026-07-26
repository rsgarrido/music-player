package com.example.cdplaya.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import android.net.Uri
import org.mockito.Mockito.mock

class FolderSelectionTest {
    private val discovered = setOf("Music/Albums", "Music/Live", "Podcasts")

    @Test
    fun freshAndLegacyEmptySelectionsResolveToAll() {
        assertEquals(FolderSelectionMode.ALL, FolderSelection.All.mode)
        assertEquals(
            FolderSelectionMode.ALL,
            FolderSelection.fromStored(null, emptySet()).mode
        )
    }

    @Test
    fun legacyNonEmptySelectionResolvesToCustom() {
        val selection = FolderSelection.fromStored(null, setOf("Music/Live"))

        assertEquals(FolderSelectionMode.CUSTOM, selection.mode)
        assertEquals(setOf("Music/Live"), selection.customFolders)
    }

    @Test
    fun allRendersEveryDiscoveredFolderEnabled() {
        val enabled = FolderSelection.All.effectiveFolders(discovered)

        assertEquals(discovered, enabled)
        assertTrue(discovered.all(FolderSelection.All::includes))
    }

    @Test
    fun togglingOneFolderFromAllCreatesCustomSelectionWithoutOnlyThatFolder() {
        val selection = FolderSelection.All.toggle("Music/Live", discovered)

        assertEquals(FolderSelectionMode.CUSTOM, selection.mode)
        assertEquals(discovered - "Music/Live", selection.customFolders)
        assertFalse(selection.includes("Music/Live"))
    }

    @Test
    fun selectAllRestoresDynamicAllBehavior() {
        val all = FolderSelection.All

        assertTrue(all.includes("Music/Newly discovered"))
        assertEquals(
            discovered + "Music/Newly discovered",
            all.effectiveFolders(discovered + "Music/Newly discovered")
        )
    }

    @Test
    fun customEmptySelectionShowsNoSongsAndIsNotAll() {
        val selection = FolderSelection(FolderSelectionMode.CUSTOM, emptySet())
        val data = buildMusicLibraryData(
            allSongs = listOf(song("Music/Albums"), song("Podcasts")),
            folderSelection = selection
        )

        assertEquals(FolderSelectionMode.CUSTOM, selection.mode)
        assertTrue(data.songs.isEmpty())
    }

    @Test
    fun newlyDiscoveredFoldersAreIncludedOnlyInAll() {
        val custom = FolderSelection(
            FolderSelectionMode.CUSTOM,
            setOf("Music/Albums")
        )

        assertTrue(FolderSelection.All.includes("Music/New"))
        assertFalse(custom.includes("Music/New"))
    }

    private fun song(folder: String) = Song(
        id = folder.hashCode().toLong(),
        title = "Song",
        artist = "Artist",
        album = "Album",
        trackNumber = 1,
        duration = 1L,
        uri = mock(Uri::class.java),
        filePath = "$folder/song.flac",
        folderPath = folder,
        albumArtUri = null
    )
}
