package com.example.cdplaya.ui.player.classicwheel

sealed class ClassicWheelMenuScreen {
    data object NowPlaying : ClassicWheelMenuScreen()
    data object MainMenu : ClassicWheelMenuScreen()
    data object Songs : ClassicWheelMenuScreen()
}