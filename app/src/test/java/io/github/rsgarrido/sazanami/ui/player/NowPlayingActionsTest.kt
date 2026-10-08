package io.github.rsgarrido.sazanami.ui.player

import android.net.Uri
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.data.membershipKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class NowPlayingActionsTest {
    @Test
    fun favoriteActionTracksTheSharedMembershipSet() {
        val target = song(1)
        val add = nowPlayingActions(target, emptySet(), listOf(target)).first()
        val remove = nowPlayingActions(target, setOf(target.membershipKey()), listOf(target)).first()
        assertEquals(NowPlayingAction.FAVORITE, add.action)
        assertEquals(R.string.player_add_favorite, add.labelRes)
        assertFalse(add.isActive)
        assertEquals(R.string.player_remove_favorite, remove.labelRes)
        assertTrue(remove.isActive)
    }

    @Test
    fun albumRequiresMembershipRatherThanMatchingAlbumTextAndLyricsRemainAvailable() {
        val target = song(1)
        val differentFile = song(2).copy(folderPath = "/other", filePath = "/other/2.flac")
        assertNull(resolveNowPlayingAlbumKey(target, listOf(differentFile)))
        assertEquals(
            listOf(NowPlayingAction.FAVORITE, NowPlayingAction.LYRICS),
            nowPlayingActions(target, emptySet(), listOf(differentFile)).map { it.action }
        )
        assertEquals(
            listOf(NowPlayingAction.FAVORITE, NowPlayingAction.GO_TO_ALBUM, NowPlayingAction.LYRICS),
            nowPlayingActions(target, emptySet(), listOf(target, differentFile)).map { it.action }
        )
    }

    @Test
    fun everyActionDismissesBeforeInvokingItsExistingCallbackWithTheCapturedSong() {
        val target = song(1)
        NowPlayingAction.entries.forEach { action ->
            val events = mutableListOf<String>()
            val performed = performNowPlayingAction(
                action, target, target, listOf(target),
                onDismiss = { events += "dismiss" },
                onToggleFavorite = { assertSame(target, it); events += "favorite" },
                onOpenAlbum = { assertSame(target, it); events += "album" },
                onOpenLyrics = { events += "lyrics" }
            )
            assertTrue(performed)
            assertEquals(listOf("dismiss", when (action) {
                NowPlayingAction.FAVORITE -> "favorite"
                NowPlayingAction.GO_TO_ALBUM -> "album"
                NowPlayingAction.LYRICS -> "lyrics"
            }), events)
        }
    }

    @Test
    fun changedTrackIncludingSameIdOnAnotherVolumeCannotDispatchStaleActions() {
        val target = song(1)
        listOf(null, song(2), target.copy(volumeName = "other_volume")).forEach { current ->
            NowPlayingAction.entries.forEach { action ->
                var dismissals = 0
                val performed = performNowPlayingAction(
                    action, target, current, listOf(target),
                    onDismiss = { dismissals++ },
                    onToggleFavorite = { error("Stale favorite") },
                    onOpenAlbum = { error("Stale navigation") },
                    onOpenLyrics = { error("Stale lyrics") }
                )
                assertFalse(performed)
                assertEquals(1, dismissals)
            }
        }
    }

    @Test
    fun albumRemovedAfterOpeningCannotNavigate() {
        val target = song(1)
        assertTrue(nowPlayingActions(target, emptySet(), listOf(target)).any {
            it.action == NowPlayingAction.GO_TO_ALBUM
        })
        var dismissed = false
        assertFalse(performNowPlayingAction(
            NowPlayingAction.GO_TO_ALBUM, target, target, emptyList(),
            onDismiss = { dismissed = true },
            onToggleFavorite = {},
            onOpenAlbum = { error("Removed album must not navigate") },
            onOpenLyrics = {}
        ))
        assertTrue(dismissed)
    }

    private fun song(id: Long) = Song(
        id, "Song", "Artist", "Album", 1, 120_000L, mock(Uri::class.java),
        "/music/$id.flac", "/music", null, volumeName = "external"
    )
}
