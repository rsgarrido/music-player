package com.example.cdplaya.ui

import androidx.compose.runtime.mutableStateOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicRouteStateTest {
    @Test
    fun primaryDestinationsAreMutuallyExclusive() {
        val state = MusicOverlayState(
            isPlayerExpanded = mutableStateOf(false),
            primaryDestination = mutableStateOf(null),
            transientDestination = mutableStateOf(null)
        )

        state.isSettingsScreenVisible.value = true
        state.isDiagnosticsScreenVisible.value = true
        state.isEqualizerScreenVisible.value = true

        assertFalse(state.isSettingsScreenVisible.value)
        assertFalse(state.isDiagnosticsScreenVisible.value)
        assertTrue(state.isEqualizerScreenVisible.value)
        assertFalse(state.isFolderScreenVisible.value)
    }

    @Test
    fun transientOverlaysAreMutuallyExclusiveButDoNotCollapsePlayer() {
        val state = MusicOverlayState(
            isPlayerExpanded = mutableStateOf(true),
            primaryDestination = mutableStateOf(null),
            transientDestination = mutableStateOf(null)
        )

        state.isExpandedUpNextSheetVisible.value = true
        state.isSleepTimerDialogVisible.value = true

        assertFalse(state.isExpandedUpNextSheetVisible.value)
        assertTrue(state.isSleepTimerDialogVisible.value)
        assertTrue(state.isPlayerExpanded.value)
    }
}
