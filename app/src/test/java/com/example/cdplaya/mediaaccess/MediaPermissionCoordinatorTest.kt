package com.example.cdplaya.mediaaccess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPermissionCoordinatorTest {
    @Test
    fun oneLibraryLoadIsEmittedForGrantAcrossRepeatedEvaluation() {
        val coordinator = MediaPermissionCoordinator()

        assertEquals(emptyList<MediaAccessEffect>(), coordinator.onStateEvaluated(deniedState()))
        assertEquals(
            listOf(MediaAccessEffect.LOAD_LIBRARY),
            coordinator.onStateEvaluated(grantedState())
        )
        assertEquals(emptyList<MediaAccessEffect>(), coordinator.onStateEvaluated(grantedState()))
    }

    @Test
    fun revocationAndSettingsGrantEmitDeterministicEffects() {
        val coordinator = MediaPermissionCoordinator()

        coordinator.onStateEvaluated(grantedState())
        assertEquals(
            listOf(MediaAccessEffect.REVOKE_LIBRARY_ACCESS),
            coordinator.onStateEvaluated(deniedState())
        )
        assertEquals(
            listOf(MediaAccessEffect.LOAD_LIBRARY),
            coordinator.onStateEvaluated(grantedState())
        )
    }

    @Test
    fun artworkGrantRefreshesWithoutReloadingAudioLibrary() {
        val coordinator = MediaPermissionCoordinator()
        coordinator.onStateEvaluated(grantedState(artworkGranted = false))

        assertEquals(
            listOf(MediaAccessEffect.REFRESH_ARTWORK),
            coordinator.onStateEvaluated(grantedState(artworkGranted = true))
        )
    }

    @Test
    fun concurrentLauncherEventsAreRejected() {
        val coordinator = MediaPermissionCoordinator()

        assertTrue(coordinator.beginRequest(MediaPermissionRequest.AUDIO))
        assertFalse(coordinator.beginRequest(MediaPermissionRequest.ARTWORK))
        coordinator.finishRequest(MediaPermissionRequest.AUDIO)
        assertTrue(coordinator.beginRequest(MediaPermissionRequest.ARTWORK))
    }

    private fun grantedState(artworkGranted: Boolean = true) = MediaAccessPolicy.evaluate(
        sdkInt = 33,
        grantedPermissions = buildSet {
            add(MediaPermissions.READ_MEDIA_AUDIO)
            if (artworkGranted) add(MediaPermissions.READ_MEDIA_IMAGES)
        },
        requestedPermissions = setOf(
            MediaPermissions.READ_MEDIA_AUDIO,
            MediaPermissions.READ_MEDIA_IMAGES
        ),
        permissionsWithRationale = emptySet()
    )

    private fun deniedState() = MediaAccessPolicy.evaluate(
        sdkInt = 33,
        grantedPermissions = emptySet(),
        requestedPermissions = emptySet(),
        permissionsWithRationale = emptySet()
    )
}

