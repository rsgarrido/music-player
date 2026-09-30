package io.github.rsgarrido.sazanami.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.rsgarrido.sazanami.controller.LinkedHistoricalReconciliation
import io.github.rsgarrido.sazanami.controller.ListeningHistoryReconciliationUiState
import io.github.rsgarrido.sazanami.controller.ReconciliationAlbumKey
import io.github.rsgarrido.sazanami.controller.ReconciliationAlbumPresentation
import io.github.rsgarrido.sazanami.controller.ReconciliationArtistPresentation
import io.github.rsgarrido.sazanami.controller.ReconciliationBrowseMode
import io.github.rsgarrido.sazanami.controller.ReconciliationConfirmation
import io.github.rsgarrido.sazanami.controller.ReconciliationReviewContent
import io.github.rsgarrido.sazanami.controller.ReconciliationReviewFilter
import io.github.rsgarrido.sazanami.controller.ReconciliationReviewTab
import io.github.rsgarrido.sazanami.controller.ReconciliationSortOption
import io.github.rsgarrido.sazanami.controller.ReconciliationTrackPresentation
import io.github.rsgarrido.sazanami.controller.ReconciliationTrackStatus
import io.github.rsgarrido.sazanami.data.HistoricalReconciliationSource
import io.github.rsgarrido.sazanami.data.ListeningIdentityReconciliationCandidate
import io.github.rsgarrido.sazanami.data.ListeningIdentityReconciliationRatings
import io.github.rsgarrido.sazanami.data.ListeningIdentityReconciliationRatingState
import io.github.rsgarrido.sazanami.data.LocalReconciliationTarget
import io.github.rsgarrido.sazanami.data.ReconciliationCandidateCategory
import io.github.rsgarrido.sazanami.data.ReconciliationCandidateDisposition
import io.github.rsgarrido.sazanami.data.ReconciliationMissingField
import io.github.rsgarrido.sazanami.data.local.ListeningSource
import io.github.rsgarrido.sazanami.ui.AppShellTypography
import io.github.rsgarrido.sazanami.ui.state.UiMessage
import io.github.rsgarrido.sazanami.ui.state.resolve
import io.github.rsgarrido.sazanami.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ListeningHistoryReconciliationUiActions(
    val onEnter: () -> Unit,
    val onBack: () -> Unit,
    val onRetry: () -> Unit,
    val onTabSelected: (ReconciliationReviewTab) -> Unit,
    val onBrowseModeSelected: (ReconciliationBrowseMode) -> Unit,
    val onBrowseQueryChanged: (String) -> Unit,
    val onSortSelected: (ReconciliationSortOption) -> Unit,
    val onReviewFilterSelected: (ReconciliationReviewFilter) -> Unit,
    val onToggleExpanded: (Long) -> Unit,
    val onToggleAlbum: (ReconciliationAlbumKey) -> Unit,
    val onToggleArtist: (String) -> Unit,
    val onToggleSelected: (Long) -> Unit,
    val onSelectItems: (List<Long>) -> Unit,
    val onClearSelection: () -> Unit,
    val onLinkSelectedRequested: () -> Unit,
    val onSkip: (Long) -> Unit,
    val onCandidateSelected: (List<Long>, LocalReconciliationTarget) -> Unit,
    val onSearchRequested: (List<Long>) -> Unit,
    val onSearchQueryChanged: (String) -> Unit,
    val onSearchDismissed: () -> Unit,
    val onUnlinkRequested: (LinkedHistoricalReconciliation) -> Unit,
    val onConfirmationCancelled: () -> Unit,
    val onConfirmed: () -> Unit,
    val onMessageDismissed: () -> Unit
)

@Composable
fun ListeningHistoryReconciliationScreen(
    state: ListeningHistoryReconciliationUiState,
    actions: ListeningHistoryReconciliationUiActions,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) { actions.onEnter() }
    BackHandler(onBack = actions.onBack)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ReconciliationHeader(actions.onBack)
        when (state) {
            ListeningHistoryReconciliationUiState.Loading -> LoadingContent()
            is ListeningHistoryReconciliationUiState.Error -> ErrorContent(
                state.message,
                actions.onRetry
            )
            is ListeningHistoryReconciliationUiState.Content -> Content(
                state.value,
                actions,
                Modifier.weight(1f)
            )
        }
    }
    val content = (state as? ListeningHistoryReconciliationUiState.Content)?.value
    content?.search?.let { search ->
        SearchDialog(
            query = search.query,
            results = search.results,
            isWorking = content.isWorking,
            onQueryChanged = actions.onSearchQueryChanged,
            onSelected = { target -> actions.onCandidateSelected(search.sourceIds, target) },
            onDismiss = actions.onSearchDismissed
        )
    }
    content?.confirmation?.let { confirmation ->
        ConfirmationDialog(
            confirmation = confirmation,
            isWorking = content.isWorking,
            onConfirm = actions.onConfirmed,
            onDismiss = actions.onConfirmationCancelled
        )
    }
}

@Composable
private fun ReconciliationHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 10.dp, end = 20.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.history_import_back))
        }
        Column(modifier = Modifier.padding(start = 4.dp)) {
            Text(
                stringResource(R.string.history_match_title),
                style = AppShellTypography.ScreenTitle,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                stringResource(R.string.history_match_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    val loadingDescription = stringResource(R.string.history_match_finding_description)
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.semantics {
            contentDescription = loadingDescription
        })
        Text(
            stringResource(R.string.history_match_finding),
            modifier = Modifier.padding(top = 18.dp),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            stringResource(R.string.history_match_loading_help),
            modifier = Modifier.padding(top = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(message: UiMessage, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message.resolve(), style = MaterialTheme.typography.titleMedium)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 18.dp)) { Text(stringResource(R.string.history_import_try_again)) }
    }
}

@Composable
private fun Content(
    content: ReconciliationReviewContent,
    actions: ListeningHistoryReconciliationUiActions,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        TabRow(content, actions.onTabSelected)
        BrowseControls(content, actions)
        content.message?.let { message ->
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(message.resolve(), modifier = Modifier.weight(1f))
                    TextButton(onClick = actions.onMessageDismissed) { Text(stringResource(R.string.history_match_dismiss)) }
                }
            }
        }
        if (content.selectedSourceIds.isNotEmpty()) {
            SelectionBar(content, actions)
        }
        when (content.browseMode) {
            ReconciliationBrowseMode.TRACKS -> TrackPresentationList(
                content.visibleTracks,
                content,
                actions
            )
            ReconciliationBrowseMode.ALBUMS -> AlbumPresentationList(
                content.visibleAlbums,
                content,
                actions
            )
            ReconciliationBrowseMode.ARTISTS -> ArtistPresentationList(
                content.visibleArtists,
                content,
                actions
            )
        }
    }
}

@Composable
private fun BrowseControls(
    content: ReconciliationReviewContent,
    actions: ListeningHistoryReconciliationUiActions
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReconciliationBrowseMode.entries.forEach { mode ->
                FilterChip(
                    selected = content.browseMode == mode,
                    onClick = { actions.onBrowseModeSelected(mode) },
                    label = { Text(stringResource(mode.labelRes)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        OutlinedTextField(
            value = content.browseQuery,
            onValueChange = actions.onBrowseQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            label = { Text(stringResource(R.string.history_match_search_hint)) }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SortMenu(content.sortOption, actions.onSortSelected)
            Text(
                pluralStringResource(R.plurals.history_match_shown, content.visibleTracks.size, content.visibleTracks.size),
                modifier = Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (content.activeTab == ReconciliationReviewTab.REVIEW) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReconciliationReviewFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = content.reviewFilter == filter,
                        onClick = { actions.onReviewFilterSelected(filter) },
                        label = { Text(stringResource(filter.labelRes)) }
                    )
                }
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun SortMenu(
    selected: ReconciliationSortOption,
    onSelected: (ReconciliationSortOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Sort, contentDescription = null)
            Text(stringResource(selected.labelRes), modifier = Modifier.padding(start = 6.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ReconciliationSortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes)) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun SelectionBar(
    content: ReconciliationReviewContent,
    actions: ListeningHistoryReconciliationUiActions
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                pluralStringResource(R.plurals.history_match_selected, content.selectedSourceIds.size, content.selectedSourceIds.size),
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = actions.onClearSelection, enabled = !content.isWorking) {
                Text(stringResource(R.string.history_match_clear))
            }
            Button(onClick = actions.onLinkSelectedRequested, enabled = !content.isWorking) {
                Text(stringResource(R.string.history_match_link_selected))
            }
        }
    }
}

@Composable
private fun TabRow(content: ReconciliationReviewContent, onSelected: (ReconciliationReviewTab) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
        ReconciliationReviewTab.entries.forEach { tab ->
            val count = when (tab) {
                ReconciliationReviewTab.REVIEW -> content.reviewCount
                ReconciliationReviewTab.UNMATCHED -> content.unmatchedCount
                ReconciliationReviewTab.LINKED -> content.linkedCount
            }
            val label = stringResource(tab.labelRes, count)
            val selected = content.activeTab == tab
            val tabDescription = if (selected) stringResource(R.string.history_match_selected_tab, label) else label
            TextButton(
                onClick = { onSelected(tab) },
                modifier = Modifier.weight(1f).semantics {
                    contentDescription = tabDescription
                }
            ) {
                Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun TrackPresentationList(
    tracks: List<ReconciliationTrackPresentation>,
    content: ReconciliationReviewContent,
    actions: ListeningHistoryReconciliationUiActions
) {
    if (tracks.isEmpty()) {
        EmptyState(stringResource(emptyTitleRes(content)), stringResource(emptyTextRes(content)))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tracks, key = { "track-${it.status}-${it.sourceId}" }) { track ->
            CompactTrackCard(track, content, actions)
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun AlbumPresentationList(
    albums: List<ReconciliationAlbumPresentation>,
    content: ReconciliationReviewContent,
    actions: ListeningHistoryReconciliationUiActions
) {
    if (albums.isEmpty()) {
        EmptyState(stringResource(emptyTitleRes(content)), stringResource(emptyTextRes(content)))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(albums, key = { "album-${it.key.stableKey}" }) { album ->
            val expanded = content.expandedAlbumKey == album.key
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                CompactGroupHeader(
                    title = album.title,
                    subtitle = album.artist,
                    summary = stringResource(R.string.history_match_group_summary, album.importedCount, album.linkedCount, album.reviewCount, album.unmatchedCount),
                    expanded = expanded,
                    onClick = { actions.onToggleAlbum(album.key) }
                )
                if (expanded) {
                    Column(
                        modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider()
                        val eligible = album.tracks.filter(ReconciliationTrackPresentation::isSelectable)
                        if (content.activeTab == ReconciliationReviewTab.REVIEW && eligible.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { actions.onSelectItems(eligible.map { it.sourceId }) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(pluralStringResource(R.plurals.history_match_select_review_matches, eligible.size, eligible.size)) }
                        }
                        album.tracks.forEach { track -> CompactTrackCard(track, content, actions) }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun ArtistPresentationList(
    artists: List<ReconciliationArtistPresentation>,
    content: ReconciliationReviewContent,
    actions: ListeningHistoryReconciliationUiActions
) {
    if (artists.isEmpty()) {
        EmptyState(stringResource(emptyTitleRes(content)), stringResource(emptyTextRes(content)))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(artists, key = { "artist-${it.key}" }) { artist ->
            val expanded = content.expandedArtistKey == artist.key
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                CompactGroupHeader(
                    title = artist.artist,
                    subtitle = pluralStringResource(R.plurals.history_match_album_count, artist.albums.size, artist.albums.size),
                    summary = stringResource(R.string.history_match_group_summary, artist.importedCount, artist.linkedCount, artist.reviewCount, artist.unmatchedCount),
                    expanded = expanded,
                    onClick = { actions.onToggleArtist(artist.key) }
                )
                if (expanded) {
                    Column(
                        modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider()
                        artist.albums.forEach { album ->
                            Text(
                                album.title,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )
                            Text(
                                stringResource(R.string.history_match_group_summary, album.importedCount, album.linkedCount, album.reviewCount, album.unmatchedCount),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                            album.tracks.forEach { track -> CompactTrackCard(track, content, actions) }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun CompactGroupHeader(
    title: String,
    subtitle: String,
    summary: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val expandLabel = stringResource(if (expanded) R.string.history_match_collapse_group else R.string.history_match_expand_group)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClickLabel = expandLabel,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                summary,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        Icon(
            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = stringResource(if (expanded) R.string.history_match_collapse else R.string.history_match_expand)
        )
    }
}

@Composable
private fun CompactTrackCard(
    track: ReconciliationTrackPresentation,
    content: ReconciliationReviewContent,
    actions: ListeningHistoryReconciliationUiActions
) {
    val resources = LocalResources.current
    val expanded = content.expandedSourceId == track.sourceId
    val selected = track.sourceId in content.selectedSourceIds
    val expandTrackLabel = stringResource(if (expanded) R.string.history_match_collapse_track else R.string.history_match_expand_track)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Button,
                    onClickLabel = expandTrackLabel
                ) { actions.onToggleExpanded(track.sourceId) }
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (track.isSelectable && content.activeTab == ReconciliationReviewTab.REVIEW) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { actions.onToggleSelected(track.sourceId) },
                    enabled = !content.isWorking,
                    modifier = Modifier.semantics {
                        contentDescription = resources.getString(R.string.history_match_select_track, track.source.title)
                    }
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    track.source.title.ifBlank { stringResource(R.string.player_unknown_title) },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    localizedArtistAlbum(track.source.artist, track.source.album),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(pluralStringResource(R.plurals.history_match_historical_play_count, track.source.metrics.qualifiedPlayCount.toInt(), track.source.metrics.qualifiedPlayCount), style = MaterialTheme.typography.labelSmall)
                    Text(
                        stringResource(track.reason.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(if (expanded) R.string.history_match_collapse else R.string.history_match_expand)
            )
        }
        if (expanded) {
            HorizontalDivider()
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HistoricalDetails(track.source)
                when (track.status) {
                    ReconciliationTrackStatus.LINKED -> {
                        val linked = requireNotNull(track.linkedItem)
                        Text(stringResource(R.string.history_match_linked_local_song), style = MaterialTheme.typography.labelLarge)
                        TargetMetadata(linked.target)
                        OutlinedButton(
                            onClick = { actions.onUnlinkRequested(linked) },
                            modifier = Modifier.fillMaxWidth().semantics {
                                contentDescription =
                                    resources.getString(R.string.history_match_unlink_description, linked.source.title, linked.target.title)
                            }
                        ) { Text(stringResource(R.string.history_match_unlink_history)) }
                    }
                    ReconciliationTrackStatus.REVIEW,
                    ReconciliationTrackStatus.UNMATCHED -> {
                        val item = requireNotNull(track.reviewItem)
                        if (item.disposition == ReconciliationCandidateDisposition.AMBIGUOUS) {
                            WarningText(stringResource(R.string.history_match_ambiguous_warning))
                        }
                        item.candidates.forEach { candidate ->
                            CandidateRow(
                                candidate = candidate,
                                actionLabel = if (item.disposition == ReconciliationCandidateDisposition.AMBIGUOUS) {
                                    stringResource(R.string.history_match_select_this_track)
                                } else {
                                    stringResource(R.string.history_match_link_history)
                                },
                                evidenceLabel = if (item.candidates.size == 1) {
                                    stringResource(track.reason.labelRes)
                                } else {
                                    stringResource(candidate.evidence.category.evidenceRes)
                                },
                                onSelected = {
                                    actions.onCandidateSelected(listOf(track.sourceId), candidate.target)
                                }
                            )
                        }
                        if (item.hasMoreCandidates) {
                            Text(
                                stringResource(R.string.history_match_more_candidates),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { actions.onSearchRequested(listOf(track.sourceId)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Text(stringResource(R.string.history_match_choose_library), modifier = Modifier.padding(start = 8.dp))
                        }
                        if (track.status == ReconciliationTrackStatus.REVIEW) {
                            TextButton(
                                onClick = { actions.onSkip(track.sourceId) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(stringResource(R.string.history_match_skip_later)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoricalDetails(source: HistoricalReconciliationSource) {
    val formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
    val providers = source.importedProviders.mapNotNull { provider ->
        when (provider) {
            ListeningSource.SPOTIFY_IMPORT -> stringResource(R.string.history_match_spotify_import)
            ListeningSource.NATIVE -> null
            ListeningSource.LASTFM_IMPORT -> stringResource(R.string.history_match_imported_history)
        }
    }.sorted().joinToString()
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (providers.isNotBlank()) Text(providers, style = MaterialTheme.typography.labelMedium)
        Text(stringResource(R.string.history_match_first_listened, formatter.format(Instant.ofEpochMilli(source.metrics.firstListenedAt))))
        Text(stringResource(R.string.history_match_last_listened, formatter.format(Instant.ofEpochMilli(source.metrics.lastListenedAt))))
        if (source.metrics.recordedListeningMs > 0) {
            Text(stringResource(R.string.history_match_recorded_listening, localizedListeningDuration(source.metrics.recordedListeningMs)))
        }
    }
}

@Composable
private fun CandidateRow(
    candidate: ListeningIdentityReconciliationCandidate,
    actionLabel: String,
    evidenceLabel: String,
    onSelected: () -> Unit
) {
    val warningRes = candidate.evidence.category.warningRes
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(evidenceLabel, style = MaterialTheme.typography.labelLarge)
            TargetMetadata(candidate.target)
            if (candidate.evidence.missingFields.contains(ReconciliationMissingField.ALBUM)) {
                Text(stringResource(R.string.history_match_album_missing), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            warningRes?.let { WarningText(stringResource(it)) }
            Button(onClick = onSelected, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Icon(Icons.Default.Link, contentDescription = null)
                Text(actionLabel, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun TargetMetadata(target: LocalReconciliationTarget) {
    Text(if (target.title.isBlank()) stringResource(R.string.player_unknown_title) else target.title, style = MaterialTheme.typography.titleMedium)
    Text(
        localizedArtistAlbum(target.artist, target.album),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
        formatTargetDetails(target),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun WarningText(text: String) {
    val warningDescription = stringResource(R.string.history_match_warning_description, text)
    Row(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = warningDescription },
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.WarningAmber,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary
        )
        Text(text, modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyState(title: String, text: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.History, contentDescription = null)
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun SearchDialog(
    query: String,
    results: List<LocalReconciliationTarget>,
    isWorking: Boolean,
    onQueryChanged: (String) -> Unit,
    onSelected: (LocalReconciliationTarget) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        title = { Text(stringResource(R.string.history_match_choose_library)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text(stringResource(R.string.history_match_search_hint)) }
                )
                if (results.isEmpty()) {
                    Text(stringResource(R.string.history_match_no_library_songs), modifier = Modifier.padding(top = 20.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(results, key = { "search-${it.identityId}" }) { target ->
                            val selectDescription = stringResource(R.string.history_match_select_track, target.title)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        enabled = !isWorking,
                                        role = Role.Button,
                                        onClickLabel = selectDescription
                                    ) { onSelected(target) },
                                tonalElevation = 2.dp,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) { TargetMetadata(target) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isWorking) { Text(stringResource(R.string.eq_cancel)) } }
    )
}

@Composable
private fun ConfirmationDialog(
    confirmation: ReconciliationConfirmation,
    isWorking: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        title = {
            Text(
                when (confirmation) {
                    is ReconciliationConfirmation.Link -> stringResource(R.string.history_match_confirm_link)
                    is ReconciliationConfirmation.Batch -> stringResource(R.string.history_match_confirm_batch)
                    is ReconciliationConfirmation.Unlink -> stringResource(R.string.history_match_confirm_unlink)
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 470.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (confirmation) {
                    is ReconciliationConfirmation.Link -> {
                        val first = confirmation.sources.first()
                        Text(stringResource(R.string.history_match_imported_history), fontWeight = FontWeight.SemiBold)
                        Text(first.title, style = MaterialTheme.typography.titleMedium)
                        Text(localizedArtistAlbum(first.artist, first.album))
                        Text(
                            if (confirmation.sources.size == 1) pluralStringResource(R.plurals.history_match_historical_play_count, first.metrics.qualifiedPlayCount.toInt(), first.metrics.qualifiedPlayCount)
                            else pluralStringResource(R.plurals.history_match_fragments, confirmation.sources.size, confirmation.sources.size)
                        )
                        Text(stringResource(R.string.history_match_connected_to))
                        TargetMetadata(confirmation.target)
                        localizedRatingWarning(confirmation.ratings)?.let { WarningText(it) }
                        Text(stringResource(R.string.history_match_link_statistics))
                        Text(stringResource(R.string.history_match_unlink_later), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is ReconciliationConfirmation.Batch -> {
                        Text(
                            pluralStringResource(R.plurals.history_match_batch_summary, confirmation.selections.size, confirmation.selections.size)
                        )
                        confirmation.selections.take(8).forEach { selection ->
                            Text(
                                stringResource(R.string.history_match_link_pair, selection.source.title, selection.target.title),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (confirmation.selections.size > 8) {
                            Text(
                                pluralStringResource(R.plurals.history_match_more_count, confirmation.selections.size - 8, confirmation.selections.size - 8),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(stringResource(R.string.history_match_conflicts_not_overwritten))
                        Text(stringResource(R.string.history_match_unlink_these_later), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is ReconciliationConfirmation.Unlink -> {
                        Text(
                            stringResource(R.string.history_match_unlink_statistics)
                        )
                        Text(confirmation.item.source.title, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.history_match_linked_to, confirmation.item.target.title))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isWorking) {
                Text(
                    when (confirmation) {
                        is ReconciliationConfirmation.Link -> if (confirmation.sources.size == 1) {
                            stringResource(R.string.history_match_link_history)
                        } else pluralStringResource(R.plurals.history_match_link_all, confirmation.sources.size, confirmation.sources.size)
                        is ReconciliationConfirmation.Batch ->
                            pluralStringResource(R.plurals.history_match_link_count, confirmation.selections.size, confirmation.selections.size)
                        is ReconciliationConfirmation.Unlink -> stringResource(R.string.history_match_unlink)
                    }
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isWorking) { Text(stringResource(R.string.eq_cancel)) } }
    )
}

internal val ReconciliationCandidateCategory.evidenceRes: Int get() = when (this) {
    ReconciliationCandidateCategory.STRONG_METADATA -> R.string.history_match_evidence_strong
    ReconciliationCandidateCategory.CANONICAL_METADATA -> R.string.history_match_evidence_canonical
    ReconciliationCandidateCategory.TYPOGRAPHY_VARIANT -> R.string.history_match_evidence_typography
    ReconciliationCandidateCategory.INCOMPLETE_EVIDENCE -> R.string.history_match_evidence_incomplete
    ReconciliationCandidateCategory.VERSION_SENSITIVE -> R.string.history_match_evidence_version
    ReconciliationCandidateCategory.AMBIGUOUS -> R.string.history_match_evidence_ambiguous
}

internal val ReconciliationCandidateCategory.warningRes: Int? get() = when (this) {
    ReconciliationCandidateCategory.VERSION_SENSITIVE -> R.string.history_match_warning_version
    ReconciliationCandidateCategory.AMBIGUOUS -> R.string.history_match_warning_ambiguous
    else -> null
}

private val ReconciliationReviewTab.labelRes: Int get() = when (this) {
    ReconciliationReviewTab.REVIEW -> R.string.history_match_tab_review
    ReconciliationReviewTab.UNMATCHED -> R.string.history_match_tab_unmatched
    ReconciliationReviewTab.LINKED -> R.string.history_match_tab_linked
}

private val ReconciliationBrowseMode.labelRes: Int get() = when (this) {
    ReconciliationBrowseMode.TRACKS -> R.string.history_match_tracks
    ReconciliationBrowseMode.ALBUMS -> R.string.history_match_albums
    ReconciliationBrowseMode.ARTISTS -> R.string.history_match_artists
}

private val ReconciliationSortOption.labelRes: Int get() = when (this) {
    ReconciliationSortOption.HISTORICAL_PLAYS -> R.string.history_match_historical_plays
    ReconciliationSortOption.TRACK_TITLE -> R.string.history_match_track_title
    ReconciliationSortOption.ARTIST -> R.string.history_match_artist
    ReconciliationSortOption.ALBUM -> R.string.history_match_album
}

private val ReconciliationReviewFilter.labelRes: Int get() = when (this) {
    ReconciliationReviewFilter.ALL -> R.string.history_match_all_review
    ReconciliationReviewFilter.TITLE_FORMATTING -> R.string.history_match_title_formatting
    ReconciliationReviewFilter.ACCENT_DIACRITIC -> R.string.history_match_accent
    ReconciliationReviewFilter.SIMILAR_TITLE -> R.string.history_match_similar_title
    ReconciliationReviewFilter.AMBIGUOUS -> R.string.history_match_ambiguous
}

private fun emptyTitleRes(content: ReconciliationReviewContent): Int = when (content.activeTab) {
    ReconciliationReviewTab.REVIEW -> R.string.history_match_empty_review
    ReconciliationReviewTab.UNMATCHED -> R.string.history_match_empty_unmatched
    ReconciliationReviewTab.LINKED -> R.string.history_match_empty_linked
}

private fun emptyTextRes(content: ReconciliationReviewContent): Int = when {
    content.browseQuery.isNotBlank() -> R.string.history_match_empty_search
    content.activeTab == ReconciliationReviewTab.REVIEW ->
        R.string.history_match_empty_review_help
    content.activeTab == ReconciliationReviewTab.UNMATCHED ->
        R.string.history_match_empty_unmatched_help
    else -> R.string.history_match_empty_linked_help
}

fun formatArtistAlbum(artist: String, album: String): String =
    listOf(artist.trim(), album.trim()).filter(String::isNotBlank).joinToString(" · ")

@Composable
private fun localizedArtistAlbum(artist: String, album: String): String =
    formatArtistAlbum(artist, album).let { if (it.isBlank()) stringResource(R.string.player_unknown_artist) else it }

fun formatTargetDetails(target: LocalReconciliationTarget): String = buildList {
    target.durationMs?.takeIf { it > 0 }?.let { add(formatTrackDuration(it)) }
    target.fileExtension?.takeIf(String::isNotBlank)?.let { add(it.uppercase(Locale.ROOT)) }
    if (isEmpty()) target.relativeFolder?.substringAfterLast('/')?.takeIf(String::isNotBlank)?.let(::add)
}.joinToString(" · ")

private fun formatTrackDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1_000L
    return "%d:%02d".format(Locale.ROOT, totalSeconds / 60L, totalSeconds % 60L)
}

@Composable
private fun localizedListeningDuration(milliseconds: Long): String {
    val minutes = milliseconds / 60_000L
    return if (minutes < 60) pluralStringResource(R.plurals.history_match_minutes, minutes.toInt(), minutes)
    else stringResource(R.string.history_match_hours_minutes, minutes / 60, minutes % 60)
}

@Composable
private fun localizedRatingWarning(ratings: List<ListeningIdentityReconciliationRatings>): String? {
    val conflict = ratings.firstOrNull { it.state == ListeningIdentityReconciliationRatingState.CONFLICTING_RATINGS }
    if (conflict != null) {
        conflict.targetRating?.let { rating ->
            return stringResource(R.string.history_match_rating_conflict, rating)
        }
    }
    if (ratings.any { it.state == ListeningIdentityReconciliationRatingState.SOURCE_ONLY }) {
        return stringResource(R.string.history_match_rating_source_only)
    }
    return null
}
