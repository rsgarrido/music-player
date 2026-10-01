package io.github.rsgarrido.sazanami.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.mediaaccess.MediaAccessState
import io.github.rsgarrido.sazanami.mediaaccess.PermissionAccess

@Composable
internal fun LibraryStartupScreen(
    mediaAccessState: MediaAccessState,
    initialLibraryReady: Boolean,
    folderArtworkOnboardingComplete: Boolean,
    onRequestAudioAccess: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onChooseFolderArtwork: () -> Unit,
    onSkipFolderArtwork: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 30.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.size(24.dp))

        when {
            !mediaAccessState.hasAudioAccess -> {
                Text(
                    text = stringResource(R.string.startup_find_music),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    text = stringResource(R.string.startup_audio_explanation),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(22.dp))
                if (mediaAccessState.audioAccess == PermissionAccess.PERMANENTLY_DENIED) {
                    Button(onClick = onOpenAppSettings) { Text(stringResource(R.string.settings_open_app_settings)) }
                } else {
                    Button(onClick = onRequestAudioAccess) {
                        Text(stringResource(R.string.startup_grant_music_access))
                    }
                }
            }

            !initialLibraryReady -> {
                CircularProgressIndicator()
                Spacer(Modifier.size(18.dp))
                Text(
                    text = stringResource(R.string.startup_finding_music),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = stringResource(R.string.startup_index_explanation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            !folderArtworkOnboardingComplete -> {
                Text(
                    text = stringResource(R.string.startup_optional_folder_artwork),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    text = stringResource(R.string.startup_folder_artwork_explanation),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(22.dp))
                Button(onClick = onChooseFolderArtwork) {
                    Text(stringResource(R.string.startup_allow_folder_artwork))
                }
                Spacer(Modifier.size(10.dp))
                OutlinedButton(onClick = onSkipFolderArtwork) { Text(stringResource(R.string.folder_selection_not_now)) }
            }
        }
    }
}
