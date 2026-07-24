package com.example.cdplaya.ui.equalizer

import com.example.cdplaya.data.preferences.AppPreferencesRepository
import com.example.cdplaya.player.equalizer.EqualizerPreferencesState
import com.example.cdplaya.player.equalizer.EqualizerRuntimeBridge
import com.example.cdplaya.player.equalizer.EqualizerRuntimeState
import com.example.cdplaya.player.equalizer.UserEqualizerPreset
import com.example.cdplaya.player.equalizer.applyPreset
import com.example.cdplaya.player.equalizer.toDspConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

internal class EqualizerUiController(
    private val preferencesRepository:
        AppPreferencesRepository,
    private val runtimeState: StateFlow<EqualizerRuntimeState>,
    private val scope: CoroutineScope
) {
    private val analysisController =
        EqualizerAnalysisController(scope)
    private val _state =
        MutableStateFlow(EqualizerScreenState())
    val state: StateFlow<EqualizerScreenState> =
        _state.asStateFlow()

    private var stateCollectionJob: Job? = null
    private var hasPreviewEdits = false
    private var pendingCommit:
        EqualizerPreferencesState? = null

    init {
        stateCollectionJob = scope.launch {
            launch {
                combine(
                    preferencesRepository.state.filter {
                            preferences ->
                        preferences.isLoaded
                    },
                    runtimeState
                ) { appPreferences, runtime ->
                    appPreferences to runtime
                }.collectLatest {
                        (appPreferences, runtime) ->
                    val durable =
                        appPreferences.equalizerPreferences
                    if (
                        pendingCommit?.hasSameConfigurationAs(
                            durable
                        ) == true
                    ) {
                        pendingCommit = null
                    }
                    val editable = if (hasPreviewEdits) {
                        _state.value.editablePreferences.copy(
                            userPresets = durable.userPresets
                        )
                    } else if (pendingCommit != null) {
                        pendingCommit!!.copy(
                            userPresets = durable.userPresets
                        )
                    } else {
                        durable
                    }
                    _state.value = _state.value.copy(
                        durablePreferences = durable,
                        editablePreferences = editable,
                        presetMatch = presetMatchFor(editable),
                        runtimeState = runtime,
                        isLoaded = true
                    )
                    analysisController.submit(
                        preferences = editable,
                        currentSampleRateHz =
                            runtime.sampleRateHz
                    )
                }
            }
            launch {
                analysisController.state.collectLatest { analysis ->
                    _state.value = _state.value.copy(
                        analysis = analysis
                    )
                }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        val updated = _state.value.editablePreferences
            .withEnabled(enabled)
        updatePreview(
            updated,
            markDirty = false
        )
        if (!hasPreviewEdits) {
            pendingCommit = updated
        }
        scope.launch {
            preferencesRepository.setEqualizerEnabled(enabled)
        }
    }

    fun previewBandGain(
        index: Int,
        gainDb: Double
    ) {
        updatePreview(
            _state.value.editablePreferences
                .withBandGainDb(index, gainDb)
        )
    }

    fun commitBandGain(
        index: Int,
        gainDb: Double
    ) {
        previewBandGain(index, gainDb)
        commitEditablePreferences()
    }

    fun previewPreamp(preampDb: Double) {
        updatePreview(
            _state.value.editablePreferences
                .withPreampDb(preampDb)
        )
    }

    fun commitPreamp(preampDb: Double) {
        previewPreamp(preampDb)
        commitEditablePreferences()
    }

    fun cancelBandGainPreview(
        index: Int,
        gainDb: Double
    ) {
        cancelPreview(
            _state.value.editablePreferences
                .withBandGainDb(index, gainDb)
        )
    }

    fun cancelPreampPreview(preampDb: Double) {
        cancelPreview(
            _state.value.editablePreferences
                .withPreampDb(preampDb)
        )
    }

    fun setAutomaticHeadroomEnabled(enabled: Boolean) {
        updatePreview(
            _state.value.editablePreferences
                .withAutomaticHeadroomEnabled(enabled)
        )
        commitEditablePreferences()
    }

    fun applyBuiltInPreset(index: Int) {
        val preset = builtInEqualizerPresets[index]
        updatePreview(
            _state.value.editablePreferences
                .applyPreset(preset)
        )
        commitEditablePreferences()
    }

    fun applyUserPreset(presetId: String) {
        val preset = _state.value.userPresets
            .first { candidate -> candidate.id == presetId }
        updatePreview(
            _state.value.editablePreferences
                .applyPreset(preset)
        )
        commitEditablePreferences()
    }

    fun saveUserPreset(name: String) {
        val settled = _state.value.editablePreferences
        beginPendingCommit(settled)
        scope.launch {
            preferencesRepository.saveUserEqualizerPreset(
                name = name,
                curve = settled
            )
        }
    }

    fun renameUserPreset(
        presetId: String,
        name: String
    ) {
        scope.launch {
            preferencesRepository.renameUserEqualizerPreset(
                presetId,
                name
            )
        }
    }

    fun deleteUserPreset(presetId: String) {
        scope.launch {
            preferencesRepository.deleteUserEqualizerPreset(
                presetId
            )
        }
    }

    fun resetToFlat() {
        updatePreview(
            _state.value.editablePreferences.flatCurve()
        )
        commitEditablePreferences()
    }

    fun setComparisonBypassed(bypassed: Boolean) {
        val current = _state.value
        if (!current.comparisonAvailable) return
        EqualizerRuntimeBridge.setComparisonState(
            sessionActive = true,
            bypassed = bypassed
        )
        requestRuntime(
            preferences = current.editablePreferences,
            enabledOverride = if (bypassed) {
                false
            } else {
                current.editablePreferences.enabled
            }
        )
        _state.value = current.copy(
            comparisonBypassed = bypassed
        )
    }

    fun closeScreen() {
        if (hasPreviewEdits) {
            commitEditablePreferences()
        }
        requestRuntime(_state.value.editablePreferences)
        EqualizerRuntimeBridge.setComparisonState(
            sessionActive = false,
            bypassed = false
        )
        _state.value = _state.value.copy(
            comparisonBypassed = false
        )
    }

    fun release() {
        closeScreen()
        stateCollectionJob?.cancel()
        stateCollectionJob = null
        analysisController.release()
    }

    private fun updatePreview(
        updated: EqualizerPreferencesState,
        markDirty: Boolean = true
    ) {
        hasPreviewEdits = hasPreviewEdits || markDirty
        _state.value = _state.value.copy(
            editablePreferences = updated,
            presetMatch = presetMatchFor(updated),
            comparisonBypassed = false,
            hasUncommittedPreview = hasPreviewEdits
        )
        requestRuntime(updated)
        EqualizerRuntimeBridge.setComparisonState(
            sessionActive = false,
            bypassed = false
        )
        analysisController.submit(
            preferences = updated,
            currentSampleRateHz =
                runtimeState.value.sampleRateHz
        )
    }

    private fun commitEditablePreferences() {
        val settled = _state.value.editablePreferences
        beginPendingCommit(settled)
        scope.launch {
            preferencesRepository.replaceEqualizerCurve(
                preampDb = settled.preampDb,
                automaticHeadroomEnabled =
                    settled.automaticHeadroomEnabled,
                bandGainsDb = settled.bandGainsDb
            )
        }
    }

    private fun beginPendingCommit(
        settled: EqualizerPreferencesState
    ) {
        hasPreviewEdits = false
        pendingCommit = settled
        _state.value = _state.value.copy(
            hasUncommittedPreview = false
        )
        requestRuntime(settled)
    }

    private fun cancelPreview(
        restored: EqualizerPreferencesState
    ) {
        hasPreviewEdits = false
        _state.value = _state.value.copy(
            editablePreferences = restored,
            presetMatch = presetMatchFor(restored),
            hasUncommittedPreview = false
        )
        requestRuntime(restored)
        analysisController.submit(
            preferences = restored,
            currentSampleRateHz =
                runtimeState.value.sampleRateHz
        )
    }

    private fun requestRuntime(
        preferences: EqualizerPreferencesState,
        enabledOverride: Boolean = preferences.enabled
    ) {
        EqualizerRuntimeBridge.requestConfiguration(
            configuration = preferences.toDspConfiguration(
                enabledOverride = enabledOverride
            ),
            automaticHeadroomEnabled =
                preferences.automaticHeadroomEnabled
        )
    }
}

private fun EqualizerPreferencesState
    .hasSameConfigurationAs(
        other: EqualizerPreferencesState
    ): Boolean {
    return enabled == other.enabled &&
        preampDb.toBits() == other.preampDb.toBits() &&
        automaticHeadroomEnabled ==
            other.automaticHeadroomEnabled &&
        bandGainsDb == other.bandGainsDb
}
