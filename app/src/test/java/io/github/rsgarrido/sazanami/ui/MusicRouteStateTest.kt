package io.github.rsgarrido.sazanami.ui

import androidx.compose.runtime.mutableStateOf
import android.net.Uri
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.ui.player.PlayerMorphState
import io.github.rsgarrido.sazanami.ui.player.PlayerPresentation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class MusicRouteStateTest {
    @Test
    fun primaryDestinationsAreMutuallyExclusive() {
        val state = MusicOverlayState(
            playerMorphState = playerMorphState(PlayerPresentation.Collapsed),
            primaryDestination = mutableStateOf(null),
            transientDestination = mutableStateOf(null)
        )

        state.isSettingsScreenVisible.value = true
        state.isDiagnosticsScreenVisible.value = true
        state.isEqualizerScreenVisible.value = true
        state.isListeningHistoryReconciliationVisible.value = true
        state.isStatisticsScreenVisible.value = true

        assertFalse(state.isSettingsScreenVisible.value)
        assertFalse(state.isDiagnosticsScreenVisible.value)
        assertFalse(state.isEqualizerScreenVisible.value)
        assertFalse(state.isListeningHistoryReconciliationVisible.value)
        assertTrue(state.isStatisticsScreenVisible.value)
        assertFalse(state.isFolderScreenVisible.value)
    }

    @Test
    fun transientOverlaysAreMutuallyExclusiveButDoNotCollapsePlayer() {
        val state = MusicOverlayState(
            playerMorphState = playerMorphState(PlayerPresentation.Expanded),
            primaryDestination = mutableStateOf(null),
            transientDestination = mutableStateOf(null)
        )

        state.isExpandedUpNextSheetVisible.value = true
        state.isSleepTimerDialogVisible.value = true

        assertFalse(state.isExpandedUpNextSheetVisible.value)
        assertTrue(state.isSleepTimerDialogVisible.value)
        assertTrue(state.playerMorphState.isExpandedOrTransitioning)
    }

    @Test
    fun moreCapturesItsSongWithoutOpeningQueueHubOrCollapsingThePlayer() {
        val state = expandedOverlayState()
        val target = song(1)
        state.isQueueHubVisible.value = true
        state.openNowPlayingMore(target)

        assertTrue(state.isNowPlayingMoreVisible.value)
        assertFalse(state.isQueueHubVisible.value)
        assertSame(target, state.currentNowPlayingMoreTarget(target))
        assertEquals(PlayerPresentation.Expanded, state.playerMorphState.targetPresentation)
        assertEquals(1f, state.playerMorphState.progress, 0f)

        state.dismissNowPlayingMore()
        assertFalse(state.isNowPlayingMoreVisible.value)
        assertNull(state.nowPlayingMoreTarget)
        assertEquals(PlayerPresentation.Expanded, state.playerMorphState.targetPresentation)
    }

    @Test
    fun switchingToAnyOtherTransientOverlayClearsMoreAndItsTarget() {
        val state = expandedOverlayState()
        listOf(
            state.isQueueHubVisible,
            state.isExpandedUpNextSheetVisible,
            state.isSleepTimerDialogVisible,
            state.isCreatePlaylistDialogVisible
        ).forEach { destination ->
            state.openNowPlayingMore(song(1))
            destination.value = true
            assertTrue(destination.value)
            assertFalse(state.isNowPlayingMoreVisible.value)
            assertNull(state.nowPlayingMoreTarget)
            assertEquals(PlayerPresentation.Expanded, state.playerMorphState.targetPresentation)
        }
    }

    @Test
    fun openingSettingsClearsMoreWithoutChangingSettingsAccess() {
        val state = expandedOverlayState()
        state.openNowPlayingMore(song(1))
        state.isSettingsScreenVisible.value = true
        assertTrue(state.isSettingsScreenVisible.value)
        assertFalse(state.isNowPlayingMoreVisible.value)
        assertNull(state.nowPlayingMoreTarget)
    }

    @Test
    fun trackChangeHidesTheTargetImmediatelyAndReconciliationClearsTheOverlay() {
        val state = expandedOverlayState()
        state.openNowPlayingMore(song(1))
        assertNull(state.currentNowPlayingMoreTarget(song(2)))
        state.reconcileNowPlayingMore(song(2), canPresent = true)
        assertFalse(state.isNowPlayingMoreVisible.value)
        assertNull(state.nowPlayingMoreTarget)
    }

    @Test
    fun nullTrackOrUnavailablePlayerClearsMoreButMetadataRefreshDoesNot() {
        val state = expandedOverlayState()
        val target = song(1)
        state.openNowPlayingMore(target)
        state.reconcileNowPlayingMore(target.copy(title = "Updated title"), canPresent = true)
        assertTrue(state.isNowPlayingMoreVisible.value)
        state.reconcileNowPlayingMore(null, canPresent = true)
        assertFalse(state.isNowPlayingMoreVisible.value)
        state.openNowPlayingMore(target)
        state.reconcileNowPlayingMore(target, canPresent = false)
        assertNull(state.nowPlayingMoreTarget)
    }

    @Test
    fun restoredMoreDestinationWithoutACapturedSongIsDismissed() {
        val state = MusicOverlayState(
            playerMorphState(PlayerPresentation.Expanded),
            mutableStateOf(null),
            mutableStateOf(MusicOverlayDestination.NOW_PLAYING_MORE)
        )
        state.reconcileNowPlayingMore(song(1), canPresent = true)
        assertFalse(state.isNowPlayingMoreVisible.value)
    }

    private fun expandedOverlayState() = MusicOverlayState(
        playerMorphState(PlayerPresentation.Expanded), mutableStateOf(null), mutableStateOf(null)
    )

    private fun song(id: Long) = Song(
        id, "Song", "Artist", "Album", 1, 120_000L, mock(Uri::class.java),
        "/music/$id.flac", "/music", null, volumeName = "external"
    )

    private fun playerMorphState(presentation: PlayerPresentation) =
        PlayerMorphState(
            initialPresentation = presentation,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )
}
