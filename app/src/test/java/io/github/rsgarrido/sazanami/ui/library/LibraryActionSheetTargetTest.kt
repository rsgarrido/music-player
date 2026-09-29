package io.github.rsgarrido.sazanami.ui.library

import android.net.Uri
import io.github.rsgarrido.sazanami.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.data.artistIdentity
import io.github.rsgarrido.sazanami.data.UNKNOWN_ARTIST_IDENTITY
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class LibraryActionSheetTargetTest {
    @Test
    fun artistPictureActionsReflectAssignmentAndUnknownArtistPolicy() {
        val song = testSong()
        val noOp: (String, List<Song>) -> Unit = { _, _ -> }
        var chosen = false
        var removed = false
        val customTarget = artistActionSheetTarget(
            artistName = "Artist",
            subtitle = "1 song",
            artworkUri = null,
            artistIdentity = artistIdentity("Artist"),
            hasCustomPicture = true,
            onChoosePicture = { chosen = true },
            onRemovePicture = { removed = true },
            artistSongs = listOf(song),
            onPlayClick = noOp,
            onShuffleClick = noOp,
            onPlayNextClick = noOp,
            onAddToQueueClick = noOp,
            onAddToPlaylistClick = noOp,
            resolveString = ::testActionString,
            resolveArtworkDescription = { "Artwork for $it" }
        )
        customTarget.actions.first { it.label == "Change artist picture" }.onClick()
        customTarget.actions.first { it.label == "Remove artist picture" }.onClick()
        assertEquals(true, chosen)
        assertEquals(true, removed)

        val unknownTarget = artistActionSheetTarget(
            artistName = "Unknown Artist",
            subtitle = "1 song",
            artworkUri = null,
            artistIdentity = UNKNOWN_ARTIST_IDENTITY,
            hasCustomPicture = false,
            onChoosePicture = {},
            onRemovePicture = {},
            artistSongs = listOf(song),
            onPlayClick = noOp,
            onShuffleClick = noOp,
            onPlayNextClick = noOp,
            onAddToQueueClick = noOp,
            onAddToPlaylistClick = noOp,
            resolveString = ::testActionString,
            resolveArtworkDescription = { "Artwork for $it" }
        )
        assertEquals(
            emptyList<String>(),
            unknownTarget.actions.map { it.label }.filter { "artist picture" in it.lowercase() }
        )
    }

    @Test
    fun songTargetPreservesExistingActionsAndCallbacks() {
        val song = testSong()
        val invokedActions = mutableListOf<String>()

        val target = songActionSheetTarget(
            song = song,
            wasRecentlyAdded = false,
            isFavorite = false,
            onPlayNextClick = { invokedActions += "play_next" },
            onAddToQueueClick = { invokedActions += "queue" },
            onAddToAnotherQueueClick = { invokedActions += "another" },
            onPlayInNewQueueClick = { invokedActions += "new" },
            onToggleFavoriteClick = { invokedActions += "favorite" },
            onAddToPlaylistClick = { invokedActions += "playlist" },
            onEditSongTagsClick = { invokedActions += "edit" },
            rateSongLabel = "Rate song",
            onRateSongClick = { invokedActions += "rate" },
            resolveString = Int::toString,
            resolveArtworkDescription = { it }
        )

        assertEquals(
            listOf(
                R.string.playlist_play_next.toString(),
                R.string.playlist_add_to_queue.toString(),
                R.string.playlist_add_to_another_queue.toString(),
                R.string.playlist_play_in_new_queue.toString(),
                R.string.playlist_add_favorites.toString(),
                "Rate song",
                R.string.library_song_add_to_playlist.toString(),
                R.string.playlist_edit_tags.toString()
            ),
            target.actions.map { action -> action.label }
        )

        target.actions.forEach { action -> action.onClick() }
        assertEquals(
            listOf("play_next", "queue", "another", "new", "favorite", "rate", "playlist", "edit"),
            invokedActions
        )
    }

    @Test
    fun collectionTargetsPreserveExistingActionOrder() {
        val song = testSong()
        val noOp: (String, List<Song>) -> Unit = { _, _ -> }

        val albumTarget = albumActionSheetTarget(
            albumTitle = "Album",
            subtitle = "Artist • 1 song",
            artworkUri = null,
            albumSongs = listOf(song),
            onPlayClick = noOp,
            onShuffleClick = noOp,
            onPlayNextClick = noOp,
            onAddToQueueClick = noOp,
            onAddToPlaylistClick = noOp,
            resolveString = ::testActionString,
            resolveArtworkDescription = { "Album art for $it" }
        )
        val artistTarget = artistActionSheetTarget(
            artistName = "Artist",
            subtitle = "1 song",
            artworkUri = null,
            artistIdentity = artistIdentity("Artist"),
            hasCustomPicture = false,
            onChoosePicture = {},
            onRemovePicture = {},
            artistSongs = listOf(song),
            onPlayClick = noOp,
            onShuffleClick = noOp,
            onPlayNextClick = noOp,
            onAddToQueueClick = noOp,
            onAddToPlaylistClick = noOp,
            resolveString = ::testActionString,
            resolveArtworkDescription = { "Artwork for $it" }
        )
        val expected = listOf(
            "Play",
            "Shuffle",
            "Play next",
            "Add to queue",
            "Add to another queue...",
            "Play in new queue",
            "Add to playlist"
        )

        assertEquals(expected, albumTarget.actions.map { action -> action.label })
        assertEquals(expected + "Set artist picture", artistTarget.actions.map { action -> action.label })
    }

    @Test
    fun albumQueueActionsForwardTheDeterministicSongOrder() {
        val songs = listOf(testSong(3L), testSong(1L), testSong(2L))
        var newQueueSongs = emptyList<Song>()
        var anotherQueueSongs = emptyList<Song>()
        val noOp: (String, List<Song>) -> Unit = { _, _ -> }
        val target = albumActionSheetTarget(
            albumTitle = "Album",
            subtitle = "Artist",
            artworkUri = null,
            albumSongs = songs,
            onPlayClick = noOp,
            onShuffleClick = noOp,
            onPlayNextClick = noOp,
            onAddToQueueClick = noOp,
            onAddToAnotherQueueClick = { anotherQueueSongs = it },
            onPlayInNewQueueClick = { _, selectedSongs -> newQueueSongs = selectedSongs },
            onAddToPlaylistClick = noOp,
            resolveString = ::testActionString,
            resolveArtworkDescription = { "Album art for $it" }
        )

        target.actions.first { it.label == "Add to another queue..." }.onClick()
        target.actions.first { it.label == "Play in new queue" }.onClick()

        assertEquals(listOf(3L, 1L, 2L), anotherQueueSongs.map { it.id })
        assertEquals(listOf(3L, 1L, 2L), newQueueSongs.map { it.id })
    }

    @Test
    fun homePinActionCanBeInjectedWithoutChangingExistingCollectionActions() {
        val song = testSong()
        val noOp: (String, List<Song>) -> Unit = { _, _ -> }
        var pinInvoked = false
        val pinAction = LibraryItemAction(
            label = "Pin to Home",
            icon = Icons.Filled.PushPin,
            onClick = { pinInvoked = true }
        )

        val songTarget = songActionSheetTarget(
            song = song,
            wasRecentlyAdded = false,
            isFavorite = false,
            onPlayNextClick = {},
            onAddToQueueClick = {},
            onToggleFavoriteClick = {},
            onAddToPlaylistClick = {},
            onEditSongTagsClick = {},
            rateSongLabel = "Rate song",
            onRateSongClick = {},
            homePinAction = pinAction,
            resolveString = Int::toString,
            resolveArtworkDescription = { it }
        )
        val albumTarget = albumActionSheetTarget(
            albumTitle = "Album",
            subtitle = "Artist • 1 song",
            artworkUri = null,
            albumSongs = listOf(song),
            onPlayClick = noOp,
            onShuffleClick = noOp,
            onPlayNextClick = noOp,
            onAddToQueueClick = noOp,
            onAddToPlaylistClick = noOp,
            homePinAction = pinAction,
            resolveString = ::testActionString,
            resolveArtworkDescription = { "Album art for $it" }
        )
        val artistTarget = artistActionSheetTarget(
            artistName = "Artist",
            subtitle = "1 song",
            artworkUri = null,
            artistIdentity = artistIdentity("Artist"),
            hasCustomPicture = false,
            onChoosePicture = {},
            onRemovePicture = {},
            artistSongs = listOf(song),
            onPlayClick = noOp,
            onShuffleClick = noOp,
            onPlayNextClick = noOp,
            onAddToQueueClick = noOp,
            onAddToPlaylistClick = noOp,
            homePinAction = pinAction,
            resolveString = ::testActionString,
            resolveArtworkDescription = { "Artwork for $it" }
        )

        assertEquals(
            listOf(
                R.string.playlist_play_next.toString(),
                R.string.playlist_add_to_queue.toString(),
                R.string.playlist_add_to_another_queue.toString(),
                R.string.playlist_play_in_new_queue.toString(),
                R.string.playlist_add_favorites.toString(),
                "Pin to Home",
                "Rate song",
                R.string.library_song_add_to_playlist.toString(),
                R.string.playlist_edit_tags.toString()
            ),
            songTarget.actions.map { it.label }
        )
        assertEquals("Pin to Home", albumTarget.actions.last().label)
        assertEquals("Pin to Home", artistTarget.actions.last().label)

        songTarget.actions.first { it.label == "Pin to Home" }.onClick()
        assertEquals(true, pinInvoked)
    }

    private fun testActionString(id: Int): String = when (id) {
        R.string.playlist_play -> "Play"
        R.string.playlist_shuffle -> "Shuffle"
        R.string.playlist_play_next -> "Play next"
        R.string.playlist_add_to_queue -> "Add to queue"
        R.string.playlist_add_to_another_queue -> "Add to another queue..."
        R.string.playlist_play_in_new_queue -> "Play in new queue"
        R.string.library_song_add_to_playlist -> "Add to playlist"
        R.string.library_artist_picture_change -> "Change artist picture"
        R.string.library_artist_picture_set -> "Set artist picture"
        R.string.library_artist_picture_remove -> "Remove artist picture"
        R.string.library_album_edit_metadata -> "Edit album metadata"
        else -> error("Unexpected resource: $id")
    }

    private fun testSong(id: Long = 1L): Song {
        return Song(
            id = id,
            title = "Song",
            artist = "Artist",
            album = "Album",
            trackNumber = 1,
            duration = 120_000L,
            uri = mock(Uri::class.java),
            filePath = "/music/song.flac",
            folderPath = "/music",
            albumArtUri = null
        )
    }
}
