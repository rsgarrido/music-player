package com.example.cdplaya.player.feasibility

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UsbMixerFeasibilityControllerTest {
    private val output = Media3OutputConfigSnapshot(
        encoding = 21,
        sampleRateHz = 96_000,
        channelMask = 12
    )
    private val exact = MixerAttributeSnapshot(
        encoding = 21,
        sampleRateHz = 96_000,
        channelMask = 12,
        mixerBehavior = FeasibilityMixerBehavior.BIT_PERFECT
    )

    @Before
    fun setUp() {
        BitPerfectFeasibilityRuntimeBridge.reset()
        BitPerfectFeasibilityRuntimeBridge.setMode(
            FeasibilityProbeMode.EXPERIMENTAL_USB_EXACT_PATH
        )
    }

    @After
    fun tearDown() {
        BitPerfectFeasibilityRuntimeBridge.reset()
    }

    @Test
    fun api33IsUnavailable() {
        val result = controller(FakeBackend(sdkInt = 33))
            .activate(output)
        assertEquals(
            FeasibilityRejectionReason.API_TOO_OLD,
            result.rejectionReason
        )
    }

    @Test
    fun noUsbAndMultipleUsbAreTruthfullyDistinguished() {
        assertEquals(
            FeasibilityRejectionReason.NON_USB_DEVICE,
            controller(FakeBackend(usbCount = 0))
                .activate(output).rejectionReason
        )
        assertEquals(
            FeasibilityRejectionReason.MULTIPLE_USB_DEVICES,
            controller(FakeBackend(usbCount = 2))
                .activate(output).rejectionReason
        )
    }

    @Test
    fun noBitPerfectAttributeIsUnsupported() {
        val backend = FakeBackend(
            attributes = listOf(
                exact.copy(
                    mixerBehavior = FeasibilityMixerBehavior.DEFAULT
                )
            )
        )
        assertEquals(
            FeasibilityRejectionReason.NO_BIT_PERFECT_ATTRIBUTE,
            controller(backend).activate(output).rejectionReason
        )
    }

    @Test
    fun setFailureStillRemovesListenerAndClearsExactlyOnce() {
        val backend = FakeBackend(attributes = listOf(exact), setResult = false)
        val result = controller(backend).activate(output)
        assertEquals(FeasibilityRejectionReason.SET_FAILED, result.rejectionReason)
        assertEquals(1, backend.clearCount)
        assertEquals(1, backend.removeListenerCount)
    }

    @Test
    fun listenerFailureStillAttemptsListenerRemoval() {
        val backend = FakeBackend(
            attributes = listOf(exact),
            listenerFailure = true
        )
        val result = controller(backend).activate(output)
        assertEquals(FeasibilityRejectionReason.SET_FAILED, result.rejectionReason)
        assertEquals(1, backend.removeListenerCount)
    }

    @Test
    fun successRequiresPostQueryConfirmation() {
        val backend = FakeBackend(
            attributes = listOf(exact),
            preferredAfterSet = exact.copy(sampleRateHz = 48_000)
        )
        val result = controller(backend).activate(output)
        assertFalse(result.activated)
        assertEquals(
            FeasibilityRejectionReason.PREFERRED_ATTRIBUTE_NOT_CONFIRMED,
            result.rejectionReason
        )
        assertEquals(1, backend.clearCount)
    }

    @Test
    fun listenerUpdatesStateAndCleanupIsIdempotent() {
        val backend = FakeBackend(
            attributes = listOf(exact),
            preferredAfterSet = exact,
            callbackOnSet = true
        )
        val controller = controller(backend)
        assertTrue(controller.activate(output).activated)
        assertTrue(
            BitPerfectFeasibilityRuntimeBridge.state.value
                .listenerCallbackObserved == true
        )
        controller.cleanup()
        controller.cleanup()
        assertEquals(1, backend.clearCount)
        assertEquals(1, backend.removeListenerCount)
        assertEquals(
            FeasibilityCleanupResult.CLEARED_AND_CONFIRMED,
            BitPerfectFeasibilityRuntimeBridge.state.value.cleanupResult
        )
    }

    private fun controller(backend: FakeBackend) =
        UsbMixerFeasibilityController(backend)

    private class FakeBackend(
        override val sdkInt: Int = 36,
        private val usbCount: Int = 1,
        private val attributes: List<MixerAttributeSnapshot> = emptyList(),
        private val setResult: Boolean = true,
        private val preferredAfterSet: MixerAttributeSnapshot? = null,
        private val callbackOnSet: Boolean = false,
        private val listenerFailure: Boolean = false
    ) : UsbMixerProbeBackend {
        var clearCount = 0
        var removeListenerCount = 0
        private var preferred: MixerAttributeSnapshot? = null
        private var listener: ((MixerAttributeSnapshot?) -> Unit)? = null

        override fun refreshUsbOutputs(): Int = usbCount
        override fun supportedAttributes(): List<MixerAttributeSnapshot> =
            attributes.toList()

        override fun setPreferred(
            attribute: MixerAttributeSnapshot
        ): Boolean {
            preferred = preferredAfterSet
            if (callbackOnSet) listener?.invoke(preferred)
            return setResult
        }

        override fun getPreferred(): MixerAttributeSnapshot? = preferred

        override fun clearPreferred(): Boolean {
            clearCount++
            preferred = null
            return true
        }

        override fun addListener(
            listener: (MixerAttributeSnapshot?) -> Unit
        ) {
            if (listenerFailure) throw IllegalStateException("listener failure")
            this.listener = listener
        }

        override fun removeListener() {
            removeListenerCount++
            listener = null
        }

        override fun clearStaleOwnedAttributes(): Int = 0
    }
}
