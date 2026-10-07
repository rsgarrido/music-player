package io.github.rsgarrido.sazanami.external

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.CancellationSignal
import android.os.OperationCanceledException
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.player.FragmentedMp4SeekExtractorsFactory
import io.github.rsgarrido.sazanami.ui.theme.SazanamiTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** A file preview, independent of every normal Sazanami playback/controller/database object. */
@OptIn(UnstableApi::class)
class ExternalAudioPlayerActivity : AppCompatActivity() {
    private val session = ExternalAudioPlayerSession()
    private var uiState by mutableStateOf(ExternalAudioUiState())
    private var requestJob: Job? = null
    private var progressJob: Job? = null
    private var providerCancellation: CancellationSignal? = null
    private var requestId = 0
    private var errorReported = false

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (session.player != null) session.recordPlaybackState(playbackState)
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (session.player === player) publishPlayerState(player)
        }

        override fun onPlayerError(error: PlaybackException) {
            showErrorAndClose(ExternalAudioFailure.PLAYBACK_FAILED)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        setFinishOnTouchOutside(true)
        onBackPressedDispatcher.addCallback(this) { finish() }
        setContent {
            SazanamiTheme(darkTheme = isSystemInDarkTheme()) {
                ExternalAudioPlayerScreen(
                    state = uiState,
                    onClose = ::finish,
                    onPlayPause = ::togglePlayPause,
                    onSeek = ::seekToFraction,
                    onSeekBack = { seekBy(backward = true) },
                    onSeekForward = { seekBy(backward = false) }
                )
            }
        }
        openRequest(
            incoming = intent,
            positionMs = savedInstanceState?.getLong(KEY_POSITION, 0L)?.coerceAtLeast(0L) ?: 0L,
            shouldPlay = savedInstanceState?.getBoolean(KEY_PLAY_WHEN_READY, true) ?: true,
            wasReady = savedInstanceState?.getBoolean(KEY_HAS_REACHED_READY, false) ?: false
        )
    }

    override fun onStart() {
        super.onStart()
        val density = resources.displayMetrics.density
        val availableWidth = (resources.displayMetrics.widthPixels - 32f * density)
            .roundToInt().coerceAtLeast(1)
        window.setGravity(Gravity.CENTER)
        window.setLayout(
            minOf((420f * density).roundToInt(), availableWidth),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!isFinishing) openRequest(intent, positionMs = 0L, shouldPlay = true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val player = session.player
        // Android already retains the launch Intent/grant for recreation. Never save another URI.
        outState.putLong(
            KEY_POSITION,
            player?.let { externalAudioRecreationPosition(it.currentPosition, it.isCurrentMediaItemSeekable) }
                ?: uiState.positionMs
        )
        outState.putBoolean(
            KEY_PLAY_WHEN_READY,
            player?.let { it.playWhenReady && it.playbackState != Player.STATE_ENDED }
                ?: uiState.showPause
        )
        outState.putBoolean(KEY_HAS_REACHED_READY, session.hasReachedReady)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        releasePreview()
        super.onStop()
        // Home, locking the screen, or another full-screen Activity ends this preview. Rotation
        // alone recreates it from the small position/play-state checkpoint above.
        if (!isChangingConfigurations && !isFinishing) finish()
    }

    override fun finish() {
        releasePreview()
        super.finish()
    }

    override fun onDestroy() {
        releasePreview()
        super.onDestroy()
    }

    private fun openRequest(
        incoming: Intent,
        positionMs: Long,
        shouldPlay: Boolean,
        wasReady: Boolean = false
    ) {
        releasePreview()
        session.beginRequest(wasReady)
        val generation = ++requestId
        errorReported = false
        uiState = ExternalAudioUiState(
            requestId = generation,
            displayName = getString(R.string.external_audio_title),
            positionMs = positionMs,
            isPreparing = session.isInitiallyPreparing,
            showPause = shouldPlay
        )
        validateExternalAudioIntent(incoming)?.let {
            showErrorAndClose(it)
            return
        }
        val signal = CancellationSignal().also { providerCancellation = it }
        val fallbackName = getString(R.string.external_audio_title)
        requestJob = lifecycleScope.launch {
            try {
                val request = withContext(Dispatchers.IO) {
                    resolveExternalAudioRequest(contentResolver, incoming, fallbackName, signal)
                }
                ensureActive()
                // API 35+ requires a foreground app for audio focus; do not request it before
                // this floating Activity has actually resumed.
                lifecycle.withResumed {
                    if (!isFinishing && !isDestroyed && generation == requestId) {
                        uiState = uiState.copy(displayName = request.displayName)
                        preparePlayer(request, positionMs, shouldPlay)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: OperationCanceledException) {
                if (!signal.isCanceled && isActive && generation == requestId && !isFinishing) {
                    showErrorAndClose(ExternalAudioFailure.INACCESSIBLE_AUDIO)
                }
            } catch (failure: ExternalAudioRequestException) {
                if (generation == requestId && !isFinishing) showErrorAndClose(failure.failure)
            } catch (_: Exception) {
                if (generation == requestId && !isFinishing) {
                    showErrorAndClose(ExternalAudioFailure.PLAYBACK_FAILED)
                }
            } finally {
                if (providerCancellation === signal) providerCancellation = null
            }
        }
    }

    private fun preparePlayer(request: ExternalAudioRequest, positionMs: Long, shouldPlay: Boolean) {
        val extractors = FragmentedMp4SeekExtractorsFactory(
            applicationContext,
            DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
        )
        val player = ExoPlayer.Builder(applicationContext)
            // Progressive local audio only; no adaptive/network source routing or shared pipeline.
            .setMediaSourceFactory(
                ProgressiveMediaSource.Factory(DefaultDataSource.Factory(applicationContext), extractors)
            )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        session.replace(player)
        player.addListener(playerListener)
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
            .build()
        player.setMediaItem(MediaItem.fromUri(request.uri), positionMs)
        player.playWhenReady = shouldPlay
        player.prepare()
        publishPlayerState(player)
        progressJob = lifecycleScope.launch {
            while (isActive && session.player === player) {
                publishPlayerState(player)
                delay(250L)
            }
        }
    }

    private fun publishPlayerState(player: Player) {
        if (session.player !== player || isFinishing || isDestroyed) return
        if ((player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_ENDED) &&
            !player.currentTracks.containsType(C.TRACK_TYPE_AUDIO)
        ) {
            showErrorAndClose(ExternalAudioFailure.UNSUPPORTED_AUDIO)
            return
        }
        val duration = knownExternalAudioDuration(player.duration)
        val position = player.currentPosition.coerceAtLeast(0L)
            .let { if (duration != null) it.coerceAtMost(duration) else it }
        uiState = uiState.copy(
            positionMs = position,
            durationMs = duration,
            isPreparing = session.isInitiallyPreparing,
            showPause = player.playWhenReady && player.playbackState != Player.STATE_ENDED &&
                player.playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_NONE,
            canPlayPause = true,
            canSeek = canSeekExternalAudio(player.isCurrentMediaItemSeekable, duration) &&
                player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
        )
    }

    private fun togglePlayPause() = withPlayer { player ->
        if (player.playWhenReady && player.playbackState != Player.STATE_ENDED &&
            player.playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_NONE
        ) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0L)
            player.play()
        }
    }

    private fun seekToFraction(fraction: Float) = withSeekablePlayer { player, duration ->
        externalAudioSeekPosition(fraction, duration)?.let(player::seekTo)
    }

    private fun seekBy(backward: Boolean) = withSeekablePlayer { player, duration ->
        val position = if (backward) externalAudioSeekBack(player.currentPosition) else {
            externalAudioSeekForward(player.currentPosition, duration) ?: return@withSeekablePlayer
        }
        player.seekTo(position.coerceAtMost(duration))
    }

    private inline fun withSeekablePlayer(action: (Player, Long) -> Unit) = withPlayer { player ->
        val duration = knownExternalAudioDuration(player.duration) ?: return@withPlayer
        if (canSeekExternalAudio(player.isCurrentMediaItemSeekable, duration) &&
            player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
        ) action(player, duration)
    }

    private inline fun withPlayer(action: (Player) -> Unit) {
        val player = session.player ?: return
        try {
            action(player)
            if (session.player === player) publishPlayerState(player)
        } catch (_: Exception) {
            showErrorAndClose(ExternalAudioFailure.PLAYBACK_FAILED)
        }
    }

    private fun showErrorAndClose(failure: ExternalAudioFailure) {
        if (errorReported || isFinishing || isDestroyed) return
        errorReported = true
        val message = when (failure) {
            ExternalAudioFailure.INVALID_REQUEST -> R.string.external_audio_invalid_request
            ExternalAudioFailure.UNSUPPORTED_URI -> R.string.external_audio_unsupported_uri
            ExternalAudioFailure.UNSUPPORTED_AUDIO -> R.string.external_audio_unsupported_audio
            ExternalAudioFailure.INACCESSIBLE_AUDIO -> R.string.external_audio_cannot_open
            ExternalAudioFailure.PLAYBACK_FAILED -> R.string.external_audio_cannot_play
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun releasePreview() {
        requestJob?.cancel()
        requestJob = null
        providerCancellation?.let { runCatching { it.cancel() } }
        providerCancellation = null
        progressJob?.cancel()
        progressJob = null
        session.player?.let { runCatching { it.removeListener(playerListener) } }
        session.close()
    }

    private companion object {
        const val KEY_POSITION = "external_audio_position"
        const val KEY_PLAY_WHEN_READY = "external_audio_play_when_ready"
        const val KEY_HAS_REACHED_READY = "external_audio_has_reached_ready"
    }
}
