package com.example.cdplaya.lyrics

data class LyricsScrollRequest(
    val itemIndex: Int,
    val animate: Boolean
)

class LyricsAutoFollowController(
    private val largeJumpThreshold: Int = 8
) {
    var isEnabled: Boolean = true
        private set
    private var lastRequestedIndex: Int? = null

    fun onTrackChanged() {
        isEnabled = true
        lastRequestedIndex = null
    }

    fun onUserScroll() {
        isEnabled = false
    }

    fun onActiveItemChanged(itemIndex: Int?): LyricsScrollRequest? {
        if (!isEnabled || itemIndex == null || itemIndex == lastRequestedIndex) return null
        val previous = lastRequestedIndex
        lastRequestedIndex = itemIndex
        return LyricsScrollRequest(
            itemIndex = itemIndex,
            animate = previous != null && kotlin.math.abs(itemIndex - previous) <= largeJumpThreshold
        )
    }

    fun returnToCurrent(itemIndex: Int?): LyricsScrollRequest? {
        isEnabled = true
        if (itemIndex == null) return null
        lastRequestedIndex = itemIndex
        return LyricsScrollRequest(itemIndex = itemIndex, animate = true)
    }
}

internal fun lyricsAnchorScrollOffset(viewportHeightPx: Int): Int =
    -(viewportHeightPx * 0.42f).toInt()
