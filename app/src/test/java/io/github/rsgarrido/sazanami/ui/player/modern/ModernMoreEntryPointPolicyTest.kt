package io.github.rsgarrido.sazanami.ui.player.modern

import io.github.rsgarrido.sazanami.ui.player.PlayerLyricsTransitionState
import io.github.rsgarrido.sazanami.ui.player.PlayerMorphState
import io.github.rsgarrido.sazanami.ui.player.PlayerPresentation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModernMoreEntryPointPolicyTest {
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    @Test
    fun onlySettledExpandedCurrentContentCanExposeMore() {
        val player = PlayerMorphState(PlayerPresentation.Expanded, scope)
        val lyrics = PlayerLyricsTransitionState(false, scope) {}
        assertTrue(canOpenModernMore(player, lyrics, 0f, true, true))
        assertFalse(canOpenModernMore(player, lyrics, 0f, false, true))
        assertFalse(canOpenModernMore(player, lyrics, 0f, true, false))
        assertFalse(canOpenModernMore(player, lyrics, -120f, true, true))
        assertFalse(canOpenModernMore(player, lyrics, 120f, true, true))
        assertFalse(canOpenModernMore(
            PlayerMorphState(PlayerPresentation.Collapsed, scope), lyrics, 0f, true, true
        ))
        player.beginDrag(1_000f)
        assertFalse(canOpenModernMore(player, lyrics, 0f, true, true))
    }

    @Test
    fun lyricsDraggingOrOwningInputSuppressesMore() {
        val player = PlayerMorphState(PlayerPresentation.Expanded, scope)
        val lyrics = PlayerLyricsTransitionState(false, scope) {}
        lyrics.beginOpeningDrag()
        assertFalse(canOpenModernMore(player, lyrics, 0f, true, true))
        assertFalse(canOpenModernMore(
            player, PlayerLyricsTransitionState(true, scope) {}, 0f, true, true
        ))
    }
}
