package com.example.cdplaya.mediaaccess

internal enum class MediaPermissionRequest {
    AUDIO,
    ARTWORK
}

internal enum class MediaAccessEffect {
    LOAD_LIBRARY,
    REVOKE_LIBRARY_ACCESS,
    REFRESH_ARTWORK
}

internal class MediaPermissionCoordinator {
    private var lastState: MediaAccessState? = null
    private var activeRequest: MediaPermissionRequest? = null

    fun onStateEvaluated(state: MediaAccessState): List<MediaAccessEffect> {
        val previous = lastState
        lastState = state
        val effects = mutableListOf<MediaAccessEffect>()

        if (state.hasAudioAccess && previous?.hasAudioAccess != true) {
            effects += MediaAccessEffect.LOAD_LIBRARY
        } else if (!state.hasAudioAccess && previous?.hasAudioAccess == true) {
            effects += MediaAccessEffect.REVOKE_LIBRARY_ACCESS
        }

        if (
            state.hasAudioAccess &&
            state.hasArtworkAccess &&
            previous?.hasAudioAccess == true &&
            previous?.hasArtworkAccess == false
        ) {
            effects += MediaAccessEffect.REFRESH_ARTWORK
        }
        return effects
    }

    fun beginRequest(request: MediaPermissionRequest): Boolean {
        if (activeRequest != null) return false
        activeRequest = request
        return true
    }

    fun finishRequest(request: MediaPermissionRequest) {
        if (activeRequest == request) activeRequest = null
    }
}
