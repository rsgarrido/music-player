package com.example.cdplaya.ui.player.classicwheel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ClassicWheelMenuState {
    var currentScreen by mutableStateOf<ClassicWheelMenuScreen>(
        ClassicWheelMenuScreen.NowPlaying
    )
        private set

    var selectedIndex by mutableIntStateOf(0)
        private set

    private val backStack = mutableStateListOf<ClassicWheelMenuScreen>()

    fun openMainMenu() {
        if (currentScreen != ClassicWheelMenuScreen.MainMenu) {
            backStack.add(currentScreen)
        }

        currentScreen = ClassicWheelMenuScreen.MainMenu
        selectedIndex = 0
    }

    fun openSongs() {
        backStack.add(currentScreen)
        currentScreen = ClassicWheelMenuScreen.Songs
        selectedIndex = 0
    }

    fun openNowPlaying() {
        backStack.clear()
        currentScreen = ClassicWheelMenuScreen.NowPlaying
        selectedIndex = 0
    }

    fun moveSelectionUp(itemCount: Int) {
        if (itemCount <= 0) {
            selectedIndex = 0
            return
        }

        selectedIndex = selectedIndex.coerceIn(0, itemCount - 1)

        selectedIndex = if (selectedIndex <= 0) {
            itemCount - 1
        } else {
            selectedIndex - 1
        }
    }

    fun moveSelectionDown(itemCount: Int) {
        if (itemCount <= 0) {
            selectedIndex = 0
            return
        }

        selectedIndex = selectedIndex.coerceIn(0, itemCount - 1)

        selectedIndex = if (selectedIndex >= itemCount - 1) {
            0
        } else {
            selectedIndex + 1
        }
    }

    fun goBack() {
        val previousScreen = backStack.removeLastOrNull()

        if (previousScreen == null) {
            currentScreen = ClassicWheelMenuScreen.NowPlaying
            selectedIndex = 0
            return
        }

        currentScreen = previousScreen
        selectedIndex = 0
    }
}