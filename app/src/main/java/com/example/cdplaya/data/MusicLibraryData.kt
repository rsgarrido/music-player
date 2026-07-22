package com.example.cdplaya.data

import java.io.File

data class MusicLibraryData(
    val songs: List<Song>,
    val libraryFolders: List<LibraryFolder>,
    /** Complete snapshot used for identity reconciliation even when folder filtering is active. */
    val referenceSongs: List<Song> = songs
)

fun buildMusicLibraryData(
    allSongs: List<Song>,
    selectedFolders: Set<String> = emptySet()
): MusicLibraryData {
    val filteredSongs = if (selectedFolders.isEmpty()) {
        allSongs
    } else {
        allSongs.filter { song ->
            selectedFolders.contains(song.folderPath)
        }
    }

    return MusicLibraryData(
        songs = filteredSongs,
        libraryFolders = buildLibraryFolders(allSongs),
        referenceSongs = allSongs
    )
}

fun buildLibraryFolders(songs: List<Song>): List<LibraryFolder> {
    return songs
        .groupBy { song -> song.folderPath }
        .map { entry ->
            val folderPath = entry.key
            val folderName = File(folderPath).name.ifBlank {
                folderPath
            }

            LibraryFolder(
                path = folderPath,
                name = folderName,
                songCount = entry.value.size
            )
        }
        .sortedBy { folder ->
            folder.name.lowercase()
        }
}
