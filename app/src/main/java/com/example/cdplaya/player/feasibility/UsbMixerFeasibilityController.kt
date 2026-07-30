package com.example.cdplaya.player.feasibility

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioMixerAttributes
import android.os.Build
import androidx.annotation.RequiresApi

internal interface UsbMixerProbeBackend {
    val sdkInt: Int
    fun refreshUsbOutputs(): Int
    fun supportedAttributes(): List<MixerAttributeSnapshot>
    fun setPreferred(attribute: MixerAttributeSnapshot): Boolean
    fun getPreferred(): MixerAttributeSnapshot?
    fun clearPreferred(): Boolean
    fun addListener(listener: (MixerAttributeSnapshot?) -> Unit)
    fun removeListener()
    fun clearStaleOwnedAttributes(): Int
}

internal data class UsbMixerActivationResult(
    val activated: Boolean,
    val rejectionReason: FeasibilityRejectionReason? = null
)

internal class UsbMixerFeasibilityController(
    private val backend: UsbMixerProbeBackend,
    private val bridge: BitPerfectFeasibilityRuntimeBridge =
        BitPerfectFeasibilityRuntimeBridge
) {
    private var listenerRegistered = false
    private var setAttempted = false
    private var cleaned = true

    fun inspect(
        output: Media3OutputConfigSnapshot?
    ): ExactMixerAttributeMatch {
        val readiness = try {
            validateDevice()
        } catch (_: RuntimeException) {
            FeasibilityRejectionReason.SET_FAILED
        }
        if (readiness != null) {
            bridge.recordMixerCapabilities(
                attributes = emptyList(),
                rejectionReason = readiness
            )
            return ExactMixerAttributeMatch(rejectionReason = readiness)
        }
        val attributes = try {
            backend.supportedAttributes().toList()
        } catch (_: RuntimeException) {
            bridge.recordMixerCapabilities(
                attributes = emptyList(),
                rejectionReason = FeasibilityRejectionReason.SET_FAILED
            )
            return ExactMixerAttributeMatch(
                rejectionReason = FeasibilityRejectionReason.SET_FAILED
            )
        }
        val match = ExactMixerAttributeMatcher.select(output, attributes)
        bridge.recordMixerCapabilities(
            attributes = attributes,
            rejectionReason =
                match.rejectionReason.takeUnless {
                    it == FeasibilityRejectionReason.NONE
                }
        )
        return match
    }

    fun activate(
        output: Media3OutputConfigSnapshot?
    ): UsbMixerActivationResult {
        val match = inspect(output)
        val selected = match.selected
        if (!match.isMatch || selected == null) {
            bridge.recordActivationRejected(match.rejectionReason)
            cleanup()
            return UsbMixerActivationResult(
                activated = false,
                rejectionReason = match.rejectionReason
            )
        }

        cleaned = false
        listenerRegistered = true
        try {
            backend.addListener { callbackAttribute ->
                bridge.recordPreferredCallback(callbackAttribute)
            }
        } catch (_: RuntimeException) {
            bridge.recordActivationRejected(
                FeasibilityRejectionReason.SET_FAILED
            )
            cleanup()
            return UsbMixerActivationResult(
                activated = false,
                rejectionReason = FeasibilityRejectionReason.SET_FAILED
            )
        }
        setAttempted = true
        val setResult = try {
            backend.setPreferred(selected)
        } catch (_: RuntimeException) {
            false
        }
        bridge.recordPreferredSet(selected, setResult)
        if (!setResult) {
            bridge.recordActivationRejected(
                FeasibilityRejectionReason.SET_FAILED
            )
            cleanup()
            return UsbMixerActivationResult(
                activated = false,
                rejectionReason = FeasibilityRejectionReason.SET_FAILED
            )
        }

        val confirmed = try {
            backend.getPreferred()
        } catch (_: RuntimeException) {
            null
        }
        bridge.recordPreferredConfirmation(confirmed)
        if (confirmed != selected) {
            bridge.recordActivationRejected(
                FeasibilityRejectionReason.PREFERRED_ATTRIBUTE_NOT_CONFIRMED
            )
            cleanup()
            return UsbMixerActivationResult(
                activated = false,
                rejectionReason =
                    FeasibilityRejectionReason
                        .PREFERRED_ATTRIBUTE_NOT_CONFIRMED
            )
        }
        return UsbMixerActivationResult(activated = true)
    }

    fun clearStaleAtStartup() {
        if (backend.sdkInt < 34) return
        val cleared = try {
            backend.clearStaleOwnedAttributes()
        } catch (_: RuntimeException) {
            0
        }
        if (cleared > 0) {
            bridge.recordCleanup(
                result = FeasibilityCleanupResult.CLEARED_AND_CONFIRMED,
                stale = true
            )
        }
    }

    fun cleanup(): FeasibilityCleanupResult {
        if (cleaned) {
            val currentResult = bridge.state.value.cleanupResult
            if (currentResult == FeasibilityCleanupResult.PENDING) {
                bridge.recordCleanup(FeasibilityCleanupResult.NOT_REQUIRED)
                return FeasibilityCleanupResult.NOT_REQUIRED
            }
            return currentResult
        }
        cleaned = true
        val clearResult = if (setAttempted) {
            try {
                backend.clearPreferred()
            } catch (_: RuntimeException) {
                false
            }
        } else {
            true
        }
        setAttempted = false
        if (listenerRegistered) {
            try {
                backend.removeListener()
            } catch (_: RuntimeException) {
                // Clearing the UID-owned preference remains the primary safety
                // operation; a listener removal failure is still reflected by
                // lack of full cleanup confirmation below.
            }
            listenerRegistered = false
        }
        val preferredAfterClear = try {
            backend.getPreferred()
        } catch (_: RuntimeException) {
            null
        }
        val result = when {
            !clearResult -> FeasibilityCleanupResult.CLEAR_FAILED
            preferredAfterClear != null ->
                FeasibilityCleanupResult.CLEAR_NOT_CONFIRMED
            else -> FeasibilityCleanupResult.CLEARED_AND_CONFIRMED
        }
        bridge.recordCleanup(result)
        return result
    }

    private fun validateDevice(): FeasibilityRejectionReason? {
        if (backend.sdkInt < 34) {
            return FeasibilityRejectionReason.API_TOO_OLD
        }
        return when (backend.refreshUsbOutputs()) {
            0 -> FeasibilityRejectionReason.NON_USB_DEVICE
            1 -> null
            else -> FeasibilityRejectionReason.MULTIPLE_USB_DEVICES
        }
    }
}

internal class AndroidUsbMixerBackend private constructor(
    private val delegate: UsbMixerProbeBackend
) : UsbMixerProbeBackend by delegate {
    val selectedDevice: AudioDeviceInfo?
        @RequiresApi(34)
        get() = (delegate as? Api34UsbMixerBackend)?.selectedDevice

    companion object {
        fun create(context: Context): AndroidUsbMixerBackend {
            val delegate = if (Build.VERSION.SDK_INT >= 34) {
                Api34UsbMixerBackend(context.applicationContext)
            } else {
                UnsupportedUsbMixerBackend(Build.VERSION.SDK_INT)
            }
            return AndroidUsbMixerBackend(delegate)
        }
    }
}

private class UnsupportedUsbMixerBackend(
    override val sdkInt: Int
) : UsbMixerProbeBackend {
    override fun refreshUsbOutputs(): Int = 0
    override fun supportedAttributes(): List<MixerAttributeSnapshot> =
        emptyList()
    override fun setPreferred(attribute: MixerAttributeSnapshot): Boolean =
        false
    override fun getPreferred(): MixerAttributeSnapshot? = null
    override fun clearPreferred(): Boolean = true
    override fun addListener(
        listener: (MixerAttributeSnapshot?) -> Unit
    ) = Unit
    override fun removeListener() = Unit
    override fun clearStaleOwnedAttributes(): Int = 0
}

@RequiresApi(34)
private class Api34UsbMixerBackend(
    context: Context
) : UsbMixerProbeBackend {
    override val sdkInt: Int = Build.VERSION.SDK_INT
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val mediaAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .build()
    private var usbOutputs: List<AudioDeviceInfo> = emptyList()
    private var platformAttributes:
        List<Pair<MixerAttributeSnapshot, AudioMixerAttributes>> =
        emptyList()
    private var listener:
        AudioManager.OnPreferredMixerAttributesChangedListener? = null

    val selectedDevice: AudioDeviceInfo?
        get() = usbOutputs.singleOrNull()

    override fun refreshUsbOutputs(): Int {
        usbOutputs = audioManager
            .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .filter(AudioDeviceInfo::isUsbOutput)
            .toList()
        platformAttributes = emptyList()
        return usbOutputs.size
    }

    override fun supportedAttributes(): List<MixerAttributeSnapshot> {
        val device = selectedDevice ?: return emptyList()
        platformAttributes = audioManager
            .getSupportedMixerAttributes(device)
            .map { it.toSnapshot() to it }
        return platformAttributes.map { it.first }
    }

    override fun setPreferred(
        attribute: MixerAttributeSnapshot
    ): Boolean {
        val device = selectedDevice ?: return false
        val platform = platformAttributes
            .firstOrNull { it.first == attribute }
            ?.second
            ?: return false
        return audioManager.setPreferredMixerAttributes(
            mediaAttributes,
            device,
            platform
        )
    }

    override fun getPreferred(): MixerAttributeSnapshot? {
        val device = selectedDevice ?: return null
        return audioManager
            .getPreferredMixerAttributes(mediaAttributes, device)
            ?.toSnapshot()
    }

    override fun clearPreferred(): Boolean {
        val device = selectedDevice ?: return true
        return audioManager.clearPreferredMixerAttributes(
            mediaAttributes,
            device
        )
    }

    override fun addListener(
        listener: (MixerAttributeSnapshot?) -> Unit
    ) {
        removeListener()
        val selectedId = selectedDevice?.id ?: return
        val platformListener =
            AudioManager.OnPreferredMixerAttributesChangedListener {
                    attributes,
                    device,
                    mixerAttributes ->
                if (
                    attributes.usage == AudioAttributes.USAGE_MEDIA &&
                    device.id == selectedId
                ) {
                    listener(mixerAttributes?.toSnapshot())
                }
            }
        this.listener = platformListener
        audioManager.addOnPreferredMixerAttributesChangedListener(
            { command -> command.run() },
            platformListener
        )
    }

    override fun removeListener() {
        listener?.let(
            audioManager::removeOnPreferredMixerAttributesChangedListener
        )
        listener = null
    }

    override fun clearStaleOwnedAttributes(): Int {
        refreshUsbOutputs()
        var clearedCount = 0
        usbOutputs.forEach { device ->
            if (
                audioManager.getPreferredMixerAttributes(
                    mediaAttributes,
                    device
                ) != null &&
                audioManager.clearPreferredMixerAttributes(
                    mediaAttributes,
                    device
                ) &&
                audioManager.getPreferredMixerAttributes(
                    mediaAttributes,
                    device
                ) == null
            ) {
                clearedCount++
            }
        }
        return clearedCount
    }
}

private fun AudioDeviceInfo.isUsbOutput(): Boolean =
    isSink && type in setOf(
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_ACCESSORY,
        AudioDeviceInfo.TYPE_USB_HEADSET
    )

@RequiresApi(34)
private fun AudioMixerAttributes.toSnapshot(): MixerAttributeSnapshot {
    val format = format
    return MixerAttributeSnapshot(
        encoding = format.encoding.takeIf {
            it != AudioFormat.ENCODING_INVALID
        },
        sampleRateHz = format.sampleRate.takeIf { it > 0 },
        channelMask = format.channelMask.takeIf {
            it != AudioFormat.CHANNEL_INVALID && it != 0
        },
        mixerBehavior = when (mixerBehavior) {
            AudioMixerAttributes.MIXER_BEHAVIOR_DEFAULT ->
                FeasibilityMixerBehavior.DEFAULT
            AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT ->
                FeasibilityMixerBehavior.BIT_PERFECT
            else -> FeasibilityMixerBehavior.UNKNOWN
        }
    )
}
