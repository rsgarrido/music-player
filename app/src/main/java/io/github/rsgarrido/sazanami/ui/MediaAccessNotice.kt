package io.github.rsgarrido.sazanami.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.mediaaccess.MediaAccessState
import io.github.rsgarrido.sazanami.mediaaccess.PermissionAccess

@Composable
internal fun MediaAccessNotice(
    state: MediaAccessState,
    onRequestAudioAccess: () -> Unit,
    onRequestArtworkAccess: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!state.hasAudioAccess) {
            Text(
                text = stringResource(R.string.media_access_audio_needed),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = if (state.audioPermissionRequested) {
                    stringResource(R.string.media_access_audio_still_needed)
                } else {
                    stringResource(R.string.media_access_audio_explanation)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            when (state.audioAccess) {
                PermissionAccess.PERMANENTLY_DENIED -> {
                    Text(
                        text = stringResource(R.string.media_access_audio_disabled),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = onOpenAppSettings) {
                        Text(stringResource(R.string.settings_open_app_settings))
                    }
                }
                PermissionAccess.REQUESTABLE,
                PermissionAccess.DENIED -> {
                    Button(onClick = onRequestAudioAccess) {
                        Text(
                            if (state.audioPermissionRequested) {
                                stringResource(R.string.media_access_try_again)
                            } else {
                                stringResource(R.string.media_access_grant_audio)
                            }
                        )
                    }
                }
                PermissionAccess.GRANTED,
                PermissionAccess.NOT_REQUIRED -> Unit
            }
        } else if (!state.hasArtworkAccess) {
            Text(
                text = stringResource(R.string.media_access_folder_optional),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(R.string.media_access_folder_explanation),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (state.artworkAccess == PermissionAccess.PERMANENTLY_DENIED) {
                OutlinedButton(onClick = onOpenAppSettings) {
                    Text(stringResource(R.string.settings_open_app_settings))
                }
            } else {
                OutlinedButton(onClick = onRequestArtworkAccess) {
                    Text(
                        if (state.artworkPermissionRequested) {
                            stringResource(R.string.media_access_try_folder_again)
                        } else {
                            stringResource(R.string.media_access_allow_folder)
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun LibraryLoadingNotice(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.media_access_loading_library))
    }
}

@Composable
internal fun EmptyLibraryNotice(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(R.string.media_access_no_music),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(R.string.media_access_no_music_explanation),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun LibraryErrorNotice(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(R.string.media_access_library_unavailable),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error
        )
    }
}
