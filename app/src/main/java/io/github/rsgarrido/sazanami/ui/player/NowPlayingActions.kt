package io.github.rsgarrido.sazanami.ui.player

import androidx.annotation.StringRes
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.data.membershipKey
import io.github.rsgarrido.sazanami.data.artistIdentity
import io.github.rsgarrido.sazanami.data.normalizeArtistName
import io.github.rsgarrido.sazanami.ui.library.buildLibraryAlbumGroups
import io.github.rsgarrido.sazanami.ui.library.buildLibraryArtistGroups
import io.github.rsgarrido.sazanami.ui.library.findLibraryAlbumGroupForSong

enum class NowPlayingAction { FAVORITE, GO_TO_ARTIST, GO_TO_ALBUM, LYRICS }

internal enum class NowPlayingFavoriteFeedback { ADDED_TO_FAVORITES, REMOVED_FROM_FAVORITES }

internal data class NowPlayingActionItem(
    val action: NowPlayingAction,
    @StringRes val labelRes: Int,
    val isActive: Boolean = false
)

internal fun isCurrentNowPlayingTarget(target: Song?, currentSong: Song?): Boolean =
    target != null && currentSong != null && target.membershipKey() == currentSong.membershipKey()

internal fun resolveNowPlayingAlbumKey(song: Song, librarySongs: List<Song>): String? =
    findLibraryAlbumGroupForSong(song, buildLibraryAlbumGroups(librarySongs))?.key

/** Navigation policy only; preserve the library's single-string artist grouping semantics. */
internal fun resolveNowPlayingArtistName(song: Song, librarySongs: List<Song>): String? {
    val normalized = normalizeArtistName(song.artist)
    if (normalized in nonNavigableArtistNames) {
        return null
    }
    val identity = artistIdentity(song.artist)
    return buildLibraryArtistGroups(librarySongs)
        .firstOrNull { it.identity == identity && !it.identity.isUnknown }?.name
}

private val nonNavigableArtistNames =
    setOf("", "unknown", "unknown artist", "<unknown>", "artista desconocido")

/** Revalidate both entry points against the latest track and library before navigating. */
internal fun openCurrentNowPlayingArtist(
    target: Song,
    currentSong: Song?,
    librarySongs: List<Song>,
    onOpenArtist: (String) -> Unit
): Boolean {
    if (!isCurrentNowPlayingTarget(target, currentSong)) return false
    val artistName = resolveNowPlayingArtistName(requireNotNull(currentSong), librarySongs) ?: return false
    onOpenArtist(artistName)
    return true
}

internal fun nowPlayingActions(
    target: Song,
    favoriteMembershipKeys: Set<String>,
    librarySongs: List<Song>
): List<NowPlayingActionItem> = buildList {
    val isFavorite = target.membershipKey() in favoriteMembershipKeys
    add(NowPlayingActionItem(
        action = NowPlayingAction.FAVORITE,
        labelRes = if (isFavorite) R.string.player_remove_favorite else R.string.player_add_favorite,
        isActive = isFavorite
    ))
    if (resolveNowPlayingArtistName(target, librarySongs) != null) {
        add(NowPlayingActionItem(NowPlayingAction.GO_TO_ARTIST, R.string.library_search_go_artist))
    }
    if (resolveNowPlayingAlbumKey(target, librarySongs) != null) {
        add(NowPlayingActionItem(NowPlayingAction.GO_TO_ALBUM, R.string.player_go_to_album))
    }
    // Missing or unconfigured local lyrics are handled by the existing Lyrics screen.
    add(NowPlayingActionItem(NowPlayingAction.LYRICS, R.string.player_lyrics))
}

/**
 * Dismiss first, then validate the captured target against the latest playback/library state.
 * [isFavorite] captures membership at selection time, before the optimistic toggle changes it.
 */
internal fun performNowPlayingAction(
    action: NowPlayingAction,
    target: Song,
    currentSong: Song?,
    librarySongs: List<Song>,
    onDismiss: () -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onOpenAlbum: (Song) -> Unit,
    onOpenLyrics: () -> Unit,
    isFavorite: Boolean = false,
    onFavoriteFeedback: (NowPlayingFavoriteFeedback) -> Unit = {},
    onOpenArtist: (Song) -> Unit = {}
): Boolean {
    onDismiss()
    if (!isCurrentNowPlayingTarget(target, currentSong)) return false
    when (action) {
        NowPlayingAction.FAVORITE -> {
            val feedback = if (isFavorite) {
                NowPlayingFavoriteFeedback.REMOVED_FROM_FAVORITES
            } else {
                NowPlayingFavoriteFeedback.ADDED_TO_FAVORITES
            }
            onToggleFavorite(target)
            onFavoriteFeedback(feedback)
        }
        NowPlayingAction.GO_TO_ARTIST -> {
            if (resolveNowPlayingArtistName(requireNotNull(currentSong), librarySongs) == null) return false
            onOpenArtist(target)
        }
        NowPlayingAction.GO_TO_ALBUM -> {
            if (resolveNowPlayingAlbumKey(target, librarySongs) == null) return false
            onOpenAlbum(target)
        }
        NowPlayingAction.LYRICS -> onOpenLyrics()
    }
    return true
}
