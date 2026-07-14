package com.example.cdplaya.ui.library

import com.example.cdplaya.data.Song
import com.example.cdplaya.ui.sortSongsForArtistDetail

data class LibraryArtistGroup(
    val name: String,
    val songs: List<Song>
)

fun buildLibraryArtistGroups(songs: List<Song>): List<LibraryArtistGroup> {
    return songs
        .groupBy { song -> song.artist.ifBlank { "Unknown Artist" } }
        .map { (name, artistSongs) ->
            LibraryArtistGroup(
                name = name,
                songs = sortSongsForArtistDetail(artistSongs)
            )
        }
}
