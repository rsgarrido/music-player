package io.github.rsgarrido.sazanami.ui.player.classicwheel

import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.ui.library.buildLibraryAlbumGroups
import io.github.rsgarrido.sazanami.ui.state.UiMessage

fun buildClassicWheelMainMenuItems(): List<ClassicWheelMenuItem> {
    return listOf(
        ClassicWheelMenuItem(
            title = UiMessage.Text(R.string.player_wheel_now_playing),
            action = ClassicWheelMenuAction.OPEN_NOW_PLAYING
        ),
        ClassicWheelMenuItem(
            title = UiMessage.Text(R.string.player_wheel_songs),
            action = ClassicWheelMenuAction.OPEN_SONGS
        ),
        ClassicWheelMenuItem(
            title = UiMessage.Text(R.string.player_wheel_artists),
            action = ClassicWheelMenuAction.OPEN_ARTISTS
        ),
        ClassicWheelMenuItem(
            title = UiMessage.Text(R.string.player_wheel_albums),
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
                title = UiMessage.Text(R.string.player_wheel_no_songs),
                subtitle = UiMessage.Text(R.string.player_wheel_check_library),
                action = ClassicWheelMenuAction.OPEN_NOW_PLAYING
            )
        )
    }

    return songs.map { song ->
        ClassicWheelMenuItem(
            title = song.title.takeIf(String::isNotBlank)?.let { UiMessage.Literal(it) }
                ?: UiMessage.Text(R.string.player_unknown_title),
            subtitle = song.artist.takeIf(String::isNotBlank)?.let { UiMessage.Literal(it) }
                ?: UiMessage.Text(R.string.player_unknown_artist),
            action = ClassicWheelMenuAction.OPEN_NOW_PLAYING
        )
    }
}

fun buildClassicWheelArtistGroups(
    songs: List<Song>
): List<ClassicWheelArtistGroup> {
    return songs
        .groupBy { song ->
            song.artist
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
                title = UiMessage.Text(R.string.player_wheel_no_artists),
                subtitle = UiMessage.Text(R.string.player_wheel_check_library),
                action = ClassicWheelMenuAction.OPEN_NOW_PLAYING
            )
        )
    }

    return artistGroups.map { artistGroup ->
        ClassicWheelMenuItem(
            title = artistGroup.name.takeIf(String::isNotBlank)?.let { UiMessage.Literal(it) }
                ?: UiMessage.Text(R.string.player_unknown_artist),
            subtitle = UiMessage.Quantity(R.plurals.player_wheel_song_count, artistGroup.songs.size),
            action = ClassicWheelMenuAction.OPEN_NOW_PLAYING
        )
    }
}

fun buildClassicWheelAlbumGroups(
    songs: List<Song>
): List<ClassicWheelAlbumGroup> {
    return buildLibraryAlbumGroups(songs)
        .map { albumGroup ->
            ClassicWheelAlbumGroup(
                key = albumGroup.key,
                title = albumGroup.title,
                artist = albumGroup.artistText,
                songs = albumGroup.songs
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
                title = UiMessage.Text(R.string.player_wheel_no_albums),
                subtitle = UiMessage.Text(R.string.player_wheel_check_library),
                action = ClassicWheelMenuAction.OPEN_NOW_PLAYING
            )
        )
    }

    return albumGroups.map { albumGroup ->
        ClassicWheelMenuItem(
            title = UiMessage.Literal(albumGroup.title),
            subtitle = UiMessage.Literal(albumGroup.artist),
            action = ClassicWheelMenuAction.OPEN_NOW_PLAYING
        )
    }
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
