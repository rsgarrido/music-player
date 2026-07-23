package com.example.cdplaya.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cdplaya.R
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.player.audio.AudioOffloadPreference
import com.example.cdplaya.player.replaygain.ReplayGainMode
import com.example.cdplaya.ui.player.modern.ModernArtworkTransitionStyle
import com.example.cdplaya.ui.player.modern.ModernSeekbarStyle
import com.example.cdplaya.ui.player.theme.PlayerThemeTokenField
import com.example.cdplaya.ui.player.theme.PlayerThemeTokens
import com.example.cdplaya.ui.player.theme.customizationOptions

@Composable
fun SettingsScreen(
    totalSongCount: Int,
    availableFolderCount: Int,
    selectedFolderCount: Int,
    onBackClick: () -> Unit,
    onLibraryFoldersClick: () -> Unit,
    onExportBackupClick: () -> Unit,
    onRestoreBackupClick: () -> Unit,
    onDiagnosticsClick: () -> Unit,
    isSleepTimerActive: Boolean,
    sleepTimerDisplayText: String,
    onSleepTimerClick: () -> Unit,
    selectedPlayerTheme: PlayerTheme,
    selectedPlayerThemeTokens: PlayerThemeTokens,
    onPlayerThemeSelected: (PlayerTheme) -> Unit,
    onUpdatePlayerThemeTokenOverride: (PlayerTheme, PlayerThemeTokenField, Color) -> Unit,
    onResetPlayerThemeTokenOverrides: (PlayerTheme) -> Unit,
    selectedModernArtworkTransitionStyle: ModernArtworkTransitionStyle,
    onModernArtworkTransitionStyleSelected: (ModernArtworkTransitionStyle) -> Unit,
    selectedModernSeekbarStyle: ModernSeekbarStyle,
    onModernSeekbarStyleSelected: (ModernSeekbarStyle) -> Unit,
    selectedReplayGainMode: ReplayGainMode,
    onReplayGainModeSelected: (ReplayGainMode) -> Unit,
    selectedAudioOffloadPreference: AudioOffloadPreference,
    onAudioOffloadPreferenceSelected: (AudioOffloadPreference) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPlayerThemeDialogVisible by remember {
        mutableStateOf(false)
    }

    var isReplayGainDialogVisible by remember {
        mutableStateOf(false)
    }

    var isAudioOffloadDialogVisible by remember {
        mutableStateOf(false)
    }

    var isThemeCustomizationDialogVisible by remember {
        mutableStateOf(false)
    }

    var isArtworkTransitionDialogVisible by remember {
        mutableStateOf(false)
    }

    var isSeekbarStyleDialogVisible by remember {
        mutableStateOf(false)
    }

    val themeCustomizationOptions = selectedPlayerTheme.customizationOptions()

    val folderSelectionText = if (selectedFolderCount == 0) {
        "All folders • $availableFolderCount available"
    } else {
        "$selectedFolderCount selected • $availableFolderCount available"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge
            )
        }

        SettingsSectionTitle(text = "Library")

        ListItem(
            headlineContent = {
                Text(text = "Library folders")
            },
            supportingContent = {
                Text(text = folderSelectionText)
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = "Open library folders"
                )
            },
            modifier = Modifier.clickable {
                onLibraryFoldersClick()
            }
        )

        ListItem(
            headlineContent = {
                Text(text = "Songs")
            },
            supportingContent = {
                Text(text = "$totalSongCount song(s) in your current library")
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp)
        )

        SettingsSectionTitle(text = "Backup and Restore")

        ListItem(
            headlineContent = {
                Text(text = "Export Backup")
            },
            supportingContent = {
                Text(text = "Save favorites, playlists, history, and preferences as JSON.")
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = "Export backup"
                )
            },
            modifier = Modifier.clickable {
                onExportBackupClick()
            }
        )

        ListItem(
            headlineContent = {
                Text(text = "Restore Backup")
            },
            supportingContent = {
                Text(text = "Replace app data from a CDPlaya backup JSON file.")
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = "Restore backup"
                )
            },
            modifier = Modifier.clickable {
                onRestoreBackupClick()
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp)
        )

        SettingsSectionTitle(text = stringResource(R.string.settings_support))

        ListItem(
            headlineContent = { Text(text = stringResource(R.string.settings_diagnostics)) },
            supportingContent = {
                Text(text = stringResource(R.string.settings_diagnostics_summary))
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.settings_open_diagnostics)
                )
            },
            modifier = Modifier.clickable(onClick = onDiagnosticsClick)
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp)
        )

        SettingsSectionTitle(text = "Playback")

        ListItem(
            headlineContent = {
                Text(text = "Playback controls")
            },
            supportingContent = {
                Text(text = "Use the player card, notification, or Up Next screen.")
            }
        )

        ListItem(
            headlineContent = {
                Text(text = "ReplayGain")
            },
            supportingContent = {
                Text(text = selectedReplayGainMode.displayName)
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = "Open ReplayGain settings"
                )
            },
            modifier = Modifier.clickable {
                isReplayGainDialogVisible = true
            }
        )

        ListItem(
            headlineContent = {
                Text(text = "Audio offload")
            },
            supportingContent = {
                Text(
                    text = "${selectedAudioOffloadPreference.displayName}. " +
                        "Automatic uses offload only when the device, route, file, " +
                        "and required playback behavior support it. Unsupported playback " +
                        "falls back normally."
                )
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = "Open audio offload settings"
                )
            },
            modifier = Modifier.clickable {
                isAudioOffloadDialogVisible = true
            }
        )

        Text(
            text = "Offload may reduce power use during long background playback. " +
                "Some future audio effects may require normal decoded playback.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        ListItem(
            headlineContent = {
                Text(text = "Sleep Timer")
            },
            supportingContent = {
                Text(
                    text = if (isSleepTimerActive) {
                        sleepTimerDisplayText
                    } else {
                        "Pause playback after a set time"
                    }
                )
            },
            modifier = Modifier.clickable {
                onSleepTimerClick()
            }
        )

        ListItem(
            headlineContent = {
                Text(text = "Player Theme")
            },
            supportingContent = {
                Text(text = selectedPlayerTheme.displayName)
            },
            modifier = Modifier.clickable {
                isPlayerThemeDialogVisible = true
            }
        )

        if (selectedPlayerTheme == PlayerTheme.DEFAULT) {
            ListItem(
                headlineContent = {
                    Text(text = "Artwork transition style")
                },
                supportingContent = {
                    Text(text = selectedModernArtworkTransitionStyle.displayName)
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = "Choose artwork transition style"
                    )
                },
                modifier = Modifier.clickable {
                    isArtworkTransitionDialogVisible = true
                }
            )

            ListItem(
                headlineContent = {
                    Text(text = "Modern player seekbar style")
                },
                supportingContent = {
                    Text(text = selectedModernSeekbarStyle.displayName)
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = "Choose modern player seekbar style"
                    )
                },
                modifier = Modifier.clickable {
                    isSeekbarStyleDialogVisible = true
                }
            )
        }

        if (themeCustomizationOptions.isNotEmpty()) {
            ListItem(
                headlineContent = {
                    Text(text = "Customize theme colors")
                },
                supportingContent = {
                    Text(text = "Choose preset colors for ${selectedPlayerTheme.displayName}")
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = "Customize theme colors"
                    )
                },
                modifier = Modifier.clickable {
                    isThemeCustomizationDialogVisible = true
                }
            )
        }

        if (isReplayGainDialogVisible) {
            AlertDialog(
                onDismissRequest = {
                    isReplayGainDialogVisible = false
                },
                title = {
                    Text(text = "ReplayGain")
                },
                text = {
                    Column {
                        ReplayGainMode.values().forEach { replayGainMode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onReplayGainModeSelected(replayGainMode)
                                        isReplayGainDialogVisible = false
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedReplayGainMode == replayGainMode,
                                    onClick = {
                                        onReplayGainModeSelected(replayGainMode)
                                        isReplayGainDialogVisible = false
                                    }
                                )

                                Column(
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Text(text = replayGainMode.displayName)

                                    Text(
                                        text = replayGainMode.description,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            isReplayGainDialogVisible = false
                        }
                    ) {
                        Text(text = "Close")
                    }
                }
            )
        }

        if (isAudioOffloadDialogVisible) {
            AlertDialog(
                onDismissRequest = {
                    isAudioOffloadDialogVisible = false
                },
                title = {
                    Text(text = "Audio offload")
                },
                text = {
                    Column {
                        AudioOffloadPreference.entries.forEach { preference ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAudioOffloadPreferenceSelected(preference)
                                        isAudioOffloadDialogVisible = false
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedAudioOffloadPreference == preference,
                                    onClick = {
                                        onAudioOffloadPreferenceSelected(preference)
                                        isAudioOffloadDialogVisible = false
                                    }
                                )
                                Column(modifier = Modifier.padding(start = 4.dp)) {
                                    Text(text = preference.displayName)
                                    Text(
                                        text = if (preference == AudioOffloadPreference.AUTOMATIC) {
                                            "Use offload when compatible and otherwise play normally."
                                        } else {
                                            "Use normal decoded playback."
                                        },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { isAudioOffloadDialogVisible = false }) {
                        Text(text = "Close")
                    }
                }
            )
        }

        if (
            isArtworkTransitionDialogVisible &&
            selectedPlayerTheme == PlayerTheme.DEFAULT
        ) {
            AlertDialog(
                onDismissRequest = {
                    isArtworkTransitionDialogVisible = false
                },
                title = {
                    Text(text = "Artwork transition style")
                },
                text = {
                    Column {
                        ModernArtworkTransitionStyle.values().forEach { transitionStyle ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onModernArtworkTransitionStyleSelected(transitionStyle)
                                        isArtworkTransitionDialogVisible = false
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedModernArtworkTransitionStyle == transitionStyle,
                                    onClick = {
                                        onModernArtworkTransitionStyleSelected(transitionStyle)
                                        isArtworkTransitionDialogVisible = false
                                    }
                                )

                                Column(
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Text(text = transitionStyle.displayName)

                                    Text(
                                        text = transitionStyle.description,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            isArtworkTransitionDialogVisible = false
                        }
                    ) {
                        Text(text = "Close")
                    }
                }
            )
        }

        if (
            isSeekbarStyleDialogVisible &&
            selectedPlayerTheme == PlayerTheme.DEFAULT
        ) {
            AlertDialog(
                onDismissRequest = {
                    isSeekbarStyleDialogVisible = false
                },
                title = {
                    Text(text = "Modern player seekbar style")
                },
                text = {
                    Column {
                        ModernSeekbarStyle.values().forEach { seekbarStyle ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onModernSeekbarStyleSelected(seekbarStyle)
                                        isSeekbarStyleDialogVisible = false
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedModernSeekbarStyle == seekbarStyle,
                                    onClick = {
                                        onModernSeekbarStyleSelected(seekbarStyle)
                                        isSeekbarStyleDialogVisible = false
                                    }
                                )

                                Column(
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Text(text = seekbarStyle.displayName)

                                    Text(
                                        text = seekbarStyle.description,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            isSeekbarStyleDialogVisible = false
                        }
                    ) {
                        Text(text = "Close")
                    }
                }
            )
        }

        if (isPlayerThemeDialogVisible) {
            AlertDialog(
                onDismissRequest = {
                    isPlayerThemeDialogVisible = false
                },
                title = {
                    Text(text = "Player Theme")
                },
                text = {
                    Column {
                        PlayerTheme.values().forEach { playerTheme ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onPlayerThemeSelected(playerTheme)
                                        isPlayerThemeDialogVisible = false
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPlayerTheme == playerTheme,
                                    onClick = {
                                        onPlayerThemeSelected(playerTheme)
                                        isPlayerThemeDialogVisible = false
                                    }
                                )

                                Text(text = playerTheme.displayName)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            isPlayerThemeDialogVisible = false
                        }
                    ) {
                        Text(text = "Close")
                    }
                }
            )
        }

        if (isThemeCustomizationDialogVisible && themeCustomizationOptions.isNotEmpty()) {
            ThemeColorCustomizationDialog(
                playerTheme = selectedPlayerTheme,
                tokens = selectedPlayerThemeTokens,
                onColorSelected = { field, color ->
                    onUpdatePlayerThemeTokenOverride(selectedPlayerTheme, field, color)
                },
                onReset = {
                    onResetPlayerThemeTokenOverrides(selectedPlayerTheme)
                },
                onDismiss = {
                    isThemeCustomizationDialogVisible = false
                }
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp)
        )

        SettingsSectionTitle(text = "About")

        ListItem(
            headlineContent = {
                Text(text = "CDPlaya")
            },
            supportingContent = {
                Text(text = "A local music player for your personal library.")
            }
        )
    }
}

@Composable
private fun SettingsSectionTitle(
    text: String
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
