package com.example.cdplaya.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cdplaya.R
import com.example.cdplaya.data.Song
import com.example.cdplaya.data.favoriteKey
import com.example.cdplaya.ui.filterSongsByAlbumSearch
import com.example.cdplaya.ui.filterSongsByArtistSearch
import com.example.cdplaya.ui.filterSongsForSearch
import com.example.cdplaya.ui.sortSongsByAlbumOrder
import com.example.cdplaya.ui.sortSongsForArtistDetail
import com.example.cdplaya.ui.sortSongsForLibrary

@Composable
fun SongsTabContent(
    songs: List<Song>,
    searchQuery: String,
    sortOption: LibrarySortOption,
    currentSong: Song?,
    viewMode: LibraryViewMode,
    gridColumnCount: Int,
    recentlyAddedSongIds: Set<Long>,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayNextClick: (Song) -> Unit,
    onAddToQueueClick: (Song) -> Unit,
    favoriteSongKeys: Set<String>,
    onToggleFavoriteClick: (Song) -> Unit,
    onAddToPlaylistClick: (Song) -> Unit,
    onEditSongTagsClick: (Song) -> Unit,
    bottomContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val filteredSongs = filterSongsForSearch(
        songs = songs,
        searchQuery = searchQuery
    )

    val displayedSongs = sortSongsForLibrary(
        songs = filteredSongs,
        sortOption = sortOption
    )

    if (songs.isEmpty()) {
        Text(
            text = "No songs found.",
            modifier = Modifier.padding(16.dp)
        )
    } else if (filteredSongs.isEmpty()) {
        Text(
            text = "No songs match your search.",
            modifier = Modifier.padding(16.dp)
        )
    } else {
        LibraryLayoutTransition(
            viewMode = viewMode,
            modifier = modifier,
            listContent = {
                SongList(
                    songs = displayedSongs,
                    currentSongId = currentSong?.id,
                    recentlyAddedSongIds = recentlyAddedSongIds,
                    onSongClick = onSongClick,
                    onPlayNextClick = onPlayNextClick,
                    onAddToQueueClick = onAddToQueueClick,
                    onToggleFavoriteClick = onToggleFavoriteClick,
                    favoriteSongKeys = favoriteSongKeys,
                    onAddToPlaylistClick = onAddToPlaylistClick,
                    onEditSongTagsClick = onEditSongTagsClick,
                    bottomContentPadding = bottomContentPadding,
                    modifier = Modifier.fillMaxSize()
                )
            },
            gridContent = {
                SongGrid(
                    songs = displayedSongs,
                    currentSongId = currentSong?.id,
                    gridColumnCount = gridColumnCount,
                    recentlyAddedSongIds = recentlyAddedSongIds,
                    favoriteSongKeys = favoriteSongKeys,
                    onSongClick = onSongClick,
                    onPlayNextClick = onPlayNextClick,
                    onAddToQueueClick = onAddToQueueClick,
                    onToggleFavoriteClick = onToggleFavoriteClick,
                    onAddToPlaylistClick = onAddToPlaylistClick,
                    onEditSongTagsClick = onEditSongTagsClick,
                    bottomContentPadding = bottomContentPadding,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
    }
}

@Composable
fun FavoritesTabContent(
    songs: List<Song>,
    favoriteSongKeys: Set<String>,
    searchQuery: String,
    sortOption: LibrarySortOption,
    currentSong: Song?,
    recentlyAddedSongIds: Set<Long>,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayNextClick: (Song) -> Unit,
    onAddToQueueClick: (Song) -> Unit,
    onToggleFavoriteClick: (Song) -> Unit,
    onAddToPlaylistClick: (Song) -> Unit,
    onAddSongsToPlaylistClick: (List<Song>) -> Unit,
    onEditSongTagsClick: (Song) -> Unit,
    bottomContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val favoriteSongs = songs.filter { song ->
        song.favoriteKey() in favoriteSongKeys
    }

    val filteredSongs = filterSongsForSearch(
        songs = favoriteSongs,
        searchQuery = searchQuery
    )

    val displayedSongs = sortSongsForLibrary(
        songs = filteredSongs,
        sortOption = sortOption
    )

    if (favoriteSongs.isEmpty()) {
        Text(
            text = "No favorite songs yet.",
            modifier = Modifier.padding(16.dp)
        )
    } else if (filteredSongs.isEmpty()) {
        Text(
            text = "No favorite songs match your search.",
            modifier = Modifier.padding(16.dp)
        )
    } else {
        Column(
            modifier = modifier
        ) {
            Button(
                onClick = {
                    onAddSongsToPlaylistClick(displayedSongs)
                },
                enabled = displayedSongs.isNotEmpty(),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(text = "Add all to playlist")
            }

            SongList(
                songs = displayedSongs,
                currentSongId = currentSong?.id,
                recentlyAddedSongIds = recentlyAddedSongIds,
                favoriteSongKeys = favoriteSongKeys,
                onSongClick = onSongClick,
                onPlayNextClick = onPlayNextClick,
                onAddToQueueClick = onAddToQueueClick,
                onToggleFavoriteClick = onToggleFavoriteClick,
                onAddToPlaylistClick = onAddToPlaylistClick,
                onEditSongTagsClick = onEditSongTagsClick,
                bottomContentPadding = bottomContentPadding,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ArtistsTabContent(
    songs: List<Song>,
    searchQuery: String,
    selectedArtistName: String?,
    currentSong: Song?,
    viewMode: LibraryViewMode,
    gridColumnCount: Int,
    sortOption: LibrarySortOption,
    recentlyAddedSongIds: Set<Long>,
    onArtistSelected: (String) -> Unit,
    onBackFromArtist: () -> Unit,
    onPlaySongsClick: (List<Song>, Boolean) -> Unit,
    onPlayNextClick: (Song) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onAddToQueueClick: (Song) -> Unit,
    onPlayNextSongsClick: (String, List<Song>) -> Unit,
    onAddSongsToQueueClick: (String, List<Song>) -> Unit,
    favoriteSongKeys: Set<String>,
    onToggleFavoriteClick: (Song) -> Unit,
    onAddToPlaylistClick: (Song) -> Unit,
    onAddSongsToPlaylistClick: (List<Song>) -> Unit,
    onEditSongTagsClick: (Song) -> Unit,
    bottomContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val artistSearchSongs = filterSongsByArtistSearch(
        songs = songs,
        searchQuery = searchQuery
    )

    if (songs.isEmpty()) {
        Text(
            text = "No artists found.",
            modifier = Modifier.padding(16.dp)
        )
    } else if (selectedArtistName == null) {
        if (artistSearchSongs.isEmpty()) {
            Text(
                text = "No artists match your search.",
                modifier = Modifier.padding(16.dp)
            )
        } else {
            val onArtistPlay: (String, List<Song>) -> Unit = { _, artistSongs ->
                onPlaySongsClick(artistSongs, false)
            }
            val onArtistShuffle: (String, List<Song>) -> Unit = { _, artistSongs ->
                onPlaySongsClick(artistSongs, true)
            }
            val onArtistPlayNext: (String, List<Song>) -> Unit =
                { artistName, artistSongs ->
                    onPlayNextSongsClick(artistName, artistSongs)
                }
            val onArtistAddToQueue: (String, List<Song>) -> Unit =
                { artistName, artistSongs ->
                    onAddSongsToQueueClick(artistName, artistSongs)
                }
            val onArtistAddToPlaylist: (String, List<Song>) -> Unit =
                { _, artistSongs ->
                    onAddSongsToPlaylistClick(artistSongs)
                }

            LibraryLayoutTransition(
                viewMode = viewMode,
                modifier = modifier,
                listContent = {
                    ArtistListScreen(
                        songs = artistSearchSongs,
                        onArtistClick = onArtistSelected,
                        sortOption = sortOption,
                        onArtistPlayClick = onArtistPlay,
                        onArtistShuffleClick = onArtistShuffle,
                        onArtistPlayNextClick = onArtistPlayNext,
                        onArtistAddToQueueClick = onArtistAddToQueue,
                        onArtistAddToPlaylistClick = onArtistAddToPlaylist,
                        bottomContentPadding = bottomContentPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                },
                gridContent = {
                    ArtistGridScreen(
                        songs = artistSearchSongs,
                        onArtistClick = onArtistSelected,
                        sortOption = sortOption,
                        gridColumnCount = gridColumnCount,
                        onArtistPlayClick = onArtistPlay,
                        onArtistShuffleClick = onArtistShuffle,
                        onArtistPlayNextClick = onArtistPlayNext,
                        onArtistAddToQueueClick = onArtistAddToQueue,
                        onArtistAddToPlaylistClick = onArtistAddToPlaylist,
                        bottomContentPadding = bottomContentPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )
        }
    } else {
        val artistSongs = sortSongsForArtistDetail(
            songs.filter { song ->
                song.artist.ifBlank { "Unknown Artist" } == selectedArtistName
            }
        )

        val displayedArtistSongs = filterSongsForSearch(
            songs = artistSongs,
            searchQuery = searchQuery
        )

        val subtitle = if (searchQuery.isBlank()) {
            pluralStringResource(
                R.plurals.song_count,
                artistSongs.size,
                artistSongs.size
            )
        } else {
            pluralStringResource(
                R.plurals.filtered_song_count,
                artistSongs.size,
                displayedArtistSongs.size,
                artistSongs.size
            )
        }

        SongGroupDetailScreen(
            title = selectedArtistName,
            subtitle = subtitle,
            artworkUri = artistSongs.firstOrNull()?.albumArtUri,
            songs = displayedArtistSongs,
            currentSongId = currentSong?.id,
            recentlyAddedSongIds = recentlyAddedSongIds,
            showAlbumName = true,
            showTrackNumbers = false,
            onBackClick = onBackFromArtist,
            onPlayAllClick = {
                onPlaySongsClick(displayedArtistSongs, false)
            },
            onShuffleAllClick = {
                onPlaySongsClick(displayedArtistSongs, true)
            },
            onSongClick = onSongClick,
            onPlayNextClick = onPlayNextClick,
            onAddToQueueClick = onAddToQueueClick,
            favoriteSongKeys = favoriteSongKeys,
            onToggleFavoriteClick = onToggleFavoriteClick,
            onAddToPlaylistClick = onAddToPlaylistClick,
            onAddAllToPlaylistClick = {
                onAddSongsToPlaylistClick(displayedArtistSongs)
            },
            onEditSongTagsClick = onEditSongTagsClick,
            bottomContentPadding = bottomContentPadding,
            modifier = modifier
        )
    }
}

@Composable
fun AlbumsTabContent(
    songs: List<Song>,
    searchQuery: String,
    selectedAlbumFolderPath: String?,
    currentSong: Song?,
    viewMode: LibraryViewMode,
    gridColumnCount: Int,
    sortOption: LibrarySortOption,
    recentlyAddedSongIds: Set<Long>,
    onAlbumSelected: (String) -> Unit,
    onBackFromAlbum: () -> Unit,
    onPlaySongsClick: (List<Song>, Boolean) -> Unit,
    onPlayNextClick: (Song) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onAddToQueueClick: (Song) -> Unit,
    onPlayNextSongsClick: (String, List<Song>) -> Unit,
    onAddSongsToQueueClick: (String, List<Song>) -> Unit,
    favoriteSongKeys: Set<String>,
    onToggleFavoriteClick: (Song) -> Unit,
    onAddToPlaylistClick: (Song) -> Unit,
    onAddSongsToPlaylistClick: (List<Song>) -> Unit,
    onEditSongTagsClick: (Song) -> Unit,
    bottomContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val albumSearchSongs = filterSongsByAlbumSearch(
        songs = songs,
        searchQuery = searchQuery
    )

    if (songs.isEmpty()) {
        Text(
            text = "No albums found.",
            modifier = Modifier.padding(16.dp)
        )
    } else if (selectedAlbumFolderPath == null) {
        if (albumSearchSongs.isEmpty()) {
            Text(
                text = "No albums match your search.",
                modifier = Modifier.padding(16.dp)
            )
        } else {
            val onAlbumPlay: (String, List<Song>) -> Unit = { _, albumSongs ->
                onPlaySongsClick(albumSongs, false)
            }
            val onAlbumShuffle: (String, List<Song>) -> Unit = { _, albumSongs ->
                onPlaySongsClick(albumSongs, true)
            }
            val onAlbumPlayNext: (String, List<Song>) -> Unit =
                { albumTitle, albumSongs ->
                    onPlayNextSongsClick(albumTitle, albumSongs)
                }
            val onAlbumAddToQueue: (String, List<Song>) -> Unit =
                { albumTitle, albumSongs ->
                    onAddSongsToQueueClick(albumTitle, albumSongs)
                }
            val onAlbumAddToPlaylist: (String, List<Song>) -> Unit =
                { _, albumSongs ->
                    onAddSongsToPlaylistClick(albumSongs)
                }

            LibraryLayoutTransition(
                viewMode = viewMode,
                modifier = modifier,
                listContent = {
                    AlbumListScreen(
                        songs = albumSearchSongs,
                        onAlbumClick = onAlbumSelected,
                        sortOption = sortOption,
                        onAlbumPlayClick = onAlbumPlay,
                        onAlbumShuffleClick = onAlbumShuffle,
                        onAlbumPlayNextClick = onAlbumPlayNext,
                        onAlbumAddToQueueClick = onAlbumAddToQueue,
                        onAlbumAddToPlaylistClick = onAlbumAddToPlaylist,
                        bottomContentPadding = bottomContentPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                },
                gridContent = {
                    AlbumGridScreen(
                        songs = albumSearchSongs,
                        onAlbumClick = onAlbumSelected,
                        sortOption = sortOption,
                        gridColumnCount = gridColumnCount,
                        onAlbumPlayClick = onAlbumPlay,
                        onAlbumShuffleClick = onAlbumShuffle,
                        onAlbumPlayNextClick = onAlbumPlayNext,
                        onAlbumAddToQueueClick = onAlbumAddToQueue,
                        onAlbumAddToPlaylistClick = onAlbumAddToPlaylist,
                        bottomContentPadding = bottomContentPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )
        }
    } else {
        val albumSongs = sortSongsByAlbumOrder(
            songs.filter { song ->
                song.folderPath == selectedAlbumFolderPath
            }
        )

        val displayedAlbumSongs = filterSongsForSearch(
            songs = albumSongs,
            searchQuery = searchQuery
        )

        val firstSong = albumSongs.firstOrNull()

        val subtitle = if (searchQuery.isBlank()) {
            val songCountText = pluralStringResource(
                R.plurals.song_count,
                albumSongs.size,
                albumSongs.size
            )
            "${firstSong?.artist ?: "Unknown Artist"} • $songCountText"
        } else {
            val songCountText = pluralStringResource(
                R.plurals.filtered_song_count,
                albumSongs.size,
                displayedAlbumSongs.size,
                albumSongs.size
            )
            "${firstSong?.artist ?: "Unknown Artist"} • $songCountText"
        }

        SongGroupDetailScreen(
            title = firstSong?.album?.ifBlank { "Unknown Album" } ?: "Album",
            subtitle = subtitle,
            artworkUri = firstSong?.albumArtUri,
            songs = displayedAlbumSongs,
            currentSongId = currentSong?.id,
            recentlyAddedSongIds = recentlyAddedSongIds,
            showAlbumName = false,
            showTrackNumbers = true,
            onBackClick = onBackFromAlbum,
            onPlayAllClick = {
                onPlaySongsClick(displayedAlbumSongs, false)
            },
            onShuffleAllClick = {
                onPlaySongsClick(displayedAlbumSongs, true)
            },
            onSongClick = onSongClick,
            onPlayNextClick = onPlayNextClick,
            onAddToQueueClick = onAddToQueueClick,
            favoriteSongKeys = favoriteSongKeys,
            onToggleFavoriteClick = onToggleFavoriteClick,
            onAddToPlaylistClick = onAddToPlaylistClick,
            onAddAllToPlaylistClick = {
                onAddSongsToPlaylistClick(displayedAlbumSongs)
            },
            onEditSongTagsClick = onEditSongTagsClick,
            bottomContentPadding = bottomContentPadding,
            modifier = modifier
        )
    }
}
