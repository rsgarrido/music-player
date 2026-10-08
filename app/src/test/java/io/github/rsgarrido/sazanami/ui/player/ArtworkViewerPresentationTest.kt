package io.github.rsgarrido.sazanami.ui.player

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import io.github.rsgarrido.sazanami.data.PlayerTheme
import io.github.rsgarrido.sazanami.data.Song
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.mock

class ArtworkViewerPresentationTest {
    @Test fun eachOpeningHasIsolatedLoadingStateEvenForTheSameSong() {
        val song = Song(1, "Song", "Artist", "Album", 1, 120_000, mock(Uri::class.java),
            "/music/1.flac", "/music", mock(Uri::class.java))
        val first = NowPlayingArtworkViewerRequest(song, requireNotNull(song.albumArtUri))
        val second = NowPlayingArtworkViewerRequest(song, requireNotNull(song.albumArtUri))
        val old = ArtworkViewerImageState(first)
        val current = ArtworkViewerImageState(second)
        old.update(first, ArtworkViewerLoadStatus.READY)
        current.update(first, ArtworkViewerLoadStatus.READY)
        assertEquals(ArtworkViewerLoadStatus.LOADING, current.status)
        current.update(second, ArtworkViewerLoadStatus.ERROR)
        old.update(first, ArtworkViewerLoadStatus.ERROR)
        assertEquals(ArtworkViewerLoadStatus.ERROR, current.status)
        assertNotSame(first, second)
    }

    @Test fun dragIsBoundedAndEndOrCancellationReturnsNeutralTargets() {
        val tilt = ArtworkViewerTiltState()
        tilt.begin(0f, 0f)
        tilt.dragBy(Offset(10_000f, -10_000f), IntSize(200, 400))
        assertEquals(16f, tilt.rotationX, 0f)
        assertEquals(16f, tilt.rotationY, 0f)
        tilt.dragBy(Offset(-20_000f, 20_000f), IntSize(200, 400))
        assertEquals(-16f, tilt.rotationX, 0f)
        assertEquals(-16f, tilt.rotationY, 0f)
        tilt.reset()
        assertFalse(tilt.dragging)
        assertEquals(0f, tilt.rotationX, 0f)
        assertEquals(0f, tilt.rotationY, 0f)
        tilt.begin(2f, -3f) // Resume an interrupted reset from its displayed angles.
        tilt.dragBy(Offset(20f, 40f), IntSize(200, 400))
        assertEquals(-2.2667f, tilt.rotationX, 0.001f)
        assertEquals(1.2667f, tilt.rotationY, 0.001f)
        tilt.reset()
        tilt.dragBy(Offset(500f, 500f), IntSize.Zero)
        assertEquals(0f, tilt.rotationX, 0f)
    }

    @Test fun decodeBoundsPreserveNonSquareRatioAndCameraDistanceIsPositive() {
        assertEquals(IntSize(800, 400), artworkViewerDecodeSize(IntSize(800, 400)))
        assertEquals(IntSize(2048, 1024), artworkViewerDecodeSize(IntSize(4000, 2000)))
        assertEquals(IntSize(1, 1), artworkViewerDecodeSize(IntSize.Zero))
        assertTrue(artworkViewerCameraDistance(IntSize(800, 400)) > 800f)
        assertEquals(1200f, artworkViewerCameraDistance(IntSize(800, 400)), 0f)
        assertTrue(artworkViewerCameraDistance(IntSize.Zero) > 0f)
    }

    @Test fun moderateMovementReachesTheStrongerLimitInBothDirections() {
        val tilt = ArtworkViewerTiltState()
        tilt.begin(0f, 0f)
        tilt.dragBy(Offset(70f, -140f), IntSize(200, 400)) // 35% of each dimension.
        assertEquals(14.9333f, tilt.rotationX, 0.001f)
        assertEquals(14.9333f, tilt.rotationY, 0.001f)
        tilt.dragBy(Offset(5f, -10f), IntSize(200, 400)) // 37.5% reaches the cap.
        assertEquals(16f, tilt.rotationX, 0.001f)
        assertEquals(16f, tilt.rotationY, 0.001f)
        tilt.reset()
        tilt.begin(0f, 0f)
        tilt.dragBy(Offset(-100f, 200f), IntSize(200, 400))
        assertEquals(-16f, tilt.rotationX, 0f)
        assertEquals(-16f, tilt.rotationY, 0f)
    }

    @Test fun fitLetterboxingDoesNotDiluteTiltSensitivityOrCrossRequests() {
        assertEquals(IntSize(300, 300), artworkViewerTiltBounds(IntSize(300, 600), IntSize(400, 400)))
        assertEquals(IntSize(300, 150), artworkViewerTiltBounds(IntSize(300, 600), IntSize(400, 200)))
        assertEquals(IntSize(300, 600), artworkViewerTiltBounds(IntSize(300, 600), IntSize.Zero))
        val song = Song(1, "Song", "Artist", "Album", 1, 120_000, mock(Uri::class.java),
            "/music/1.flac", "/music", mock(Uri::class.java))
        val first = NowPlayingArtworkViewerRequest(song, requireNotNull(song.albumArtUri))
        val second = NowPlayingArtworkViewerRequest(song, requireNotNull(song.albumArtUri))
        val image = ArtworkViewerImageState(second)
        image.resolvedSize(first, IntSize(400, 200))
        assertEquals(IntSize.Zero, image.intrinsicSize)
    }

    @Test fun motionAndThemeBoundariesRemainExplicit() {
        assertFalse(ArtworkViewerMotionPolicy(false, false).tiltEnabled)
        assertFalse(ArtworkViewerMotionPolicy(true, true).tiltEnabled)
        assertTrue(ArtworkViewerMotionPolicy(true, false).tiltEnabled)
        PlayerTheme.entries.forEach { theme ->
            assertEquals(theme !in setOf(PlayerTheme.POCKET_CASSETTE, PlayerTheme.CLASSIC_WHEEL),
                supportsNowPlayingArtworkViewer(theme))
        }
    }
}
