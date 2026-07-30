package com.example.cdplaya.player.feasibility

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import com.example.cdplaya.data.preferences.AppPreferencesState
import com.example.cdplaya.player.PlaybackService
import com.example.cdplaya.player.equalizer.EqualizerAudioProcessor
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BitPerfectFeasibilityArchitectureTest {
    @Test
    fun serviceRetainsOnePlayerOneSessionOneProcessorAndOneProvider() {
        val fields = PlaybackService::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
        assertEquals(1, fields.count { it.type == ExoPlayer::class.java })
        assertEquals(
            1,
            fields.count { it.type == MediaLibrarySession::class.java }
        )
        assertEquals(
            1,
            fields.count { it.type == EqualizerAudioProcessor::class.java }
        )
        assertEquals(
            1,
            fields.count {
                it.type == FeasibilityAudioOutputProvider::class.java
            }
        )
    }

    @Test
    fun feasibilityBridgeOwnsNoPlayerQueueSessionOrRepository() {
        val forbidden = listOf(
            "ExoPlayer",
            "MediaSession",
            "MediaItem",
            "Queue",
            "Repository",
            "DataStore"
        )
        BitPerfectFeasibilityRuntimeBridge::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
            .forEach { field ->
                forbidden.forEach { fragment ->
                    assertFalse(
                        "${field.name} exposes $fragment",
                        field.type.name.contains(fragment)
                    )
                }
            }
    }

    @Test
    fun noPersistentBitPerfectPreferenceExists() {
        AppPreferencesState::class.java.declaredFields.forEach { field ->
            assertFalse(
                field.name.contains("bitPerfect", ignoreCase = true)
            )
            assertFalse(
                field.name.contains("usbDevice", ignoreCase = true)
            )
        }
    }
}
