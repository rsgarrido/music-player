package com.example.cdplaya.player.equalizer

import com.example.cdplaya.player.equalizer.dsp.EqualizerConfiguration
import com.example.cdplaya.player.equalizer.limiter.LIMITER_RELEASE_MILLISECONDS
import com.example.cdplaya.player.equalizer.limiter.LimiterConfiguration
import com.example.cdplaya.player.equalizer.limiter.LimiterMath
import com.example.cdplaya.player.equalizer.limiter.LimiterMeterSnapshot
import com.example.cdplaya.player.equalizer.limiter.LimiterPreparedConfiguration
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
    private val preparedLimiterConfiguration =
        AtomicReference<LimiterPreparedConfiguration?>(null)
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
    private val limiterEffectivelyActive = AtomicBoolean(false)
    private val limiterPrimed = AtomicBoolean(false)
    private val limiterReprimeCount = AtomicInteger(0)
    private val limiterMeterResetVersion = AtomicLong(0L)
    private val limiterMeter =
        AtomicReference(LimiterMeterSnapshot())
    private val preLimiterPeakHold =
        AtomicReference(LimiterPeakHold())
    private val postLimiterPeakHold =
        AtomicReference(LimiterPeakHold())
    private val limiterMaximumHold =
        AtomicReference(LimiterMaximumHold())

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
        preparedLimiterConfiguration.set(null)
        latestPreparedVersion.set(-1L)
        latestPreparedNanos.set(0L)
        comparisonSessionActive.set(false)
        comparisonBypassed.set(false)
        limiterMeterResetVersion.set(0L)
        clearProcessorTelemetry()
        _state.value = EqualizerRuntimeState()
    }

    fun requestConfiguration(
        configuration: EqualizerConfiguration,
        automaticHeadroomEnabled: Boolean,
        limiterConfiguration: LimiterConfiguration =
            LimiterConfiguration()
    ): EqualizerRuntimeSnapshot {
        val version = versionCounter.incrementAndGet()
        latestRequestVersion.set(version)
        latestRequestNanos.set(System.nanoTime())
        val snapshot = EqualizerRuntimeSnapshot(
            version = version,
            configuration = configuration,
            automaticHeadroomEnabled = automaticHeadroomEnabled,
            limiterConfiguration = limiterConfiguration
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
            preparedLimiterConfiguration.set(null)
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

    fun latestCompatibleLimiterConfiguration(
        format: EqualizerProcessorFormat
    ): LimiterPreparedConfiguration? {
        val configuration =
            preparedLimiterConfiguration.get() ?: return null
        return configuration.takeIf { candidate ->
            candidate.sampleRateHz == format.sampleRateHz &&
                candidate.channelCount == format.channelCount
        }
    }

    fun isLimiterPreparationPending(
        format: EqualizerProcessorFormat
    ): Boolean {
        val snapshot = requestedSnapshot.get()
        if (!snapshot.limiterConfiguration.enabled) return false
        val prepared = preparedLimiterConfiguration.get()
        return prepared == null ||
            prepared.configurationVersion != snapshot.version ||
            prepared.sampleRateHz != format.sampleRateHz ||
            prepared.channelCount != format.channelCount
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

    fun publishLimiterProcessorState(
        effectivelyActive: Boolean,
        primed: Boolean,
        reprimeCount: Int
    ) {
        limiterEffectivelyActive.set(effectivelyActive)
        limiterPrimed.set(primed)
        limiterReprimeCount.set(reprimeCount)
    }

    fun publishLimiterMeterSnapshot(snapshot: LimiterMeterSnapshot) {
        limiterMeter.set(snapshot)
        val now = System.nanoTime()
        updatePeakHold(
            holder = preLimiterPeakHold,
            newPeakDbfs = snapshot.preLimiterPeakDbfs,
            nowNanos = now
        )
        updatePeakHold(
            holder = postLimiterPeakHold,
            newPeakDbfs = snapshot.postLimiterPeakDbfs,
            nowNanos = now
        )
        if (snapshot.maximumGainReductionDb > 0.0) {
            while (true) {
                val previous = limiterMaximumHold.get()
                val replacement = if (
                    now - previous.timestampNanos >
                    MAXIMUM_HOLD_NANOS ||
                    snapshot.maximumGainReductionDb >= previous.reductionDb
                ) {
                    LimiterMaximumHold(
                        reductionDb =
                            snapshot.maximumGainReductionDb,
                        timestampNanos = now
                    )
                } else {
                    previous
                }
                if (
                    replacement === previous ||
                    limiterMaximumHold.compareAndSet(
                        previous,
                        replacement
                    )
                ) {
                    break
                }
            }
        }
    }

    fun requestLimiterMeterReset() {
        limiterMeterResetVersion.incrementAndGet()
        limiterMeter.set(LimiterMeterSnapshot())
        preLimiterPeakHold.set(LimiterPeakHold())
        postLimiterPeakHold.set(LimiterPeakHold())
        limiterMaximumHold.set(LimiterMaximumHold())
        publishState()
    }

    fun limiterMeterResetVersion(): Long =
        limiterMeterResetVersion.get()

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
        limiterEffectivelyActive.set(false)
        limiterPrimed.set(false)
        limiterReprimeCount.set(0)
        limiterMeter.set(LimiterMeterSnapshot())
        preLimiterPeakHold.set(LimiterPeakHold())
        postLimiterPeakHold.set(LimiterPeakHold())
        limiterMaximumHold.set(LimiterMaximumHold())
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
        preparedLimiterConfiguration.set(
            LimiterPreparedConfiguration.prepare(
                configuration =
                    requestedSnapshot.get().limiterConfiguration,
                sampleRateHz =
                    path.plan.processorFormat.sampleRateHz,
                channelCount =
                    path.plan.processorFormat.channelCount,
                configurationVersion =
                    path.plan.sourceSnapshotVersion
            )
        )
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
                    val preparedPath = EqualizerPlanPreparer.prepare(
                        snapshot = snapshot,
                        processorFormat = format
                    ).createProcessingPath()
                    val limiter =
                        LimiterPreparedConfiguration.prepare(
                            configuration =
                                snapshot.limiterConfiguration,
                            sampleRateHz = format.sampleRateHz,
                            channelCount = format.channelCount,
                            configurationVersion =
                                snapshot.version
                        )
                    preparedPath to limiter
                }
                if (
                    requestedSnapshot.get() === snapshot &&
                    processorFormat.get() == format
                ) {
                    latestPreparedVersion.set(snapshot.version)
                    latestPreparedNanos.set(System.nanoTime())
                    preparedPath.set(path.first)
                    preparedLimiterConfiguration.set(path.second)
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
        val preparedLimiter =
            preparedLimiterConfiguration.get()
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
                comparisonSessionActive.get() ||
                snapshot.limiterConfiguration.enabled ||
                limiterEffectivelyActive.get()
        val diagnosticPlan = if (latestMatchesRequest) {
            latestPlan
        } else {
            applied
        }
        val limiterIsActive = limiterEffectivelyActive.get()
        val displayedPreLimiterPeakDbfs = decayedPeakDbfs(
            preLimiterPeakHold.get()
        )
        val displayedPostLimiterPeakDbfs = if (limiterIsActive) {
            decayedPeakDbfs(postLimiterPeakHold.get())
        } else {
            // In bypass both labels describe the same signal point. Reusing one
            // visual hold prevents independent decay histories from implying
            // gain or attenuation that the processor did not apply.
            displayedPreLimiterPeakDbfs
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
            scratchBufferGrowthCount = scratchBufferGrowthCount.get(),
            limiterRequestedEnabled =
                snapshot.limiterConfiguration.enabled,
            limiterEffectivelyActive = limiterIsActive,
            limiterCeilingDbfs =
                snapshot.limiterConfiguration.ceilingDbfs,
            limiterLookaheadFrames =
                preparedLimiter?.lookaheadFrames ?: 0,
            limiterLookaheadMilliseconds =
                preparedLimiter?.let { limiter ->
                    limiter.lookaheadFrames * 1_000.0 /
                        limiter.sampleRateHz
                } ?: 0.0,
            limiterReleaseMilliseconds =
                LIMITER_RELEASE_MILLISECONDS,
            limiterPrimed = limiterPrimed.get(),
            preLimiterPeakDbfs = displayedPreLimiterPeakDbfs,
            postLimiterPeakDbfs = displayedPostLimiterPeakDbfs,
            currentGainReductionDb =
                limiterMeter.get().currentGainReductionDb,
            maximumRecentGainReductionDb =
                recentMaximumGainReductionDb(),
            overRangeSampleCount =
                limiterMeter.get().overRangeSampleCount,
            saturatedSampleCount =
                limiterMeter.get().saturatedSampleCount,
            limiterActiveFrameCount =
                limiterMeter.get().limiterActiveFrameCount,
            limiterReducedFrameCount =
                limiterMeter.get().limiterReducedFrameCount,
            limiterReprimeCount = limiterReprimeCount.get()
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
    private const val MAXIMUM_HOLD_NANOS =
        1_500L * NANOS_PER_MILLISECOND
    private const val METER_DECAY_DB_PER_SECOND = 18.0

    private fun decayedPeakDbfs(hold: LimiterPeakHold): Double {
        if (hold.timestampNanos <= 0L) {
            return LimiterMath.SILENCE_FLOOR_DBFS
        }
        val elapsedSeconds =
            (System.nanoTime() - hold.timestampNanos)
                .coerceAtLeast(0L) / 1_000_000_000.0
        return (
            hold.peakDbfs -
                elapsedSeconds * METER_DECAY_DB_PER_SECOND
            ).coerceAtLeast(LimiterMath.SILENCE_FLOOR_DBFS)
    }

    private fun updatePeakHold(
        holder: AtomicReference<LimiterPeakHold>,
        newPeakDbfs: Double,
        nowNanos: Long
    ) {
        while (true) {
            val previous = holder.get()
            val decayedPrevious = if (
                previous.timestampNanos <= 0L
            ) {
                LimiterMath.SILENCE_FLOOR_DBFS
            } else {
                (
                    previous.peakDbfs -
                        (
                            nowNanos - previous.timestampNanos
                            ).coerceAtLeast(0L) /
                        1_000_000_000.0 *
                        METER_DECAY_DB_PER_SECOND
                    ).coerceAtLeast(
                    LimiterMath.SILENCE_FLOOR_DBFS
                )
            }
            val replacement = LimiterPeakHold(
                peakDbfs = maxOf(
                    newPeakDbfs,
                    decayedPrevious
                ),
                timestampNanos = nowNanos
            )
            if (holder.compareAndSet(previous, replacement)) {
                return
            }
        }
    }

    private fun recentMaximumGainReductionDb(): Double {
        val hold = limiterMaximumHold.get()
        if (
            hold.timestampNanos <= 0L ||
            System.nanoTime() - hold.timestampNanos >
            MAXIMUM_HOLD_NANOS
        ) {
            return limiterMeter.get().currentGainReductionDb
        }
        return maxOf(
            hold.reductionDb,
            limiterMeter.get().currentGainReductionDb
        )
    }

    private data class LimiterMaximumHold(
        val reductionDb: Double = 0.0,
        val timestampNanos: Long = 0L
    )

    private data class LimiterPeakHold(
        val peakDbfs: Double =
            LimiterMath.SILENCE_FLOOR_DBFS,
        val timestampNanos: Long = 0L
    )
}
