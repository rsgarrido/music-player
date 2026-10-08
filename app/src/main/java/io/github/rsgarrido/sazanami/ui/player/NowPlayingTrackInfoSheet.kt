package io.github.rsgarrido.sazanami.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.player.audioquality.AudioQualityInfo
import io.github.rsgarrido.sazanami.player.audioquality.AudioQualityRepository
import java.util.Locale

internal const val TrackInfoSheetTag = "now_playing_track_information"
internal const val TrackInfoDetailsTag = "now_playing_track_information_details"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NowPlayingTrackInfoSheet(
    request: NowPlayingTrackInfoRequest,
    onDismiss: () -> Unit,
    loadAudio: (suspend (Song) -> AudioQualityInfo)? = null
) {
    val context = LocalContext.current
    val repository = remember(context) { AudioQualityRepository(context) }
    val audio = rememberTrackInfoAudio(request, loadAudio ?: repository::getAudioQualityInfo)
    val song = request.song
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val sections = trackInfoSections(song, audio, locale)
    val title = stringResource(R.string.player_track_information)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag(TrackInfoSheetTag).semantics { paneTitle = title }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text(knownTrackInfoText(song.title) ?: stringResource(R.string.player_unknown_title),
                style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp))
            Text(stringResource(R.string.player_more_song_subtitle,
                knownTrackInfoText(song.artist) ?: stringResource(R.string.player_unknown_artist),
                knownTrackInfoText(song.album) ?: stringResource(R.string.player_unknown_album)),
                style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.metadata_done)) }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false).testTag(TrackInfoDetailsTag),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            sections.forEach { section ->
                item(key = section.title) {
                    HorizontalDivider()
                    Text(stringResource(section.title), style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 16.dp).semantics { heading() })
                }
                items(section.rows, key = { it.label }) { row ->
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(row.label), style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val value = when (val content = row.value) {
                            is TrackInfoValue.Text -> content.text
                            is TrackInfoValue.Localized -> stringResource(content.resource, *content.arguments.toTypedArray())
                        }
                        Text(value, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
