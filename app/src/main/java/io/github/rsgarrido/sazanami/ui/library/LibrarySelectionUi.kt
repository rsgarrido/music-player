package io.github.rsgarrido.sazanami.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.data.FavoriteBatchOperation
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.data.membershipKey
import io.github.rsgarrido.sazanami.data.planFavoriteBatch
import io.github.rsgarrido.sazanami.ui.state.LibrarySelectionEntity
import io.github.rsgarrido.sazanami.ui.state.LibrarySelectionUiState

@Stable
data class LibrarySelectionUiEnvironment(
    val state: LibrarySelectionUiState = LibrarySelectionUiState(),
    val allSongs: List<Song> = emptyList(),
    val favoriteMembershipKeys: Set<String> = emptySet(),
    val headerState: LibrarySelectionHeaderState = LibrarySelectionHeaderState(),
    val onEnter: (LibrarySelectionEntity, String) -> Unit = { _, _ -> },
    val onToggle: (LibrarySelectionEntity, String) -> Unit = { _, _ -> },
    val onSelectDisplayed: (LibrarySelectionEntity, Collection<String>) -> Unit = { _, _ -> },
    val onClear: () -> Unit = {},
    val onPlayNext: (List<Song>) -> Unit = {},
    val onAddToQueue: (List<Song>) -> Unit = {},
    val onAddToAnotherQueue: (List<Song>) -> Unit = {},
    val onPlayInNewQueue: (String, List<Song>) -> Unit = { _, _ -> },
    val onApplyFavoriteBatch: (List<Song>) -> Unit = {}
)

val LocalLibrarySelectionUi = staticCompositionLocalOf { LibrarySelectionUiEnvironment() }

@Immutable
data class LibrarySelectionHeaderBinding(
    val entity: LibrarySelectionEntity,
    val displayedKeys: List<String>,
    val searchActive: Boolean,
    val hasMoreAction: Boolean
)

@Stable
class LibrarySelectionHeaderState {
    var binding by mutableStateOf<LibrarySelectionHeaderBinding?>(null)
        private set

    var activeActionTarget by mutableStateOf<LibraryItemActionSheetTarget?>(null)
        private set

    private var actionTargetProvider: (() -> LibraryItemActionSheetTarget?)? = null

    fun bind(
        entity: LibrarySelectionEntity,
        displayedKeys: List<String>,
        searchActive: Boolean,
        selectionActionTarget: (() -> LibraryItemActionSheetTarget?)?
    ) {
        if (binding?.entity != entity) {
            actionTargetProvider = null
            activeActionTarget = null
        }
        if (selectionActionTarget != null) {
            actionTargetProvider = selectionActionTarget
        }
        val updated = LibrarySelectionHeaderBinding(
            entity = entity,
            displayedKeys = displayedKeys,
            searchActive = searchActive,
            hasMoreAction = actionTargetProvider != null
        )
        if (binding != updated) binding = updated
    }

    fun showMore() {
        activeActionTarget = actionTargetProvider?.invoke()
    }

    fun dismissActions() {
        activeActionTarget = null
    }

    fun resetActions() {
        actionTargetProvider = null
        activeActionTarget = null
    }
}

internal fun resolveSelectedSongs(
    selectedKeys: Set<String>,
    displayedSongs: List<Song>,
    fallbackSongs: List<Song>
): List<Song> = (displayedSongs + fallbackSongs)
    .distinctBy(Song::membershipKey)
    .filter { it.membershipKey() in selectedKeys }

internal fun resolveSelectedAlbums(
    selectedKeys: Set<String>,
    displayedAlbums: List<LibraryAlbumGroup>,
    fallbackAlbums: List<LibraryAlbumGroup>
): List<LibraryAlbumGroup> = (displayedAlbums + fallbackAlbums)
    .distinctBy(LibraryAlbumGroup::key)
    .filter { it.key in selectedKeys }

internal fun songSelectionActionSheetTarget(
    selectedSongs: List<Song>,
    singleSongTarget: LibraryItemActionSheetTarget?,
    favoriteMembershipKeys: Set<String>,
    rateSongLabel: String,
    onAddToAnotherQueue: (List<Song>) -> Unit,
    onPlayInNewQueue: (String, List<Song>) -> Unit,
    onApplyFavoriteBatch: (List<Song>) -> Unit,
    onClearSelection: () -> Unit,
    resolveString: (Int) -> String,
    resolvePlural: (Int, Int) -> String
): LibraryItemActionSheetTarget? {
    val songs = selectedSongs.distinctBy(Song::membershipKey)
    if (songs.isEmpty()) return null

    val favoriteOperation = planFavoriteBatch(songs, favoriteMembershipKeys).operation
    val removeFromFavorites = favoriteOperation == FavoriteBatchOperation.REMOVE_SELECTED
    val exactActions = singleSongTarget?.actions.orEmpty()
    val directSelectionActions = buildList {
        add(
            LibraryItemAction(
                label = resolveString(R.string.playlist_add_to_another_queue),
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                onClick = { onAddToAnotherQueue(songs) }
            )
        )
        add(
            LibraryItemAction(
                label = resolveString(R.string.playlist_play_in_new_queue),
                icon = Icons.Filled.PlayArrow,
                onClick = {
                    onPlayInNewQueue("", songs)
                    onClearSelection()
                }
            )
        )
        add(
            LibraryItemAction(
                label = if (removeFromFavorites) {
                    resolveString(R.string.playlist_remove_favorites)
                } else {
                    resolveString(R.string.playlist_add_favorites)
                },
                icon = if (removeFromFavorites) {
                    Icons.Filled.Favorite
                } else {
                    Icons.Filled.FavoriteBorder
                },
                onClick = {
                    onApplyFavoriteBatch(songs)
                    onClearSelection()
                }
            )
        )
        if (songs.size == 1) {
            addExactSelectionAction(exactActions, onClearSelection) { it.id == LibraryActionId.HOME_PIN }
            addExactSelectionAction(exactActions, onClearSelection) {
                it.id == LibraryActionId.RATE_SONG
            }
            addExactSelectionAction(exactActions, onClearSelection) {
                it.id == LibraryActionId.EDIT_TAGS
            }
        }
    }

    return if (songs.size == 1 && singleSongTarget != null) {
        singleSongTarget.copy(actions = directSelectionActions)
    } else {
        LibraryItemActionSheetTarget(
            title = resolvePlural(R.plurals.library_selection_songs_title, songs.size),
            subtitle = resolveString(R.string.library_selection_summary_subtitle),
            artworkUri = null,
            artworkDescription = resolvePlural(R.plurals.library_selection_songs_artwork, songs.size),
            actions = directSelectionActions
        )
    }
}

internal fun albumSelectionActionSheetTarget(
    selectedAlbums: List<LibraryAlbumGroup>,
    singleAlbumTarget: LibraryItemActionSheetTarget?,
    onAddToAnotherQueue: (List<Song>) -> Unit,
    onPlayInNewQueue: (String, List<Song>) -> Unit,
    onClearSelection: () -> Unit,
    resolveString: (Int) -> String,
    resolvePlural: (Int, Int) -> String
): LibraryItemActionSheetTarget? {
    val albums = selectedAlbums.distinctBy(LibraryAlbumGroup::key)
    if (albums.isEmpty()) return null

    val songs = albums.flatMap(LibraryAlbumGroup::songs)
    val exactActions = singleAlbumTarget?.actions.orEmpty()
    val directSelectionActions = buildList {
        add(
            LibraryItemAction(
                label = resolveString(R.string.playlist_add_to_another_queue),
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                onClick = { onAddToAnotherQueue(songs) }
            )
        )
        add(
            LibraryItemAction(
                label = resolveString(R.string.playlist_play_in_new_queue),
                icon = Icons.Filled.PlayArrow,
                onClick = {
                    onPlayInNewQueue(albums.singleOrNull()?.title.orEmpty(), songs)
                    onClearSelection()
                }
            )
        )
        if (albums.size == 1) {
            addExactSelectionAction(exactActions, onClearSelection) { it.id == LibraryActionId.PLAY }
            addExactSelectionAction(exactActions, onClearSelection) { it.id == LibraryActionId.SHUFFLE }
            addExactSelectionAction(exactActions, onClearSelection) { it.id == LibraryActionId.HOME_PIN }
            addExactSelectionAction(exactActions, onClearSelection) {
                it.id == LibraryActionId.EDIT_ALBUM_METADATA
            }
        }
    }

    return if (albums.size == 1 && singleAlbumTarget != null) {
        singleAlbumTarget.copy(actions = directSelectionActions)
    } else {
        LibraryItemActionSheetTarget(
            title = resolvePlural(R.plurals.library_selection_albums_title, albums.size),
            subtitle = resolveString(R.string.library_selection_summary_subtitle),
            artworkUri = null,
            artworkDescription = resolvePlural(R.plurals.library_selection_albums_artwork, albums.size),
            actions = directSelectionActions
        )
    }
}

private fun MutableList<LibraryItemAction>.addExactSelectionAction(
    actions: List<LibraryItemAction>,
    onClearSelection: () -> Unit,
    predicate: (LibraryItemAction) -> Boolean
) {
    actions.firstOrNull(predicate)?.let { action ->
        add(
            action.copy(onClick = {
                onClearSelection()
                action.onClick()
            })
        )
    }
}

@Composable
internal fun LibrarySelectionHeader(
    entity: LibrarySelectionEntity,
    displayedKeys: List<String>,
    searchActive: Boolean,
    selectionActionTarget: (() -> LibraryItemActionSheetTarget?)?
) {
    val selection = LocalLibrarySelectionUi.current
    SideEffect {
        if (selection.state.isActive) {
            selection.headerState.bind(
                entity,
                displayedKeys,
                searchActive,
                selectionActionTarget
            )
        } else {
            selection.headerState.resetActions()
            selection.headerState.bind(entity, displayedKeys, searchActive, selectionActionTarget)
        }
    }
}

@Composable
internal fun LibrarySelectionHeaderContent(modifier: Modifier = Modifier) {
    val selection = LocalLibrarySelectionUi.current
    val binding = selection.headerState.binding ?: return
    if (selection.state.entity != binding.entity || !selection.state.isActive) return

    Surface(modifier = modifier, tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = selection.onClear) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.library_selection_clear))
            }
            Text(
                text = pluralStringResource(R.plurals.library_selection_count, selection.state.selectedCount, selection.state.selectedCount),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    selection.onSelectDisplayed(binding.entity, binding.displayedKeys)
                },
                enabled = binding.displayedKeys.isNotEmpty()
            ) {
                Text(if (binding.searchActive) stringResource(R.string.library_selection_select_results) else stringResource(R.string.library_selection_select_all))
            }
            if (binding.hasMoreAction) {
                IconButton(onClick = selection.headerState::showMore) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.library_selection_actions))
                }
            }
        }
    }

    selection.headerState.activeActionTarget?.let { target ->
        LibraryItemActionSheet(
            target = target,
            onDismissRequest = selection.headerState::dismissActions
        )
    }
}

@Composable
internal fun LibrarySelectionCheckBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(30.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 4.dp,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = stringResource(R.string.playlist_selected),
            modifier = Modifier.padding(4.dp)
        )
    }
}

@Composable
internal fun LibrarySelectionActionBar(
    selectedSongs: () -> List<Song>,
    onAddToPlaylist: (List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    val selection = LocalLibrarySelectionUi.current

    Surface(modifier = modifier, tonalElevation = 6.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp),
                onClick = {
                val songs = selectedSongs()
                if (songs.isNotEmpty()) {
                    selection.onPlayNext(songs)
                    selection.onClear()
                }
            }) {
                Icon(Icons.Filled.SkipNext, contentDescription = null)
                Text(stringResource(R.string.playlist_play_next))
            }
            FilledTonalButton(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp),
                onClick = {
                val songs = selectedSongs()
                if (songs.isNotEmpty()) {
                    selection.onAddToQueue(songs)
                    selection.onClear()
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null)
                Text(stringResource(R.string.library_selection_queue))
            }
            FilledTonalButton(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp),
                onClick = {
                val songs = selectedSongs()
                if (songs.isNotEmpty()) onAddToPlaylist(songs)
            }) {
                Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null)
                Text(stringResource(R.string.library_selection_playlist))
            }
        }
    }
}
