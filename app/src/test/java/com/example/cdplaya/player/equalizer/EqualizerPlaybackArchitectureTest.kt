package com.example.cdplaya.player.equalizer

import androidx.media3.exoplayer.ExoPlayer
import com.example.cdplaya.player.PlaybackService
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerPlaybackArchitectureTest {
    @Test
    fun playbackServiceDeclaresOnePlayerAndOneEqualizerProcessor() {
        val instanceFields = PlaybackService::class.java.declaredFields
            .filterNot { field -> Modifier.isStatic(field.modifiers) }

        assertEquals(
            1,
            instanceFields.count { field ->
                field.type == EqualizerAudioProcessor::class.java
            }
        )
        assertEquals(
            1,
            instanceFields.count { field ->
                field.type == ExoPlayer::class.java
            }
        )
    }

    @Test
    fun runtimeBridgeOwnsNoPlayerQueueOrPersistenceObjects() {
        val forbiddenTypeFragments = listOf(
            "ExoPlayer",
            "MediaItem",
            "PlaybackQueue",
            "DataStore",
            "Repository",
            "Room"
        )

        EqualizerRuntimeBridge::class.java.declaredFields
            .filterNot { field -> Modifier.isStatic(field.modifiers) }
            .forEach { field ->
                forbiddenTypeFragments.forEach { fragment ->
                    assertFalse(
                        "${field.name} exposes $fragment",
                        field.type.name.contains(fragment)
                    )
                }
            }
    }

    @Test
    fun preparedPlanModelsContainNoAndroidTypes() {
        listOf(
            EqualizerRuntimeSnapshot::class.java,
            EqualizerProcessorFormat::class.java,
            EqualizerRuntimeState::class.java,
            PreparedEqualizerPlan::class.java
        ).flatMap { type ->
            type.declaredFields.filterNot { field ->
                Modifier.isStatic(field.modifiers)
            }
        }.forEach { field ->
            assertFalse(field.type.name.startsWith("android."))
            assertFalse(field.type.name.startsWith("androidx.media3."))
        }
    }

    @Test
    fun debugConfigurationsRemainTransientRuntimeRequests() {
        val methods = DebugEqualizerConfigurations::class.java.declaredMethods
            .map { method -> method.name }

        assertTrue(methods.any { name -> name.contains("requestBassTest") })
        assertTrue(methods.any { name -> name.contains("requestTrebleTest") })
        assertFalse(
            DebugEqualizerConfigurations::class.java.declaredFields.any {
                field -> field.type.name.contains("Repository")
            }
        )
    }
}

