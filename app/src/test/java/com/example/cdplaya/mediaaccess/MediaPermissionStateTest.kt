package com.example.cdplaya.mediaaccess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPermissionStateTest {
    @Test
    fun initialAudioPermissionIsRequestable() {
        val state = evaluate()

        assertEquals(PermissionAccess.REQUESTABLE, state.audioAccess)
        assertFalse(state.audioPermissionRequested)
    }

    @Test
    fun denialWithRationaleCanBeRequestedAgain() {
        val state = evaluate(
            requested = setOf(MediaPermissions.READ_MEDIA_AUDIO),
            rationale = setOf(MediaPermissions.READ_MEDIA_AUDIO)
        )

        assertEquals(PermissionAccess.DENIED, state.audioAccess)
        assertTrue(state.audioPermissionRequested)
    }

    @Test
    fun denialWithoutRationaleAfterRequestIsPermanent() {
        val state = evaluate(
            requested = setOf(MediaPermissions.READ_MEDIA_AUDIO),
            permanentlyDenied = setOf(MediaPermissions.READ_MEDIA_AUDIO)
        )

        assertEquals(PermissionAccess.PERMANENTLY_DENIED, state.audioAccess)
    }

    @Test
    fun settingsRevocationWithoutUserFixedFlagCanBeRequestedAgain() {
        val state = evaluate(requested = setOf(MediaPermissions.READ_MEDIA_AUDIO))

        assertEquals(PermissionAccess.DENIED, state.audioAccess)
    }

    @Test
    fun imageDenialDoesNotBlockGrantedAudio() {
        val state = evaluate(
            granted = setOf(MediaPermissions.READ_MEDIA_AUDIO),
            requested = setOf(
                MediaPermissions.READ_MEDIA_AUDIO,
                MediaPermissions.READ_MEDIA_IMAGES
            ),
            permanentlyDenied = setOf(MediaPermissions.READ_MEDIA_IMAGES)
        )

        assertTrue(state.hasAudioAccess)
        assertFalse(state.hasArtworkAccess)
        assertEquals(PermissionAccess.PERMANENTLY_DENIED, state.artworkAccess)
    }

    @Test
    fun legacyGrantProvidesAudioAndArtworkAccess() {
        val state = MediaAccessPolicy.evaluate(
            sdkInt = 29,
            grantedPermissions = setOf(MediaPermissions.READ_EXTERNAL_STORAGE),
            requestedPermissions = setOf(MediaPermissions.READ_EXTERNAL_STORAGE),
            permissionsWithRationale = emptySet()
        )

        assertTrue(state.hasAudioAccess)
        assertTrue(state.hasArtworkAccess)
    }

    private fun evaluate(
        granted: Set<String> = emptySet(),
        requested: Set<String> = emptySet(),
        rationale: Set<String> = emptySet(),
        permanentlyDenied: Set<String> = emptySet()
    ) = MediaAccessPolicy.evaluate(
        sdkInt = 33,
        grantedPermissions = granted,
        requestedPermissions = requested,
        permissionsWithRationale = rationale,
        permanentlyDeniedPermissions = permanentlyDenied
    )
}
