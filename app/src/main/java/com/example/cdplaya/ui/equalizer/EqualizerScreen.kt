package com.example.cdplaya.ui.equalizer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.cdplaya.player.equalizer.MAX_EQUALIZER_BAND_DB
import com.example.cdplaya.player.equalizer.MAX_EQUALIZER_PREAMP_DB
import com.example.cdplaya.player.equalizer.MIN_EQUALIZER_BAND_DB
import com.example.cdplaya.player.equalizer.MIN_EQUALIZER_PREAMP_DB
import com.example.cdplaya.player.equalizer.UserEqualizerPreset
import com.example.cdplaya.player.equalizer.dsp.GraphicEqualizerDefaults
import com.example.cdplaya.player.equalizer.normalizeEqualizerDb
import kotlin.math.round

@Composable
internal fun EqualizerScreen(
    state: EqualizerScreenState,
    actions: EqualizerUiActions,
    modifier: Modifier = Modifier
) {
    var presetSelectorVisible by remember {
        mutableStateOf(false)
    }
    var saveDialogVisible by remember {
        mutableStateOf(false)
    }
    var renamePreset by remember {
        mutableStateOf<UserEqualizerPreset?>(null)
    }
    var deletePreset by remember {
        mutableStateOf<UserEqualizerPreset?>(null)
    }
    var resetConfirmationVisible by remember {
        mutableStateOf(false)
    }
    var fineEditTarget by remember {
        mutableStateOf<FineEditTarget?>(null)
    }
    val preferences = state.editablePreferences
    var latestPreampDragValue by remember(
        preferences.preampDb
    ) {
        mutableDoubleStateOf(preferences.preampDb)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = actions.onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "Equalizer",
                style = MaterialTheme.typography.titleLarge
            )
        }

        ListItem(
            headlineContent = { Text("Equalizer") },
            supportingContent = {
                Text(state.statusText)
            },
            trailingContent = {
                Switch(
                    checked = preferences.enabled,
                    onCheckedChange = actions.onEnabledChanged,
                    modifier = Modifier.semantics {
                        contentDescription =
                            "Equalizer enabled"
                    }
                )
            }
        )

        ListItem(
            headlineContent = { Text("Preset") },
            supportingContent = { Text(state.presetLabel) },
            trailingContent = {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Choose equalizer preset"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription =
                        "Equalizer preset, ${state.presetLabel}"
                }
                .padding(horizontal = 4.dp)
        )
        TextButton(
            onClick = { presetSelectorVisible = true },
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text("Choose or manage presets")
        }

        EqualizerResponseGraph(
            analysis = state.analysis,
            modifier = Modifier.padding(16.dp)
        )

        EqualizerAnalysisStatus(state)

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Text(
            text = "Preamp",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Slider(
                value = preferences.preampDb.toFloat(),
                onValueChange = { value ->
                    latestPreampDragValue =
                        snapPreamp(value.toDouble())
                    actions.onPreviewPreamp(
                        latestPreampDragValue
                    )
                },
                onValueChangeFinished = {
                    actions.onCommitPreamp(
                        latestPreampDragValue
                    )
                },
                valueRange =
                    MIN_EQUALIZER_PREAMP_DB.toFloat()..
                        MAX_EQUALIZER_PREAMP_DB.toFloat(),
                steps = 41,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription =
                            "Equalizer preamp, " +
                                formatEqualizerDb(
                                    preferences.preampDb
                                )
                    }
            )
            TextButton(
                onClick = {
                    fineEditTarget = FineEditTarget(
                        title = "Preamp",
                        initialValueDb =
                            preferences.preampDb,
                        minimumDb =
                            MIN_EQUALIZER_PREAMP_DB,
                        maximumDb =
                            MAX_EQUALIZER_PREAMP_DB,
                        bandIndex = null
                    )
                }
            ) {
                Text(formatEqualizerDb(preferences.preampDb))
            }
        }

        Text(
            text = "Graphic bands",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(
                start = 16.dp,
                top = 16.dp
            )
        )
        Row(
            horizontalArrangement =
                Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            GraphicEqualizerDefaults.frequenciesHz
                .forEachIndexed { index, frequencyHz ->
                    EqualizerBandSlider(
                        frequencyHz = frequencyHz,
                        gainDb =
                            preferences.bandGainsDb[index],
                        unavailable =
                            index in state.analysis
                                .ignoredBandIndices,
                        onValueChange = { gain ->
                            actions.onPreviewBandGain(index, gain)
                        },
                        onValueChangeFinished = { gain ->
                            actions.onCommitBandGain(index, gain)
                        },
                        onFineEditClick = {
                            fineEditTarget = FineEditTarget(
                                title = formatEqualizerFrequency(
                                    frequencyHz
                                ),
                                initialValueDb =
                                    preferences.bandGainsDb[index],
                                minimumDb =
                                    MIN_EQUALIZER_BAND_DB,
                                maximumDb =
                                    MAX_EQUALIZER_BAND_DB,
                                bandIndex = index
                            )
                        }
                    )
                }
        }

        ListItem(
            headlineContent = {
                Text("Automatic headroom")
            },
            supportingContent = {
                Text(
                    "Reduces the signal before equalization when " +
                        "the combined curve is predicted to exceed " +
                        "digital full scale."
                )
            },
            trailingContent = {
                Switch(
                    checked =
                        preferences.automaticHeadroomEnabled,
                    onCheckedChange =
                        actions.onAutomaticHeadroomChanged,
                    modifier = Modifier.semantics {
                        contentDescription =
                            "Automatic equalizer headroom"
                    }
                )
            }
        )
        if (
            !preferences.automaticHeadroomEnabled &&
            state.analysis.predictedMaximumDb > 0.0
        ) {
            Text(
                text = "The predicted response exceeds 0 dB. " +
                    "PCM16 saturation is not a limiter.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (state.comparisonAvailable) {
            Text(
                text = "A/B comparison",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(
                    start = 16.dp,
                    top = 20.dp
                )
            )
            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                if (!state.comparisonBypassed) {
                    Button(
                        onClick = {
                            actions
                                .onComparisonBypassedChanged(false)
                        }
                    ) {
                        Text("A · Equalized")
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            actions
                                .onComparisonBypassedChanged(false)
                        }
                    ) {
                        Text("A · Equalized")
                    }
                }
                if (state.comparisonBypassed) {
                    Button(
                        onClick = {
                            actions
                                .onComparisonBypassedChanged(true)
                        }
                    ) {
                        Text("B · Bypass")
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            actions
                                .onComparisonBypassedChanged(true)
                        }
                    ) {
                        Text("B · Bypass")
                    }
                }
            }
            Text(
                text = "B uses exact DSP bypass while keeping decoded " +
                    "PCM active to avoid offload or renderer churn.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        FilledTonalButton(
            onClick = {
                if (state.presetLabel == "Flat") {
                    actions.onResetToFlat()
                } else {
                    resetConfirmationVisible = true
                }
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Reset to Flat")
        }

        Card(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "An active equalizer requires decoded PCM. " +
                    "CDPlaya does not claim bit-perfect or " +
                    "high-resolution output while processing.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (presetSelectorVisible) {
        EqualizerPresetSelectorDialog(
            userPresets = state.userPresets,
            onDismiss = {
                presetSelectorVisible = false
            },
            onApplyBuiltIn =
                actions.onApplyBuiltInPreset,
            onApplyUser = actions.onApplyUserPreset,
            onSaveAs = { saveDialogVisible = true },
            onRename = { preset ->
                presetSelectorVisible = false
                renamePreset = preset
            },
            onDelete = { preset ->
                presetSelectorVisible = false
                deletePreset = preset
            }
        )
    }
    if (saveDialogVisible) {
        EqualizerPresetNameDialog(
            title = "Save as preset",
            initialName = "",
            userPresets = state.userPresets,
            confirmText = "Save",
            onDismiss = { saveDialogVisible = false },
            onConfirm = { name ->
                actions.onSaveUserPreset(name)
                saveDialogVisible = false
            }
        )
    }
    renamePreset?.let { preset ->
        EqualizerPresetNameDialog(
            title = "Rename preset",
            initialName = preset.name,
            userPresets = state.userPresets,
            excludingPresetId = preset.id,
            confirmText = "Rename",
            onDismiss = { renamePreset = null },
            onConfirm = { name ->
                actions.onRenameUserPreset(preset.id, name)
                renamePreset = null
            }
        )
    }
    deletePreset?.let { preset ->
        ConfirmEqualizerActionDialog(
            title = "Delete ${preset.name}?",
            message = "The active equalizer curve will not change.",
            confirmText = "Delete",
            onDismiss = { deletePreset = null },
            onConfirm = {
                actions.onDeleteUserPreset(preset.id)
                deletePreset = null
            }
        )
    }
    if (resetConfirmationVisible) {
        ConfirmEqualizerActionDialog(
            title = "Reset to Flat?",
            message = "Preamp and all band gains will reset. " +
                "Your saved presets will remain.",
            confirmText = "Reset",
            onDismiss = {
                resetConfirmationVisible = false
            },
            onConfirm = {
                actions.onResetToFlat()
                resetConfirmationVisible = false
            }
        )
    }
    fineEditTarget?.let { target ->
        EqualizerValueDialog(
            title = target.title,
            initialValueDb = target.initialValueDb,
            minimumDb = target.minimumDb,
            maximumDb = target.maximumDb,
            onPreview = { value ->
                target.bandIndex?.let { index ->
                    actions.onPreviewBandGain(index, value)
                } ?: actions.onPreviewPreamp(value)
            },
            onCancel = {
                target.bandIndex?.let { index ->
                    actions.onCancelBandGainPreview(
                        index,
                        target.initialValueDb
                    )
                } ?: actions.onCancelPreampPreview(
                    target.initialValueDb
                )
                fineEditTarget = null
            },
            onApply = { value ->
                target.bandIndex?.let { index ->
                    actions.onCommitBandGain(index, value)
                } ?: actions.onCommitPreamp(value)
                fineEditTarget = null
            }
        )
    }
}

@Composable
private fun EqualizerAnalysisStatus(
    state: EqualizerScreenState
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Analysis",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                "User preamp: " +
                    formatEqualizerDb(
                        state.editablePreferences.preampDb
                    )
            )
            Text(
                "Automatic attenuation: " +
                    formatEqualizerDb(
                        state.analysis.automaticHeadroom
                            .attenuationDb,
                        includePlus = false
                    )
            )
            Text(
                "Effective preamp: " +
                    formatEqualizerDb(
                        state.analysis.automaticHeadroom
                            .effectivePreampDb
                    )
            )
            Text(
                "Predicted maximum: " +
                    formatEqualizerDb(
                        state.analysis.predictedMaximumDb
                    )
            )
            Text(
                "Sample rate: " +
                    "${state.analysis.sampleRateHz} Hz" +
                    if (
                        state.analysis.usesFallbackSampleRate
                    ) {
                        " (preview fallback)"
                    } else {
                        ""
                    }
            )
            if (state.analysis.ignoredBandIndices.isNotEmpty()) {
                val labels = state.analysis.ignoredBandIndices
                    .sorted()
                    .joinToString { index ->
                        formatEqualizerFrequency(
                            GraphicEqualizerDefaults
                                .frequenciesHz[index]
                        )
                    }
                Text(
                    text = "Unavailable for current source: $labels",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private data class FineEditTarget(
    val title: String,
    val initialValueDb: Double,
    val minimumDb: Double,
    val maximumDb: Double,
    val bandIndex: Int?
)

internal fun snapPreamp(value: Double): Double {
    return normalizeEqualizerDb(
        round(
            value.coerceIn(
                MIN_EQUALIZER_PREAMP_DB,
                MAX_EQUALIZER_PREAMP_DB
            ) * 2.0
        ) / 2.0
    )
}
