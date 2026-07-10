package com.example.cdplaya.player.replaygain

import kotlin.math.pow

private val signedDecimalPattern = Regex("""[-+]?\d+(?:\.\d+)?""")

fun replayGainDbToVolumeMultiplier(gainDb: Float): Float {
    val rawMultiplier = 10.0.pow(gainDb / 20.0).toFloat()

    return rawMultiplier.coerceIn(
        minimumValue = 0f,
        maximumValue = 1f
    )
}

fun replayGainTrackMultiplier(
    replayGainInfo: ReplayGainInfo?,
    isReplayGainEnabled: Boolean
): Float {
    if (!isReplayGainEnabled) {
        return 1f
    }

    val trackGainDb = replayGainInfo?.trackGainDb ?: return 1f

    return replayGainDbToVolumeMultiplier(trackGainDb)
}

fun parseReplayGainDb(rawValue: String?): Float? {
    val cleanedValue = rawValue
        ?.trim()
        ?.takeIf { value ->
            value.isNotBlank()
        }
        ?: return null

    return signedDecimalPattern
        .find(cleanedValue)
        ?.value
        ?.toFloatOrNull()
}

fun parseReplayGainPeak(rawValue: String?): Float? {
    val cleanedValue = rawValue
        ?.trim()
        ?.takeIf { value ->
            value.isNotBlank()
        }
        ?: return null

    return signedDecimalPattern
        .find(cleanedValue)
        ?.value
        ?.toFloatOrNull()
}