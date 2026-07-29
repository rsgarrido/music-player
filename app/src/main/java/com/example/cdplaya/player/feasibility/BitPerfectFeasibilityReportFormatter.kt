package com.example.cdplaya.player.feasibility

import com.example.cdplaya.player.audio.AudioSourceFormat

internal fun formatBitPerfectFeasibilityReport(
    snapshot: BitPerfectFeasibilitySnapshot,
    source: AudioSourceFormat?
): String = buildString {
    appendLine("CDPlaya USB bit-perfect feasibility")
    appendLine("Evidence only; experimental session state is not persisted.")
    appendLine("Probe mode: ${snapshot.probeMode}")
    appendLine("Source file metadata: ${source.formatForFeasibility()}")
    appendLine("Media3 renderer input: ${source.formatForFeasibility()}")
    appendLine(
        "Equalizer processor input: " +
            snapshot.processorInputFormat.formatForFeasibility()
    )
    appendLine(
        "Equalizer processor output: " +
            snapshot.processorOutputFormat.formatForFeasibility()
    )
    appendLine(
        "Equalizer processor buffers: " +
            snapshot.processorReceivedBufferCount
    )
    appendLine(
        "Media3 FormatConfig: " +
            snapshot.media3FormatConfig.formatForFeasibility()
    )
    appendLine(
        "Media3 OutputConfig: " +
            snapshot.media3OutputConfig.formatForFeasibility()
    )
    appendLine(
        "Actual AudioTrack format: " +
            snapshot.audioTrackFormat.formatForFeasibility()
    )
    appendLine(
        "Current routed-device category: " +
            (snapshot.audioTrackFormat?.routedDeviceCategory
                ?: FeasibilityRouteCategory.UNKNOWN)
    )
    appendLine("Supported mixer attributes:")
    if (snapshot.supportedMixerAttributes.isEmpty()) {
        appendLine("- None reported")
    } else {
        snapshot.supportedMixerAttributes.forEachIndexed { index, attribute ->
            appendLine("- ${index + 1}: ${attribute.formatForFeasibility()}")
        }
    }
    appendLine(
        "Selected exact mixer attribute: " +
            snapshot.selectedMixerAttribute.formatForFeasibility()
    )
    appendLine("Preferred set result: ${snapshot.setResult ?: "Unknown"}")
    appendLine(
        "Preferred query result: " +
            snapshot.confirmedPreferredAttribute.formatForFeasibility()
    )
    appendLine(
        "Preferred listener observed: " +
            (snapshot.listenerCallbackObserved ?: "Unknown")
    )
    appendLine("Route confirmed: ${snapshot.routeConfirmed ?: "Unknown"}")
    appendLine(
        "Exact format confirmed: " +
            (snapshot.exactFormatConfirmed ?: "Unknown")
    )
    appendLine("Cleanup result: ${snapshot.cleanupResult}")
    appendLine(
        "Rejection reason: " +
            (snapshot.rejectionReason ?: "None")
    )
    appendLine(
        "Output create/reuse/flush/stop/release: " +
            "${snapshot.outputCreationCount} / " +
            "${snapshot.outputReuseCount} / " +
            "${snapshot.outputFlushCount} / " +
            "${snapshot.outputStopCount} / " +
            snapshot.outputReleaseCount
    )
    appendLine("Events:")
    snapshot.events.forEach { event ->
        appendLine(
            "- ${event.sequence}: ${event.type}" +
                (event.generation?.let { " generation=$it" } ?: "") +
                (event.reason?.let { " reason=$it" } ?: "")
        )
    }
    appendLine()
    appendLine("Caveats:")
    appendLine("- Source format is not final output format.")
    appendLine(
        "- Processor bypass at PCM16 is not proof of high-resolution preservation."
    )
    appendLine(
        "- A supported mixer attribute is not proof it was activated."
    )
    appendLine(
        "- A successful set call is not proof the AudioTrack matched."
    )
    appendLine("- A DAC sample-rate display is secondary evidence.")
    appendLine("- No analog-output claim is made.")
    appendLine("- No support is inferred for other devices.")
    appendLine("- Device/vendor bit-perfect support is optional.")
}

private fun AudioSourceFormat?.formatForFeasibility(): String {
    if (this == null) return "Unknown"
    return listOfNotNull(
        sampleMimeType?.let { "mime=$it" },
        pcmEncoding?.let { "encoding=$it" },
        sampleRateHz?.let { "rate=$it Hz" },
        channelCount?.let { "channels=$it" },
        sourceBitDepth?.let { "sourceBits=$it" }
    ).joinToString().ifBlank { "Unknown" }
}

private fun FeasibilityAudioFormatSnapshot?.formatForFeasibility(): String {
    if (this == null) return "Unknown"
    return "encoding=${encoding ?: "Unknown"}, " +
        "rate=${sampleRateHz ?: "Unknown"}, " +
        "channels=${channelCount ?: "Unknown"}, " +
        "mask=${channelMask ?: "Unknown"}"
}

private fun Media3FormatConfigSnapshot?.formatForFeasibility(): String {
    if (this == null) return "Unknown"
    return "mime=${mimeType ?: "Unknown"}, " +
        "encoding=${pcmEncoding ?: "Unknown"}, " +
        "rate=${sampleRateHz ?: "Unknown"}, " +
        "channels=${channelCount ?: "Unknown"}, " +
        "offload=$offloadEnabled, tunneling=$tunnelingEnabled, " +
        "highResolution=$highResolutionPcmEnabled, " +
        "playbackParameters=$playbackParametersEnabled, " +
        "preferredRoute=$preferredDeviceCategory"
}

private fun Media3OutputConfigSnapshot?.formatForFeasibility(): String {
    if (this == null) return "Unknown"
    return "encoding=${encoding ?: "Unknown"}, " +
        "rate=${sampleRateHz ?: "Unknown"}, " +
        "mask=${channelMask ?: "Unknown"}, " +
        "buffer=${bufferSizeBytes ?: "Unknown"} bytes, " +
        "offload=$offloadEnabled, tunneling=$tunnelingEnabled, " +
        "playbackParameters=$playbackParametersEnabled"
}

private fun AudioTrackFormatSnapshot?.formatForFeasibility(): String {
    if (this == null) return "Unknown"
    return "encoding=${encoding ?: "Unknown"}, " +
        "rate=${sampleRateHz ?: "Unknown"}, " +
        "mask=${channelMask ?: "Unknown"}, " +
        "indexMask=${channelIndexMask ?: "Unknown"}, " +
        "channels=${channelCount ?: "Unknown"}, " +
        "buffer=${bufferSizeFrames ?: "Unknown"} frames, " +
        "session=${audioSessionId ?: "Unknown"}, " +
        "route=$routedDeviceCategory, " +
        "intendedRoute=${routeMatchesIntendedUsbDevice ?: "Unknown"}"
}

private fun MixerAttributeSnapshot?.formatForFeasibility(): String {
    if (this == null) return "None"
    return "encoding=${encoding ?: "Unknown"}, " +
        "rate=${sampleRateHz ?: "Unknown"}, " +
        "mask=${channelMask ?: "Unknown"}, " +
        "behavior=$mixerBehavior"
}

