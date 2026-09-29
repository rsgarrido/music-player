package io.github.rsgarrido.sazanami.ui.library

import androidx.annotation.StringRes
import io.github.rsgarrido.sazanami.R

enum class LibraryTab(@StringRes val titleRes: Int) {
    SONGS(R.string.library_tab_songs),
    ARTISTS(R.string.library_tab_artists),
    FOLDERS(R.string.library_tab_folders),
    ALBUMS(R.string.library_tab_albums),
    GENRES(R.string.library_tab_genres),
    FAVORITES(R.string.library_tab_favorites),
    RATED(R.string.library_tab_rated),
    PLAYLISTS(R.string.library_tab_playlists),
    RECENTLY_ADDED(R.string.library_tab_recently_added),
    RECENTLY_PLAYED(R.string.library_tab_recently_played),
    MOST_PLAYED(R.string.library_tab_most_played),
    QUEUE(R.string.library_tab_queue)
}
