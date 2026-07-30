package com.example.cdplaya.player.feasibility

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BitPerfectFeasibilityStateTest {
    @Before
    fun setUp() {
        BitPerfectFeasibilityRuntimeBridge.reset()
    }

    @After
    fun tearDown() {
        BitPerfectFeasibilityRuntimeBridge.reset()
    }

    @Test
    fun safeDefaultContainsOnlyUnknownOrInactiveFacts() {
        val state = BitPerfectFeasibilitySnapshot()
        assertEquals(FeasibilityProbeMode.OFF, state.probeMode)
        assertNull(state.media3OutputConfig)
        assertNull(state.audioTrackFormat)
        assertEquals(
            FeasibilityCleanupResult.NOT_REQUIRED,
            state.cleanupResult
        )
    }

    @Test
    fun structuralEqualityIncludesCopiedLists() {
        val event = FeasibilityEvent(1, FeasibilityEventType.MODE_CHANGED)
        assertEquals(
            BitPerfectFeasibilitySnapshot(events = listOf(event)),
            BitPerfectFeasibilitySnapshot(events = listOf(event))
        )
    }

    @Test
    fun constructorDefensivelyCopiesAndExposesReadOnlyLists() {
        val mutable = mutableListOf(
            MixerAttributeSnapshot(
                encoding = 2,
                sampleRateHz = 44_100,
                channelMask = 12
            )
        )
        val state = BitPerfectFeasibilitySnapshot(
            supportedMixerAttributes = mutable
        )
        mutable.clear()
        assertEquals(1, state.supportedMixerAttributes.size)
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (state.supportedMixerAttributes as MutableList).clear()
        }
    }

    @Test
    fun eventHistoryIsBounded() {
        BitPerfectFeasibilityRuntimeBridge.setMode(
            FeasibilityProbeMode.OBSERVE_CURRENT_PATH
        )
        repeat(150) {
            BitPerfectFeasibilityRuntimeBridge.recordFailure(
                FeasibilityEventType.WRITE_FAILED
            )
        }
        val events = BitPerfectFeasibilityRuntimeBridge.state.value.events
        assertEquals(96, events.size)
        assertTrue(events.zipWithNext().all { it.first.sequence < it.second.sequence })
    }

    @Test
    fun unsupportedRejectionSurvivesOffWithoutInventingASetAttempt() {
        BitPerfectFeasibilityRuntimeBridge.setMode(
            FeasibilityProbeMode.EXPERIMENTAL_USB_EXACT_PATH
        )
        BitPerfectFeasibilityRuntimeBridge.recordActivationRejected(
            FeasibilityRejectionReason.NO_BIT_PERFECT_ATTRIBUTE
        )
        BitPerfectFeasibilityRuntimeBridge.setMode(FeasibilityProbeMode.OFF)

        val state = BitPerfectFeasibilityRuntimeBridge.state.value
        assertEquals(FeasibilityProbeMode.OFF, state.probeMode)
        assertEquals(
            FeasibilityRejectionReason.NO_BIT_PERFECT_ATTRIBUTE,
            state.rejectionReason
        )
        assertNull(state.setResult)
    }

    @Test
    fun exactFormatNeedsConfirmedMixerOutputAndActualTrackAgreement() {
        val exact = MixerAttributeSnapshot(
            encoding = 21,
            sampleRateHz = 96_000,
            channelMask = 12,
            mixerBehavior = FeasibilityMixerBehavior.BIT_PERFECT
        )
        BitPerfectFeasibilityRuntimeBridge.setMode(
            FeasibilityProbeMode.EXPERIMENTAL_USB_EXACT_PATH
        )
        BitPerfectFeasibilityRuntimeBridge.recordOutputConfig(
            Media3OutputConfigSnapshot(
                encoding = 21,
                sampleRateHz = 96_000,
                channelMask = 12
            )
        )
        BitPerfectFeasibilityRuntimeBridge.recordPreferredSet(exact, true)
        BitPerfectFeasibilityRuntimeBridge.recordPreferredConfirmation(exact)
        assertNull(
            BitPerfectFeasibilityRuntimeBridge.state.value
                .exactFormatConfirmed
        )

        BitPerfectFeasibilityRuntimeBridge.recordOutputCreated(
            generation = 1,
            audioTrack = AudioTrackFormatSnapshot(
                encoding = 21,
                sampleRateHz = 96_000,
                channelMask = 12
            )
        )
        assertTrue(
            BitPerfectFeasibilityRuntimeBridge.state.value
                .exactFormatConfirmed == true
        )

        BitPerfectFeasibilityRuntimeBridge.updateAudioTrack(
            generation = 1,
            audioTrack = AudioTrackFormatSnapshot(
                encoding = 2,
                sampleRateHz = 96_000,
                channelMask = 12
            )
        )
        assertFalse(
            BitPerfectFeasibilityRuntimeBridge.state.value
                .exactFormatConfirmed == true
        )
    }

    @Test
    fun publicSnapshotGraphContainsNoAndroidOrMedia3Types() {
        val classes = listOf(
            BitPerfectFeasibilitySnapshot::class.java,
            Media3FormatConfigSnapshot::class.java,
            Media3OutputConfigSnapshot::class.java,
            AudioTrackFormatSnapshot::class.java,
            MixerAttributeSnapshot::class.java
        )
        classes.flatMap { it.declaredFields.asList() }.forEach { field ->
            assertFalse(field.type.name.startsWith("android."))
            assertFalse(field.type.name.startsWith("androidx.media3."))
        }
    }
}
