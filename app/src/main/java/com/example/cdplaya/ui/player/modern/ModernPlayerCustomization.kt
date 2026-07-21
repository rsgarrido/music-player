package com.example.cdplaya.ui.player.modern

enum class ModernArtworkTransitionStyle(
    val storageValue: String,
    val displayName: String,
    val description: String
) {
    SLIDE(
        storageValue = "slide",
        displayName = "Slide",
        description = "Current artwork slides naturally with the next cover."
    ),
    DEPTH_SCALE(
        storageValue = "depth_scale",
        displayName = "Depth & Scale",
        description = "Covers shrink and fade slightly as they move, creating depth."
    ),
    PARALLAX(
        storageValue = "parallax",
        displayName = "Parallax",
        description = "Artwork and text move at different speeds."
    ),
    COVER_FLOW(
        storageValue = "cover_flow",
        displayName = "Cover Flow",
        description = "Covers tilt slightly as they move off-center."
    ),
    STACK_REVEAL(
        storageValue = "stack_reveal",
        displayName = "Stack Reveal",
        description = "Current cover peels away to reveal the next cover underneath."
    );

    companion object {
        fun fromStorageValue(storageValue: String?): ModernArtworkTransitionStyle {
            return values().firstOrNull { style ->
                style.storageValue == storageValue
            } ?: SLIDE
        }
    }
}

enum class ModernSeekbarStyle(
    val storageValue: String,
    val displayName: String,
    val description: String
) {
    CLASSIC_BAR(
        storageValue = "classic_bar",
        displayName = "Classic Bar",
        description = "The current simple progress bar."
    ),
    SLIM_LINE(
        storageValue = "slim_line",
        displayName = "Slim Line",
        description = "A minimal thin progress line."
    ),
    THICK_CAPSULE(
        storageValue = "thick_capsule",
        displayName = "Thick Capsule",
        description = "A larger rounded progress control."
    ),
    SEGMENTED(
        storageValue = "segmented",
        displayName = "Segmented",
        description = "Small separated blocks that fill with progress."
    ),
    WAVEFORM_PREVIEW(
        storageValue = "waveform_preview",
        displayName = "Waveform Preview",
        description = "Uses analyzed track shape when available, with a generated preview while loading or unsupported."
    ),
    WAVEFORM_PEAKS(
        storageValue = "waveform_peaks",
        displayName = "Waveform Peaks",
        description = "Mirrors analyzed track peaks when available, with a generated preview as fallback."
    ),
    WAVEFORM_GLOW(
        storageValue = "waveform_glow",
        displayName = "Waveform Glow",
        description = "Adds a soft glow to analyzed track shape, with a generated preview as fallback."
    );

    val usesWaveformData: Boolean
        get() = when (this) {
            WAVEFORM_PREVIEW, WAVEFORM_PEAKS, WAVEFORM_GLOW -> true
            CLASSIC_BAR, SLIM_LINE, THICK_CAPSULE, SEGMENTED -> false
        }

    companion object {
        fun fromStorageValue(storageValue: String?): ModernSeekbarStyle {
            return values().firstOrNull { style ->
                style.storageValue == storageValue
            } ?: CLASSIC_BAR
        }
    }
}
