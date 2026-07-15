package com.example.cdplaya.data.playlistfile

import com.example.cdplaya.data.Song

data class PreparedPlaylistExport(
    val playlistName: String,
    val songs: List<Song>,
    val unavailableSongCount: Int
)
