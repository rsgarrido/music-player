package io.github.rsgarrido.sazanami.ui.player

import androidx.annotation.StringRes
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.data.membershipKey
import io.github.rsgarrido.sazanami.ui.library.buildLibraryAlbumGroups
import io.github.rsgarrido.sazanami.ui.library.findLibraryAlbumGroupForSong

enum class NowPlayingAction { FAVORITE, GO_TO_ALBUM, LYRICS }

internal data class NowPlayingActionItem(
    val action: NowPlayingAction,
    @StringRes val labelRes: Int,
    val isActive: Boolean = false
)

internal fun isCurrentNowPlayingTarget(target: Song?, currentSong: Song?): Boolean =
    target != null && currentSong != null && target.membershipKey() == currentSong.membershipKey()

internal fun resolveNowPlayingAlbumKey(song: Song, librarySongs: List<Song>): String? =
    findLibraryAlbumGroupForSong(song, buildLibraryAlbumGroups(librarySongs))?.key

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
    if (resolveNowPlayingAlbumKey(target, librarySongs) != null) {
        add(NowPlayingActionItem(NowPlayingAction.GO_TO_ALBUM, R.string.player_go_to_album))
    }
    // Missing or unconfigured local lyrics are handled by the existing Lyrics screen.
    add(NowPlayingActionItem(NowPlayingAction.LYRICS, R.string.player_lyrics))
}

/** Dismiss first, then validate the captured target against the latest playback/library state. */
internal fun performNowPlayingAction(
    action: NowPlayingAction,
    target: Song,
    currentSong: Song?,
    librarySongs: List<Song>,
    onDismiss: () -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onOpenAlbum: (Song) -> Unit,
    onOpenLyrics: () -> Unit
): Boolean {
    onDismiss()
    if (!isCurrentNowPlayingTarget(target, currentSong)) return false
    when (action) {
        NowPlayingAction.FAVORITE -> onToggleFavorite(target)
        NowPlayingAction.GO_TO_ALBUM -> {
            if (resolveNowPlayingAlbumKey(target, librarySongs) == null) return false
            onOpenAlbum(target)
        }
        NowPlayingAction.LYRICS -> onOpenLyrics()
    }
    return true
}
