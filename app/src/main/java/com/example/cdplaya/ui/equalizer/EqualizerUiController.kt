package com.example.cdplaya.ui.equalizer

import com.example.cdplaya.data.preferences.AppPreferencesRepository
import com.example.cdplaya.player.equalizer.EqualizerMode
import com.example.cdplaya.player.equalizer.EqualizerPreferencesState
import com.example.cdplaya.player.equalizer.EqualizerRuntimeBridge
import com.example.cdplaya.player.equalizer.EqualizerRuntimeState
import com.example.cdplaya.player.equalizer.UserEqualizerPreset
import com.example.cdplaya.player.equalizer.applyPreset
import com.example.cdplaya.player.equalizer.activeAutomaticHeadroomEnabled
import com.example.cdplaya.player.equalizer.toDspConfiguration
import com.example.cdplaya.player.equalizer.limiter.LimiterConfiguration
import com.example.cdplaya.player.equalizer.parametric.MAX_PARAMETRIC_FILTER_COUNT
import com.example.cdplaya.player.equalizer.parametric.ParametricFilter
import com.example.cdplaya.player.equalizer.parametric.ParametricFilterFactory
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
                        _state.value.editablePreferences
                            .withDurablePresetLists(durable)
                    } else if (pendingCommit != null) {
                        pendingCommit!!
                            .withDurablePresetLists(durable)
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

    fun setMode(mode: EqualizerMode) {
        val current = _state.value
        if (current.editablePreferences.mode == mode) return
        val updated = current.editablePreferences.withMode(mode)
        updatePreview(updated, markDirty = false)
        pendingCommit = updated
        val selectedId = if (mode == EqualizerMode.PARAMETRIC) {
            updated.parametricState.filters.firstOrNull()?.id
        } else {
            null
        }
        _state.value = _state.value.copy(
            selectedParametricFilterId = selectedId,
            comparisonBypassed = false
        )
        scope.launch {
            preferencesRepository.setEqualizerMode(mode)
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
        val preferences = _state.value.editablePreferences
        val updated = when (preferences.mode) {
            EqualizerMode.GRAPHIC ->
                preferences.withPreampDb(preampDb)
            EqualizerMode.PARAMETRIC ->
                preferences.withParametricState(
                    preferences.parametricState
                        .withPreampDb(preampDb)
                )
        }
        updatePreview(updated)
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
        val preferences = _state.value.editablePreferences
        cancelPreview(
            when (preferences.mode) {
                EqualizerMode.GRAPHIC ->
                    preferences.withPreampDb(preampDb)
                EqualizerMode.PARAMETRIC ->
                    preferences.withParametricState(
                        preferences.parametricState
                            .withPreampDb(preampDb)
                    )
            }
        )
    }

    fun setAutomaticHeadroomEnabled(enabled: Boolean) {
        val preferences = _state.value.editablePreferences
        updatePreview(
            when (preferences.mode) {
                EqualizerMode.GRAPHIC ->
                    preferences.withAutomaticHeadroomEnabled(enabled)
                EqualizerMode.PARAMETRIC ->
                    preferences.withParametricState(
                        preferences.parametricState
                            .withAutomaticHeadroomEnabled(enabled)
                    )
            }
        )
        commitEditablePreferences()
    }

    fun setLimiterEnabled(enabled: Boolean) {
        if (enabled) {
            EqualizerRuntimeBridge.setComparisonState(
                sessionActive = false,
                bypassed = false
            )
        }
        val updated = _state.value.editablePreferences
            .withLimiterEnabled(enabled)
        updatePreview(updated, markDirty = false)
        pendingCommit = updated
        scope.launch {
            preferencesRepository.setLimiterEnabled(enabled)
        }
    }

    fun previewLimiterCeiling(ceilingDbfs: Double) {
        updatePreview(
            _state.value.editablePreferences
                .withLimiterCeilingDbfs(ceilingDbfs)
        )
    }

    fun commitLimiterCeiling(ceilingDbfs: Double) {
        previewLimiterCeiling(ceilingDbfs)
        commitEditablePreferences()
    }

    fun cancelLimiterCeilingPreview(ceilingDbfs: Double) {
        cancelPreview(
            _state.value.editablePreferences
                .withLimiterCeilingDbfs(ceilingDbfs)
        )
    }

    fun resetLimiterMeters() {
        EqualizerRuntimeBridge.requestLimiterMeterReset()
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

    fun selectParametricFilter(filterId: String?) {
        if (filterId != null) {
            require(
                _state.value.editablePreferences
                    .parametricState.filters.any { filter ->
                        filter.id == filterId
                    }
            ) {
                "Unknown parametric filter ID: $filterId"
            }
        }
        _state.value = _state.value.copy(
            selectedParametricFilterId = filterId
        )
    }

    fun addParametricFilter() {
        val preferences = _state.value.editablePreferences
        val parametric = preferences.parametricState
        require(
            parametric.filters.size < MAX_PARAMETRIC_FILTER_COUNT
        ) {
            "Parametric filter limit reached"
        }
        val filter = ParametricFilterFactory.default()
        val updated = preferences.withParametricState(
            parametric.addFilter(filter)
        )
        updatePreview(updated)
        _state.value = _state.value.copy(
            selectedParametricFilterId = filter.id
        )
        commitEditablePreferences()
    }

    fun previewParametricFilter(filter: ParametricFilter) {
        val preferences = _state.value.editablePreferences
        updatePreview(
            preferences.withParametricState(
                preferences.parametricState.withFilter(filter)
            )
        )
        _state.value = _state.value.copy(
            selectedParametricFilterId = filter.id
        )
    }

    fun commitParametricFilter(filter: ParametricFilter) {
        previewParametricFilter(filter)
        commitEditablePreferences()
    }

    fun cancelParametricFilterPreview(
        original: ParametricFilter
    ) {
        val preferences = _state.value.editablePreferences
        cancelPreview(
            preferences.withParametricState(
                preferences.parametricState.withFilter(original)
            )
        )
        _state.value = _state.value.copy(
            selectedParametricFilterId = original.id
        )
    }

    fun moveParametricFilter(
        filterId: String,
        destinationIndex: Int
    ) {
        val preferences = _state.value.editablePreferences
        updatePreview(
            preferences.withParametricState(
                preferences.parametricState.moveFilter(
                    filterId,
                    destinationIndex
                )
            )
        )
        commitEditablePreferences()
    }

    fun deleteParametricFilter(filterId: String) {
        val preferences = _state.value.editablePreferences
        val filters = preferences.parametricState.filters
        val removedIndex =
            filters.indexOfFirst { filter -> filter.id == filterId }
        require(removedIndex >= 0) {
            "Unknown parametric filter ID: $filterId"
        }
        val updatedParametric =
            preferences.parametricState.removeFilter(filterId)
        updatePreview(
            preferences.withParametricState(updatedParametric)
        )
        val nextSelection = updatedParametric.filters
            .getOrNull(
                removedIndex.coerceAtMost(
                    updatedParametric.filters.lastIndex
                )
            )
            ?.id
        _state.value = _state.value.copy(
            selectedParametricFilterId = nextSelection
        )
        commitEditablePreferences()
    }

    fun applyParametricFlatPreset() {
        val preferences = _state.value.editablePreferences
        updatePreview(
            preferences.withParametricState(
                preferences.parametricState.flatCurve()
            )
        )
        _state.value = _state.value.copy(
            selectedParametricFilterId = null
        )
        commitEditablePreferences()
    }

    fun applyParametricUserPreset(presetId: String) {
        val preferences = _state.value.editablePreferences
        val parametric = preferences.parametricState
        val preset = parametric.userPresets.first { candidate ->
            candidate.id == presetId
        }
        val updated = parametric.applyPreset(preset)
        updatePreview(
            preferences.withParametricState(updated)
        )
        _state.value = _state.value.copy(
            selectedParametricFilterId =
                updated.filters.firstOrNull()?.id
        )
        commitEditablePreferences()
    }

    fun saveParametricUserPreset(name: String) {
        val settled = _state.value.editablePreferences
        beginPendingCommit(settled)
        scope.launch {
            preferencesRepository.saveParametricEqualizerPreset(
                name = name,
                curve = settled.parametricState
            )
        }
    }

    fun renameParametricUserPreset(
        presetId: String,
        name: String
    ) {
        scope.launch {
            preferencesRepository.renameParametricEqualizerPreset(
                presetId,
                name
            )
        }
    }

    fun deleteParametricUserPreset(presetId: String) {
        scope.launch {
            preferencesRepository.deleteParametricEqualizerPreset(
                presetId
            )
        }
    }

    fun resetToFlat() {
        val preferences = _state.value.editablePreferences
        updatePreview(
            when (preferences.mode) {
                EqualizerMode.GRAPHIC -> preferences.flatCurve()
                EqualizerMode.PARAMETRIC ->
                    preferences.withParametricState(
                        preferences.parametricState.flatCurve()
                    )
            }
        )
        if (preferences.mode == EqualizerMode.PARAMETRIC) {
            _state.value = _state.value.copy(
                selectedParametricFilterId = null
            )
        }
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
            preferencesRepository.replaceEqualizerPreferences(
                settled
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
                preferences.activeAutomaticHeadroomEnabled,
            mode = preferences.mode,
            limiterConfiguration = LimiterConfiguration(
                enabled = preferences.limiterEnabled,
                ceilingDbfs = preferences.limiterCeilingDbfs
            )
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
            bandGainsDb == other.bandGainsDb &&
        mode == other.mode &&
        parametricState == other.parametricState &&
        limiterEnabled == other.limiterEnabled &&
        limiterCeilingDbfs.toBits() ==
            other.limiterCeilingDbfs.toBits()
}

private fun EqualizerPreferencesState.withDurablePresetLists(
    durable: EqualizerPreferencesState
): EqualizerPreferencesState = copy(
    userPresets = durable.userPresets,
    parametricState = parametricState.copy(
        userPresets = durable.parametricState.userPresets
    )
)
