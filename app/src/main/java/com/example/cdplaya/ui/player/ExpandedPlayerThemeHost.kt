package com.example.cdplaya.ui.player

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.RepeatMode
import com.example.cdplaya.ui.player.classicwheel.ClassicWheelExpandedPlayer
import com.example.cdplaya.ui.player.classicwheel.ClassicWheelPlayerMorph
import com.example.cdplaya.ui.player.classicwheel.playerMorphRendererFor
import com.example.cdplaya.ui.player.classicwheel.PlayerMorphRenderer
import com.example.cdplaya.ui.player.classicwheel.resolveClassicWheelMorphGeometry
import com.example.cdplaya.ui.player.classicwheel.ClassicWheelMorphBounds
import com.example.cdplaya.ui.player.classicwheel.resolveClassicWheelSharedGeometry
import com.example.cdplaya.ui.player.classicwheel.classicWheelMorphTravelDistance
import com.example.cdplaya.ui.player.modern.ModernExpandedPlayer
import com.example.cdplaya.ui.player.modern.ModernArtworkTransitionStyle
import com.example.cdplaya.ui.player.modern.ModernSeekbarStyle
import com.example.cdplaya.ui.player.modern.selectNearbyWaveformSongs
import com.example.cdplaya.ui.player.pocketcassette.PocketCassetteExpandedPlayer
import com.example.cdplaya.ui.player.pocketflip.PocketFlipExpandedPlayer
import com.example.cdplaya.ui.player.retrorack.RetroRackExpandedPlayer
import com.example.cdplaya.ui.player.theme.PlayerThemeTokens
import com.example.cdplaya.ui.player.PlayerEndpointBounds
import com.example.cdplaya.ui.player.modern.DefaultPlayerMorph
import com.example.cdplaya.ui.player.modern.DefaultPlayerMorphBounds
import com.example.cdplaya.ui.player.modern.ModernPlayerDefaults
import com.example.cdplaya.ui.player.modern.resolveDefaultPlayerMorphGeometry
import com.example.cdplaya.ui.player.modern.rememberModernArtworkCarouselPresentation
import com.example.cdplaya.ui.player.modern.shouldRunDefaultExpandedWork
import com.example.cdplaya.ui.player.modern.defaultMorphTravelDistance

@Composable
fun ExpandedPlayerThemeHost(
    selectedPlayerTheme: PlayerTheme,
    tokens: PlayerThemeTokens,
    currentSong: Song?,
    previousPreviewSong: Song?,
    nextPreviewSong: Song?,
    modernArtworkTransitionStyle: ModernArtworkTransitionStyle,
    modernSeekbarStyle: ModernSeekbarStyle,
    isVisualizerWorkAllowed: Boolean,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    currentPosition: Int,
    duration: Int,
    isCurrentSongFavorite: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekChange: (Int) -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onCollapseClick: () -> Unit,
    playerMorphState: PlayerMorphState,
    lyricsTransitionState: PlayerLyricsTransitionState,
    onOpenUpNextClick: () -> Unit,
    onOpenSleepTimerClick: () -> Unit,
    onOpenMoreClick: () -> Unit,
    onToggleFavoriteClick: (Song) -> Unit,
    songs: List<Song>,
    upcomingSongs: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit,
    endpointBounds: PlayerEndpointBounds,
    defaultMorphBounds: DefaultPlayerMorphBounds,
    classicMorphBounds: ClassicWheelMorphBounds
) {
    val shouldLoadWaveform = shouldLoadExpandedPlayerWaveform(
        selectedPlayerTheme = selectedPlayerTheme,
        modernSeekbarStyle = modernSeekbarStyle
    ) && (
            selectedPlayerTheme != PlayerTheme.DEFAULT ||
                    shouldRunDefaultExpandedWork(playerMorphState.progress)
            )
    val shouldPrefetchWaveforms = selectedPlayerTheme == PlayerTheme.DEFAULT &&
            modernSeekbarStyle.usesWaveformData
    val nearbyWaveformSongs = remember(
        shouldPrefetchWaveforms,
        currentSong?.id,
        currentSong?.filePath,
        nextPreviewSong?.id,
        nextPreviewSong?.filePath,
        previousPreviewSong?.id,
        previousPreviewSong?.filePath
    ) {
        if (shouldPrefetchWaveforms && currentSong != null) {
            selectNearbyWaveformSongs(
                currentSong = currentSong,
                nextSong = nextPreviewSong,
                previousSong = previousPreviewSong
            )
        } else {
            emptyList()
        }
    }
    val waveformData = rememberExpandedPlayerWaveformData(
        currentSong = currentSong,
        shouldLoad = shouldLoadWaveform,
        prefetchSongs = nearbyWaveformSongs
    )

    var hostHeightPx by remember { mutableFloatStateOf(1f) }
    var hostDragOffset by remember { mutableFloatStateOf(0f) }
    val hostDragState = rememberDraggableState { delta ->
        if (delta < 0f && lyricsTransitionState.progress == 0f) {
            lyricsTransitionState.beginOpeningDrag()
        }
        hostDragOffset = (hostDragOffset + delta).coerceAtMost(0f)
        lyricsTransitionState.dragOpeningBy(delta, hostHeightPx)
    }
    val sharedGestureModifier = if (selectedPlayerTheme == PlayerTheme.DEFAULT) {
        Modifier
    } else {
        Modifier
            .onSizeChanged { size -> hostHeightPx = size.height.toFloat().coerceAtLeast(1f) }
            .draggable(
                state = hostDragState,
                orientation = Orientation.Vertical,
                enabled = !lyricsTransitionState.lyricsInteractive,
                onDragStarted = { hostDragOffset = 0f },
                onDragStopped = { velocity ->
                    lyricsTransitionState.settleOpening(velocity)
                    hostDragOffset = 0f
                }
            )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Open lyrics") {
                        lyricsTransitionState.openLyrics()
                        true
                    }
                )
            }
            .then(sharedGestureModifier)
    ) {
    when (selectedPlayerTheme) {
        PlayerTheme.DEFAULT -> {
            if (currentSong != null) {
                val modernStyle = ModernPlayerDefaults.style()
                val geometry = resolveDefaultPlayerMorphGeometry(
                    progress = playerMorphState.progress,
                    endpointBounds = endpointBounds,
                    elementBounds = defaultMorphBounds
                )
                val carouselPresentation =
                    rememberModernArtworkCarouselPresentation(
                        currentSong = currentSong,
                        previousPreviewSong = previousPreviewSong,
                        nextPreviewSong = nextPreviewSong,
                        onPreviousClick = onPreviousClick,
                        onNextClick = onNextClick
                    )
                DefaultPlayerMorph(
                    progress = playerMorphState.progress,
                    geometry = geometry,
                    carouselPresentation = carouselPresentation,
                    artworkTransitionStyle = modernArtworkTransitionStyle,
                    isPlaying = isPlaying,
                    onPlayPauseClick = onPlayPauseClick,
                    style = modernStyle
                ) { visualState ->
                    ModernExpandedPlayer(
                        currentSong = currentSong,
                        previousPreviewSong = previousPreviewSong,
                        nextPreviewSong = nextPreviewSong,
                        artworkTransitionStyle = modernArtworkTransitionStyle,
                        seekbarStyle = modernSeekbarStyle,
                        waveformData = waveformData,
                        isPlaying = isPlaying,
                        isShuffleEnabled = isShuffleEnabled,
                        repeatMode = repeatMode,
                        currentPosition = currentPosition,
                        duration = duration,
                        isCurrentSongFavorite = isCurrentSongFavorite,
                        onPlayPauseClick = onPlayPauseClick,
                        onPreviousClick = onPreviousClick,
                        onNextClick = onNextClick,
                        onSeekChange = onSeekChange,
                        onShuffleClick = onShuffleClick,
                        onRepeatClick = onRepeatClick,
                        onCollapseClick = onCollapseClick,
                        playerMorphState = playerMorphState,
                        lyricsTransitionState = lyricsTransitionState,
                        onOpenUpNextClick = onOpenUpNextClick,
                        onToggleFavoriteClick = onToggleFavoriteClick,
                        style = modernStyle,
                        defaultMorphBounds = defaultMorphBounds,
                        defaultMorphVisualState = visualState,
                        carouselPresentation = carouselPresentation,
                        defaultMorphDragRangePx = defaultMorphTravelDistance(
                            endpointBounds = endpointBounds,
                            elementBounds = defaultMorphBounds
                        )
                    )
                }
            }
        }

        PlayerTheme.CLASSIC_WHEEL -> {
            val geometry = resolveClassicWheelMorphGeometry(
                playerMorphState.progress, endpointBounds
            )
            val sharedGeometry = resolveClassicWheelSharedGeometry(
                playerMorphState.progress, classicMorphBounds
            )
            ClassicWheelPlayerMorph(
                progress = playerMorphState.progress,
                geometry = geometry,
                sharedGeometry = sharedGeometry,
                currentSong = currentSong,
                isPlaying = isPlaying,
                tokens = tokens
            ) { screenAlpha, wheelAlpha, controlsActive -> ClassicWheelExpandedPlayer(
                currentSong = currentSong,
                isPlaying = isPlaying,
                isShuffleEnabled = isShuffleEnabled,
                repeatMode = repeatMode,
                currentPosition = currentPosition,
                duration = duration,
                isCurrentSongFavorite = isCurrentSongFavorite,
                onPlayPauseClick = onPlayPauseClick,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onSeekChange = onSeekChange,
                onShuffleClick = onShuffleClick,
                onRepeatClick = onRepeatClick,
                onCollapseClick = onCollapseClick,
                onOpenUpNextClick = onOpenUpNextClick,
                onToggleFavoriteClick = onToggleFavoriteClick,
                songs = songs,
                onSongClick = onSongClick,
                tokens = tokens,
                screenAlpha = screenAlpha,
                wheelAlpha = wheelAlpha,
                wheelInputEnabled = controlsActive,
                morphBounds = classicMorphBounds,
                sharedContentVisible = sharedGeometry == null
                ,onMorphDragStart = {
                    playerMorphState.beginDragWithRange(classicWheelMorphTravelDistance(endpointBounds))
                }
                ,onMorphDragBy = playerMorphState::dragBy
                ,onMorphDragEnd = playerMorphState::endDrag
                ,onMorphDragCancel = playerMorphState::cancelDrag
                ,wheelPlayControlAlpha = if (controlsActive) 1f else 0f
            ) }
        }

        PlayerTheme.RETRO_RACK -> {
            RetroRackExpandedPlayer(
                currentSong = currentSong,
                waveformData = waveformData,
                isVisualizerWorkAllowed = isVisualizerWorkAllowed,
                isPlaying = isPlaying,
                isShuffleEnabled = isShuffleEnabled,
                repeatMode = repeatMode,
                currentPosition = currentPosition,
                duration = duration,
                isCurrentSongFavorite = isCurrentSongFavorite,
                upcomingSongs = upcomingSongs,
                onPlayPauseClick = onPlayPauseClick,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onSeekChange = onSeekChange,
                onShuffleClick = onShuffleClick,
                onRepeatClick = onRepeatClick,
                onCollapseClick = onCollapseClick,
                onOpenUpNextClick = onOpenUpNextClick,
                onToggleFavoriteClick = onToggleFavoriteClick,
                onSongClick = onSongClick,
                tokens = tokens
            )
        }

        PlayerTheme.POCKET_FLIP -> {
            PocketFlipExpandedPlayer(
                currentSong = currentSong,
                waveformData = waveformData,
                isVisualizerWorkAllowed = isVisualizerWorkAllowed,
                isPlaying = isPlaying,
                isShuffleEnabled = isShuffleEnabled,
                repeatMode = repeatMode,
                currentPosition = currentPosition,
                duration = duration,
                isCurrentSongFavorite = isCurrentSongFavorite,
                onPlayPauseClick = onPlayPauseClick,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onSeekChange = onSeekChange,
                onShuffleClick = onShuffleClick,
                onRepeatClick = onRepeatClick,
                onCollapseClick = onCollapseClick,
                onOpenUpNextClick = onOpenUpNextClick,
                onToggleFavoriteClick = onToggleFavoriteClick,
                tokens = tokens
            )
        }

        PlayerTheme.POCKET_CASSETTE -> {
            PocketCassetteExpandedPlayer(
                currentSong = currentSong,
                isVisualizerWorkAllowed = isVisualizerWorkAllowed,
                isPlaying = isPlaying,
                isShuffleEnabled = isShuffleEnabled,
                repeatMode = repeatMode,
                currentPosition = currentPosition,
                duration = duration,
                isCurrentSongFavorite = isCurrentSongFavorite,
                onPlayPauseClick = onPlayPauseClick,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onSeekChange = onSeekChange,
                onShuffleClick = onShuffleClick,
                onRepeatClick = onRepeatClick,
                onCollapseClick = onCollapseClick,
                onOpenUpNextClick = onOpenUpNextClick,
                onToggleFavoriteClick = onToggleFavoriteClick,
                tokens = tokens
            )
        }

    }
    }
}
