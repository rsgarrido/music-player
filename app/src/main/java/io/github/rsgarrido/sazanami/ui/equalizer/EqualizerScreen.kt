package io.github.rsgarrido.sazanami.ui.equalizer

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.ui.state.resolve
import io.github.rsgarrido.sazanami.player.equalizer.MAX_EQUALIZER_BAND_DB
import io.github.rsgarrido.sazanami.player.equalizer.MAX_EQUALIZER_PREAMP_DB
import io.github.rsgarrido.sazanami.player.equalizer.MIN_EQUALIZER_BAND_DB
import io.github.rsgarrido.sazanami.player.equalizer.MIN_EQUALIZER_PREAMP_DB
import io.github.rsgarrido.sazanami.player.equalizer.EqualizerMode
import io.github.rsgarrido.sazanami.player.equalizer.UserEqualizerPreset
import io.github.rsgarrido.sazanami.player.equalizer.dsp.GraphicEqualizerDefaults
import io.github.rsgarrido.sazanami.player.equalizer.normalizeEqualizerDb
import io.github.rsgarrido.sazanami.player.equalizer.toDspConfiguration
import io.github.rsgarrido.sazanami.player.equalizer.limiter.MAX_LIMITER_CEILING_DBFS
import io.github.rsgarrido.sazanami.player.equalizer.limiter.MIN_LIMITER_CEILING_DBFS
import java.util.Locale
import kotlin.math.round

@Composable
internal fun EqualizerScreen(
    state: EqualizerScreenState,
    actions: EqualizerUiActions,
    modifier: Modifier = Modifier
) {
    val resources = LocalResources.current
    val modeLabel = stringResource(state.editablePreferences.mode.labelRes)
    val presetLabel = state.presetMatch.localizedLabel()
    val statusText = when {
        !state.editablePreferences.enabled -> stringResource(R.string.eq_status_off, modeLabel, presetLabel)
        state.editablePreferences.toDspConfiguration(enabledOverride = true).isEffectivelyFlat -> stringResource(R.string.eq_status_flat)
        state.comparisonBypassed -> stringResource(R.string.eq_status_bypass)
        else -> stringResource(R.string.eq_status_active, presetLabel)
    }
    if (state.importPreview != null) {
        EqualizerImportPreviewScreen(
            state = state,
            actions = actions,
            modifier = modifier
        )
        return
    }
    var presetSelectorVisible by remember {
        mutableStateOf(false)
    }
    var exportPresetSelectorVisible by remember {
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
    var limiterCeilingDialogVisible by remember {
        mutableStateOf(false)
    }
    var limiterCeilingDialogInitialValue by remember {
        mutableDoubleStateOf(-1.0)
    }
    val preferences = state.editablePreferences
    val activePreampDb = when (preferences.mode) {
        EqualizerMode.GRAPHIC -> preferences.preampDb
        EqualizerMode.PARAMETRIC ->
            preferences.parametricState.preampDb
    }
    val activeAutomaticHeadroom = when (preferences.mode) {
        EqualizerMode.GRAPHIC ->
            preferences.automaticHeadroomEnabled
        EqualizerMode.PARAMETRIC ->
            preferences.parametricState.automaticHeadroomEnabled
    }
    var latestPreampDragValue by remember(
        activePreampDb
    ) {
        mutableDoubleStateOf(activePreampDb)
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
                    contentDescription = stringResource(R.string.eq_back)
                )
            }
            Text(
                text = stringResource(R.string.eq_title),
                style = MaterialTheme.typography.titleLarge
            )
        }

        ListItem(
            headlineContent = { Text(stringResource(R.string.eq_title)) },
            supportingContent = {
                Text(statusText)
            },
            trailingContent = {
                Switch(
                    checked = preferences.enabled,
                    onCheckedChange = actions.onEnabledChanged,
                    modifier = Modifier.semantics {
                        contentDescription =
                            resources.getString(R.string.eq_enabled)
                    }
                )
            }
        )

        Text(
            text = stringResource(R.string.eq_mode_heading),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(
                start = 16.dp,
                top = 12.dp
            )
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics {
                    contentDescription = resources.getString(R.string.eq_mode_selector)
                }
        ) {
            EqualizerMode.entries.forEach { mode ->
                val modeName = stringResource(mode.labelRes)
                val modeDescription = stringResource(R.string.eq_mode_description, modeName)
                FilterChip(
                    selected = preferences.mode == mode,
                    onClick = { actions.onModeChanged(mode) },
                    label = { Text(modeName) },
                    modifier = Modifier.semantics {
                        contentDescription =
                            modeDescription
                    }
                )
            }
        }

        Text(
            text = stringResource(R.string.eq_import_export),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(
                start = 16.dp,
                top = 12.dp
            )
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp
            )
        ) {
            OutlinedButton(
                onClick = actions.onImportFromFile,
                modifier = Modifier.semantics {
                    contentDescription = resources.getString(R.string.eq_import_from_file_description)
                }
            ) {
                Text(stringResource(R.string.eq_import_from_file))
            }
            OutlinedButton(
                onClick = actions.onPasteEqText,
                modifier = Modifier.semantics {
                    contentDescription = resources.getString(R.string.eq_paste_text)
                }
            ) {
                Text(stringResource(R.string.eq_paste_text))
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            OutlinedButton(
                onClick = actions.onExportCurrentEqText,
                enabled = preferences.mode ==
                    EqualizerMode.PARAMETRIC,
                modifier = Modifier.semantics {
                    contentDescription =
                        resources.getString(R.string.eq_export_current_description)
                }
            ) {
                Text(stringResource(R.string.eq_export_current))
            }
            OutlinedButton(
                onClick = actions.onCopyCurrentEqText,
                enabled = preferences.mode ==
                    EqualizerMode.PARAMETRIC,
                modifier = Modifier.semantics {
                    contentDescription =
                        resources.getString(R.string.eq_copy_text_description)
                }
            ) {
                Text(stringResource(R.string.eq_copy_text))
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp
            )
        ) {
            OutlinedButton(
                onClick = actions.onExportCurrentNative,
                enabled = preferences.mode ==
                    EqualizerMode.PARAMETRIC,
                modifier = Modifier.semantics {
                    contentDescription =
                        resources.getString(R.string.eq_export_native_description)
                }
            ) {
                Text(stringResource(R.string.eq_export_native))
            }
            OutlinedButton(
                onClick = {
                    exportPresetSelectorVisible = true
                },
                enabled =
                    state.parametricUserPresets.isNotEmpty(),
                modifier = Modifier.semantics {
                    contentDescription =
                        resources.getString(R.string.eq_export_preset_description)
                }
            ) {
                Text(stringResource(R.string.eq_export_preset))
            }
        }
        Text(
            text = if (preferences.mode == EqualizerMode.GRAPHIC) {
                stringResource(R.string.eq_export_graphic_unavailable)
            } else {
                stringResource(R.string.eq_export_format_help)
            },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (state.importInProgress) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .semantics {
                        contentDescription =
                            resources.getString(R.string.eq_reading_import)
                    }
            )
        }
        state.importMessage?.let { message ->
            Text(
                message.resolve(),
                color = if (state.importMessageIsError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 4.dp
                )
            )
        }

        if (preferences.mode == EqualizerMode.GRAPHIC) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.eq_preset)) },
                supportingContent = { Text(presetLabel) },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.eq_choose_preset)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription =
                            resources.getString(R.string.eq_preset_description, presetLabel)
                    }
                    .padding(horizontal = 4.dp)
            )
            TextButton(
                onClick = { presetSelectorVisible = true },
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Text(stringResource(R.string.eq_manage_presets))
            }
        } else {
            ParametricEqualizerEditor(
                state = state,
                actions = actions
            )
        }

        EqualizerResponseGraph(
            analysis = state.analysis,
            filters = if (
                preferences.mode == EqualizerMode.PARAMETRIC
            ) {
                preferences.parametricState.filters
            } else {
                emptyList()
            },
            selectedFilterId = state.selectedParametricFilterId,
            ignoredFilterIndices = state.analysis.ignoredFilterIndices,
            onSelectFilter = actions.onSelectParametricFilter,
            onPreviewFilter = actions.onPreviewParametricFilter,
            onCommitFilter = actions.onCommitParametricFilter,
            modifier = Modifier.padding(16.dp)
        )

        EqualizerAnalysisStatus(state)

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Text(
            text = stringResource(R.string.eq_preamp),
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
                value = activePreampDb.toFloat(),
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
                            resources.getString(R.string.eq_preamp_description, formatEqualizerDb(activePreampDb))
                    }
            )
            TextButton(
                onClick = {
                    fineEditTarget = FineEditTarget(
                        title = resources.getString(R.string.eq_preamp),
                        initialValueDb =
                            activePreampDb,
                        minimumDb =
                            MIN_EQUALIZER_PREAMP_DB,
                        maximumDb =
                            MAX_EQUALIZER_PREAMP_DB,
                        bandIndex = null
                    )
                }
            ) {
                Text(formatEqualizerDb(activePreampDb))
            }
        }

        if (preferences.mode == EqualizerMode.GRAPHIC) {
            Text(
                text = stringResource(R.string.eq_graphic_bands),
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
        }

        ListItem(
            headlineContent = {
                Text(stringResource(R.string.eq_automatic_headroom))
            },
            supportingContent = {
                Text(
                    stringResource(R.string.eq_automatic_headroom_help)
                )
            },
            trailingContent = {
                Switch(
                    checked =
                        activeAutomaticHeadroom,
                    onCheckedChange =
                        actions.onAutomaticHeadroomChanged,
                    modifier = Modifier.semantics {
                        contentDescription =
                            resources.getString(R.string.eq_automatic_headroom_description)
                    }
                )
            }
        )
        if (
            !activeAutomaticHeadroom &&
            state.analysis.predictedMaximumDb > 0.0
        ) {
            Text(
                text = stringResource(R.string.eq_predicted_response_warning),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Text(
            text = stringResource(R.string.eq_limiter),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.eq_sample_peak_limiter)) },
            supportingContent = {
                Text(
                    stringResource(R.string.eq_sample_peak_limiter_help)
                )
            },
            trailingContent = {
                Switch(
                    checked = preferences.limiterEnabled,
                    onCheckedChange =
                        actions.onLimiterEnabledChanged,
                    modifier = Modifier.semantics {
                        contentDescription =
                            resources.getString(R.string.eq_sample_peak_limiter_enabled)
                    }
                )
            }
        )
        var latestLimiterCeiling by remember(
            preferences.limiterCeilingDbfs
        ) {
            mutableDoubleStateOf(
                preferences.limiterCeilingDbfs
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Slider(
                value =
                    preferences.limiterCeilingDbfs.toFloat(),
                onValueChange = { value ->
                    latestLimiterCeiling =
                        (round(value * 10.0) / 10.0)
                    actions.onPreviewLimiterCeiling(
                        latestLimiterCeiling
                    )
                },
                onValueChangeFinished = {
                    actions.onCommitLimiterCeiling(
                        latestLimiterCeiling
                    )
                },
                valueRange =
                    MIN_LIMITER_CEILING_DBFS.toFloat()..
                        MAX_LIMITER_CEILING_DBFS.toFloat(),
                steps = 29,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription =
                            resources.getString(R.string.eq_limiter_ceiling_description, formatLimiterDb(preferences.limiterCeilingDbfs))
                    }
            )
            TextButton(
                onClick = {
                    limiterCeilingDialogInitialValue =
                        preferences.limiterCeilingDbfs
                    limiterCeilingDialogVisible = true
                }
            ) {
                Text(
                    formatLimiterDb(
                        preferences.limiterCeilingDbfs
                    )
                )
            }
        }
        LimiterMeters(
            state = state,
            limiterEnabled = preferences.limiterEnabled,
            onReset = actions.onResetLimiterMeters
        )
        Text(
            text = stringResource(R.string.eq_limiter_lookahead),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = stringResource(R.string.eq_limiter_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                )
                .semantics {
                    contentDescription =
                        resources.getString(R.string.eq_limiter_disclaimer_description)
                }
        )
        if (
            !preferences.limiterEnabled &&
            state.runtimeState.saturatedSampleCount > 0L
        ) {
            Text(
                text = stringResource(R.string.eq_saturation_warning),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (state.comparisonAvailable) {
            Text(
                text = stringResource(R.string.eq_ab_comparison),
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
                        Text(stringResource(R.string.eq_ab_equalized))
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            actions
                                .onComparisonBypassedChanged(false)
                        }
                    ) {
                        Text(stringResource(R.string.eq_ab_equalized))
                    }
                }
                if (state.comparisonBypassed) {
                    Button(
                        onClick = {
                            actions
                                .onComparisonBypassedChanged(true)
                        }
                    ) {
                        Text(stringResource(R.string.eq_ab_bypass))
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            actions
                                .onComparisonBypassedChanged(true)
                        }
                    ) {
                        Text(stringResource(R.string.eq_ab_bypass))
                    }
                }
            }
            Text(
                text = stringResource(R.string.eq_ab_help),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else if (preferences.limiterEnabled) {
            Text(
                text = stringResource(R.string.eq_ab_disable_limiter),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
            )
        }

        FilledTonalButton(
            onClick = {
                if (state.isFlatBuiltInPreset) {
                    actions.onResetToFlat()
                } else {
                    resetConfirmationVisible = true
                }
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(stringResource(R.string.eq_reset_flat))
        }

        Card(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.eq_pcm_disclaimer),
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
    if (exportPresetSelectorVisible) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                exportPresetSelectorVisible = false
            },
            title = { Text(stringResource(R.string.eq_export_parametric_preset)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(
                        rememberScrollState()
                    )
                ) {
                    state.parametricUserPresets.forEach { preset ->
                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                preset.name,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    exportPresetSelectorVisible =
                                        false
                                    actions
                                        .onExportParametricPresetText(
                                            preset
                                        )
                                }
                            ) {
                                Text(stringResource(R.string.eq_text_format))
                            }
                            TextButton(
                                onClick = {
                                    exportPresetSelectorVisible =
                                        false
                                    actions
                                        .onExportParametricPresetNative(
                                            preset
                                        )
                                }
                            ) {
                                Text(stringResource(R.string.eq_native_format))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        exportPresetSelectorVisible = false
                    }
                ) {
                    Text(stringResource(R.string.eq_cancel))
                }
            }
        )
    }
    if (saveDialogVisible) {
        EqualizerPresetNameDialog(
            title = stringResource(R.string.eq_save_as_preset),
            initialName = "",
            userPresets = state.userPresets,
            confirmText = stringResource(R.string.eq_save),
            onDismiss = { saveDialogVisible = false },
            onConfirm = { name ->
                actions.onSaveUserPreset(name)
                saveDialogVisible = false
            }
        )
    }
    renamePreset?.let { preset ->
        EqualizerPresetNameDialog(
            title = stringResource(R.string.eq_rename_preset),
            initialName = preset.name,
            userPresets = state.userPresets,
            excludingPresetId = preset.id,
            confirmText = stringResource(R.string.eq_rename),
            onDismiss = { renamePreset = null },
            onConfirm = { name ->
                actions.onRenameUserPreset(preset.id, name)
                renamePreset = null
            }
        )
    }
    deletePreset?.let { preset ->
        ConfirmEqualizerActionDialog(
            title = stringResource(R.string.eq_delete_preset_confirm, preset.name),
            message = stringResource(R.string.eq_delete_preset_help),
            confirmText = stringResource(R.string.eq_delete),
            onDismiss = { deletePreset = null },
            onConfirm = {
                actions.onDeleteUserPreset(preset.id)
                deletePreset = null
            }
        )
    }
    if (resetConfirmationVisible) {
        ConfirmEqualizerActionDialog(
            title = stringResource(R.string.eq_reset_flat_confirm),
            message = if (
                preferences.mode == EqualizerMode.GRAPHIC
            ) {
                stringResource(R.string.eq_reset_graphic_help)
            } else {
                stringResource(R.string.eq_reset_parametric_help)
            },
            confirmText = stringResource(R.string.eq_reset),
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
    if (limiterCeilingDialogVisible) {
        EqualizerValueDialog(
            title = stringResource(R.string.eq_limiter_ceiling),
            initialValueDb =
                limiterCeilingDialogInitialValue,
            minimumDb = MIN_LIMITER_CEILING_DBFS,
            maximumDb = MAX_LIMITER_CEILING_DBFS,
            onPreview =
                actions.onPreviewLimiterCeiling,
            onCancel = {
                actions.onCancelLimiterCeilingPreview(
                    limiterCeilingDialogInitialValue
                )
                limiterCeilingDialogVisible = false
            },
            onApply = { value ->
                actions.onCommitLimiterCeiling(value)
                limiterCeilingDialogVisible = false
            }
        )
    }
}

@Composable
private fun LimiterMeters(
    state: EqualizerScreenState,
    limiterEnabled: Boolean,
    onReset: () -> Unit
) {
    val resources = LocalResources.current
    val runtime = state.runtimeState
    Column(
        modifier = Modifier.padding(
            horizontal = 16.dp,
            vertical = 8.dp
        )
    ) {
        LimiterMeter(
            label = stringResource(R.string.eq_pre_limiter_peak),
            valueDb = runtime.preLimiterPeakDbfs,
            progress = meterProgress(runtime.preLimiterPeakDbfs)
        )
        LimiterMeter(
            label = stringResource(R.string.eq_post_limiter_peak),
            valueDb = runtime.postLimiterPeakDbfs,
            progress = meterProgress(runtime.postLimiterPeakDbfs)
        )
        LimiterMeter(
            label = stringResource(R.string.eq_gain_reduction),
            valueDb = runtime.currentGainReductionDb,
            progress =
                (runtime.currentGainReductionDb / 12.0)
                    .toFloat()
                    .coerceIn(0f, 1f),
            positive = true
        )
        Text(
            stringResource(R.string.eq_recent_max_reduction, formatLimiterDb(runtime.maximumRecentGainReductionDb, positive = true))
        )
        Text(
            stringResource(R.string.eq_sample_counts, runtime.overRangeSampleCount, runtime.saturatedSampleCount),
            modifier = Modifier.semantics {
                contentDescription =
                    resources.getString(R.string.eq_sample_counts_description, runtime.overRangeSampleCount, runtime.saturatedSampleCount)
            }
        )
        Text(
            stringResource(R.string.eq_active_reduced_frames, runtime.limiterActiveFrameCount, runtime.limiterReducedFrameCount)
        )
        if (limiterEnabled) {
            Text(
                if (runtime.limiterPrimed) {
                    stringResource(R.string.eq_limiter_primed)
                } else {
                    stringResource(R.string.eq_limiter_priming)
                }
            )
        }
        TextButton(
            onClick = onReset,
            modifier = Modifier.semantics {
                contentDescription =
                    resources.getString(R.string.eq_reset_meters_description)
            }
        ) {
            Text(stringResource(R.string.eq_reset_meters))
        }
    }
}

@Composable
private fun LimiterMeter(
    label: String,
    valueDb: Double,
    progress: Float,
    positive: Boolean = false
) {
    val formattedValue = formatLimiterDb(valueDb, positive)
    val meterDescription = stringResource(R.string.eq_meter_description, label, formattedValue)
    Text(
        stringResource(R.string.eq_meter_value, label, formattedValue),
        style = MaterialTheme.typography.bodyMedium
    )
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    meterDescription
            }
    )
}

private fun meterProgress(valueDb: Double): Float =
    ((valueDb.coerceIn(-60.0, 0.0) + 60.0) / 60.0)
        .toFloat()

private fun formatLimiterDb(
    value: Double,
    positive: Boolean = false
): String = String.format(
    Locale.ROOT,
    if (positive) "%.1f dB" else "%.1f dBFS",
    value
)

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
                stringResource(R.string.eq_analysis),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                stringResource(R.string.eq_user_preamp, formatEqualizerDb(
                        when (
                            state.editablePreferences.mode
                        ) {
                            EqualizerMode.GRAPHIC ->
                                state.editablePreferences.preampDb
                            EqualizerMode.PARAMETRIC ->
                                state.editablePreferences
                                    .parametricState.preampDb
                        }
                    ))
            )
            Text(
                stringResource(R.string.eq_automatic_attenuation, formatEqualizerDb(
                        state.analysis.automaticHeadroom
                            .attenuationDb,
                        includePlus = false
                    ))
            )
            Text(
                stringResource(R.string.eq_effective_preamp, formatEqualizerDb(
                        state.analysis.automaticHeadroom
                            .effectivePreampDb
                    ))
            )
            Text(
                stringResource(R.string.eq_predicted_maximum, formatEqualizerDb(
                        state.analysis.predictedMaximumDb
                    ))
            )
            Text(
                stringResource(if (state.analysis.usesFallbackSampleRate) R.string.eq_sample_rate_fallback else R.string.eq_sample_rate, state.analysis.sampleRateHz)
            )
            if (state.analysis.ignoredBandIndices.isNotEmpty()) {
                val labels = state.analysis.ignoredBandIndices
                    .sorted()
                    .joinToString { index ->
                        when (state.editablePreferences.mode) {
                            EqualizerMode.GRAPHIC ->
                                formatEqualizerFrequency(
                                    GraphicEqualizerDefaults
                                        .frequenciesHz[index]
                                )
                            EqualizerMode.PARAMETRIC -> {
                                val filter =
                                    state.editablePreferences
                                        .parametricState.filters[index]
                                "${index + 1} " +
                                    formatEqualizerFrequency(
                                        filter.frequencyHz
                                    )
                            }
                        }
                    }
                Text(
                    text = stringResource(R.string.eq_unavailable_for_source, labels),
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
