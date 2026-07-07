package com.example.cdplaya.ui.player.classicwheel

data class ClassicWheelMenuItem(
    val title: String,
    val subtitle: String? = null,
    val action: ClassicWheelMenuAction
)

enum class ClassicWheelMenuAction {
    OPEN_NOW_PLAYING,
    OPEN_SONGS,
    OPEN_ARTISTS,
    OPEN_ALBUMS
}