package io.github.rsgarrido.sazanami.ui.player

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.data.membershipKey
import io.github.rsgarrido.sazanami.player.RepeatMode
import io.github.rsgarrido.sazanami.ui.player.modern.ModernExpandedPlayer
import io.github.rsgarrido.sazanami.ui.player.modern.ModernPlayerAppearance
import io.github.rsgarrido.sazanami.ui.player.modern.ModernArtworkCarouselPresentation
import io.github.rsgarrido.sazanami.ui.player.modern.rememberModernArtworkCarouselPresentation
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NowPlayingMoreDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun compactHeaderAndFavoriteLabelFollowTheSharedFavoriteState() {
        val target = song(1).copy(artist = "", album = "")
        val favorites = mutableStateOf(emptySet<String>())
        composeRule.setContent {
            MaterialTheme {
                NowPlayingMoreDialog(
                    target, nowPlayingActions(target, favorites.value, listOf(target)), {}, {}
                )
            }
        }
        composeRule.onNodeWithText("Song 1").assertExists()
        composeRule.onNodeWithText("Unknown Artist · Unknown Album").assertExists()
        composeRule.onNodeWithText("Add to favorites").assertExists()
        composeRule.onNodeWithText("Go to album").assertExists()
        composeRule.onNodeWithText("Go to artist").assertDoesNotExist()
        composeRule.onNodeWithText("Lyrics").assertExists()
        composeRule.runOnIdle { favorites.value = setOf(target.membershipKey()) }
        composeRule.onNodeWithText("Add to favorites").assertDoesNotExist()
        composeRule.onNodeWithText("Remove from favorites").assertExists()
    }

    @Test
    fun backDismissesTheDialogBeforeTheUnderlyingPlayerBackHandler() {
        val visible = mutableStateOf(true)
        var playerBackCount = 0
        composeRule.setContent {
            MaterialTheme {
                BackHandler { playerBackCount++ }
                if (visible.value) {
                    val target = song(1)
                    NowPlayingMoreDialog(
                        target, nowPlayingActions(target, emptySet(), listOf(target)),
                        onDismiss = { visible.value = false }, onAction = {}
                    )
                }
            }
        }
        composeRule.onNodeWithTag(NowPlayingMoreDialogTag).assertExists()
        Espresso.pressBack()
        composeRule.onNodeWithTag(NowPlayingMoreDialogTag).assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(0, playerBackCount) }
    }

    @Test
    fun outsideTapDismissesWithoutClickingThePlayerBehindTheDialog() {
        val visible = mutableStateOf(true)
        var playerClickCount = 0
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize().clickable { playerClickCount++ })
                if (visible.value) {
                    val target = song(1)
                    NowPlayingMoreDialog(
                        target, nowPlayingActions(target, emptySet(), listOf(target)),
                        onDismiss = { visible.value = false }, onAction = {}
                    )
                }
            }
        }
        composeRule.onNodeWithTag(NowPlayingMoreDialogTag).assertExists()
        Espresso.onView(isRoot()).inRoot(isDialog()).perform(GeneralClickAction(
            Tap.SINGLE,
            CoordinatesProvider { view ->
                floatArrayOf(8f, view.resources.displayMetrics.heightPixels / 2f)
            },
            Press.FINGER
        ))
        composeRule.onNodeWithTag(NowPlayingMoreDialogTag).assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(0, playerClickCount) }
    }

    @Test
    fun actionTapDoesNotReachThePlayerBehindTheDialog() {
        val visible = mutableStateOf(true)
        var playerClickCount = 0
        var actionCount = 0
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize().clickable { playerClickCount++ })
                if (visible.value) {
                    val target = song(1)
                    NowPlayingMoreDialog(
                        target, nowPlayingActions(target, emptySet(), listOf(target)),
                        onDismiss = { visible.value = false },
                        onAction = {
                            assertEquals(NowPlayingAction.FAVORITE, it)
                            actionCount++
                            visible.value = false
                        }
                    )
                }
            }
        }
        composeRule.onNodeWithText("Add to favorites").performTouchInput { click(center) }
        composeRule.onNodeWithTag(NowPlayingMoreDialogTag).assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(1, actionCount)
            assertEquals(0, playerClickCount)
        }
    }

    @Test
    fun lyricsDismissesMoreAndOpensTheExistingLyricsTransition() {
        val visible = mutableStateOf(true)
        lateinit var lyrics: PlayerLyricsTransitionState
        composeRule.setContent {
            MaterialTheme {
                lyrics = rememberPlayerLyricsTransitionState(false) {}
                if (visible.value) {
                    val target = song(1)
                    NowPlayingMoreDialog(
                        target, nowPlayingActions(target, emptySet(), listOf(target)),
                        onDismiss = { visible.value = false },
                        onAction = { action ->
                            performNowPlayingAction(
                                action, target, target, listOf(target),
                                onDismiss = { visible.value = false },
                                onToggleFavorite = {}, onOpenAlbum = {},
                                onOpenLyrics = lyrics::openLyrics
                            )
                        }
                    )
                }
            }
        }
        composeRule.onNodeWithText("Lyrics").performClick()
        composeRule.onNodeWithTag(NowPlayingMoreDialogTag).assertDoesNotExist()
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.runOnIdle { assertEquals(PlayerSurfaceState.LYRICS, lyrics.settledSurface) }
    }

    @Test
    fun defaultPlayerHasOneMoreTriggerWithPreviewTracksAndSuppressesItDuringCarouselDrag() {
        lateinit var carousel: ModernArtworkCarouselPresentation
        var moreCount = 0
        composeRule.setContent {
            MaterialTheme {
                val scope = rememberCoroutineScope()
                val target = remember { song(1) }
                val player = remember { PlayerMorphState(PlayerPresentation.Expanded, scope) }
                val lyrics = rememberPlayerLyricsTransitionState(false) {}
                carousel = rememberModernArtworkCarouselPresentation(
                    target, song(2), song(3), {}, {}
                )
                ModernExpandedPlayer(
                    currentSong = target,
                    previousPreviewSong = song(2), nextPreviewSong = song(3),
                    appearance = ModernPlayerAppearance.Default.copy(
                        layout = ModernPlayerAppearance.Default.layout.copy(showAudioQualityBadge = false)
                    ),
                    isPlaying = false, isShuffleEnabled = false, repeatMode = RepeatMode.OFF,
                    currentPosition = 0, duration = 120_000, isCurrentSongFavorite = false,
                    onPlayPauseClick = {}, onPreviousClick = {}, onNextClick = {},
                    onSeekChange = {}, onShuffleClick = {}, onRepeatClick = {}, onCollapseClick = {},
                    playerMorphState = player, lyricsTransitionState = lyrics,
                    onOpenUpNextClick = {}, onToggleFavoriteClick = {},
                    onOpenMoreClick = { moreCount++ }, carouselPresentation = carousel
                )
            }
        }
        composeRule.onAllNodesWithContentDescription("More actions").assertCountEquals(1)
        composeRule.onNodeWithContentDescription("More actions").performClick()
        composeRule.runOnIdle {
            assertEquals(1, moreCount)
            carousel.state.updateArtworkWidth(300)
            carousel.state.startDrag()
            carousel.state.dragBy(-100f)
        }
        composeRule.onAllNodesWithContentDescription("More actions").assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription("Open queues").assertCountEquals(1)
    }

    @Test
    fun libraryArtistRowUsesTheSharedActionAndDismissesBeforeNavigation() {
        val target = song(1)
        val visible = mutableStateOf(true)
        val events = mutableListOf<String>()
        composeRule.setContent {
            MaterialTheme {
                if (visible.value) NowPlayingMoreDialog(
                    target, nowPlayingActions(target, emptySet(), listOf(target)),
                    onDismiss = { visible.value = false },
                    onAction = { action ->
                        performNowPlayingAction(
                            action, target, target, listOf(target),
                            onDismiss = { events += "dismiss"; visible.value = false },
                            onToggleFavorite = {}, onOpenAlbum = {}, onOpenLyrics = {},
                            onOpenArtist = { events += "artist" },
                            onFavoriteFeedback = { error("Artist must not emit Favorite feedback") }
                        )
                    }
                )
            }
        }
        composeRule.onNodeWithText("Go to artist").performClick()
        composeRule.onNodeWithTag(NowPlayingMoreDialogTag).assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(listOf("dismiss", "artist"), events) }
    }

    private fun song(id: Long) = Song(
        id, "Song $id", "Artist", "Album", 1, 120_000L,
        Uri.parse("content://media/external/audio/media/$id"), "/music/$id.flac", "/music", null,
        volumeName = "external"
    )
}
