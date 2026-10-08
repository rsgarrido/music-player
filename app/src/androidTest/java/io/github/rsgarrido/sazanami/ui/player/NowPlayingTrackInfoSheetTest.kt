package io.github.rsgarrido.sazanami.ui.player

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.player.audioquality.AudioQualityInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NowPlayingTrackInfoSheetTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test fun songMetadataAppearsBeforeAudioAndLongLocationsRemainScrollable() {
        val longPath = "/storage/emulated/0/Music/" + "Long album folder/".repeat(12) + "track.flac"
        val pending = CompletableDeferred<AudioQualityInfo>()
        composeRule.setContent {
            MaterialTheme {
                NowPlayingTrackInfoSheet(NowPlayingTrackInfoRequest(song(1).copy(filePath = longPath)), {},
                    loadAudio = { pending.await() })
            }
        }
        try {
            composeRule.onNodeWithText("Track information").assertExists()
            composeRule.onNodeWithTag(TrackInfoDetailsTag).performScrollToNode(hasText("File"))
            composeRule.onNodeWithTag(TrackInfoDetailsTag).performScrollToNode(hasText(longPath))
            composeRule.onNodeWithText(longPath).assertExists()
            composeRule.runOnIdle { pending.complete(AudioQualityInfo("FLAC", 16, 44_100, 830, "audio/flac", 2)) }
            composeRule.onNodeWithTag(TrackInfoDetailsTag).performScrollToNode(hasText("44.1 kHz"))
            composeRule.onNodeWithText("44.1 kHz").assertExists()
            composeRule.onNodeWithTag(TrackInfoDetailsTag).performScrollToNode(hasText("Stereo / 2 channels"))
            composeRule.onNodeWithText("Stereo / 2 channels").assertExists()
        } finally {
            pending.complete(AudioQualityInfo(null, null, null, null))
        }
    }

    @Test fun oldExtractionCannotPopulateANewInformationTarget() {
        val firstStarted = CompletableDeferred<Unit>()
        val oldAudio = CompletableDeferred<AudioQualityInfo>()
        val request = mutableStateOf(NowPlayingTrackInfoRequest(song(1)))
        composeRule.setContent {
            MaterialTheme {
                NowPlayingTrackInfoSheet(request.value, {}, loadAudio = { target ->
                    if (target.id == 1L) withContext(NonCancellable) {
                        firstStarted.complete(Unit)
                        oldAudio.await()
                    } else AudioQualityInfo("FLAC", 24, 48_000, 1000)
                })
            }
        }
        try {
            composeRule.waitUntil { firstStarted.isCompleted }
            composeRule.runOnIdle { request.value = NowPlayingTrackInfoRequest(song(2)) }
            composeRule.onNodeWithTag(TrackInfoDetailsTag).performScrollToNode(hasText("48 kHz"))
            composeRule.runOnIdle { oldAudio.complete(AudioQualityInfo("FLAC", 16, 96_000, 2000)) }
            composeRule.onNodeWithText("48 kHz").assertExists()
            composeRule.onNodeWithText("96 kHz").assertDoesNotExist()
        } finally {
            oldAudio.complete(AudioQualityInfo(null, null, null, null))
        }
    }

    @Test fun backAndDoneCloseOnlyTheInformationSheet() {
        val visible = mutableStateOf(true)
        var playerClicks = 0
        var dismissed = 0
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize().clickable { playerClicks++ })
                if (visible.value) NowPlayingTrackInfoSheet(NowPlayingTrackInfoRequest(song(1)),
                    onDismiss = { dismissed++; visible.value = false },
                    loadAudio = { AudioQualityInfo(null, null, null, null) })
            }
        }
        Espresso.pressBack()
        composeRule.onNodeWithTag(TrackInfoSheetTag).assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(1, dismissed); assertEquals(0, playerClicks); visible.value = true }
        composeRule.onNodeWithText("Done").performClick()
        composeRule.onNodeWithTag(TrackInfoSheetTag).assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(2, dismissed); assertEquals(0, playerClicks) }
    }

    private fun song(id: Long) = Song(id, "Track $id", "Artist", "Album", 1001, 185_000L,
        Uri.parse("content://media/external/audio/media/$id"), "/music/track.flac", "/music", null,
        volumeName = "external", fileSizeBytes = 1536, albumArtist = "Album Artist", year = 2024,
        genres = listOf("Rock", "Metal"))
}
