package io.github.rsgarrido.sazanami.ui.playlist

import android.R
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import io.github.rsgarrido.sazanami.data.PlaylistSong
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.ui.AppShellAccent
import io.github.rsgarrido.sazanami.data.membershipKey
import io.github.rsgarrido.sazanami.ui.library.LibraryItemAction
import io.github.rsgarrido.sazanami.ui.library.LibraryItemActionSheet
import io.github.rsgarrido.sazanami.ui.library.LibraryItemActionSheetTarget
import io.github.rsgarrido.sazanami.ui.library.LibraryLayoutMotionDurationMillis
import io.github.rsgarrido.sazanami.ui.library.libraryItemActions
import io.github.rsgarrido.sazanami.ui.library.LocalLibraryQueueUi
import io.github.rsgarrido.sazanami.R as AppR
import io.github.rsgarrido.sazanami.ui.ratings.LocalSongRatingUi
import io.github.rsgarrido.sazanami.ui.ratings.CompactRatingIndicator
import io.github.rsgarrido.sazanami.ui.home.LocalHomePinUi

@Composable
fun PlaylistSongList(
    playlistSongs: List<Song>,
    playlistSongRows: List<PlaylistSong>,
    currentSongId: Long?,
    recentlyAddedSongIds: Set<Long>,
    favoriteMembershipKeys: Set<String>,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayNextClick: (Song) -> Unit,
    onAddToQueueClick: (Song) -> Unit,
    onToggleFavoriteClick: (Song) -> Unit,
    onRemovePlaylistSongClick: (PlaylistSong) -> Unit,
    allowManualRemoval: Boolean = true,
    onEditSongTagsClick: (Song) -> Unit,
    listState: LazyListState? = null,
    headerContent: (@Composable () -> Unit)? = null,
    emptyContent: (@Composable () -> Unit)? = null,
    bottomContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val resources = LocalResources.current
    var actionSheetTarget by remember {
        mutableStateOf<LibraryItemActionSheetTarget?>(null)
    }
    val ratingUi = LocalSongRatingUi.current
    val homePinUi = LocalHomePinUi.current
    val libraryQueueUi = LocalLibraryQueueUi.current
    val rateSongLabel = stringResource(AppR.string.rate_song)
    val rememberedListState = rememberLazyListState()

    LazyColumn(
        state = listState ?: rememberedListState,
        modifier = modifier,
        contentPadding = PaddingValues(bottom = bottomContentPadding)
    ) {
        headerContent?.let { content ->
            item(key = "playlist-detail-header") {
                content()
            }
        }

        if (playlistSongs.isEmpty()) {
            emptyContent?.let { content ->
                item(key = "playlist-detail-empty") {
                    content()
                }
            }
        }

        itemsIndexed(
            items = playlistSongs,
            key = { index, song ->
                playlistSongRows.getOrNull(index)?.playlistSongId ?: "${song.id}-$index"
            }
        ) { index, song ->
            val playlistSong = playlistSongRows.getOrNull(index)
            val isCurrentSong = song.id == currentSongId
            val wasRecentlyAdded = song.id in recentlyAddedSongIds
            val isFavorite = song.membershipKey() in favoriteMembershipKeys
            val rating = ratingUi.state.ratingsByReferenceKey[song.membershipKey()]

            ListItem(
                leadingContent = {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = stringResource(AppR.string.playlist_album_art_for, song.title),
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_media_play),
                        placeholder = painterResource(R.drawable.ic_media_play)
                    )
                },
                headlineContent = {
                    Text(
                        text = song.title.ifBlank { stringResource(AppR.string.playlist_unknown_title) },
                        fontWeight = if (isCurrentSong) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )
                },
                supportingContent = {
                    Text(text = song.artist.ifBlank { stringResource(AppR.string.playlist_unknown_artist) })
                },
                trailingContent = rating?.let { value ->
                    {
                        CompactRatingIndicator(
                            rating = value,
                            iconFirst = true,
                            modifier = Modifier.clickable { ratingUi.onOpen(song) }
                        )
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = if (isCurrentSong) {
                        AppShellAccent.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                modifier = Modifier
                    .animateItem(
                        placementSpec = tween(
                            durationMillis = LibraryLayoutMotionDurationMillis,
                            easing = FastOutSlowInEasing
                        )
                    )
                    .libraryItemActions(
                    clickLabel = stringResource(AppR.string.playlist_play_song, song.title),
                    onClick = {
                        onSongClick(song, playlistSongs)
                    },
                    onShowActions = {
                        val subtitleParts = buildList {
                            add(song.artist.ifBlank { resources.getString(AppR.string.playlist_unknown_artist) })
                            add(song.album.ifBlank { resources.getString(AppR.string.playlist_unknown_album) })
                            if (wasRecentlyAdded) add(resources.getString(AppR.string.playlist_recently_added))
                        }
                        val actions = buildList {
                            add(
                                LibraryItemAction(
                                    label = resources.getString(AppR.string.playlist_play_next),
                                    icon = Icons.Filled.SkipNext,
                                    onClick = { onPlayNextClick(song) }
                                )
                            )
                            add(
                                LibraryItemAction(
                                    label = resources.getString(AppR.string.playlist_add_to_queue),
                                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                                    onClick = { onAddToQueueClick(song) }
                                )
                            )
                            add(
                                LibraryItemAction(
                                    label = resources.getString(AppR.string.playlist_add_to_another_queue),
                                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                                    onClick = {
                                        libraryQueueUi.onAddToAnotherQueue(listOf(song))
                                    }
                                )
                            )
                            add(
                                LibraryItemAction(
                                    label = resources.getString(AppR.string.playlist_play_in_new_queue),
                                    icon = Icons.Filled.PlayArrow,
                                    onClick = {
                                        libraryQueueUi.onPlayInNewQueue("", listOf(song))
                                    }
                                )
                            )
                            add(
                                LibraryItemAction(
                                    label = if (isFavorite) {
                                        resources.getString(AppR.string.playlist_remove_favorites)
                                    } else {
                                        resources.getString(AppR.string.playlist_add_favorites)
                                    },
                                    icon = if (isFavorite) {
                                        Icons.Filled.Favorite
                                    } else {
                                        Icons.Filled.FavoriteBorder
                                    },
                                    onClick = { onToggleFavoriteClick(song) }
                                )
                            )
                            add(homePinUi.actionForSong(song))
                            add(
                                LibraryItemAction(
                                    label = rateSongLabel,
                                    icon = Icons.Filled.Star,
                                    onClick = { ratingUi.onOpen(song) }
                                )
                            )
                            add(
                                LibraryItemAction(
                                    label = resources.getString(AppR.string.playlist_edit_tags),
                                    icon = Icons.Filled.Edit,
                                    onClick = { onEditSongTagsClick(song) }
                                )
                            )
                            if (allowManualRemoval && playlistSong != null) {
                                add(
                                    LibraryItemAction(
                                        label = resources.getString(AppR.string.playlist_remove_song),
                                        icon = Icons.Filled.Delete,
                                        isDestructive = true,
                                        onClick = { onRemovePlaylistSongClick(playlistSong) }
                                    )
                                )
                            }
                        }

                        actionSheetTarget = LibraryItemActionSheetTarget(
                            title = song.title.ifBlank { resources.getString(AppR.string.playlist_unknown_title) },
                            subtitle = subtitleParts.joinToString(" • "),
                            artworkUri = song.albumArtUri,
                            artworkDescription = resources.getString(AppR.string.playlist_album_art_for, song.title),
                            actions = actions
                        )
                    }
                )
            )
        }
    }

    actionSheetTarget?.let { target ->
        LibraryItemActionSheet(
            target = target,
            onDismissRequest = {
                actionSheetTarget = null
            }
        )
    }
}
