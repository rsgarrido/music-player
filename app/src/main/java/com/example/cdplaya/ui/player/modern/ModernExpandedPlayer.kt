package com.example.cdplaya.ui.player.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.RepeatMode
import com.example.cdplaya.player.audioquality.AudioQualityRepository
import com.example.cdplaya.player.waveform.WaveformData
import com.example.cdplaya.player.waveform.WaveformRepository
import com.example.cdplaya.ui.player.PlayerMorphState
import com.example.cdplaya.ui.player.PlayerPresentation
import com.example.cdplaya.ui.player.PlayerLyricsTransitionState

@Composable
fun ModernExpandedPlayer(
    currentSong: Song?,
    previousPreviewSong: Song? = null,
    nextPreviewSong: Song? = null,
    artworkTransitionStyle: ModernArtworkTransitionStyle = ModernArtworkTransitionStyle.SLIDE,
    seekbarStyle: ModernSeekbarStyle = ModernSeekbarStyle.CLASSIC_BAR,
    waveformData: WaveformData? = null,
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
    onToggleFavoriteClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
    style: ModernPlayerStyle = ModernPlayerDefaults.style(),
    albumArtSize: Dp = ModernPlayerDefaults.MaximumArtworkSize,
    defaultMorphBounds: DefaultPlayerMorphBounds? = null,
    defaultMorphVisualState: DefaultPlayerMorphVisualState? = null,
    defaultMorphDragRangePx: Float? = null,
    lyricsContent: @Composable () -> Unit = {}
) {
    if (currentSong == null) {
        return
    }

    val context = LocalContext.current
    val audioQualityRepository = remember(context) { AudioQualityRepository(context) }
    val carouselState = rememberModernArtworkCarouselState(
        onPrevious = onPreviousClick,
        onNext = onNextClick
    )
    val actualCarouselSongs = ModernCarouselSongs(
        current = currentSong,
        previous = previousPreviewSong,
        next = nextPreviewSong
    )
    var displayedCarouselSongs by remember {
        mutableStateOf(actualCarouselSongs)
    }
    val latestActualCarouselSongs by rememberUpdatedState(actualCarouselSongs)

    LaunchedEffect(currentSong.id) {
        if (displayedCarouselSongs.current.id != currentSong.id) {
            val transition = carouselState.consumeTransitionForSongChange(
                newSongId = currentSong.id
            )
            val hasMatchingPreview = transition?.let { pending ->
                displayedCarouselSongs.previewFor(pending.direction)?.id ==
                    currentSong.id
            } ?: false

            if (transition != null && hasMatchingPreview) {
                carouselState.animateSongChange(
                    direction = transition.direction,
                    durationMillis = if (transition.startedFromDrag) {
                        MODERN_ARTWORK_ACCEPTED_DRAG_DURATION_MILLIS
                    } else {
                        MODERN_ARTWORK_BUTTON_TRANSITION_DURATION_MILLIS
                    }
                )
            }

            displayedCarouselSongs = latestActualCarouselSongs
            carouselState.resetForSongChange()
        }
    }

    LaunchedEffect(actualCarouselSongs) {
        if (displayedCarouselSongs.current.id == actualCarouselSongs.current.id) {
            displayedCarouselSongs = actualCarouselSongs
        }
    }

    val onPreviousButtonClick = {
        carouselState.recordButtonNavigation(
            direction = ModernCarouselDirection.PREVIOUS,
            sourceSongId = currentSong.id
        )
        onPreviousClick()
    }
    val onNextButtonClick = {
        carouselState.recordButtonNavigation(
            direction = ModernCarouselDirection.NEXT,
            sourceSongId = currentSong.id
        )
        onNextClick()
    }

    var containerHeightPx by remember { mutableFloatStateOf(1f) }
    var isMorphDrag by remember { mutableStateOf(false) }
    val verticalDragState = rememberDraggableState { deltaY ->
        if (playerMorphState.progress < 1f &&
            lyricsTransitionState.progress == 0f
        ) {
            isMorphDrag = true
            playerMorphState.dragBy(deltaY)
        } else if (deltaY < 0f || lyricsTransitionState.progress > 0f) {
            if (lyricsTransitionState.progress == 0f) {
                lyricsTransitionState.beginOpeningDrag()
            }
            lyricsTransitionState.dragOpeningBy(deltaY, containerHeightPx)
            playerMorphState.updateProgressFromDrag(1f)
        } else {
            isMorphDrag = true
            playerMorphState.dragBy(deltaY)
        }
    }
    val dragProgress = 1f - playerMorphState.progress
    val morphOwnsPersistentContent = defaultMorphVisualState?.isReady == true

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Color.Black.copy(
                    alpha = if (defaultMorphVisualState == null) {
                        0.24f * (1f - dragProgress)
                    } else {
                        0f
                    }
                )
            )
            .onSizeChanged { size ->
                containerHeightPx = size.height.toFloat().coerceAtLeast(1f)
            }
    ) {
        val foregroundAlbumArtSize = minOf(
            albumArtSize,
            maxWidth - 32.dp,
            maxHeight * 0.42f
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (defaultMorphVisualState == null) {
                        translationY = dragProgress * containerHeightPx * 0.46f
                        val contentScale = 1f - dragProgress * 0.04f
                        scaleX = contentScale
                        scaleY = contentScale
                        alpha = 1f - dragProgress * 0.1f
                        shape = RoundedCornerShape(28.dp * dragProgress)
                        clip = dragProgress > 0f
                    }
                }
                .background(
                    if (defaultMorphVisualState == null) {
                        style.backgroundColor
                    } else {
                        Color.Transparent
                    }
                )
                .draggable(
                    state = verticalDragState,
                    orientation = Orientation.Vertical,
                    onDragStarted = {
                        isMorphDrag = playerMorphState.progress < 1f
                        val morphDragRange = defaultMorphDragRangePx
                        if (morphDragRange != null) {
                            playerMorphState.beginDragWithRange(morphDragRange)
                        } else {
                            playerMorphState.beginDrag(containerHeightPx)
                        }
                    },
                    enabled = !lyricsTransitionState.lyricsInteractive,
                    onDragStopped = { velocityY ->
                        if (!isMorphDrag && (
                            lyricsTransitionState.progress > 0f ||
                            playerMorphState.progress >= 1f &&
                            velocityY <=
                            PlayerLyricsTransitionState.OPEN_VELOCITY_PX_PER_SECOND
                            )
                        ) {
                            playerMorphState.snapTo(PlayerPresentation.Expanded)
                            lyricsTransitionState.settleOpening(velocityY)
                        } else {
                            playerMorphState.endDrag(velocityY)
                        }
                        isMorphDrag = false
                    }
                )
        ) {
            if (defaultMorphVisualState == null ||
                defaultMorphVisualState.expensiveContentActive
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = defaultMorphVisualState?.backgroundAlpha ?: 1f
                        }
                ) {
                    ModernPlayerBackground(
                        currentSong = currentSong,
                        style = style
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (defaultMorphVisualState == null) {
                            alpha = 1f - dragProgress * 0.18f
                            translationY = dragProgress * 14.dp.toPx()
                        }
                    }
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(
                        horizontal = ModernPlayerDefaults.ContentHorizontalPadding,
                        vertical = ModernPlayerDefaults.ContentVerticalPadding
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ModernPlayerArtwork(
                    carouselSongs = displayedCarouselSongs,
                    carouselState = carouselState,
                    artworkSize = foregroundAlbumArtSize,
                    transitionStyle = artworkTransitionStyle,
                    style = style,
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            defaultMorphBounds?.updateExpandedArtwork(
                                coordinates.boundsInRoot()
                            )
                        }
                        .hiddenFromDefaultMorph(morphOwnsPersistentContent),
                    gesturesEnabled = !lyricsTransitionState.lyricsInteractive,
                    renderArtwork = defaultMorphVisualState == null
                )

                Spacer(modifier = Modifier.height(24.dp))

                ModernPlayerMetadataCarousel(
                    carouselSongs = displayedCarouselSongs,
                    carouselState = carouselState,
                    audioQualityRepository = audioQualityRepository,
                    transitionStyle = artworkTransitionStyle,
                    style = style,
                    modifier = Modifier.fillMaxWidth(),
                    onPersistentContentBoundsChanged = { bounds ->
                        defaultMorphBounds?.updateExpandedText(bounds)
                    },
                    hidePersistentContent = morphOwnsPersistentContent,
                    expandedContentAlpha =
                        defaultMorphVisualState?.metadataAlpha ?: 1f,
                    loadExpandedMetadata =
                        defaultMorphVisualState?.expensiveContentActive ?: true
                )

                lyricsContent()

                Spacer(modifier = Modifier.weight(1f))

                ModernPlayerSeekBar(
                    currentPosition = currentPosition,
                    duration = duration,
                    onSeekChange = if (defaultMorphVisualState == null ||
                        playerMorphState.settledPresentation ==
                        PlayerPresentation.Expanded
                    ) {
                        onSeekChange
                    } else {
                        {}
                    },
                    seekbarStyle = seekbarStyle,
                    waveformSeed = "${currentSong.id}|${currentSong.filePath}|${currentSong.title}",
                    waveformData = waveformData,
                    style = style,
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = defaultMorphVisualState?.metadataAlpha ?: 1f
                        }
                        .suppressDefaultMorphSemantics(
                            defaultMorphVisualState != null &&
                                    playerMorphState.settledPresentation !=
                                    PlayerPresentation.Expanded
                        )
                )

                Spacer(modifier = Modifier.height(18.dp))

                ModernPlayerControls(
                    isPlaying = isPlaying,
                    isShuffleEnabled = isShuffleEnabled,
                    repeatMode = repeatMode,
                    onPlayPauseClick = onPlayPauseClick,
                    onPreviousClick = onPreviousButtonClick,
                    onNextClick = onNextButtonClick,
                    onShuffleClick = onShuffleClick,
                    onRepeatClick = onRepeatClick,
                    style = style,
                    modifier = Modifier.suppressDefaultMorphSemantics(
                        defaultMorphVisualState != null &&
                                playerMorphState.settledPresentation !=
                                PlayerPresentation.Expanded
                    ),
                    primaryControlModifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            defaultMorphBounds?.updateExpandedPlayPause(
                                coordinates.boundsInRoot()
                            )
                        }
                        .hiddenFromDefaultMorph(morphOwnsPersistentContent),
                    expandedControlsAlpha =
                        defaultMorphVisualState?.controlsAlpha ?: 1f,
                    controlsEnabled = defaultMorphVisualState == null ||
                            playerMorphState.settledPresentation ==
                            PlayerPresentation.Expanded
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

internal fun selectNearbyWaveformSongs(
    currentSong: Song,
    nextSong: Song?,
    previousSong: Song?
): List<Song> {
    return listOfNotNull(nextSong, previousSong)
        .asSequence()
        .filterNot { song ->
            song.id == currentSong.id && song.filePath == currentSong.filePath
        }
        .distinctBy { song -> song.id to song.filePath }
        .take(WaveformRepository.MAX_PREFETCH_COUNT)
        .toList()
}
