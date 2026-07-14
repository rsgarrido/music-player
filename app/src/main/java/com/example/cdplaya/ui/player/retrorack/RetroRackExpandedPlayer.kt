package com.example.cdplaya.ui.player.retrorack

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.RepeatMode

@Composable
fun RetroRackExpandedPlayer(
    currentSong: Song?,
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
    onSongClick: (Song, List<Song>) -> Unit
) {
    val playbackContext = listOfNotNull(currentSong) + upcomingSongs

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RackBackground)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        RackModule(
            title = "CDPLAYA // MAIN DECK",
            modifier = Modifier.weight(1.15f)
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
                onCollapseClick = onCollapseClick,
                onToggleFavoriteClick = onToggleFavoriteClick
            )
        }

        RackModule(
            title = "SPECTRUM MONITOR // VISUAL",
            modifier = Modifier.weight(0.48f)
        ) {
            DecorativeSpectrum(modifier = Modifier.fillMaxSize())
        }

        RackModule(
            title = "PLAYBACK RACK // ${playbackContext.size.toString().padStart(2, '0')} TRACKS",
            modifier = Modifier.weight(1f),
            trailingAction = {
                RackIconButton(
                    icon = Icons.Filled.List,
                    label = "QUEUE",
                    active = true,
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
    onCollapseClick: () -> Unit,
    onToggleFavoriteClick: (Song) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AsyncImage(
                model = currentSong?.albumArtUri,
                contentDescription = "Current album artwork",
                modifier = Modifier
                    .size(74.dp)
                    .background(DisplayBlack)
                    .padding(3.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(DisplayBlack)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = currentSong?.title?.uppercase() ?: "NO TRACK LOADED",
                    color = LcdGreen,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentSong?.artist?.uppercase().orEmpty(),
                    color = LcdGreenDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LcdLabel(text = if (isPlaying) "PLAY" else "PAUSE")
                    LcdLabel(text = "320K")
                    LcdLabel(text = "44.1K")
                    Text(
                        text = "${formatRackTime(currentPosition)} / ${formatRackTime(duration)}",
                        color = LcdGreen,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            RackIconButton(
                icon = Icons.Filled.Close,
                label = "CLOSE",
                onClick = onCollapseClick
            )
        }

        Slider(
            value = currentPosition.coerceIn(0, duration.coerceAtLeast(1)).toFloat(),
            onValueChange = { value -> onSeekChange(value.toInt()) },
            valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = ControlSilver,
                activeTrackColor = LcdGreen,
                inactiveTrackColor = Color(0xFF30343A)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RackIconButton(
                icon = Icons.Filled.Shuffle,
                label = "SHUF",
                active = isShuffleEnabled,
                onClick = onShuffleClick
            )
            RackIconButton(
                icon = Icons.Filled.KeyboardArrowLeft,
                label = "PREV",
                onClick = onPreviousClick
            )
            RackIconButton(
                icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                label = if (isPlaying) "PAUSE" else "PLAY",
                active = true,
                onClick = onPlayPauseClick
            )
            RackIconButton(
                icon = Icons.Filled.KeyboardArrowRight,
                label = "NEXT",
                onClick = onNextClick
            )
            RackIconButton(
                icon = Icons.Filled.Repeat,
                label = when (repeatMode) {
                    RepeatMode.OFF -> "REP"
                    RepeatMode.ALL -> "ALL"
                    RepeatMode.ONE -> "ONE"
                },
                active = repeatMode != RepeatMode.OFF,
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
                onClick = { currentSong?.let(onToggleFavoriteClick) }
            )
        }
    }
}

@Composable
private fun DecorativeSpectrum(modifier: Modifier = Modifier) {
    val levels = listOf(0.32f, 0.56f, 0.76f, 0.42f, 0.88f, 0.64f, 0.94f, 0.51f, 0.72f, 0.38f, 0.61f, 0.82f)
    Canvas(
        modifier = modifier
            .background(DisplayBlack)
            .padding(8.dp)
    ) {
        val gap = size.width * 0.018f
        val barWidth = (size.width - gap * (levels.size - 1)) / levels.size
        levels.forEachIndexed { index, level ->
            val height = size.height * level
            drawRect(
                color = if (level > 0.8f) MeterAmber else LcdGreen,
                topLeft = Offset(index * (barWidth + gap), size.height - height),
                size = Size(barWidth, height)
            )
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
            .padding(vertical = 3.dp)
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
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (index + 1).toString().padStart(2, '0'),
                    color = LcdGreenDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
                Text(
                    text = "  ${song.artist} — ${song.title}",
                    color = if (index == 0) DisplayBlack else LcdGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatRackTime(song.duration.toInt()),
                    color = if (index == 0) DisplayBlack else LcdGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
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
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PanelHeader)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = ControlSilver,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f)
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
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(if (active) ActiveButton else ButtonFace)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) DisplayBlack else ControlSilver,
            modifier = Modifier.size(19.dp)
        )
        Text(
            text = label,
            color = if (active) DisplayBlack else ControlSilver,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 7.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun LcdLabel(text: String) {
    Text(
        text = text,
        color = DisplayBlack,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 8.sp,
        modifier = Modifier
            .background(LcdGreenDim)
            .padding(horizontal = 3.dp, vertical = 1.dp)
    )
}

private fun formatRackTime(milliseconds: Int): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0) / 1000)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private val RackBackground = Color(0xFF0D0F12)
private val PanelDark = Color(0xFF25282E)
private val PanelHeader = Color(0xFF343841)
private val DisplayBlack = Color(0xFF050806)
private val LcdGreen = Color(0xFF75F05F)
private val LcdGreenDim = Color(0xFF51A94A)
private val MeterAmber = Color(0xFFE0C04A)
private val ControlSilver = Color(0xFFD2D5D9)
private val ButtonFace = Color(0xFF484D56)
private val ActiveButton = Color(0xFF8ACD74)
private val SelectedRow = Color(0xFF78D866)
