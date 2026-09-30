package io.github.rsgarrido.sazanami.ui.player.classicwheel

import io.github.rsgarrido.sazanami.ui.state.UiMessage

data class ClassicWheelMenuItem(
    val title: UiMessage,
    val subtitle: UiMessage? = null,
    val action: ClassicWheelMenuAction
)

enum class ClassicWheelMenuAction {
    OPEN_NOW_PLAYING,
    OPEN_SONGS,
    OPEN_ARTISTS,
    OPEN_ALBUMS
}
