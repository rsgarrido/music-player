package com.example.cdplaya.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.cdplaya.data.Playlist
import com.example.cdplaya.data.PlaylistSong
import com.example.cdplaya.data.Song

@Composable
fun MusicLibraryContent(
    selectedLibraryTab: LibraryTab,
    songs: List<Song>,
    searchQuery: String,
    selectedSongSortOption: LibrarySortOption,
    selectedArtistSortOption: LibrarySortOption,
    selectedAlbumSortOption: LibrarySortOption,
    selectedFavoriteSortOption: LibrarySortOption,
    selectedArtistName: String?,
    selectedAlbumFolderPath: String?,
    selectedPlaylistId: Long?,
    playlists: List<Playlist>,
    selectedPlaylistName: String,
    selectedPlaylistSongs: List<PlaylistSong>,
    currentSong: Song?,
    recentlyAddedSongIds: Set<Long>,
    favoriteSongKeys: Set<String>,
    queuedSongs: List<Song>,
    upcomingSongs: List<Song>,
    isShuffleEnabled: Boolean,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlaySongsClick: (List<Song>, Boolean) -> Unit,
    onPlayNextClick: (Song) -> Unit,
    onAddToQueueClick: (Song) -> Unit,
    onPlayNextSongsClick: (String, List<Song>) -> Unit,
    onAddSongsToQueueClick: (String, List<Song>) -> Unit,
    onToggleFavoriteClick: (Song) -> Unit,
    onAddToPlaylistClick: (Song) -> Unit,
    onArtistSelected: (String) -> Unit,
    onBackFromArtist: () -> Unit,
    onAlbumSelected: (String) -> Unit,
    onBackFromAlbum: () -> Unit,
    onBackFromQueue: () -> Unit,
    onRemoveFromQueueClick: (Int) -> Unit,
    onMoveQueueItemUpClick: (Int) -> Unit,
    onMoveQueueItemDownClick: (Int) -> Unit,
    onClearQueueClick: () -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onRenamePlaylistClick: (Playlist, String) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onDeletePlaylistClick: (Playlist) -> Unit,
    onBackFromPlaylist: () -> Unit,
    onRemovePlaylistSongClick: (PlaylistSong) -> Unit,
    onAddSongsToPlaylistClick: (List<Song>) -> Unit,
    onMovePlaylistSongUpClick: (PlaylistSong) -> Unit,
    onMovePlaylistSongDownClick: (PlaylistSong) -> Unit,
    onEditSongTagsClick: (Song) -> Unit,
    recentlyPlayedSongs: List<Song>,
    mostPlayedSongs: List<Song>,
    modifier: Modifier = Modifier
) {
    when (selectedLibraryTab) {
        LibraryTab.SONGS -> {
            SongsTabContent(
                songs = songs,
                searchQuery = searchQuery,
                sortOption = selectedSongSortOption,
                currentSong = currentSong,
                recentlyAddedSongIds = recentlyAddedSongIds,
                onSongClick = onSongClick,
                onPlayNextClick = onPlayNextClick,
                onAddToQueueClick = onAddToQueueClick,
                favoriteSongKeys = favoriteSongKeys,
                onToggleFavoriteClick = onToggleFavoriteClick,
                onAddToPlaylistClick = onAddToPlaylistClick,
                onEditSongTagsClick = onEditSongTagsClick,
                modifier = modifier
            )
        }

        LibraryTab.ARTISTS -> {
            ArtistsTabContent(
                songs = songs,
                searchQuery = searchQuery,
                selectedArtistName = selectedArtistName,
                sortOption = selectedArtistSortOption,
                currentSong = currentSong,
                recentlyAddedSongIds = recentlyAddedSongIds,
                onArtistSelected = onArtistSelected,
                onBackFromArtist = onBackFromArtist,
                onPlaySongsClick = onPlaySongsClick,
                onPlayNextClick = onPlayNextClick,
                onSongClick = onSongClick,
                onAddToQueueClick = onAddToQueueClick,
                onPlayNextSongsClick = onPlayNextSongsClick,
                onAddSongsToQueueClick = onAddSongsToQueueClick,
                favoriteSongKeys = favoriteSongKeys,
                onToggleFavoriteClick = onToggleFavoriteClick,
                onAddToPlaylistClick = onAddToPlaylistClick,
                onAddSongsToPlaylistClick = onAddSongsToPlaylistClick,
                onEditSongTagsClick = onEditSongTagsClick,
                modifier = modifier
            )
        }

        LibraryTab.ALBUMS -> {
            AlbumsTabContent(
                songs = songs,
                searchQuery = searchQuery,
                selectedAlbumFolderPath = selectedAlbumFolderPath,
                currentSong = currentSong,
                sortOption = selectedAlbumSortOption,
                recentlyAddedSongIds = recentlyAddedSongIds,
                onAlbumSelected = onAlbumSelected,
                onBackFromAlbum = onBackFromAlbum,
                onPlaySongsClick = onPlaySongsClick,
                onPlayNextClick = onPlayNextClick,
                onSongClick = onSongClick,
                onAddToQueueClick = onAddToQueueClick,
                onPlayNextSongsClick = onPlayNextSongsClick,
                onAddSongsToQueueClick = onAddSongsToQueueClick,
                favoriteSongKeys = favoriteSongKeys,
                onToggleFavoriteClick = onToggleFavoriteClick,
                onAddToPlaylistClick = onAddToPlaylistClick,
                onAddSongsToPlaylistClick = onAddSongsToPlaylistClick,
                onEditSongTagsClick = onEditSongTagsClick,
                modifier = modifier
            )
        }

        LibraryTab.FAVORITES -> {
            FavoritesTabContent(
                songs = songs,
                favoriteSongKeys = favoriteSongKeys,
                searchQuery = searchQuery,
                sortOption = selectedFavoriteSortOption,
                currentSong = currentSong,
                recentlyAddedSongIds = recentlyAddedSongIds,
                onSongClick = onSongClick,
                onPlayNextClick = onPlayNextClick,
                onAddToQueueClick = onAddToQueueClick,
                onToggleFavoriteClick = onToggleFavoriteClick,
                onAddToPlaylistClick = onAddToPlaylistClick,
                onAddSongsToPlaylistClick = onAddSongsToPlaylistClick,
                onEditSongTagsClick = onEditSongTagsClick,
                modifier = modifier
            )
        }

        LibraryTab.QUEUE -> {
            QueueScreen(
                queuedSongs = queuedSongs,
                upcomingSongs = upcomingSongs,
                isShuffleEnabled = isShuffleEnabled,
                onBackClick = onBackFromQueue,
                onRemoveFromQueueClick = onRemoveFromQueueClick,
                onMoveQueueItemUpClick = onMoveQueueItemUpClick,
                onMoveQueueItemDownClick = onMoveQueueItemDownClick,
                onClearQueueClick = onClearQueueClick,
                modifier = modifier
            )
        }

        LibraryTab.PLAYLISTS -> {
            PlaylistsTabContent(
                songs = songs,
                playlists = playlists,
                selectedPlaylistId = selectedPlaylistId,
                selectedPlaylistName = selectedPlaylistName,
                selectedPlaylistSongs = selectedPlaylistSongs,
                currentSong = currentSong,
                recentlyAddedSongIds = recentlyAddedSongIds,
                favoriteSongKeys = favoriteSongKeys,
                onCreatePlaylistClick = onCreatePlaylistClick,
                onRenamePlaylistClick = onRenamePlaylistClick,
                onPlaylistClick = onPlaylistClick,
                onDeletePlaylistClick = onDeletePlaylistClick,
                onBackFromPlaylist = onBackFromPlaylist,
                onPlaySongsClick = onPlaySongsClick,
                onMovePlaylistSongUpClick = onMovePlaylistSongUpClick,
                onMovePlaylistSongDownClick = onMovePlaylistSongDownClick,
                onSongClick = onSongClick,
                onPlayNextClick = onPlayNextClick,
                onAddToQueueClick = onAddToQueueClick,
                onToggleFavoriteClick = onToggleFavoriteClick,
                onRemovePlaylistSongClick = onRemovePlaylistSongClick,
                onEditSongTagsClick = onEditSongTagsClick,
                modifier = modifier
            )
        }

        LibraryTab.RECENTLY_PLAYED -> {
            if (recentlyPlayedSongs.isEmpty()) {
                EmptyHistoryMessage(
                    message = "No recently played songs yet."
                )
            } else {
                SongList(
                    songs = recentlyPlayedSongs,
                    currentSongId = currentSong?.id,
                    recentlyAddedSongIds = recentlyAddedSongIds,
                    favoriteSongKeys = favoriteSongKeys,
                    onSongClick = onSongClick,
                    onPlayNextClick = onPlayNextClick,
                    onAddToQueueClick = onAddToQueueClick,
                    onToggleFavoriteClick = onToggleFavoriteClick,
                    onAddToPlaylistClick = onAddToPlaylistClick,
                    onEditSongTagsClick = onEditSongTagsClick
                )
            }
        }

        LibraryTab.MOST_PLAYED -> {
            if (mostPlayedSongs.isEmpty()) {
                EmptyHistoryMessage(
                    message = "No most played songs yet."
                )
            } else {
                SongList(
                    songs = mostPlayedSongs,
                    currentSongId = currentSong?.id,
                    recentlyAddedSongIds = recentlyAddedSongIds,
                    favoriteSongKeys = favoriteSongKeys,
                    onSongClick = onSongClick,
                    onPlayNextClick = onPlayNextClick,
                    onAddToQueueClick = onAddToQueueClick,
                    onToggleFavoriteClick = onToggleFavoriteClick,
                    onAddToPlaylistClick = onAddToPlaylistClick,
                    onEditSongTagsClick = onEditSongTagsClick
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}