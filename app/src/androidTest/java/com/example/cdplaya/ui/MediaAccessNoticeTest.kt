package com.example.cdplaya.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.cdplaya.mediaaccess.MediaAccessPolicy
import com.example.cdplaya.mediaaccess.MediaPermissions
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MediaAccessNoticeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initialStateExplainsAndOffersAudioGrant() {
        composeRule.setContent {
            MediaAccessNotice(
                state = state(),
                onRequestAudioAccess = {},
                onRequestArtworkAccess = {},
                onOpenAppSettings = {}
            )
        }

        composeRule.onNodeWithText("Audio access needed").assertIsDisplayed()
        composeRule.onNodeWithText("Grant audio access").assertIsDisplayed()
    }

    @Test
    fun permanentDenialOffersSettingsRecovery() {
        composeRule.setContent {
            MediaAccessNotice(
                state = state(requested = setOf(MediaPermissions.READ_MEDIA_AUDIO)),
                onRequestAudioAccess = {},
                onRequestArtworkAccess = {},
                onOpenAppSettings = {}
            )
        }

        composeRule.onNodeWithText("Open app settings").assertIsDisplayed()
    }

    @Test
    fun optionalImageDenialKeepsAudioAvailable() {
        var artworkRequested = false
        val state = state(granted = setOf(MediaPermissions.READ_MEDIA_AUDIO))
        assertTrue(state.hasAudioAccess)

        composeRule.setContent {
            MediaAccessNotice(
                state = state,
                onRequestAudioAccess = {},
                onRequestArtworkAccess = { artworkRequested = true },
                onOpenAppSettings = {}
            )
        }
        composeRule.onNodeWithText("Allow folder artwork").performClick()
        composeRule.runOnIdle { assertTrue(artworkRequested) }
    }

    private fun state(
        granted: Set<String> = emptySet(),
        requested: Set<String> = emptySet(),
        permanentlyDenied: Set<String> = if (requested.isEmpty()) {
            emptySet()
        } else {
            requested
        }
    ) = MediaAccessPolicy.evaluate(
        sdkInt = 33,
        grantedPermissions = granted,
        requestedPermissions = requested,
        permissionsWithRationale = emptySet(),
        permanentlyDeniedPermissions = permanentlyDenied
    )
}
