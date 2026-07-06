package com.example.cdplaya.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.RepeatMode
import kotlin.math.roundToInt

@Composable
fun ClassicWheelExpandedPlayer(
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
    onOpenUpNextClick: () -> Unit,
    onToggleFavoriteClick: (Song) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFDDDAD1))
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(34.dp),
            color = Color(0xFFF4F0E4),
            shadowElevation = 12.dp,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ClassicWheelScreen(
                    currentSong = currentSong,
                    currentPosition = currentPosition,
                    duration = duration,
                    isCurrentSongFavorite = isCurrentSongFavorite,
                    isShuffleEnabled = isShuffleEnabled,
                    repeatMode = repeatMode,
                    onSeekChange = onSeekChange,
                    onCollapseClick = onCollapseClick,
                    onOpenUpNextClick = onOpenUpNextClick,
                    onToggleFavoriteClick = onToggleFavoriteClick
                )

                Spacer(modifier = Modifier.height(38.dp))

                ClassicControlWheel(
                    isPlaying = isPlaying,
                    onPlayPauseClick = onPlayPauseClick,
                    onPreviousClick = onPreviousClick,
                    onNextClick = onNextClick,
                    onMenuClick = onCollapseClick
                )

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun ClassicWheelScreen(
    currentSong: Song?,
    currentPosition: Int,
    duration: Int,
    isCurrentSongFavorite: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onSeekChange: (Int) -> Unit,
    onCollapseClick: () -> Unit,
    onOpenUpNextClick: () -> Unit,
    onToggleFavoriteClick: (Song) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color.Black,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
                .background(Color(0xFFF7F7F2))
        ) {
            ClassicScreenStatusBar(
                onCollapseClick = onCollapseClick
            )

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AsyncImage(
                        model = currentSong?.albumArtUri,
                        contentDescription = currentSong?.let { song ->
                            "Album art for ${song.title}"
                        },
                        modifier = Modifier
                            .weight(0.95f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(2.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        error = painterResource(android.R.drawable.ic_media_play),
                        placeholder = painterResource(android.R.drawable.ic_media_play)
                    )

                    Column(
                        modifier = Modifier.weight(1.15f)
                    ) {
                        Text(
                            text = currentSong?.title?.ifBlank { "Unknown Title" }
                                ?: "No song selected",
                            color = Color.Black,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = currentSong?.artist?.ifBlank { "Unknown Artist" }
                                ?: "Choose a song",
                            color = Color.Black,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = currentSong?.album?.ifBlank { "Unknown Album" }
                                ?: "",
                            color = Color.DarkGray,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    currentSong?.let { song ->
                                        onToggleFavoriteClick(song)
                                    }
                                },
                                modifier = Modifier.size(30.dp),
                                enabled = currentSong != null
                            ) {
                                Icon(
                                    imageVector = if (isCurrentSongFavorite) {
                                        Icons.Filled.Favorite
                                    } else {
                                        Icons.Filled.FavoriteBorder
                                    },
                                    contentDescription = "Favorite",
                                    tint = Color.Black
                                )
                            }

                            IconButton(
                                onClick = onOpenUpNextClick,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QueueMusic,
                                    contentDescription = "Up Next",
                                    tint = Color.Black
                                )
                            }
                        }

                        Text(
                            text = buildPlaybackModeText(
                                isShuffleEnabled = isShuffleEnabled,
                                repeatMode = repeatMode
                            ),
                            color = Color.DarkGray,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                ClassicWheelProgress(
                    currentPosition = currentPosition,
                    duration = duration,
                    onSeekChange = onSeekChange
                )
            }
        }
    }
}

@Composable
private fun ClassicScreenStatusBar(
    onCollapseClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE4E4E0))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Now Playing",
            color = Color.Black,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onCollapseClick,
            modifier = Modifier.size(26.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Collapse player",
                tint = Color.Black
            )
        }

        Surface(
            modifier = Modifier.size(width = 32.dp, height = 14.dp),
            shape = RoundedCornerShape(3.dp),
            color = Color(0xFF9BCB61),
            shadowElevation = 1.dp
        ) {}
    }
}

@Composable
private fun ClassicWheelProgress(
    currentPosition: Int,
    duration: Int,
    onSeekChange: (Int) -> Unit
) {
    val safeDuration = duration.coerceAtLeast(1)
    val progress = currentPosition.toFloat() / safeDuration.toFloat()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatTime(currentPosition),
            color = Color.Black,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            LinearProgressIndicator(
                progress = {
                    progress.coerceIn(0f, 1f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color(0xFF4FA3FF),
                trackColor = Color(0xFFD8D8D8)
            )

            Slider(
                value = currentPosition.coerceIn(0, safeDuration).toFloat(),
                onValueChange = { value ->
                    onSeekChange(value.roundToInt())
                },
                valueRange = 0f..safeDuration.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            )
        }

        Text(
            text = "-${formatTime((safeDuration - currentPosition).coerceAtLeast(0))}",
            color = Color.Black,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ClassicControlWheel(
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(250.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = Color(0xFFC8C6BC),
            shadowElevation = 5.dp
        ) {}

        Text(
            text = "MENU",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp)
                .clickable {
                    onMenuClick()
                }
        )

        IconButton(
            onClick = onPreviousClick,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 18.dp)
                .size(54.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }

        IconButton(
            onClick = onNextClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 18.dp)
                .size(54.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }

        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
                .size(58.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) {
                    Icons.Filled.Pause
                } else {
                    Icons.Filled.PlayArrow
                },
                contentDescription = if (isPlaying) {
                    "Pause"
                } else {
                    "Play"
                },
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }

        Surface(
            modifier = Modifier.size(82.dp),
            shape = CircleShape,
            color = Color(0xFFF4F0E4),
            shadowElevation = 3.dp
        ) {}
    }
}

private fun formatTime(milliseconds: Int): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private fun buildPlaybackModeText(
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode
): String {
    val shuffleText = if (isShuffleEnabled) {
        "Shuffle On"
    } else {
        "Shuffle Off"
    }

    val repeatText = when (repeatMode) {
        RepeatMode.OFF -> "Repeat Off"
        RepeatMode.ALL -> "Repeat All"
        RepeatMode.ONE -> "Repeat One"
    }

    return "$shuffleText • $repeatText"
}