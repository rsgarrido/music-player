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
            listOf(NowPlayingAction.FAVORITE, NowPlayingAction.GO_TO_ARTIST, NowPlayingAction.LYRICS),
            nowPlayingActions(target, emptySet(), listOf(differentFile)).map { it.action }
        )
        assertEquals(
            listOf(NowPlayingAction.FAVORITE, NowPlayingAction.GO_TO_ARTIST,
                NowPlayingAction.GO_TO_ALBUM, NowPlayingAction.LYRICS),
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
                onOpenLyrics = { events += "lyrics" },
                onOpenArtist = { assertSame(target, it); events += "artist" }
            )
            assertTrue(performed)
            assertEquals(listOf("dismiss", when (action) {
                NowPlayingAction.FAVORITE -> "favorite"
                NowPlayingAction.GO_TO_ARTIST -> "artist"
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
                    onOpenLyrics = { error("Stale lyrics") },
                    onOpenArtist = { error("Stale artist") }
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

    @Test
    fun favoriteFeedbackUsesSelectionStateAndFollowsDismissalAndTheOptimisticToggle() {
        val target = song(1)
        listOf(false, true).forEach { initiallyFavorite ->
            val favoriteKeys = mutableSetOf<String>()
            if (initiallyFavorite) favoriteKeys += target.membershipKey()
            val events = mutableListOf<String>()
            var feedback: NowPlayingFavoriteFeedback? = null
            assertTrue(performNowPlayingAction(
                action = NowPlayingAction.FAVORITE,
                target = target,
                currentSong = target,
                librarySongs = listOf(target),
                onDismiss = { events += "dismiss" },
                onToggleFavorite = {
                    assertSame(target, it)
                    if (initiallyFavorite) favoriteKeys -= it.membershipKey()
                    else favoriteKeys += it.membershipKey()
                    events += "toggle"
                },
                onOpenAlbum = { error("Unexpected album action") },
                onOpenLyrics = { error("Unexpected lyrics action") },
                isFavorite = target.membershipKey() in favoriteKeys,
                onFavoriteFeedback = {
                    feedback = it
                    events += "feedback"
                }
            ))
            assertEquals(listOf("dismiss", "toggle", "feedback"), events)
            assertEquals(!initiallyFavorite, target.membershipKey() in favoriteKeys)
            assertEquals(
                if (initiallyFavorite) NowPlayingFavoriteFeedback.REMOVED_FROM_FAVORITES
                else NowPlayingFavoriteFeedback.ADDED_TO_FAVORITES,
                feedback
            )
        }
    }

    @Test
    fun detailAndLyricsActionsNeverEmitFavoriteFeedback() {
        val target = song(1)
        listOf(NowPlayingAction.GO_TO_ARTIST, NowPlayingAction.GO_TO_ALBUM,
            NowPlayingAction.LYRICS).forEach { action ->
            val events = mutableListOf<String>()
            assertTrue(performNowPlayingAction(
                action, target, target, listOf(target),
                onDismiss = { events += "dismiss" },
                onToggleFavorite = { error("Unexpected favorite action") },
                onOpenAlbum = { events += "album" },
                onOpenLyrics = { events += "lyrics" },
                isFavorite = true,
                onFavoriteFeedback = { error("Only Favorite emits this feedback") },
                onOpenArtist = { events += "artist" }
            ))
            assertEquals(listOf("dismiss", when (action) {
                NowPlayingAction.GO_TO_ARTIST -> "artist"
                NowPlayingAction.GO_TO_ALBUM -> "album"
                else -> "lyrics"
            }), events)
        }
    }

    @Test
    fun staleFavoriteTargetDoesNotToggleOrEmitFeedback() {
        var dismissed = false
        assertFalse(performNowPlayingAction(
            NowPlayingAction.FAVORITE, song(1), song(2), listOf(song(1)),
            onDismiss = { dismissed = true },
            onToggleFavorite = { error("Stale favorite") },
            onOpenAlbum = {}, onOpenLyrics = {},
            onFavoriteFeedback = { error("Stale action must not confirm success") }
        ))
        assertTrue(dismissed)
    }

    @Test
    fun artistResolutionUsesLibraryIdentityAndItsDisplayName() {
        val target = song(1).copy(artist = "THE WARNING")
        val library = listOf(song(2).copy(artist = "  The   Warning "),
            song(3).copy(artist = "the warning"))
        assertEquals("The Warning", resolveNowPlayingArtistName(target, library))
        val action = nowPlayingActions(target, emptySet(), library).first {
            it.action == NowPlayingAction.GO_TO_ARTIST
        }
        assertEquals(R.string.library_search_go_artist, action.labelRes)
        assertNull(resolveNowPlayingArtistName(target, emptyList()))
    }

    @Test
    fun blankAndRecognizedSentinelsCannotNavigateEvenIfTheyHaveLibraryGroups() {
        listOf("", " \t ", "Unknown Artist", " UNKNOWN   ARTIST ", "<unknown>",
            "<UNKNOWN>", "unknown", "Artista desconocido").forEach { artist ->
            val target = song(1).copy(artist = artist)
            assertNull(resolveNowPlayingArtistName(target, listOf(target)))
            assertFalse(nowPlayingActions(target, emptySet(), listOf(target)).any {
                it.action == NowPlayingAction.GO_TO_ARTIST
            })
        }
    }

    @Test
    fun variousArtistsRequiresARealMatchingGroupAndCollaborationsAreNeverSplit() {
        val various = song(1).copy(artist = "Various Artists")
        assertNull(resolveNowPlayingArtistName(various, listOf(song(2))))
        assertEquals("Various Artists", resolveNowPlayingArtistName(various, listOf(various)))
        val collaboration = song(3).copy(artist = "Artist A feat. Artist B")
        assertNull(resolveNowPlayingArtistName(collaboration, listOf(
            song(4).copy(artist = "Artist A"), song(5).copy(artist = "Artist B")
        )))
        assertEquals("Artist A feat. Artist B",
            resolveNowPlayingArtistName(collaboration, listOf(collaboration)))
    }

    @Test
    fun compilationNavigationUsesTrackArtistAndNeverFallsBackToAlbumArtist() {
        val target = song(1).copy(artist = "Track Artist", albumArtist = "Various Artists")
        val albumArtistTrack = song(2).copy(artist = "Various Artists")
        assertEquals("Track Artist", resolveNowPlayingArtistName(target, listOf(target, albumArtistTrack)))
        assertNull(resolveNowPlayingArtistName(target.copy(artist = ""), listOf(albumArtistTrack)))
        assertNull(resolveNowPlayingArtistName(target, listOf(albumArtistTrack)))
    }

    @Test
    fun artistRemovedAfterOpeningOrChangedToUnknownDismissesWithoutNavigating() {
        val target = song(1)
        assertTrue(nowPlayingActions(target, emptySet(), listOf(target)).any {
            it.action == NowPlayingAction.GO_TO_ARTIST
        })
        listOf(target to emptyList<Song>(), target.copy(artist = "<unknown>") to listOf(target))
            .forEach { (current, library) ->
                var dismissed = false
                assertFalse(performNowPlayingAction(
                    NowPlayingAction.GO_TO_ARTIST, target, current, library,
                    onDismiss = { dismissed = true }, onToggleFavorite = {},
                    onOpenAlbum = {}, onOpenLyrics = {},
                    onOpenArtist = { error("Unresolved artist must not navigate") }
                ))
                assertTrue(dismissed)
            }
    }

    @Test
    fun sharedArtistOpeningRevalidatesCurrentTrackLibraryAndLatestMetadata() {
        val target = song(1)
        listOf(null, song(2), target.copy(volumeName = "other_volume")).forEach { current ->
            assertFalse(openCurrentNowPlayingArtist(target, current, listOf(target)) {
                error("Stale track must not navigate")
            })
        }
        assertFalse(openCurrentNowPlayingArtist(target, target, emptyList()) {
            error("Removed artist must not navigate")
        })
        val updated = target.copy(artist = "Updated Artist")
        var opened: String? = null
        assertTrue(openCurrentNowPlayingArtist(target, updated, listOf(updated)) { opened = it })
        assertEquals("Updated Artist", opened)
    }

    private fun song(id: Long) = Song(
        id, "Song", "Artist", "Album", 1, 120_000L, mock(Uri::class.java),
        "/music/$id.flac", "/music", null, volumeName = "external"
    )
}
