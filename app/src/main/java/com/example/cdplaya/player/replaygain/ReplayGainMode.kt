package com.example.cdplaya.player.replaygain

enum class ReplayGainMode(
    val displayName: String,
    val description: String
) {
    OFF(
        displayName = "Off",
        description = "Playback volume is unchanged."
    ),
    TRACK(
        displayName = "Track gain",
        description = "Normalize songs using ReplayGain track tags when available."
    )
}