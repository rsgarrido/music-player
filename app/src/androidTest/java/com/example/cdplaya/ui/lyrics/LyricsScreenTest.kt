package com.example.cdplaya.ui.lyrics

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.geometry.Offset
import com.example.cdplaya.data.Song
import com.example.cdplaya.lyrics.ActiveLyricGroup
import com.example.cdplaya.lyrics.LyricCue
import com.example.cdplaya.lyrics.LyricCueContent
import com.example.cdplaya.lyrics.LyricsDocument
import com.example.cdplaya.lyrics.LyricsPlaybackUiState
import com.example.cdplaya.lyrics.LyricsUnavailableReason
import com.example.cdplaya.lyrics.StaticLyricLine
import com.example.cdplaya.ui.player.rememberPlayerLyricsTransitionState
import com.example.cdplaya.ui.player.PlayerLyricsTransitionState
import com.example.cdplaya.ui.player.PlayerSurfaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LyricsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loadingStateRenders() {
        setContent(LyricsPlaybackUiState.Loading(song()))
        composeRule.onNodeWithText("Loading local lyrics…").assertExists()

    }

    @Test
    fun permissionStateRenders() {
        setContent(
            LyricsPlaybackUiState.Unavailable(
                song(),
                LyricsUnavailableReason.PermissionLost("content://root")
            )
        )
        composeRule.onNodeWithText("Lyrics folder access unavailable").assertExists()
    }

    @Test
    fun syncedLyricsRenderActiveSemanticsAndTapSeeks() {
        var seekPosition = -1
        val document = LyricsDocument.Synced(
            listOf(
                LyricCue(1_000, LyricCueContent.Text("First line")),
                LyricCue(2_000, LyricCueContent.Text("Second line"))
            )
        )
        setContent(
            LyricsPlaybackUiState.Synced(
                song = song(),
                lyrics = document,
                activeGroup = ActiveLyricGroup(1_000, listOf("First line")),
                autoFollowEnabled = true
            ),
            onSeek = { seekPosition = it }
        )

        composeRule.onNodeWithText("First line")
            .assertIsSelected()
            .assertHasClickAction()
        composeRule.onNodeWithText("Second line").performClick()
        composeRule.onNodeWithText("Second line").assertIsSelected()
        composeRule.runOnIdle { assertEquals(2_000, seekPosition) }
    }

    @Test
    fun unsyncedLyricsRenderWithoutSeekAction() {
        var seekCalled = false
        setContent(
            LyricsPlaybackUiState.Unsynced(
                song(),
                LyricsDocument.Unsynced(listOf(StaticLyricLine("Static lyric")))
            ),
            onSeek = { seekCalled = true }
        )

        composeRule.onNodeWithText("Unsynced lyrics").assertExists()
        composeRule.onNodeWithText("Static lyric").assertHasNoClickAction()
        composeRule.runOnIdle { assertTrue(!seekCalled) }
    }

    @Test
    fun manualModeShowsReturnAction() {
        var returned = false
        setContent(
            LyricsPlaybackUiState.Synced(
                song(),
                LyricsDocument.Synced(
                    listOf(LyricCue(1_000, LyricCueContent.Text("Line")))
                ),
                ActiveLyricGroup(1_000, listOf("Line")),
                autoFollowEnabled = false
            ),
            onReturn = { returned = true }
        )

        composeRule.onNodeWithTag(LyricsReturnTag).performClick()
        composeRule.runOnIdle { assertTrue(returned) }
    }

    @Test
    fun headerBackAndPlayPauseCallbacksWork() {
        var back = false
        var toggled = false
        setContent(
            LyricsPlaybackUiState.Loading(song()),
            onBack = { back = true },
            onPlayPause = { toggled = true }
        )

        composeRule.onNodeWithTag(LyricsPlayPauseTag).performClick()
        composeRule.onNodeWithTag(LyricsBackTag).performClick()
        composeRule.runOnIdle {
            assertTrue(back)
            assertTrue(toggled)
        }
    }

    @Test
    fun noMatchShowsMultipleDeterministicNames() {
        setContent(
            LyricsPlaybackUiState.Unavailable(
                song(),
                LyricsUnavailableReason.NotFound
            )
        )

        composeRule.onNodeWithText("No local lyrics found").assertExists()
        composeRule.onNodeWithText("track.lrc", substring = true).assertExists()
        composeRule.onNodeWithText("Song.lrc", substring = true).assertExists()
        composeRule.onNodeWithText("Artist - Song.lrc", substring = true).assertExists()
    }

    @Test
    fun headerDownwardDragSettlesAtExpanded() {
        lateinit var transitionState: PlayerLyricsTransitionState
        composeRule.setContent {
            MaterialTheme {
                transitionState = rememberPlayerLyricsTransitionState(true) {}
                LyricsScreen(
                    state = LyricsPlaybackUiState.Loading(song()),
                    isPlaying = false,
                    transitionState = transitionState,
                    interactive = true,
                    onBack = {},
                    onPlayPause = {},
                    onSeek = {},
                    onSuspendAutoFollow = {},
                    onReturnToCurrentLine = {},
                    onRescan = {},
                    onOpenSettings = {}
                )
            }
        }

        composeRule.onNodeWithTag(LyricsHeaderTag).performTouchInput {
            swipe(
                start = center,
                end = center + Offset(0f, 1_000f),
                durationMillis = 700
            )
        }
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.runOnIdle {
            assertEquals(PlayerSurfaceState.EXPANDED, transitionState.settledSurface)
            assertEquals(0f, transitionState.progress, 0.01f)
        }
    }

    private fun setContent(
        state: LyricsPlaybackUiState,
        onBack: () -> Unit = {},
        onPlayPause: () -> Unit = {},
        onSeek: (Int) -> Unit = {},
        onReturn: () -> Unit = {}
    ) {
        composeRule.setContent {
            MaterialTheme {
                val transitionState = rememberPlayerLyricsTransitionState(true) {}
                LyricsScreen(
                    state = state,
                    isPlaying = false,
                    transitionState = transitionState,
                    interactive = true,
                    onBack = onBack,
                    onPlayPause = onPlayPause,
                    onSeek = onSeek,
                    onSuspendAutoFollow = {},
                    onReturnToCurrentLine = onReturn,
                    onRescan = {},
                    onOpenSettings = {}
                )
            }
        }
    }

    private fun song() = Song(
        id = 1,
        title = "Song",
        artist = "Artist",
        album = "Album",
        trackNumber = 1,
        duration = 10_000,
        uri = Uri.parse("content://song"),
        filePath = "/Music/track.flac",
        folderPath = "/Music",
        albumArtUri = null,
        displayName = "track.flac",
        relativePath = "Music/"
    )
}
