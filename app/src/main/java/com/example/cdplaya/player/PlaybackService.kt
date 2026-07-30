package com.example.cdplaya.player

import android.app.PendingIntent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioTrackAudioOutputProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.example.cdplaya.MainActivity
import com.example.cdplaya.BuildConfig
import com.example.cdplaya.data.Song
import com.example.cdplaya.data.preferences.AppPreferencesRepository
import com.example.cdplaya.performance.PerformanceTraceNames
import com.example.cdplaya.performance.tracePerformance
import com.example.cdplaya.player.audio.AdvancedAudioRuntimeBridge
import com.example.cdplaya.player.audio.AudioOffloadPreference
import com.example.cdplaya.player.audio.AudioRouteCategory
import com.example.cdplaya.player.audio.mapAudioRoute
import com.example.cdplaya.player.audio.mapAudioSourceFormat
import com.example.cdplaya.player.audio.withAudioOffloadPreference
import com.example.cdplaya.player.equalizer.AudioProcessingPolicy
import com.example.cdplaya.player.equalizer.EqualizerAudioProcessor
import com.example.cdplaya.player.equalizer.EqualizerRenderersFactory
import com.example.cdplaya.player.equalizer.EqualizerRuntimeBridge
import com.example.cdplaya.player.equalizer.activeAutomaticHeadroomEnabled
import com.example.cdplaya.player.equalizer.toDspConfiguration
import com.example.cdplaya.player.feasibility.AndroidUsbMixerBackend
import com.example.cdplaya.player.feasibility.BitPerfectFeasibilityRuntimeBridge
import com.example.cdplaya.player.feasibility.FeasibilityEventType
import com.example.cdplaya.player.feasibility.FeasibilityProbeMode
import com.example.cdplaya.player.feasibility.FeasibilityRejectionReason
import com.example.cdplaya.player.feasibility.FeasibilityAudioOutputProvider
import com.example.cdplaya.player.feasibility.UsbMixerFeasibilityController
import com.example.cdplaya.player.equalizer.limiter.LimiterConfiguration
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {

    private var mediaSession: MediaLibrarySession? = null
    private val equalizerAudioProcessor = EqualizerAudioProcessor()
    private lateinit var player: ExoPlayer
    private lateinit var playerStateStorage: PlayerStateStorage
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var appPreferencesRepository: AppPreferencesRepository
    private lateinit var audioManager: AudioManager
    private lateinit var feasibilityAudioOutputProvider:
        FeasibilityAudioOutputProvider
    private lateinit var usbMixerBackend: AndroidUsbMixerBackend
    private lateinit var usbMixerController: UsbMixerFeasibilityController
    private var feasibilityOutputRecreationInProgress = false
    private var feasibilityPreferredDeviceApplied = false
    private var isRemotePlayback = false
    private val checkpointHandler = Handler(Looper.getMainLooper())
    private val checkpointRunnable = object : Runnable {
        override fun run() {
            saveServicePlaybackState()
            if (::player.isInitialized && player.isPlaying) {
                checkpointHandler.postDelayed(
                    this,
                    PlaybackStateCheckpointPolicy.DEFAULT_INTERVAL_MILLIS
                )
            }
        }
    }
    private val persistenceListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            saveServicePlaybackState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            checkpointHandler.removeCallbacks(checkpointRunnable)
            if (isPlaying) {
                checkpointHandler.postDelayed(
                    checkpointRunnable,
                    PlaybackStateCheckpointPolicy.DEFAULT_INTERVAL_MILLIS
                )
            } else {
                saveServicePlaybackState()
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                saveServicePlaybackState()
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            saveServicePlaybackState()
        }
    }

    private val audioOffloadListener = object : ExoPlayer.AudioOffloadListener {
        override fun onOffloadedPlayback(isOffloadedPlayback: Boolean) {
            tracePerformance(PerformanceTraceNames.AUDIO_OFFLOAD_STATE_CHANGED) {
                AdvancedAudioRuntimeBridge.updateOffloadPlayback(isOffloadedPlayback)
            }
        }

        override fun onSleepingForOffloadChanged(isSleepingForOffload: Boolean) {
            tracePerformance(PerformanceTraceNames.AUDIO_OFFLOAD_SLEEPING_CHANGED) {
                AdvancedAudioRuntimeBridge.updateSleepingForOffload(isSleepingForOffload)
            }
        }
    }

    private val advancedAudioPlayerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            AdvancedAudioRuntimeBridge.updateSourceFormat(null)
        }

        override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
            isRemotePlayback = deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE
            publishAudioRoute()
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            stopAndClearFeasibilityProbe()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                stopAndClearFeasibilityProbe()
            }
        }
    }

    private val advancedAudioAnalyticsListener = object : AnalyticsListener {
        override fun onAudioInputFormatChanged(
            eventTime: AnalyticsListener.EventTime,
            format: Format,
            decoderReuseEvaluation: DecoderReuseEvaluation?
        ) {
            tracePerformance(PerformanceTraceNames.AUDIO_INPUT_FORMAT_CHANGED) {
                AdvancedAudioRuntimeBridge.updateSourceFormat(mapAudioSourceFormat(format))
            }
        }

        override fun onAudioSessionIdChanged(
            eventTime: AnalyticsListener.EventTime,
            audioSessionId: Int
        ) {
            AdvancedAudioRuntimeBridge.updateAudioSessionId(audioSessionId.takeIf { it > 0 })
        }
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            publishAudioRoute()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            if (
                removedDevices.orEmpty().any {
                    it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                        it.type == AudioDeviceInfo.TYPE_USB_ACCESSORY ||
                        it.type == AudioDeviceInfo.TYPE_USB_HEADSET
                }
            ) {
                stopAndClearFeasibilityProbe()
            }
            publishAudioRoute()
        }
    }

    private val feasibilityController =
        object : BitPerfectFeasibilityRuntimeBridge.Controller {
            override fun observeCurrentOutput() {
                stopAndClearFeasibilityProbe()
                BitPerfectFeasibilityRuntimeBridge.setMode(
                    FeasibilityProbeMode.OBSERVE_CURRENT_PATH
                )
                feasibilityAudioOutputProvider.publishRetainedCurrentFacts()
            }

            override fun runExactUsbProbe() {
                runExactUsbFeasibilityProbe()
            }

            override fun stopAndClearProbe() {
                stopAndClearFeasibilityProbe()
            }
        }

    private val libraryCallback = object : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return Futures.immediateFuture(
                LibraryResult.ofItem(buildBrowseTree().toMediaItem(), params)
            )
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val children = buildBrowseTree().findNode(parentId)?.children.orEmpty()
            val fromIndex = (page * pageSize).coerceAtMost(children.size)
            val toIndex = (fromIndex + pageSize).coerceAtMost(children.size)

            return Futures.immediateFuture(
                LibraryResult.ofItemList(
                    children.subList(fromIndex, toIndex).map { it.toMediaItem() },
                    params
                )
            )
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            if (mediaItems.all { it.localConfiguration != null }) {
                return super.onSetMediaItems(
                    mediaSession,
                    controller,
                    mediaItems,
                    startIndex,
                    startPositionMs
                )
            }

            val tree = buildBrowseTree()
            val selectedIndex = startIndex.takeIf { it in mediaItems.indices } ?: 0
            val requestedId = mediaItems.getOrNull(selectedIndex)?.mediaId.orEmpty()
            val selectedNode = tree.findNode(requestedId)
            val contextSongs = tree.findParent(requestedId)
                ?.children
                ?.mapNotNull { it.song }
                .orEmpty()
            val selectedSong = selectedNode?.song

            if (selectedSong != null) {
                val playbackContext = contextSongs.ifEmpty { listOf(selectedSong) }
                PlaybackLibraryBridge.playSelectedSong(selectedSong, playbackContext)
                val resolvedItems = playbackContext.map { song -> song.toPlayableMediaItem() }
                val resolvedIndex = resolvedItems.indexOfFirst {
                    it.mediaId == selectedSong.id.toString()
                }
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(
                        resolvedItems,
                        resolvedIndex,
                        startPositionMs
                    )
                )
            }

            return super.onSetMediaItems(
                mediaSession,
                controller,
                mediaItems,
                startIndex,
                startPositionMs
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        EqualizerRuntimeBridge.start(serviceScope)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        feasibilityAudioOutputProvider =
            FeasibilityAudioOutputProvider(
                delegate = AudioTrackAudioOutputProvider.Builder(this).build(),
                onOutputReleased = {
                    if (
                        !feasibilityOutputRecreationInProgress &&
                        BitPerfectFeasibilityRuntimeBridge.state.value
                            .probeMode ==
                            FeasibilityProbeMode
                                .EXPERIMENTAL_USB_EXACT_PATH
                    ) {
                        stopAndClearFeasibilityProbe()
                    }
                }
            )
        val renderersFactory = EqualizerRenderersFactory(
            context = this,
            equalizerAudioProcessor = equalizerAudioProcessor,
            audioOutputProvider = feasibilityAudioOutputProvider
        )
        player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        appPreferencesRepository = AppPreferencesRepository.getInstance(this)
        audioManager = getSystemService(AudioManager::class.java)
        usbMixerBackend = AndroidUsbMixerBackend.create(this)
        usbMixerController = UsbMixerFeasibilityController(usbMixerBackend)
        BitPerfectFeasibilityRuntimeBridge.attachController(
            feasibilityController
        )
        usbMixerController.clearStaleAtStartup()
        playerStateStorage = PlayerStateStorage(this)
        player.addListener(persistenceListener)
        player.addListener(advancedAudioPlayerListener)
        player.addAnalyticsListener(advancedAudioAnalyticsListener)
        player.addAudioOffloadListener(audioOffloadListener)
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, checkpointHandler)
        applyAudioOffloadPreference(AudioOffloadPreference.DISABLED)
        AdvancedAudioRuntimeBridge.onPlayerConnected(AudioOffloadPreference.DISABLED)
        isRemotePlayback = player.deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE
        publishAudioRoute()
        observeAudioOffloadPreference()
        observeEqualizerPreferences()
        observeEqualizerRuntimeState()

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaLibrarySession.Builder(this, player, libraryCallback)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    override fun onDestroy() {
        stopAndClearFeasibilityProbe()
        BitPerfectFeasibilityRuntimeBridge.detachController(
            feasibilityController
        )
        checkpointHandler.removeCallbacks(checkpointRunnable)
        saveServicePlaybackState()
        player.removeListener(persistenceListener)
        player.removeListener(advancedAudioPlayerListener)
        player.removeAnalyticsListener(advancedAudioAnalyticsListener)
        player.removeAudioOffloadListener(audioOffloadListener)
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        mediaSession?.release()
        mediaSession = null
        player.release()
        EqualizerRuntimeBridge.release()
        serviceScope.cancel()
        AdvancedAudioRuntimeBridge.disconnect()
        super.onDestroy()
    }

    private fun runExactUsbFeasibilityProbe() {
        stopAndClearFeasibilityProbe()
        if (!BuildConfig.DEBUG) {
            BitPerfectFeasibilityRuntimeBridge.recordActivationRejected(
                FeasibilityRejectionReason.DEBUG_BUILD_REQUIRED
            )
            return
        }
        val state = BitPerfectFeasibilityRuntimeBridge.state.value
        val output = state.media3OutputConfig
        val equalizer = EqualizerRuntimeBridge.state.value
        val rejection = when {
            output == null ->
                FeasibilityRejectionReason.OUTPUT_FORMAT_UNKNOWN
            player.playbackParameters.speed != 1f ->
                FeasibilityRejectionReason.NON_UNITY_PLAYBACK_SPEED
            equalizer.effectivelyActive ->
                FeasibilityRejectionReason.ACTIVE_EQUALIZER
            equalizer.limiterRequestedEnabled ||
                equalizer.limiterEffectivelyActive ->
                FeasibilityRejectionReason.ACTIVE_LIMITER
            appPreferencesRepository.state.value.replayGainMode !=
                com.example.cdplaya.player.replaygain.ReplayGainMode.OFF ||
                player.volume != 1f ->
                FeasibilityRejectionReason.ACTIVE_REPLAY_GAIN
            else -> null
        }
        BitPerfectFeasibilityRuntimeBridge.setMode(
            FeasibilityProbeMode.EXPERIMENTAL_USB_EXACT_PATH
        )
        if (rejection != null) {
            BitPerfectFeasibilityRuntimeBridge.recordActivationRejected(
                rejection
            )
            stopAndClearFeasibilityProbe()
            return
        }

        val result = usbMixerController.activate(output)
        val device = if (Build.VERSION.SDK_INT >= 34) {
            usbMixerBackend.selectedDevice
        } else {
            null
        }
        if (!result.activated || device == null) {
            stopAndClearFeasibilityProbe()
            return
        }
        feasibilityAudioOutputProvider.setIntendedUsbDevice(device)
        player.setPreferredAudioDevice(device)
        feasibilityPreferredDeviceApplied = true

        // Stop/prepare forces the renderer and sink to create a matching
        // AudioTrack after the UID-owned preferred mixer attribute is set.
        // The playlist, current index, position, repeat, shuffle, session and
        // authoritative ExoPlayer instance are retained.
        val playWhenReady = player.playWhenReady
        feasibilityOutputRecreationInProgress = true
        try {
            player.stop()
            player.prepare()
            player.playWhenReady = playWhenReady
        } catch (failure: RuntimeException) {
            BitPerfectFeasibilityRuntimeBridge.recordFailure(
                FeasibilityEventType.INITIALIZATION_FAILED
            )
            stopAndClearFeasibilityProbe()
        } finally {
            feasibilityOutputRecreationInProgress = false
        }
    }

    private fun stopAndClearFeasibilityProbe() {
        if (
            !::usbMixerController.isInitialized ||
            !::feasibilityAudioOutputProvider.isInitialized
        ) {
            return
        }
        try {
            usbMixerController.cleanup()
        } finally {
            feasibilityAudioOutputProvider.setIntendedUsbDevice(null)
            if (
                feasibilityPreferredDeviceApplied &&
                ::player.isInitialized
            ) {
                player.setPreferredAudioDevice(null)
            }
            feasibilityPreferredDeviceApplied = false
            if (
                BitPerfectFeasibilityRuntimeBridge.state.value.probeMode !=
                    FeasibilityProbeMode.OFF
            ) {
                BitPerfectFeasibilityRuntimeBridge.setMode(
                    FeasibilityProbeMode.OFF
                )
            }
        }
    }

    private fun observeAudioOffloadPreference() {
        serviceScope.launch {
            combine(
                appPreferencesRepository.state
                .filter { preferences -> preferences.isLoaded }
                .map { preferences -> preferences.audioOffloadPreference },
                EqualizerRuntimeBridge.state
                    .map { state ->
                        AudioProcessingRequirements(
                            equalizerEffectivelyActive =
                                state.effectivelyActive &&
                                    !state.limiterEffectivelyActive,
                            limiterEffectivelyActive =
                                state.limiterRequestedEnabled ||
                                    state.limiterEffectivelyActive,
                            comparisonSessionActive =
                                state.comparisonSessionActive
                        )
                    }
            ) { preference, requirements ->
                preference to requirements
            }
                .distinctUntilChanged()
                .collectLatest { (preference, requirements) ->
                    applyAudioProcessingPolicy(
                        userPreference = preference,
                        equalizerEffectivelyActive =
                            requirements.equalizerEffectivelyActive,
                        limiterEffectivelyActive =
                            requirements.limiterEffectivelyActive,
                        comparisonSessionActive =
                            requirements.comparisonSessionActive
                    )
                }
        }
    }

    private fun observeEqualizerRuntimeState() {
        serviceScope.launch {
            EqualizerRuntimeBridge.state
                .collectLatest(
                    AdvancedAudioRuntimeBridge::updateEqualizerRuntimeState
                )
        }
    }

    private fun observeEqualizerPreferences() {
        serviceScope.launch {
            appPreferencesRepository.state
                .filter { preferences -> preferences.isLoaded }
                .map { preferences ->
                    preferences.equalizerPreferences
                }
                .distinctUntilChanged()
                .collectLatest { equalizerPreferences ->
                    EqualizerRuntimeBridge.requestConfiguration(
                        configuration =
                            equalizerPreferences
                                .toDspConfiguration(),
                        automaticHeadroomEnabled =
                            equalizerPreferences
                                .activeAutomaticHeadroomEnabled,
                        mode = equalizerPreferences.mode,
                        limiterConfiguration =
                            LimiterConfiguration(
                                enabled =
                                    equalizerPreferences
                                        .limiterEnabled,
                                ceilingDbfs =
                                    equalizerPreferences
                                        .limiterCeilingDbfs
                            )
                    )
                }
        }
    }

    private fun applyAudioOffloadPreference(
        preference: AudioOffloadPreference
    ) {
        applyAudioProcessingPolicy(
            userPreference = preference,
            equalizerEffectivelyActive = false,
            limiterEffectivelyActive = false,
            comparisonSessionActive = false
        )
    }

    private fun applyAudioProcessingPolicy(
        userPreference: AudioOffloadPreference,
        equalizerEffectivelyActive: Boolean,
        limiterEffectivelyActive: Boolean,
        comparisonSessionActive: Boolean
    ) {
        tracePerformance(PerformanceTraceNames.AUDIO_OFFLOAD_PREFERENCE_APPLIED) {
            val decision = AudioProcessingPolicy.evaluate(
                userOffloadPreference = userPreference,
                equalizerEffectivelyActive =
                    equalizerEffectivelyActive,
                limiterEffectivelyActive =
                    limiterEffectivelyActive,
                comparisonSessionActive =
                    comparisonSessionActive
            )
            val updatedParameters = player.trackSelectionParameters
                .withAudioOffloadPreference(
                    decision.effectiveOffloadPreference
                )
            if (player.trackSelectionParameters != updatedParameters) {
                player.trackSelectionParameters = updatedParameters
            }
            AdvancedAudioRuntimeBridge.updateOffloadPreference(
                userPreference
            )
        }
    }

    private data class AudioProcessingRequirements(
        val equalizerEffectivelyActive: Boolean,
        val limiterEffectivelyActive: Boolean,
        val comparisonSessionActive: Boolean
    )

    private fun publishAudioRoute() {
        val route = if (isRemotePlayback) {
            mapAudioRoute(deviceType = null, isLocalPlayback = false)
        } else {
            mapAudioRoute(
                deviceType = resolveLocalMediaRouteType(),
                isLocalPlayback = true
            )
        }
        if (AdvancedAudioRuntimeBridge.state.value.routeInfo != route) {
            tracePerformance(PerformanceTraceNames.AUDIO_ROUTE_CHANGED) {
                AdvancedAudioRuntimeBridge.updateRouteInfo(route)
            }
        }
    }

    private fun resolveLocalMediaRouteType(): Int? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val attributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            return audioManager.getAudioDevicesForAttributes(attributes)
                .firstOrNull()
                ?.type
        }

        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        if (outputs.size == 1) return outputs.single().type
        val hasBuiltInSpeaker = outputs.any { device ->
            mapAudioRoute(device.type, isLocalPlayback = true).category ==
                AudioRouteCategory.BUILT_IN_SPEAKER
        }
        val hasExternalRoute = outputs.any { device ->
            val category = mapAudioRoute(device.type, isLocalPlayback = true).category
            category != AudioRouteCategory.BUILT_IN_SPEAKER &&
                category != AudioRouteCategory.UNKNOWN
        }
        return if (hasBuiltInSpeaker && !hasExternalRoute) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        } else {
            null
        }
    }

    private fun saveServicePlaybackState() {
        if (!::player.isInitialized || !::playerStateStorage.isInitialized) return
        val songId = player.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        val repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            else -> RepeatMode.OFF
        }
        playerStateStorage.saveServicePlaybackState(
            currentSongId = songId,
            currentPosition = player.currentPosition
                .coerceIn(0L, Int.MAX_VALUE.toLong())
                .toInt(),
            repeatMode = repeatMode
        )
    }

    private fun buildBrowseTree(): AutoBrowseNode {
        return buildAndroidAutoBrowseTree(PlaybackLibraryBridge.songs)
    }

    private fun AutoBrowseNode.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(subtitle)
            .setIsBrowsable(children.isNotEmpty())
            .setIsPlayable(song != null)
            .setArtworkUri(song?.albumArtUri)
            .build()

        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun Song.toPlayableMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(albumArtUri)
                    .build()
            )
            .build()
    }

}
