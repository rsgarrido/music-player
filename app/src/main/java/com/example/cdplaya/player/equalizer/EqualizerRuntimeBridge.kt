package com.example.cdplaya.player.equalizer

import com.example.cdplaya.player.equalizer.dsp.EqualizerConfiguration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Lock-free handoff between configuration owners, background plan preparation,
 * and the service-owned audio processor.
 */
internal object EqualizerRuntimeBridge {
    private const val COORDINATOR_POLL_MILLIS = 20L

    private val versionCounter = AtomicLong(0L)
    private val requestedSnapshot =
        AtomicReference(EqualizerRuntimeSnapshot.DEFAULT)
    private val processorFormat =
        AtomicReference<EqualizerProcessorFormat?>(null)
    private val preparedPath =
        AtomicReference<PreparedEqualizerProcessingPath?>(null)
    private val latestRequestVersion = AtomicLong(0L)
    private val latestRequestNanos = AtomicLong(0L)
    private val latestPreparedVersion = AtomicLong(-1L)
    private val latestPreparedNanos = AtomicLong(0L)

    private val processorConfigured = AtomicBoolean(false)
    private val processorBypassed = AtomicBoolean(true)
    private val transitionInProgress = AtomicBoolean(false)
    private val comparisonSessionActive = AtomicBoolean(false)
    private val comparisonBypassed = AtomicBoolean(false)
    private val appliedPlan = AtomicReference<PreparedEqualizerPlan?>(null)
    private val latestAppliedVersion = AtomicLong(-1L)
    private val latestAppliedNanos = AtomicLong(0L)
    private val lastPlanApplicationMode =
        AtomicReference(EqualizerPlanApplicationMode.NONE)
    private val lastTransitionFrameCount = AtomicInteger(0)
    private val lastTransitionSampleRateHz = AtomicInteger(0)
    private val scratchBufferGrowthCount = AtomicInteger(0)

    private val _state = MutableStateFlow(EqualizerRuntimeState())
    val state: StateFlow<EqualizerRuntimeState> = _state.asStateFlow()

    private var coordinatorJob: Job? = null
    private var coordinatorStartCount = 0

    fun start(scope: CoroutineScope) {
        if (coordinatorJob != null) return
        coordinatorStartCount += 1
        coordinatorJob = scope.launch {
            runCoordinator()
        }
    }

    fun release() {
        coordinatorJob?.cancel()
        coordinatorJob = null
        requestedSnapshot.set(EqualizerRuntimeSnapshot.DEFAULT)
        versionCounter.set(0L)
        latestRequestVersion.set(0L)
        latestRequestNanos.set(0L)
        processorFormat.set(null)
        preparedPath.set(null)
        latestPreparedVersion.set(-1L)
        latestPreparedNanos.set(0L)
        comparisonSessionActive.set(false)
        comparisonBypassed.set(false)
        clearProcessorTelemetry()
        _state.value = EqualizerRuntimeState()
    }

    fun requestConfiguration(
        configuration: EqualizerConfiguration,
        automaticHeadroomEnabled: Boolean
    ): EqualizerRuntimeSnapshot {
        val version = versionCounter.incrementAndGet()
        latestRequestVersion.set(version)
        latestRequestNanos.set(System.nanoTime())
        val snapshot = EqualizerRuntimeSnapshot(
            version = version,
            configuration = configuration,
            automaticHeadroomEnabled = automaticHeadroomEnabled
        )
        requestedSnapshot.set(snapshot)
        publishState()
        return snapshot
    }

    fun setComparisonState(
        sessionActive: Boolean,
        bypassed: Boolean
    ) {
        comparisonSessionActive.set(sessionActive)
        comparisonBypassed.set(sessionActive && bypassed)
        publishState()
    }

    fun requestedSnapshot(): EqualizerRuntimeSnapshot {
        return requestedSnapshot.get()
    }

    fun publishProcessorFormat(format: EqualizerProcessorFormat?) {
        processorFormat.set(format)
        if (format == null) {
            preparedPath.set(null)
        }
    }

    fun latestCompatiblePath(
        format: EqualizerProcessorFormat
    ): PreparedEqualizerProcessingPath? {
        val path = preparedPath.get() ?: return null
        return path.takeIf { candidate ->
            candidate.plan.processorFormat == format
        }
    }

    fun publishProcessorConfigured(
        configured: Boolean,
        bypassed: Boolean
    ) {
        processorConfigured.set(configured)
        processorBypassed.set(bypassed)
    }

    fun publishAppliedPlan(
        plan: PreparedEqualizerPlan?,
        applicationMode: EqualizerPlanApplicationMode
    ) {
        val previousVersion = appliedPlan.get()?.sourceSnapshotVersion
        appliedPlan.set(plan)
        processorBypassed.set(plan?.bypassed ?: true)
        if (
            plan != null &&
            plan.sourceSnapshotVersion != previousVersion
        ) {
            latestAppliedVersion.set(plan.sourceSnapshotVersion)
            latestAppliedNanos.set(System.nanoTime())
            lastPlanApplicationMode.set(applicationMode)
            if (
                applicationMode !=
                EqualizerPlanApplicationMode.CROSSFADE
            ) {
                lastTransitionFrameCount.set(0)
                lastTransitionSampleRateHz.set(0)
            }
        }
    }

    fun publishTransitionStarted(
        totalFrameCount: Int,
        sampleRateHz: Int
    ) {
        lastTransitionFrameCount.set(totalFrameCount)
        lastTransitionSampleRateHz.set(sampleRateHz)
        transitionInProgress.set(true)
    }

    fun publishTransitionInProgress(inProgress: Boolean) {
        transitionInProgress.set(inProgress)
    }

    fun publishScratchBufferGrowthCount(growthCount: Int) {
        scratchBufferGrowthCount.set(growthCount)
    }

    fun clearProcessorTelemetry() {
        processorConfigured.set(false)
        processorBypassed.set(true)
        transitionInProgress.set(false)
        appliedPlan.set(null)
        latestAppliedVersion.set(-1L)
        latestAppliedNanos.set(0L)
        lastPlanApplicationMode.set(EqualizerPlanApplicationMode.NONE)
        lastTransitionFrameCount.set(0)
        lastTransitionSampleRateHz.set(0)
        scratchBufferGrowthCount.set(0)
    }

    internal fun coordinatorStartCount(): Int = coordinatorStartCount

    internal fun isCoordinatorRunning(): Boolean {
        return coordinatorJob?.isActive == true
    }

    internal fun publishStateForTest() {
        publishState()
    }

    internal fun installPreparedPathForTest(
        path: PreparedEqualizerProcessingPath
    ) {
        processorFormat.set(path.plan.processorFormat)
        preparedPath.set(path)
        publishState()
    }

    private suspend fun runCoordinator() {
        var preparedSnapshotVersion = Long.MIN_VALUE
        var preparedFormat: EqualizerProcessorFormat? = null

        while (currentCoroutineContext().isActive) {
            val snapshot = requestedSnapshot.get()
            val format = processorFormat.get()
            if (
                format != null &&
                (
                    snapshot.version != preparedSnapshotVersion ||
                        format != preparedFormat
                    )
            ) {
                val path = withContext(Dispatchers.Default) {
                    EqualizerPlanPreparer.prepare(
                        snapshot = snapshot,
                        processorFormat = format
                    ).createProcessingPath()
                }
                if (
                    requestedSnapshot.get() === snapshot &&
                    processorFormat.get() == format
                ) {
                    latestPreparedVersion.set(snapshot.version)
                    latestPreparedNanos.set(System.nanoTime())
                    preparedPath.set(path)
                    preparedSnapshotVersion = snapshot.version
                    preparedFormat = format
                }
            }
            publishState()
            delay(COORDINATOR_POLL_MILLIS)
        }
    }

    private fun publishState() {
        val snapshot = requestedSnapshot.get()
        val format = processorFormat.get()
        val latestPlan = preparedPath.get()?.plan
        val applied = appliedPlan.get()
        val requestVersion = latestRequestVersion.get()
        val requestNanos = latestRequestNanos.get()
        val transitionFrameCount = lastTransitionFrameCount.get()
        val transitionSampleRateHz =
            lastTransitionSampleRateHz.get()
        val latestMatchesRequest =
            latestPlan?.sourceSnapshotVersion == snapshot.version &&
                latestPlan.processorFormat == format
        val plannedActive =
            latestMatchesRequest && latestPlan?.bypassed == false
        val awaitingActivePlan =
            !latestMatchesRequest &&
                snapshot.configuration.enabled &&
                !snapshot.configuration.isEffectivelyFlat
        val requiresDecodedPcm =
            plannedActive || awaitingActivePlan ||
                applied?.bypassed == false ||
                comparisonSessionActive.get()
        val diagnosticPlan = if (latestMatchesRequest) {
            latestPlan
        } else {
            applied
        }
        val nextState = EqualizerRuntimeState(
            processorConfigured = processorConfigured.get(),
            requestedEnabled = snapshot.configuration.enabled,
            effectivelyActive = requiresDecodedPcm,
            bypassed = processorBypassed.get() && !requiresDecodedPcm,
            transitionInProgress = transitionInProgress.get(),
            comparisonSessionActive =
                comparisonSessionActive.get(),
            comparisonBypassed = comparisonBypassed.get(),
            configurationVersion = snapshot.version,
            preparedPlanVersion = latestPlan?.sourceSnapshotVersion,
            appliedPlanVersion = applied?.sourceSnapshotVersion,
            planPreparationLatencyMillis = matchingLatencyMillis(
                snapshotVersion = snapshot.version,
                requestVersion = requestVersion,
                requestNanos = requestNanos,
                eventVersion = latestPreparedVersion.get(),
                eventNanos = latestPreparedNanos.get()
            ),
            planApplicationLatencyMillis = matchingLatencyMillis(
                snapshotVersion = snapshot.version,
                requestVersion = requestVersion,
                requestNanos = requestNanos,
                eventVersion = latestAppliedVersion.get(),
                eventNanos = latestAppliedNanos.get()
            ),
            lastPlanApplicationMode =
                lastPlanApplicationMode.get(),
            lastTransitionFrameCount =
                transitionFrameCount,
            lastTransitionDurationMillis =
                transitionDurationMillis(
                    frameCount = transitionFrameCount,
                    sampleRateHz = transitionSampleRateHz
                ),
            sampleRateHz = format?.sampleRateHz,
            channelCount = format?.channelCount,
            validFilterCount = diagnosticPlan?.validFilterCount ?: 0,
            ignoredFilterCount =
                diagnosticPlan?.ignoredFilters?.size ?: 0,
            automaticHeadroomDb =
                diagnosticPlan
                    ?.automaticHeadroomResult
                    ?.attenuationDb
                    ?: 0.0,
            requiresDecodedPcm = requiresDecodedPcm,
            scratchBufferGrowthCount = scratchBufferGrowthCount.get()
        )
        if (_state.value != nextState) {
            _state.value = nextState
        }
    }

    private fun matchingLatencyMillis(
        snapshotVersion: Long,
        requestVersion: Long,
        requestNanos: Long,
        eventVersion: Long,
        eventNanos: Long
    ): Long? {
        if (
            snapshotVersion != requestVersion ||
            snapshotVersion != eventVersion ||
            requestNanos <= 0L ||
            eventNanos < requestNanos
        ) {
            return null
        }
        return (eventNanos - requestNanos) / NANOS_PER_MILLISECOND
    }

    private fun transitionDurationMillis(
        frameCount: Int,
        sampleRateHz: Int
    ): Double {
        if (frameCount <= 0 || sampleRateHz <= 0) return 0.0
        return frameCount * 1_000.0 / sampleRateHz
    }

    private const val NANOS_PER_MILLISECOND = 1_000_000L
}
