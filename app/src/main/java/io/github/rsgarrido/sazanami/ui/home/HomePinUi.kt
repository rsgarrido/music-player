package io.github.rsgarrido.sazanami.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.rsgarrido.sazanami.data.Playlist
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.data.ArtistIdentity
import io.github.rsgarrido.sazanami.data.ArtistPictureAssignment
import io.github.rsgarrido.sazanami.data.SongReferenceIndex
import io.github.rsgarrido.sazanami.data.SongReferenceResolution
import io.github.rsgarrido.sazanami.data.home.HomePin
import io.github.rsgarrido.sazanami.data.home.HomePinType
import io.github.rsgarrido.sazanami.data.membershipKey
import io.github.rsgarrido.sazanami.ui.AppShellTypography
import io.github.rsgarrido.sazanami.ui.library.LibraryAlbumGroup
import io.github.rsgarrido.sazanami.ui.library.LibraryArtistGroup
import io.github.rsgarrido.sazanami.ui.library.LibraryItemAction
import io.github.rsgarrido.sazanami.ui.library.LibraryActionId
import io.github.rsgarrido.sazanami.ui.library.buildLibraryAlbumGroups
import io.github.rsgarrido.sazanami.ui.library.findLibraryAlbumGroupForSong
import io.github.rsgarrido.sazanami.ui.library.buildLibraryArtistGroups
import java.util.Locale

sealed interface HomePinTarget {
    data class SongTarget(val song: Song) : HomePinTarget
    data class AlbumTarget(val album: LibraryAlbumGroup) : HomePinTarget
    data class ArtistTarget(val artist: LibraryArtistGroup) : HomePinTarget
    data class PlaylistTarget(val playlist: Playlist) : HomePinTarget
}

@Immutable
data class ResolvedHomePin(
    val pin: HomePin,
    val target: HomePinTarget?
) {
    val title: String
        get() = when (val currentTarget = target) {
            is HomePinTarget.SongTarget -> currentTarget.song.title
            is HomePinTarget.AlbumTarget -> currentTarget.album.title
            is HomePinTarget.ArtistTarget -> currentTarget.artist.name
            is HomePinTarget.PlaylistTarget -> currentTarget.playlist.name
            null -> pin.title
        }

    val subtitle: String
        get() = when (val currentTarget = target) {
            is HomePinTarget.SongTarget -> currentTarget.song.artist
            is HomePinTarget.AlbumTarget -> currentTarget.album.artistText
            is HomePinTarget.ArtistTarget -> ""
            is HomePinTarget.PlaylistTarget -> ""
            null -> pin.subtitle
        }

    val artworkUri: Any?
        get() = when (val currentTarget = target) {
            is HomePinTarget.SongTarget -> currentTarget.song.albumArtUri
            is HomePinTarget.AlbumTarget -> currentTarget.album.songs.firstOrNull()?.albumArtUri
            is HomePinTarget.ArtistTarget -> currentTarget.artist.songs.firstOrNull()?.albumArtUri
            is HomePinTarget.PlaylistTarget -> null
            null -> null
        }

}

@Composable
internal fun ResolvedHomePin.localizedTitle(): String = when (val currentTarget = target) {
    is HomePinTarget.SongTarget -> currentTarget.song.title.ifBlank {
        stringResource(R.string.library_song_unknown_title)
    }
    else -> title
}

@Composable
internal fun ResolvedHomePin.localizedTypeLabel(): String = stringResource(when (pin.type) {
    HomePinType.SONG -> R.string.home_pin_type_song
    HomePinType.ALBUM -> R.string.home_pin_type_album
    HomePinType.ARTIST -> R.string.home_pin_type_artist
    HomePinType.PLAYLIST -> R.string.home_pin_type_playlist
})

internal fun ResolvedHomePin.artistPictureIdentityOrNull(): ArtistIdentity? =
    (target as? HomePinTarget.ArtistTarget)
        ?.artist
        ?.identity
        ?.takeIf { identity -> identity.supportsCustomPicture }

internal fun ResolvedHomePin.artistPictureAssignmentOrNull(
    assignments: Map<String, ArtistPictureAssignment>
): ArtistPictureAssignment? = artistPictureIdentityOrNull()?.let { identity ->
    assignments[identity.key]
}

@Immutable
data class HomeCustomizationUiState(
    val pins: List<HomePin> = emptyList(),
    val showRecentlyAddedOnHome: Boolean = true,
    val isLoaded: Boolean = false
)

@Immutable
data class HomePinUiEnvironment(
    val pins: List<ResolvedHomePin> = emptyList(),
    val showRecentlyAddedOnHome: Boolean = true,
    val resolveString: (Int) -> String = { "" },
    val onPinRequested: (HomePin) -> Unit = {},
    val onUnpinRequested: (String) -> Unit = {},
    val onMovePinRequested: (String, Int) -> Unit = { _, _ -> },
    val onShowRecentlyAddedChanged: (Boolean) -> Unit = {}
) {
    fun pinForSong(song: Song): ResolvedHomePin? {
        val key = song.membershipKey()
        return pins.firstOrNull { resolved ->
            val target = resolved.target as? HomePinTarget.SongTarget
            target?.song?.membershipKey() == key
        }
    }

    fun pinForAlbum(album: LibraryAlbumGroup): ResolvedHomePin? = pins.firstOrNull { resolved ->
        val target = resolved.target as? HomePinTarget.AlbumTarget
        target?.album?.key == album.key
    }

    fun pinForArtist(artist: LibraryArtistGroup): ResolvedHomePin? = pins.firstOrNull { resolved ->
        val target = resolved.target as? HomePinTarget.ArtistTarget
        target?.artist?.name.equals(artist.name, ignoreCase = true)
    }

    fun pinForPlaylist(playlist: Playlist): ResolvedHomePin? = pins.firstOrNull { resolved ->
        val target = resolved.target as? HomePinTarget.PlaylistTarget
        target?.playlist?.playlistId == playlist.playlistId
    }

    fun actionForSong(song: Song): LibraryItemAction {
        val existing = pinForSong(song)
        return LibraryItemAction(
            label = resolveString(if (existing == null) R.string.home_pin_to_home else R.string.home_unpin_from_home),
            icon = Icons.Filled.PushPin,
            id = LibraryActionId.HOME_PIN,
            onClick = {
                if (existing == null) {
                    onPinRequested(HomePin.song(song))
                } else {
                    onUnpinRequested(existing.pin.id)
                }
            }
        )
    }

    fun actionForAlbum(album: LibraryAlbumGroup): LibraryItemAction {
        val existing = pinForAlbum(album)
        return LibraryItemAction(
            label = resolveString(if (existing == null) R.string.home_pin_to_home else R.string.home_unpin_from_home),
            icon = Icons.Filled.PushPin,
            id = LibraryActionId.HOME_PIN,
            onClick = {
                if (existing == null) {
                    HomePin.album(album.title, album.artistText, album.songs)?.let(onPinRequested)
                } else {
                    onUnpinRequested(existing.pin.id)
                }
            }
        )
    }

    fun actionForArtist(artist: LibraryArtistGroup): LibraryItemAction {
        val existing = pinForArtist(artist)
        return LibraryItemAction(
            label = resolveString(if (existing == null) R.string.home_pin_to_home else R.string.home_unpin_from_home),
            icon = Icons.Filled.PushPin,
            id = LibraryActionId.HOME_PIN,
            onClick = {
                if (existing == null) {
                    HomePin.artist(artist.name, artist.songs)?.let(onPinRequested)
                } else {
                    onUnpinRequested(existing.pin.id)
                }
            }
        )
    }

    fun actionForPlaylist(playlist: Playlist): LibraryItemAction {
        val existing = pinForPlaylist(playlist)
        return LibraryItemAction(
            label = resolveString(if (existing == null) R.string.home_pin_to_home else R.string.home_unpin_from_home),
            icon = Icons.Filled.PushPin,
            id = LibraryActionId.HOME_PIN,
            onClick = {
                if (existing == null) {
                    onPinRequested(HomePin.playlist(playlist))
                } else {
                    onUnpinRequested(existing.pin.id)
                }
            }
        )
    }
}

val LocalHomePinUi = staticCompositionLocalOf { HomePinUiEnvironment() }

fun resolveHomePins(
    pins: List<HomePin>,
    songs: List<Song>,
    playlists: List<Playlist> = emptyList()
): List<ResolvedHomePin> {
    if (pins.isEmpty()) return emptyList()

    val songIndex = SongReferenceIndex.build(songs)
    val albums = buildLibraryAlbumGroups(songs)
    val artists = buildLibraryArtistGroups(songs)
    val artistsByName = artists.associateBy { artist -> artist.name.lowercase(Locale.ROOT) }

    val playlistsById = playlists.associateBy(Playlist::playlistId)

    return pins.mapNotNull { pin ->
        val anchorSong = pin.anchor?.let { anchor ->
            when (val resolution = songIndex.resolve(anchor)) {
                is SongReferenceResolution.Resolved -> resolution.song
                else -> null
            }
        }
        val target = when (pin.type) {
            HomePinType.SONG -> anchorSong?.let(HomePinTarget::SongTarget)
            HomePinType.ALBUM -> {
                val anchored = anchorSong?.let { song ->
                    findLibraryAlbumGroupForSong(song, albums)
                }
                val fallback = if (anchored == null) {
                    albums.singleOrNull { album ->
                        album.title.equals(pin.title, ignoreCase = true) &&
                                (pin.subtitle.isBlank() ||
                                        album.artistText.equals(pin.subtitle, ignoreCase = true))
                    }
                } else {
                    null
                }
                (anchored ?: fallback)?.let(HomePinTarget::AlbumTarget)
            }
            HomePinType.ARTIST -> {
                val anchoredName = anchorSong?.artist?.ifBlank { "Unknown Artist" }
                val artist = anchoredName?.lowercase(Locale.ROOT)?.let(artistsByName::get)
                    ?: artists.singleOrNull { candidate ->
                        candidate.name.equals(pin.title, ignoreCase = true)
                    }
                artist?.let(HomePinTarget::ArtistTarget)
            }
            HomePinType.PLAYLIST -> pin.playlistId
                ?.let(playlistsById::get)
                ?.let(HomePinTarget::PlaylistTarget)
        }
        if (pin.type == HomePinType.PLAYLIST && target == null) {
            null
        } else {
            ResolvedHomePin(pin = pin, target = target)
        }
    }
}

@Composable
fun HomePinReplacementDialog(
    currentPins: List<ResolvedHomePin>,
    pendingPin: HomePin,
    onReplace: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_replace_pin)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.home_replace_pin_message, HomePin.MAX_COUNT, pendingPin.title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                currentPins.forEachIndexed { index, pin ->
                    TextButton(
                        onClick = { onReplace(index) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = pin.localizedTitle(),
                                style = AppShellTypography.SongTitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = pin.localizedTypeLabel(),
                                style = AppShellTypography.Eyebrow,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.home_pin_cancel))
            }
        }
    )
}
