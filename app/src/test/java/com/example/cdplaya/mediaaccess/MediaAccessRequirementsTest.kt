package com.example.cdplaya.mediaaccess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaAccessRequirementsTest {
    @Test
    fun api29Through32RequireLegacyReadPermission() {
        (29..32).forEach { sdkInt ->
            val requirements = MediaAccessPolicy.requirementsFor(sdkInt)

            assertEquals(
                "API $sdkInt",
                setOf(MediaPermissions.READ_EXTERNAL_STORAGE),
                requirements.requiredAudioPermissions
            )
            assertEquals(
                requirements.requiredAudioPermissions,
                requirements.optionalArtworkPermissions
            )
        }
    }

    @Test
    fun api33Through36RequireGranularAudioAndOptionalImages() {
        (33..36).forEach { sdkInt ->
            val requirements = MediaAccessPolicy.requirementsFor(sdkInt)

            assertEquals(
                "API $sdkInt",
                setOf(MediaPermissions.READ_MEDIA_AUDIO),
                requirements.requiredAudioPermissions
            )
            assertEquals(
                setOf(MediaPermissions.READ_MEDIA_IMAGES),
                requirements.optionalArtworkPermissions
            )
            assertTrue(MediaPermissions.READ_EXTERNAL_STORAGE !in requirements.requiredAudioPermissions)
        }
    }

    @Test
    fun imagePermissionIsAbsentWhenStandaloneArtworkIsNotQueried() {
        val requirements = MediaAccessPolicy.requirementsFor(
            sdkInt = 36,
            queriesStandaloneArtwork = false
        )

        assertTrue(requirements.optionalArtworkPermissions.isEmpty())
    }
}

