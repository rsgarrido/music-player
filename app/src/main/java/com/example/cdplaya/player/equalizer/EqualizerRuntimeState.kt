package com.example.cdplaya.player.equalizer

data class EqualizerRuntimeState(
    val processorConfigured: Boolean = false,
    val requestedEnabled: Boolean = false,
    val effectivelyActive: Boolean = false,
    val bypassed: Boolean = true,
    val transitionInProgress: Boolean = false,
    val configurationVersion: Long = 0L,
    val appliedPlanVersion: Long? = null,
    val sampleRateHz: Int? = null,
    val channelCount: Int? = null,
    val validFilterCount: Int = 0,
    val ignoredFilterCount: Int = 0,
    val automaticHeadroomDb: Double = 0.0,
    val requiresDecodedPcm: Boolean = false,
    val scratchBufferGrowthCount: Int = 0
)
