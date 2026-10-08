package io.github.rsgarrido.sazanami.ui.player.modern

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.ui.player.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DefaultPlayerArtworkInteractionTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var player: PlayerMorphState
    private lateinit var lyrics: PlayerLyricsTransitionState
    private lateinit var carousel: ModernArtworkCarouselPresentation
    private val target = mutableStateOf(song(1))
    private val visibleOwner = mutableStateOf(true)
    private val opened = mutableListOf<Song>()
    private var nextCount = 0

    private fun content() {
        composeRule.setContent {
            MaterialTheme {
                val scope = rememberCoroutineScope()
                player = remember { PlayerMorphState(PlayerPresentation.Expanded, scope) }
                lyrics = remember { PlayerLyricsTransitionState(false, scope) {} }
                carousel = rememberModernArtworkCarouselPresentation(target.value, song(2), song(3), {}, { nextCount++ })
                SideEffect { carousel.state.updateArtworkWidth(280) }
                DefaultPlayerMorph(
                    progress = player.progress,
                    geometry = if (visibleOwner.value) DefaultPlayerMorphGeometry(
                        Rect(0f, 0f, 400f, 800f), Rect(40f, 50f, 320f, 330f),
                        Rect(16f, 350f, 380f, 450f), Rect(180f, 500f, 260f, 580f)) else null,
                    carouselPresentation = carousel, artworkTransitionStyle = ModernArtworkTransitionStyle.SLIDE,
                    isPlaying = false, onPlayPauseClick = {}, style = ModernPlayerDefaults.style(),
                    appearance = ModernPlayerAppearance.Default,
                    artworkPalette = ModernArtworkPalette.fallback(ModernPlayerDefaults.style().accentColor),
                    expandedArtworkRequestSizePx = 280,
                    onViewArtwork = modernArtworkClickCallback({ opened += it }, carousel.songs.current, target.value,
                        player, lyrics, carousel.state, visibleOwner.value),
                    artworkGestureModifier = rememberModernArtworkHorizontalDragModifier(carousel.state, carousel.songs.current.id, !lyrics.lyricsInteractive)
                        .then(rememberModernPlayerVerticalDragModifier(player, lyrics, 800f, 400f))
                ) { Box(Modifier.fillMaxSize()) }
            }
        }
    }

    @Test fun onlyCurrentSettledArtworkExposesLocalizedViewAction() {
        content()
        composeRule.onAllNodesWithTag(ArtworkViewerEntryTag).assertCountEquals(1)
        val config = composeRule.onNodeWithTag(ArtworkViewerEntryTag).fetchSemanticsNode().config
        assertEquals(Role.Button, config[SemanticsProperties.Role])
        assertEquals(composeRule.activity.getString(R.string.player_view_artwork), config[SemanticsActions.OnClick].label)
        composeRule.onNodeWithTag(ArtworkViewerEntryTag).performClick()
        composeRule.runOnIdle { assertSame(target.value, opened.single()); carousel.state.startDrag() }
        composeRule.onAllNodesWithTag(ArtworkViewerEntryTag).assertCountEquals(0)
        composeRule.runOnIdle { carousel.state.resetForSongChange(); visibleOwner.value = false }
        composeRule.onAllNodesWithTag(ArtworkViewerEntryTag).assertCountEquals(0)
        composeRule.runOnIdle { visibleOwner.value = true; target.value = target.value.copy(albumArtUri = null) }
        composeRule.onAllNodesWithTag(ArtworkViewerEntryTag).assertCountEquals(0)
    }

    @Test fun horizontalDragStillNavigatesWithoutOpeningTheViewer() {
        content()
        composeRule.onNodeWithTag(ArtworkViewerEntryTag).performTouchInput {
            swipe(center, center - Offset(150f, 0f), durationMillis = 400)
        }
        composeRule.runOnIdle { assertEquals(1, nextCount); assertTrue(opened.isEmpty()) }
    }

    @Test fun verticalArtworkDragsStillCollapseAndOpenLyricsWithoutViewerTaps() {
        content()
        composeRule.onNodeWithTag(ArtworkViewerEntryTag).performTouchInput {
            swipe(center, center + Offset(0f, 300f), durationMillis = 400)
        }
        composeRule.runOnIdle { assertEquals(PlayerPresentation.Collapsed, player.settledPresentation); player.snapTo(PlayerPresentation.Expanded) }
        composeRule.onNodeWithTag(ArtworkViewerEntryTag).performTouchInput {
            swipe(center, center - Offset(0f, 250f), durationMillis = 400)
        }
        composeRule.runOnIdle { assertEquals(PlayerSurfaceState.LYRICS, lyrics.settledSurface); assertTrue(opened.isEmpty()) }
    }

    @Test fun morphAndLyricsOwnershipRemoveTheArtworkAction() {
        content()
        composeRule.runOnIdle { player.updateProgressFromDrag(0.5f) }
        composeRule.onAllNodesWithTag(ArtworkViewerEntryTag).assertCountEquals(0)
        composeRule.runOnIdle { player.snapTo(PlayerPresentation.Expanded); lyrics.beginOpeningDrag() }
        composeRule.onAllNodesWithTag(ArtworkViewerEntryTag).assertCountEquals(0)
    }

    private fun song(id: Long) = Song(id, "Song $id", "Artist", "Album", 1, 120_000,
        Uri.parse("content://media/$id"), "/music/$id.flac", "/music", Uri.parse("content://art/$id"))
}
