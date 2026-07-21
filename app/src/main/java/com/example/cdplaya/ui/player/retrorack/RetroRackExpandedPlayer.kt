package com.example.cdplaya.ui.player.retrorack

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import coil.compose.AsyncImage
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.RepeatMode
import com.example.cdplaya.player.waveform.WaveformData
import com.example.cdplaya.ui.player.buildTrackReactiveVisualizerLevels
import com.example.cdplaya.ui.player.theme.PlayerThemeTokens
import kotlin.math.sin

@Composable
fun RetroRackExpandedPlayer(
    currentSong: Song?,
    waveformData: WaveformData? = null,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    currentPosition: Int,
    duration: Int,
    isCurrentSongFavorite: Boolean,
    upcomingSongs: List<Song>,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekChange: (Int) -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onCollapseClick: () -> Unit,
    onOpenUpNextClick: () -> Unit,
    onToggleFavoriteClick: (Song) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    tokens: PlayerThemeTokens = RetroRackDefaultTokens
) {
    val palette = remember(tokens) { RetroRackPalette.from(tokens) }
    val playbackContext = listOfNotNull(currentSong) + upcomingSongs
    val configuration = LocalConfiguration.current
    val compact = configuration.screenHeightDp < 700 || configuration.screenWidthDp < 360
    val fontScale = LocalDensity.current.fontScale
    val mainDeckHeight = when {
        compact && fontScale > 1.15f -> 212.dp
        compact -> 202.dp
        fontScale > 1.15f -> 230.dp
        else -> 216.dp
    }
    val visualProfile = remember(
        currentSong?.id,
        currentSong?.title,
        currentSong?.artist,
        currentSong?.album
    ) {
        buildRetroRackVisualProfile(
            songId = currentSong?.id,
            title = currentSong?.title,
            artist = currentSong?.artist,
            album = currentSong?.album
        )
    }

    CompositionLocalProvider(LocalRetroRackPalette provides palette) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RackBackground)
            .padding(
                horizontal = if (compact) 5.dp else 8.dp,
                vertical = if (compact) 6.dp else 10.dp
            ),
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 7.dp)
    ) {
        RackModule(
            title = "CDPLAYA // MAIN DECK",
            modifier = Modifier.height(mainDeckHeight),
            trailingAction = {
                RackIconButton(
                    icon = Icons.Filled.Close,
                    label = "CLOSE",
                    compact = true,
                    onClick = onCollapseClick
                )
            }
        ) {
            MainDeck(
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
                onToggleFavoriteClick = onToggleFavoriteClick,
                compact = compact
            )
        }

        RackModule(
            title = "SPECTRUM MONITOR // VISUAL",
            modifier = Modifier.height(if (compact) 72.dp else 88.dp),
            trailingAction = {
                RackIndicator(color = visualProfile.accent)
            }
        ) {
            DecorativeSpectrum(
                profile = visualProfile,
                waveformData = waveformData,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                modifier = Modifier.fillMaxSize()
            )
        }

        RackModule(
            title = "PLAYBACK RACK // ${playbackContext.size.toString().padStart(2, '0')} TRACKS",
            modifier = Modifier.weight(1f),
            trailingAction = {
                RackIconButton(
                    icon = Icons.Filled.List,
                    label = "QUEUE",
                    active = true,
                    compact = true,
                    onClick = onOpenUpNextClick
                )
            }
        ) {
            RackPlaylist(
                currentSong = currentSong,
                upcomingSongs = upcomingSongs,
                playbackContext = playbackContext,
                onSongClick = onSongClick
            )
        }
    }
    }
}

@Composable
private fun MainDeck(
    currentSong: Song?,
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
    onToggleFavoriteClick: (Song) -> Unit,
    compact: Boolean
) {
    val fontScale = LocalDensity.current.fontScale
    val displayHeight = when {
        compact && fontScale > 1.15f -> 76.dp
        compact -> 68.dp
        fontScale > 1.15f -> 84.dp
        else -> 76.dp
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 5.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(displayHeight),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            AsyncImage(
                model = currentSong?.albumArtUri,
                contentDescription = "Current album artwork",
                modifier = Modifier
                    .size(displayHeight)
                    .background(DisplayBlack)
                    .rackBevel()
                    .padding(2.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(DisplayBlack)
                    .rackBevel()
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currentSong?.title?.uppercase() ?: "NO TRACK LOADED",
                    color = LcdGreen,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 12.sp else 13.sp,
                    lineHeight = if (compact) 14.sp else 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentSong?.artist?.uppercase().orEmpty(),
                    color = LcdGreenDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LcdLabel(text = if (isPlaying) "PLAY" else "PAUSE")
                    LcdLabel(text = "320K")
                    LcdLabel(text = "44.1K")
                    Text(
                        text = "${formatRackTime(currentPosition)} / ${formatRackTime(duration)}",
                        color = LcdGreen,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        lineHeight = 11.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Slider(
            value = currentPosition.coerceIn(0, duration.coerceAtLeast(1)).toFloat(),
            onValueChange = { value -> onSeekChange(value.toInt()) },
            valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = ControlSilver,
                activeTrackColor = LcdGreen,
                inactiveTrackColor = InactiveTrack
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RackIconButton(
                icon = Icons.Filled.Shuffle,
                label = "SHUF",
                active = isShuffleEnabled,
                compact = compact,
                onClick = onShuffleClick
            )
            Spacer(modifier = Modifier.weight(1f))
            RackIconButton(
                icon = Icons.Filled.KeyboardArrowLeft,
                label = "PREV",
                compact = compact,
                onClick = onPreviousClick
            )
            RackIconButton(
                icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                label = if (isPlaying) "PAUSE" else "PLAY",
                active = true,
                compact = compact,
                onClick = onPlayPauseClick
            )
            RackIconButton(
                icon = Icons.Filled.KeyboardArrowRight,
                label = "NEXT",
                compact = compact,
                onClick = onNextClick
            )
            Spacer(modifier = Modifier.weight(1f))
            RackIconButton(
                icon = Icons.Filled.Repeat,
                label = when (repeatMode) {
                    RepeatMode.OFF -> "REP"
                    RepeatMode.ALL -> "ALL"
                    RepeatMode.ONE -> "ONE"
                },
                active = repeatMode != RepeatMode.OFF,
                compact = compact,
                onClick = onRepeatClick
            )
            RackIconButton(
                icon = if (isCurrentSongFavorite) {
                    Icons.Filled.Favorite
                } else {
                    Icons.Filled.FavoriteBorder
                },
                label = "FAV",
                active = isCurrentSongFavorite,
                compact = compact,
                onClick = { currentSong?.let(onToggleFavoriteClick) }
            )
        }
    }
}

@Composable
private fun DecorativeSpectrum(
    profile: RetroRackVisualProfile,
    waveformData: WaveformData?,
    isPlaying: Boolean,
    currentPosition: Int,
    duration: Int,
    modifier: Modifier = Modifier
) {
    val phase = remember(profile) { Animatable(0f) }
    LaunchedEffect(isPlaying, profile) {
        if (isPlaying) {
            while (true) {
                phase.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1_600, easing = LinearEasing)
                )
                phase.snapTo(0f)
            }
        }
    }
    Canvas(
        modifier = modifier
            .background(DisplayBlack)
            .rackBevel()
            .padding(8.dp)
    ) {
        val trackReactiveLevels = buildTrackReactiveVisualizerLevels(
            amplitudes = waveformData?.amplitudes,
            currentPositionMs = currentPosition.toLong(),
            durationMs = duration.toLong(),
            columnCount = profile.levels.size,
            animationPhase = phase.value,
            isPlaying = isPlaying
        )
        val displayLevels = trackReactiveLevels ?: profile.levels
        val gap = size.width * 0.012f
        val barWidth = (size.width - gap * (displayLevels.size - 1)) / displayLevels.size
        val segmentGap = 2.dp.toPx()
        val segmentHeight = 3.dp.toPx()
        val playbackPhase = (currentPosition / 1_000f) * 0.22f
        displayLevels.forEachIndexed { index, level ->
            val movement = if (trackReactiveLevels == null && isPlaying) {
                sin(
                    phase.value * 6.283f * (0.58f + index % 4 * 0.07f) +
                            playbackPhase +
                            profile.phaseOffset +
                            index * 0.73f
                ) * 0.11f
            } else {
                0f
            }
            val animatedLevel = (level + movement).coerceIn(0.12f, 0.98f)
            val height = size.height * animatedLevel
            val segmentStep = segmentHeight + segmentGap
            val segmentCount = (height / segmentStep).toInt().coerceAtLeast(1)
            repeat(segmentCount) { segmentIndex ->
                val segmentTop = size.height - (segmentIndex + 1) * segmentStep
                val isPeak = segmentTop < size.height * 0.18f
                drawRect(
                    color = if (isPeak) profile.peak else profile.accent,
                    topLeft = Offset(index * (barWidth + gap), segmentTop),
                    size = Size(barWidth, segmentHeight)
                )
            }
        }
    }
}

@Composable
private fun RackPlaylist(
    currentSong: Song?,
    upcomingSongs: List<Song>,
    playbackContext: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit
) {
    val rows = listOfNotNull(currentSong) + upcomingSongs
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DisplayBlack)
            .rackBevel()
            .padding(vertical = 2.dp)
    ) {
        itemsIndexed(
            items = rows,
            key = { index, song -> "${song.id}:$index" }
        ) { index, song ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSongClick(song, playbackContext) }
                    .background(if (index == 0) SelectedRow else Color.Transparent)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (index + 1).toString().padStart(2, '0'),
                    color = LcdGreenDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
                Text(
                    text = "  ${song.artist} — ${song.title}",
                    color = if (index == 0) ControlSilver else LcdGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .padding(start = 6.dp)
                ) {
                    Text(
                        text = formatRackTime(song.duration.toInt()),
                        color = if (index == 0) ControlSilver else LcdGreenDim,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun RackModule(
    title: String,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PanelDark)
            .rackBevel()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(PanelHeader, PanelHeaderEnd, PanelHeader)
                    )
                )
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(RackShadow)
                    .rackBevel(pressed = true)
            )
            Text(
                text = title,
                color = ControlSilver,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 5.dp)
            )
            trailingAction?.invoke()
        }
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
private fun RackIconButton(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Column(
        modifier = Modifier
            .sizeIn(
                minWidth = if (compact) 36.dp else 40.dp,
                minHeight = if (compact) 30.dp else 34.dp
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .background(
                when {
                    isPressed -> ButtonPressed
                    active -> ActiveButton
                    else -> ButtonFace
                }
            )
            .rackBevel(pressed = isPressed)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active && !isPressed) DisplayBlack else ControlSilver,
            modifier = Modifier.size(if (compact) 14.dp else 16.dp)
        )
        Text(
            text = label,
            color = if (active && !isPressed) DisplayBlack else ControlSilver,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 6.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun RackIndicator(color: Color) {
    Box(
        modifier = Modifier
            .padding(end = 3.dp)
            .size(width = 14.dp, height = 6.dp)
            .background(color)
            .rackBevel(pressed = true)
    )
}

@Composable
private fun LcdLabel(text: String) {
    Text(
        text = text,
        color = DisplayBlack,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 8.sp,
        lineHeight = 9.sp,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .background(LcdGreenDim)
            .padding(horizontal = 3.dp, vertical = 1.dp)
    )
}

private fun formatRackTime(milliseconds: Int): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0) / 1000)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
