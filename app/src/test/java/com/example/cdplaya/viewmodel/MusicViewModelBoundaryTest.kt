package com.example.cdplaya.viewmodel

import com.example.cdplaya.controller.LibraryController
import com.example.cdplaya.controller.SleepTimerController
import com.example.cdplaya.player.MusicPlayer
import com.example.cdplaya.player.PlaybackController
import java.lang.reflect.Modifier
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicViewModelBoundaryTest {
    @Test
    fun controllersArePrivateAndNoPublicApiReturnsImplementationOwners() {
        val forbiddenTypes = setOf(
            PlaybackController::class.java,
            LibraryController::class.java,
            SleepTimerController::class.java,
            MusicPlayer::class.java
        )
        val controllerFields = MusicViewModel::class.java.declaredFields.filter { field ->
            field.type in forbiddenTypes
        }

        assertTrue(controllerFields.isNotEmpty())
        assertTrue(controllerFields.all { field -> Modifier.isPrivate(field.modifiers) })
        assertFalse(
            MusicViewModel::class.java.declaredMethods.any { method ->
                Modifier.isPublic(method.modifiers) && !method.isSynthetic &&
                    method.returnType in forbiddenTypes
            }
        )
    }
}
