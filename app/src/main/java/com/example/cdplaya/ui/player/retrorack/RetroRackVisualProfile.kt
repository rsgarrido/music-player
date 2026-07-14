package com.example.cdplaya.ui.player.retrorack

import androidx.compose.ui.graphics.Color

internal data class RetroRackVisualProfile(
    val levels: List<Float>,
    val accent: Color,
    val peak: Color,
    val phaseOffset: Float
)

internal fun buildRetroRackVisualProfile(
    songId: Long?,
    title: String?,
    artist: String?
): RetroRackVisualProfile {
    var seed = songId ?: 0x43_44_50L
    (title.orEmpty() + '\u0000' + artist.orEmpty()).forEach { character ->
        seed = seed * 1_099_511_628_211L xor character.code.toLong()
    }

    var state = seed
    val levels = List(VISUAL_BAR_COUNT) {
        state = state * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L
        val normalized = ((state ushr 40) and 0xFFFF).toFloat() / 0xFFFF
        0.24f + normalized * 0.7f
    }
    val hue = ((seed ushr 8) and 0xFFFF).toFloat() / 0xFFFF * 360f

    return RetroRackVisualProfile(
        levels = levels,
        accent = Color.hsv(hue, saturation = 0.62f, value = 0.92f),
        peak = Color.hsv((hue + 48f) % 360f, saturation = 0.72f, value = 0.96f),
        phaseOffset = ((seed ushr 24) and 0xFF).toFloat() / 255f * 6.283f
    )
}

private const val VISUAL_BAR_COUNT = 18
