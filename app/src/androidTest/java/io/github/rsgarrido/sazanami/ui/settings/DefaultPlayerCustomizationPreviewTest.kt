package io.github.rsgarrido.sazanami.ui.settings

import androidx.activity.ComponentActivity
import android.net.Uri
import androidx.compose.ui.test.onAllNodesWithTag
import io.github.rsgarrido.sazanami.data.Song
import io.github.rsgarrido.sazanami.ui.player.ArtworkViewerEntryTag
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import io.github.rsgarrido.sazanami.ui.player.modern.ModernPlayerAppearance
import org.junit.Rule
import org.junit.Test

class DefaultPlayerCustomizationPreviewTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun previewArtworkWithARealUriHasNoLiveViewerAction() {
        val song = Song(1, "Preview", "Artist", "Album", 1, 120_000,
            Uri.parse("content://media/1"), "/music/1.flac", "/music", Uri.parse("content://art/1"))
        composeRule.setContent { MaterialTheme { ModernPlayerAppearancePreview(ModernPlayerAppearance.Default, song) } }
        composeRule.onAllNodesWithTag(ArtworkViewerEntryTag).assertCountEquals(0)
    }

    @Test
    fun previewContainsExactlyOneQueueAction() {
        composeRule.setContent {
            MaterialTheme {
                ModernPlayerAppearancePreview(
                    appearance = ModernPlayerAppearance.Default,
                    previewSong = null
                )
            }
        }

        composeRule.onAllNodesWithContentDescription("Open queues").assertCountEquals(1)
        composeRule.onNodeWithText("Sazanami").assertHasNoClickAction()
        composeRule.onNodeWithText("FLAC / 24-bit").assertHasNoClickAction()
    }
}
