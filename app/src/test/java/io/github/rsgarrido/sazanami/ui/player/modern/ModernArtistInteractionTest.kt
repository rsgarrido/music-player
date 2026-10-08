package io.github.rsgarrido.sazanami.ui.player.modern

import io.github.rsgarrido.sazanami.ui.player.PlayerLyricsTransitionState
import io.github.rsgarrido.sazanami.ui.player.PlayerMorphState
import io.github.rsgarrido.sazanami.ui.player.PlayerPresentation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ModernArtistInteractionTest {
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    @Test fun onlyAvailableSettledCurrentVisibleContentReceivesTheCallback() {
        val player = PlayerMorphState(PlayerPresentation.Expanded, scope)
        val lyrics = PlayerLyricsTransitionState(false, scope) {}
        val click: () -> Unit = {}
        assertSame(click, modernArtistClickCallback(click, player, lyrics, 0f, true, true))
        assertNull(modernArtistClickCallback(null, player, lyrics, 0f, true, true))
        assertNull(modernArtistClickCallback(click, player, lyrics, 0f, false, true))
        assertNull(modernArtistClickCallback(click, player, lyrics, 0f, true, false))
        assertNull(modernArtistClickCallback(click, player, lyrics, -120f, true, true))
        assertNull(modernArtistClickCallback(click, player, lyrics, 120f, true, true))
        assertNull(modernArtistClickCallback(click,
            PlayerMorphState(PlayerPresentation.Collapsed, scope), lyrics, 0f, true, true))
        player.beginDrag(1_000f)
        assertNull(modernArtistClickCallback(click, player, lyrics, 0f, true, true))
    }

    @Test fun lyricsDragAndLyricsInputOwnershipRemoveArtistInteraction() {
        val player = PlayerMorphState(PlayerPresentation.Expanded, scope)
        val lyrics = PlayerLyricsTransitionState(false, scope) {}
        val click: () -> Unit = {}
        lyrics.beginOpeningDrag()
        assertNull(modernArtistClickCallback(click, player, lyrics, 0f, true, true))
        assertNull(modernArtistClickCallback(click, player,
            PlayerLyricsTransitionState(true, scope) {}, 0f, true, true))
    }
}
