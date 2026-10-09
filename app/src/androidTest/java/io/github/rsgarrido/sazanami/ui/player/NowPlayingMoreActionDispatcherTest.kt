package io.github.rsgarrido.sazanami.ui.player

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.data.membershipKey
import io.github.rsgarrido.sazanami.ui.MusicOverlayState
import io.github.rsgarrido.sazanami.ui.rememberMusicOverlayState
import io.github.rsgarrido.sazanami.ui.rememberNowPlayingArtistNavigation
import io.github.rsgarrido.sazanami.ui.ratings.LocalSongRatingUi
import io.github.rsgarrido.sazanami.ui.ratings.SongRatingUiEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NowPlayingMoreActionDispatcherTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun retainedDispatcherKeepsIdentityAndUsesUpdatedSongMembershipAndCallbacks() {
        val next = song(2)
        val fixture = DispatcherFixture(song(1))
        composeRule.setContent { fixture.Content() }
        lateinit var retained: (NowPlayingAction, Song) -> Unit
        composeRule.runOnIdle {
            retained = fixture.dispatch
            fixture.current.value = next
            fixture.library.value = listOf(next)
            fixture.favorites.value = setOf(next.membershipKey())
            fixture.callbackGeneration.value = 1
        }
        composeRule.runOnIdle {
            assertSame(retained, fixture.dispatch)
            fixture.overlays.openNowPlayingMore(next)
            retained(NowPlayingAction.FAVORITE, next)
            assertEquals(listOf(DispatchEvent(NowPlayingAction.FAVORITE, next, 1)), fixture.events)
            assertEquals(listOf(NowPlayingFavoriteFeedback.REMOVED_FROM_FAVORITES), fixture.feedback)
            assertEquals(listOf(1), fixture.feedbackGenerations)
            assertFalse(next.membershipKey() in fixture.favorites.value)
        }
    }

    @Test
    fun everyActionRejectsAnOldTrackNullPlaybackOrTheSameIdOnAnotherVolume() {
        val target = song(1)
        val fixture = DispatcherFixture(target)
        composeRule.setContent { fixture.Content() }
        listOf(song(2), null, target.copy(volumeName = "other_volume")).forEach { current ->
            composeRule.runOnIdle { fixture.current.value = current }
            composeRule.runOnIdle {
                NowPlayingAction.entries.forEach { action ->
                    fixture.overlays.openNowPlayingMore(target)
                    fixture.dispatch(action, target)
                    assertFalse(fixture.overlays.isNowPlayingMoreVisible.value)
                    assertNull(fixture.overlays.nowPlayingMoreTarget)
                }
                assertTrue(fixture.events.isEmpty())
                assertTrue(fixture.feedback.isEmpty())
                assertTrue(fixture.artistNames.isEmpty())
                assertNull(fixture.overlays.trackInfoRequest)
                assertFalse(fixture.overlays.isSleepTimerDialogVisible.value)
            }
        }
    }

    @Test
    fun dispatchRequiresTheMatchingOpenMoreTargetAndLatestEligibility() {
        val target = song(1)
        val fixture = DispatcherFixture(target)
        composeRule.setContent { fixture.Content() }
        composeRule.runOnIdle {
            // A current Song alone is insufficient: More must still own that target.
            fixture.dispatch(NowPlayingAction.FAVORITE, target)
            fixture.overlays.openNowPlayingMore(song(2))
            fixture.dispatch(NowPlayingAction.FAVORITE, target)
            assertFalse(fixture.overlays.isNowPlayingMoreVisible.value)
            fixture.canPresent.value = false
        }
        composeRule.runOnIdle {
            NowPlayingAction.entries.forEach { action ->
                fixture.overlays.openNowPlayingMore(target)
                fixture.dispatch(action, target)
                assertFalse(fixture.overlays.isNowPlayingMoreVisible.value)
                assertNull(fixture.overlays.nowPlayingMoreTarget)
            }
            assertTrue(fixture.events.isEmpty())
            assertTrue(fixture.feedback.isEmpty())
            fixture.canPresent.value = true
        }
        composeRule.runOnIdle {
            fixture.overlays.openNowPlayingMore(target)
            fixture.dispatch(NowPlayingAction.ADD_TO_PLAYLIST, target)
            assertEquals(listOf(DispatchEvent(NowPlayingAction.ADD_TO_PLAYLIST, target, 0)), fixture.events)
        }
    }

    @Test
    fun favoriteFeedbackUsesPreToggleMembershipAndFollowsDismissalThenToggle() {
        val target = song(1)
        val fixture = DispatcherFixture(target)
        composeRule.setContent { fixture.Content() }
        composeRule.runOnIdle {
            fixture.overlays.openNowPlayingMore(target)
            fixture.dispatch(NowPlayingAction.FAVORITE, target)
            assertTrue(target.membershipKey() in fixture.favorites.value)
            assertEquals(listOf("FAVORITE", "ADDED_TO_FAVORITES"), fixture.order)
            // A late second event cannot toggle or emit after More has been dismissed.
            fixture.dispatch(NowPlayingAction.FAVORITE, target)
            assertEquals(1, fixture.events.size)
            assertEquals(listOf(NowPlayingFavoriteFeedback.ADDED_TO_FAVORITES), fixture.feedback)
        }
        composeRule.runOnIdle {
            fixture.overlays.openNowPlayingMore(target)
            fixture.dispatch(NowPlayingAction.FAVORITE, target)
            assertFalse(target.membershipKey() in fixture.favorites.value)
            assertEquals(
                listOf("FAVORITE", "ADDED_TO_FAVORITES", "FAVORITE", "REMOVED_FROM_FAVORITES"),
                fixture.order
            )
            assertEquals(
                listOf(NowPlayingFavoriteFeedback.ADDED_TO_FAVORITES,
                    NowPlayingFavoriteFeedback.REMOVED_FROM_FAVORITES),
                fixture.feedback
            )
        }
    }

    @Test
    fun replacementActionsDismissFirstUseLatestCallbacksAndKeepTheCapturedSong() {
        val target = song(1)
        val refreshed = target.copy(title = "Refreshed metadata")
        val fixture = DispatcherFixture(target)
        composeRule.setContent { fixture.Content() }
        composeRule.runOnIdle {
            fixture.current.value = refreshed
            fixture.library.value = listOf(refreshed)
            fixture.callbackGeneration.value = 1
        }
        composeRule.runOnIdle {
            NowPlayingAction.entries.filter { it != NowPlayingAction.FAVORITE }.forEach { action ->
                fixture.overlays.openNowPlayingMore(target)
                fixture.dispatch(action, target)
                val event = fixture.events.last()
                assertEquals(action, event.action)
                assertEquals(1, event.generation)
                if (action == NowPlayingAction.LYRICS || action == NowPlayingAction.SLEEP_TIMER) {
                    assertNull(event.song)
                } else {
                    assertSame(target, event.song)
                }
                if (action == NowPlayingAction.TRACK_INFORMATION) {
                    assertSame(target, fixture.overlays.trackInfoRequest?.song)
                }
                if (action == NowPlayingAction.SLEEP_TIMER) {
                    assertTrue(fixture.overlays.isSleepTimerDialogVisible.value)
                }
            }
            assertEquals(NowPlayingAction.entries.size - 1, fixture.events.size)
            assertEquals(listOf("Artist"), fixture.artistNames)
            assertTrue(fixture.feedback.isEmpty())
            fixture.current.value = song(2)
        }
        composeRule.runOnIdle {
            assertSame(target, fixture.events.single { it.action == NowPlayingAction.ADD_TO_PLAYLIST }.song)
            assertSame(target, fixture.events.single { it.action == NowPlayingAction.RATE_SONG }.song)
        }
    }

    @Test
    fun trackInformationOpenerPinsTheCapturedSongWhenPlaybackAdvances() {
        val target = song(1)
        val fixture = DispatcherFixture(target)
        composeRule.setContent { fixture.Content() }
        composeRule.runOnIdle {
            fixture.overlays.openNowPlayingMore(target)
            fixture.dispatch(NowPlayingAction.TRACK_INFORMATION, target)
            assertEquals(listOf(DispatchEvent(NowPlayingAction.TRACK_INFORMATION, target, 0)), fixture.events)
            assertSame(target, fixture.overlays.trackInfoRequest?.song)
            fixture.current.value = song(2)
        }
        composeRule.runOnIdle {
            assertSame(target, fixture.overlays.trackInfoRequest?.song)
            assertTrue(fixture.overlays.isTrackInformationVisible.value)
        }
    }

    @Test
    fun latestLibraryRemovalRejectsArtistAndAlbumButStillDismissesMore() {
        val target = song(1)
        val fixture = DispatcherFixture(target)
        composeRule.setContent { fixture.Content() }
        composeRule.runOnIdle { fixture.library.value = emptyList() }
        composeRule.runOnIdle {
            listOf(NowPlayingAction.GO_TO_ARTIST, NowPlayingAction.GO_TO_ALBUM).forEach { action ->
                fixture.overlays.openNowPlayingMore(target)
                fixture.dispatch(action, target)
                assertFalse(fixture.overlays.isNowPlayingMoreVisible.value)
                assertNull(fixture.overlays.nowPlayingMoreTarget)
            }
            assertTrue(fixture.events.isEmpty())
            assertTrue(fixture.artistNames.isEmpty())
        }
    }

    @Test
    fun sleepTimerDelegatesToTheExistingOverlayOpenerAfterMoreDismisses() {
        val target = song(1)
        val fixture = DispatcherFixture(target)
        composeRule.setContent { fixture.Content() }
        composeRule.runOnIdle {
            fixture.overlays.openNowPlayingMore(target)
            fixture.dispatch(NowPlayingAction.SLEEP_TIMER, target)
            assertEquals(listOf(DispatchEvent(NowPlayingAction.SLEEP_TIMER, null, 0)), fixture.events)
            assertTrue(fixture.overlays.isSleepTimerDialogVisible.value)
            assertFalse(fixture.overlays.isNowPlayingMoreVisible.value)
            assertNull(fixture.overlays.nowPlayingMoreTarget)
            assertEquals(PlayerPresentation.Collapsed, fixture.overlays.playerMorphState.targetPresentation)
        }
    }

    private data class DispatchEvent(val action: NowPlayingAction, val song: Song?, val generation: Int)

    private class DispatcherFixture(initialSong: Song) {
        val current = mutableStateOf<Song?>(initialSong)
        val library = mutableStateOf(listOf(initialSong))
        val favorites = mutableStateOf(emptySet<String>())
        val canPresent = mutableStateOf(true)
        val callbackGeneration = mutableStateOf(0)
        val events = mutableListOf<DispatchEvent>()
        val feedback = mutableListOf<NowPlayingFavoriteFeedback>()
        val feedbackGenerations = mutableListOf<Int>()
        val order = mutableListOf<String>()
        val artistNames = mutableListOf<String>()
        lateinit var overlays: MusicOverlayState
        lateinit var dispatch: (NowPlayingAction, Song) -> Unit

        private fun record(action: NowPlayingAction, song: Song?, generation: Int) {
            assertFalse("More must dismiss before $action", overlays.isNowPlayingMoreVisible.value)
            assertNull(overlays.nowPlayingMoreTarget)
            events += DispatchEvent(action, song, generation)
            order += action.name
        }

        @Composable
        fun Content() {
            overlays = rememberMusicOverlayState()
            val generation = callbackGeneration.value
            CompositionLocalProvider(
                LocalSongRatingUi provides SongRatingUiEnvironment(
                    onOpen = { record(NowPlayingAction.RATE_SONG, it, generation) }
                )
            ) {
                val workflows = rememberNowPlayingWorkflowOpeners(current.value, canPresent.value) {
                    record(NowPlayingAction.ADD_TO_PLAYLIST, it, generation)
                }
                val artist = rememberNowPlayingArtistNavigation(current.value, library.value) {
                    artistNames += it
                }
                val trackInfo = rememberNowPlayingTrackInfoOpener(current.value, overlays, canPresent.value)
                dispatch = rememberNowPlayingMoreActionDispatcher(
                    currentSong = current.value,
                    librarySongs = library.value,
                    favoriteMembershipKeys = favorites.value,
                    canPresent = canPresent.value,
                    overlayState = overlays,
                    onToggleFavorite = {
                        record(NowPlayingAction.FAVORITE, it, generation)
                        val key = it.membershipKey()
                        favorites.value = if (key in favorites.value) favorites.value - key
                            else favorites.value + key
                    },
                    onOpenAlbum = { record(NowPlayingAction.GO_TO_ALBUM, it, generation) },
                    onOpenLyrics = { record(NowPlayingAction.LYRICS, null, generation) },
                    onFavoriteFeedback = {
                        assertFalse(overlays.isNowPlayingMoreVisible.value)
                        assertNull(overlays.nowPlayingMoreTarget)
                        feedback += it
                        feedbackGenerations += generation
                        order += it.name
                    },
                    onOpenArtist = {
                        record(NowPlayingAction.GO_TO_ARTIST, it, generation)
                        artist(it)
                    },
                    onTrackInfoClick = {
                        record(NowPlayingAction.TRACK_INFORMATION, it, generation)
                        trackInfo(it)
                    },
                    onAddToPlaylist = workflows.addToPlaylist,
                    onRateSong = workflows.rateSong,
                    onOpenSleepTimer = {
                        record(NowPlayingAction.SLEEP_TIMER, null, generation)
                        overlays.isSleepTimerDialogVisible.value = true
                    }
                )
            }
        }
    }

    private fun song(id: Long) = Song(
        id, "Song $id", "Artist", "Album", 1, 120_000L,
        Uri.parse("content://media/external/audio/media/$id"), "/music/$id.flac", "/music", null,
        volumeName = "external"
    )
}
