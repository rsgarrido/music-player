package com.example.cdplaya.lyrics

import com.example.cdplaya.data.Song
import com.example.cdplaya.ui.state.PlaybackUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

fun interface LyricsPositionSource {
    fun currentPositionMs(): Long
}

class LyricsPlaybackController(
    private val repository: LocalLyricsRepository,
    playbackState: StateFlow<PlaybackUiState>,
    private val positionSource: LyricsPositionSource,
    private val scope: CoroutineScope,
    private val tickerIntervalMs: Long = DEFAULT_TICKER_INTERVAL_MS
) {
    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow<LyricsPlaybackUiState>(
        LyricsPlaybackUiState.Hidden
    )
    val uiState: StateFlow<LyricsPlaybackUiState> = _uiState

    private var currentSong: Song? = null
    private var currentContent: Content = Content.None
    private var isVisible = false
    private var isPlaying = playbackState.value.isPlaying
    private var autoFollowEnabled = true
    private var lookupJob: Job? = null
    private var tickerJob: Job? = null

    init {
        scope.launch {
            playbackState
                .map { state -> state.currentSong }
                .distinctUntilChanged { first, second -> first?.id == second?.id }
                .collect { song -> onSongChanged(song) }
        }
        scope.launch {
            playbackState
                .map { state -> state.isPlaying }
                .distinctUntilChanged()
                .collect { playing ->
                    isPlaying = playing
                    updatePositionNow()
                    updateTicker()
                }
        }
    }

    fun setVisible(visible: Boolean) {
        if (isVisible == visible) return
        isVisible = visible
        if (visible) {
            autoFollowEnabled = true
            updatePositionNow()
        }
        publish()
        updateTicker()
    }

    fun suspendAutoFollow() {
        if (!autoFollowEnabled) return
        autoFollowEnabled = false
        publish()
    }

    fun returnToCurrentLine() {
        if (autoFollowEnabled) return
        autoFollowEnabled = true
        updatePositionNow()
    }

    fun onSeek(positionMs: Long) {
        updatePosition(positionMs)
    }

    fun rescan() {
        startLookup(currentSong, refreshIndex = true)
    }

    private fun onSongChanged(song: Song?) {
        currentSong = song
        currentContent = if (song == null) Content.None else Content.Loading
        autoFollowEnabled = true
        publish()
        startLookup(song, refreshIndex = false)
        updatePositionNow()
    }

    private fun startLookup(song: Song?, refreshIndex: Boolean) {
        lookupJob?.cancel()
        if (song == null) return
        currentContent = Content.Loading
        publish()
        updateTicker()
        lookupJob = scope.launch {
            try {
                if (refreshIndex) repository.refreshIndex()
                val result = repository.findLyrics(song.toLyricsIdentity())
                if (currentSong?.id != song.id) return@launch
                currentContent = result.toContent()
                publish()
                updatePositionNow()
                updateTicker()
            } catch (cancellation: CancellationException) {
                throw cancellation
            }
        }
    }

    private fun updateTicker() {
        tickerJob?.cancel()
        tickerJob = null
        if (!isVisible || !isPlaying || currentContent !is Content.Synced) return
        tickerJob = scope.launch {
            while (true) {
                updatePositionNow()
                delay(tickerIntervalMs)
            }
        }
    }

    private fun updatePositionNow() {
        if (currentContent is Content.Synced) {
            updatePosition(positionSource.currentPositionMs())
        } else {
            publish()
        }
    }

    private fun updatePosition(positionMs: Long) {
        val content = currentContent as? Content.Synced ?: return
        val active = ActiveLyricResolver.resolve(content.document.cues, positionMs)
        if (active != content.activeGroup) {
            currentContent = content.copy(activeGroup = active)
            publish()
        } else if (isVisible && _uiState.value is LyricsPlaybackUiState.Hidden) {
            publish()
        }
    }

    private fun publish() {
        _uiState.value = if (!isVisible) {
            LyricsPlaybackUiState.Hidden
        } else {
            when (val content = currentContent) {
                Content.None -> LyricsPlaybackUiState.Hidden
                Content.Loading -> currentSong?.let(LyricsPlaybackUiState::Loading)
                    ?: LyricsPlaybackUiState.Hidden
                is Content.Synced -> LyricsPlaybackUiState.Synced(
                    song = content.song,
                    lyrics = content.document,
                    activeGroup = content.activeGroup,
                    autoFollowEnabled = autoFollowEnabled
                )
                is Content.Unsynced -> LyricsPlaybackUiState.Unsynced(
                    song = content.song,
                    lyrics = content.document
                )
                is Content.Unavailable -> LyricsPlaybackUiState.Unavailable(
                    song = content.song,
                    reason = content.reason
                )
            }
        }
    }

    private fun LyricsLookupResult.toContent(): Content = when (this) {
        is LyricsLookupResult.Found -> when (val document = lyrics.document) {
            is LyricsDocument.Synced -> Content.Synced(
                song = requireNotNull(currentSong),
                document = document
            )
            is LyricsDocument.Unsynced -> Content.Unsynced(
                song = requireNotNull(currentSong),
                document = document
            )
        }
        LyricsLookupResult.NoRootsConfigured -> unavailable(
            LyricsUnavailableReason.NoRootsConfigured
        )
        LyricsLookupResult.NotFound -> unavailable(LyricsUnavailableReason.NotFound)
        is LyricsLookupResult.Ambiguous -> unavailable(
            LyricsUnavailableReason.Ambiguous(candidates)
        )
        is LyricsLookupResult.PermissionLost -> unavailable(
            LyricsUnavailableReason.PermissionLost(rootUri)
        )
        is LyricsLookupResult.RootScanError -> unavailable(
            LyricsUnavailableReason.RootScanError(rootUri)
        )
        is LyricsLookupResult.StaleFile -> unavailable(
            LyricsUnavailableReason.StaleFile(documentUri)
        )
        is LyricsLookupResult.ReadError -> unavailable(
            LyricsUnavailableReason.ReadError(documentUri)
        )
        is LyricsLookupResult.InvalidLyrics -> unavailable(
            LyricsUnavailableReason.InvalidLyrics(documentUri)
        )
    }

    private fun unavailable(reason: LyricsUnavailableReason): Content =
        Content.Unavailable(requireNotNull(currentSong), reason)

    private sealed interface Content {
        data object None : Content
        data object Loading : Content
        data class Synced(
            val song: Song,
            val document: LyricsDocument.Synced,
            val activeGroup: ActiveLyricGroup? = null
        ) : Content
        data class Unsynced(
            val song: Song,
            val document: LyricsDocument.Unsynced
        ) : Content
        data class Unavailable(
            val song: Song,
            val reason: LyricsUnavailableReason
        ) : Content
    }

    companion object {
        const val DEFAULT_TICKER_INTERVAL_MS = 100L
    }
}
