package com.example.cdplaya.ui.player.classicwheel

import com.example.cdplaya.data.Song
import java.io.File

fun buildClassicWheelMainMenuItems(): List<ClassicWheelMenuItem> {
    return listOf(
        ClassicWheelMenuItem(
            title = "Now Playing",
            action = ClassicWheelMenuAction.OPEN_NOW_PLAYING
        ),
        ClassicWheelMenuItem(
            title = "Songs",
            action = ClassicWheelMenuAction.OPEN_SONGS
        ),
        ClassicWheelMenuItem(
            title = "Artists",
            action = ClassicWheelMenuAction.OPEN_ARTISTS
        ),
        ClassicWheelMenuItem(
            title = "Albums",
            action = ClassicWheelMenuAction.OPEN_ALBUMS
        )
    )
}


fun buildClassicWheelSongMenuItems(
    songs: List<Song>
): List<ClassicWheelMenuItem> {
    if (songs.isEmpty()) {
        return listOf(
            ClassicWheelMenuItem(
                title = "No songs found",
                subtitle = "Check your library",
                action = ClassicWheelMenuAction.OPEN_NOW_PLAYING
            )
        )
    }

    return songs.map { song ->
        ClassicWheelMenuItem(
            title = song.title.ifBlank { "Unknown Title" },
            subtitle = song.artist.ifBlank { "Unknown Artist" },
            action = ClassicWheelMenuAction.OPEN_NOW_PLAYING
        )
    }
}

fun buildClassicWheelArtistGroups(
    songs: List<Song>
): List<ClassicWheelArtistGroup> {
    return songs
        .groupBy { song ->
            song.artist.ifBlank { "Unknown Artist" }
        }
        .map { entry ->
            ClassicWheelArtistGroup(
                name = entry.key,
                songs = sortClassicWheelArtistSongs(entry.value)
            )
        }
        .sortedBy { artistGroup ->
            artistGroup.name.lowercase()
        }
}

fun buildClassicWheelArtistMenuItems(
    artistGroups: List<ClassicWheelArtistGroup>
): List<ClassicWheelMenuItem> {
    if (artistGroups.isEmpty()) {
        return listOf(
            ClassicWheelMenuItem(
                title = "No artists found",
                subtitle = "Check your library",
                action = ClassicWheelMenuAction.OPEN_NOW_PLAYING
            )
        )
    }

    return artistGroups.map { artistGroup ->
        ClassicWheelMenuItem(
            title = artistGroup.name,
            subtitle = "${artistGroup.songs.size} songs",
            action = ClassicWheelMenuAction.OPEN_NOW_PLAYING
        )
    }
}

fun buildClassicWheelAlbumGroups(
    songs: List<Song>
): List<ClassicWheelAlbumGroup> {
    return songs
        .groupBy { song ->
            buildClassicWheelAlbumKey(song)
        }
        .map { entry ->
            val albumSongs = sortClassicWheelAlbumSongs(entry.value)
            val firstSong = albumSongs.first()

            ClassicWheelAlbumGroup(
                key = entry.key,
                title = firstSong.album.ifBlank { "Unknown Album" },
                artist = firstSong.artist.ifBlank { "Unknown Artist" },
                songs = albumSongs
            )
        }
        .sortedBy { albumGroup ->
            albumGroup.title.lowercase()
        }
}

fun buildClassicWheelAlbumMenuItems(
    albumGroups: List<ClassicWheelAlbumGroup>
): List<ClassicWheelMenuItem> {
    if (albumGroups.isEmpty()) {
        return listOf(
            ClassicWheelMenuItem(
                title = "No albums found",
                subtitle = "Check your library",
                action = ClassicWheelMenuAction.OPEN_NOW_PLAYING
            )
        )
    }

    return albumGroups.map { albumGroup ->
        ClassicWheelMenuItem(
            title = albumGroup.title,
            subtitle = albumGroup.artist,
            action = ClassicWheelMenuAction.OPEN_NOW_PLAYING
        )
    }
}

private fun buildClassicWheelAlbumKey(song: Song): String {
    val albumTitle = song.album.ifBlank { "Unknown Album" }
    val artistName = song.artist.ifBlank { "Unknown Artist" }
    val folderPath = File(song.filePath).parent ?: ""

    return "$albumTitle|$artistName|$folderPath"
}


fun buildClassicWheelAlbumCarouselItems(
    albumGroups: List<ClassicWheelAlbumGroup>
): List<ClassicWheelAlbumCarouselItem> {
    return albumGroups.map { albumGroup ->
        ClassicWheelAlbumCarouselItem(
            title = albumGroup.title,
            artist = albumGroup.artist,
            albumArtUri = albumGroup.songs.firstOrNull()?.albumArtUri
        )
    }
}


private fun sortClassicWheelArtistSongs(
    songs: List<Song>
): List<Song> {
    return songs.sortedWith(
        compareBy<Song> { song ->
            song.album.ifBlank { "Unknown Album" }.lowercase()
        }.thenBy { song ->
            song.trackNumber.takeIf { trackNumber ->
                trackNumber > 0
            } ?: Int.MAX_VALUE
        }.thenBy { song ->
            song.title.lowercase()
        }
    )
}


private fun sortClassicWheelAlbumSongs(
    songs: List<Song>
): List<Song> {
    return songs.sortedWith(
        compareBy<Song> { song ->
            song.trackNumber.takeIf { trackNumber ->
                trackNumber > 0
            } ?: Int.MAX_VALUE
        }.thenBy { song ->
            song.title.lowercase()
        }
    )
}

data class ClassicWheelArtistGroup(
    val name: String,
    val songs: List<Song>
)

data class ClassicWheelAlbumGroup(
    val key: String,
    val title: String,
    val artist: String,
    val songs: List<Song>
)