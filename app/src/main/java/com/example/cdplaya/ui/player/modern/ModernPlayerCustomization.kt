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
