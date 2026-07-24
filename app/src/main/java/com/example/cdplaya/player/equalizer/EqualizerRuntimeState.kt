package com.example.cdplaya.player.equalizer

enum class EqualizerPlanApplicationMode {
    NONE,
    CROSSFADE,
    DIRECT_AFTER_FLUSH,
    DIRECT_BYPASS
}

data class EqualizerRuntimeState(
    val processorConfigured: Boolean = false,
    val requestedEnabled: Boolean = false,
    val effectivelyActive: Boolean = false,
    val bypassed: Boolean = true,
    val transitionInProgress: Boolean = false,
    val comparisonSessionActive: Boolean = false,
    val comparisonBypassed: Boolean = false,
    val configurationVersion: Long = 0L,
    val preparedPlanVersion: Long? = null,
    val appliedPlanVersion: Long? = null,
    val planPreparationLatencyMillis: Long? = null,
    val planApplicationLatencyMillis: Long? = null,
    val lastPlanApplicationMode: EqualizerPlanApplicationMode =
        EqualizerPlanApplicationMode.NONE,
    val lastTransitionFrameCount: Int = 0,
    val lastTransitionDurationMillis: Double = 0.0,
    val sampleRateHz: Int? = null,
    val channelCount: Int? = null,
    val validFilterCount: Int = 0,
    val ignoredFilterCount: Int = 0,
    val automaticHeadroomDb: Double = 0.0,
    val requiresDecodedPcm: Boolean = false,
    val scratchBufferGrowthCount: Int = 0
)
