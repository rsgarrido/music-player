package com.example.cdplaya.ui

import com.example.cdplaya.data.Song

fun filterSongsForSearch(
    songs: List<Song>,
    searchQuery: String
): List<Song> {
    val query = searchQuery.trim()

    if (query.isBlank()) {
        return songs
    }

    return songs.filter { song ->
        song.title.contains(query, ignoreCase = true) ||
                song.artist.contains(query, ignoreCase = true) ||
                song.album.contains(query, ignoreCase = true)
    }
}

fun filterSongsByArtistSearch(
    songs: List<Song>,
    searchQuery: String
): List<Song> {
    val query = searchQuery.trim()

    if (query.isBlank()) {
        return songs
    }

    val matchingArtists = songs
        .filter { song ->
            song.artist.ifBlank { "Unknown Artist" }
                .contains(query, ignoreCase = true)
        }
        .map { song ->
            song.artist.ifBlank { "Unknown Artist" }
        }
        .toSet()

    return songs.filter { song ->
        song.artist.ifBlank { "Unknown Artist" } in matchingArtists
    }
}

fun filterSongsByAlbumSearch(
    songs: List<Song>,
    searchQuery: String
): List<Song> {
    val query = searchQuery.trim()

    if (query.isBlank()) {
        return songs
    }

    val matchingAlbumFolders = songs
        .filter { song ->
            song.album.ifBlank { "Unknown Album" }
                .contains(query, ignoreCase = true) ||
                    song.artist.ifBlank { "Unknown Artist" }
                        .contains(query, ignoreCase = true)
        }
        .map { song ->
            song.folderPath
        }
        .toSet()

    return songs.filter { song ->
        song.folderPath in matchingAlbumFolders
    }
}

fun sortSongsByAlbumOrder(songs: List<Song>): List<Song> {
    return songs.sortedWith(
        compareBy<Song> { song ->
            if (song.trackNumber > 0) song.trackNumber else Int.MAX_VALUE
        }.thenBy { song ->
            song.title.lowercase()
        }
    )
}

fun getDisplayTrackNumber(trackNumber: Int): String {
    if (trackNumber <= 0) {
        return "–"
    }

    val normalizedTrackNumber = trackNumber % 1000

    return if (normalizedTrackNumber > 0) {
        normalizedTrackNumber.toString()
    } else {
        trackNumber.toString()
    }
}

fun formatDuration(milliseconds: Int): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "%d:%02d".format(minutes, seconds)
}