package com.example.cdplaya.player.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Process-local publication point for service-owned audio facts.
 *
 * PlaybackService remains authoritative. Consumers only receive immutable snapshots and never
 * receive an ExoPlayer, route callback, Format, or AudioManager object.
 */
internal object AdvancedAudioRuntimeBridge {
    private val _state = MutableStateFlow(AudioOutputRuntimeSnapshot())
    val state: StateFlow<AudioOutputRuntimeSnapshot> = _state.asStateFlow()

    fun onPlayerConnected(preference: AudioOffloadPreference) {
        _state.update { current ->
            current.copy(
                isPlayerConnected = true,
                offloadState = AudioOffloadRuntimeState.create(
                    requestedPreference = preference,
                    isOffloadedPlayback = current.offloadState.isOffloadedPlayback,
                    isSleepingForOffload = current.offloadState.isSleepingForOffload
                )
            )
        }
    }

    fun updateOffloadPreference(preference: AudioOffloadPreference) {
        _state.update { current ->
            current.copy(
                offloadState = AudioOffloadRuntimeState.create(
                    requestedPreference = preference,
                    isOffloadedPlayback = current.offloadState.isOffloadedPlayback,
                    isSleepingForOffload = current.offloadState.isSleepingForOffload
                )
            )
        }
    }

    fun updateOffloadPlayback(isOffloadedPlayback: Boolean) {
        _state.update { current ->
            current.copy(
                offloadState = AudioOffloadRuntimeState.create(
                    requestedPreference = current.offloadState.requestedPreference,
                    isOffloadedPlayback = isOffloadedPlayback,
                    isSleepingForOffload = current.offloadState.isSleepingForOffload
                )
            )
        }
    }

    fun updateSleepingForOffload(isSleepingForOffload: Boolean) {
        _state.update { current ->
            current.copy(
                offloadState = AudioOffloadRuntimeState.create(
                    requestedPreference = current.offloadState.requestedPreference,
                    isOffloadedPlayback = current.offloadState.isOffloadedPlayback,
                    isSleepingForOffload = isSleepingForOffload
                )
            )
        }
    }

    fun disconnect() {
        _state.value = AudioOutputRuntimeSnapshot()
    }
}
