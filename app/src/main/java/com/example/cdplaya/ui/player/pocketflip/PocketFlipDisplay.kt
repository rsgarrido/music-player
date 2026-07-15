package com.example.cdplaya.ui.player.pocketflip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.cdplaya.data.Song

@Composable
internal fun PocketFlipDisplayHalf(
    currentSong: Song?,
    isPlaying: Boolean,
    currentPosition: Int,
    duration: Int,
    onSeekChange: (Int) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (compact) 22.dp else 28.dp))
            .pocketFlipBezelFinish(if (compact) 22.dp else 28.dp)
            .padding(if (compact) 10.dp else 14.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 9.dp)
    ) {
        PocketFlipDisplayHeader(isPlaying = isPlaying, compact = compact)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(if (compact) 10.dp else 14.dp))
                .background(PocketFlipColors.display)
                .pocketFlipScreenFinish()
                .padding(if (compact) 10.dp else 14.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PocketFlipArtwork(
                song = currentSong,
                compact = compact,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(if (compact) 0.42f else 0.46f)
            )

            PocketFlipMetadata(
                currentSong = currentSong,
                isPlaying = isPlaying,
                compact = compact,
                modifier = Modifier.weight(if (compact) 0.58f else 0.54f)
            )
        }

        PocketFlipSeekBar(
            currentPosition = currentPosition,
            duration = duration,
            onSeekChange = onSeekChange,
            compact = compact
        )
    }
}

@Composable
private fun PocketFlipDisplayHeader(
    isPlaying: Boolean,
    compact: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 7.dp else 8.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    if (isPlaying) PocketFlipColors.statusOn else PocketFlipColors.statusIdle
                )
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = "POCKET FLIP // AUDIO",
            color = PocketFlipColors.bezelText,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 10.sp else 11.sp,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        PocketFlipBattery(compact = compact)
    }
}

@Composable
private fun PocketFlipBattery(compact: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(PocketFlipColors.display)
            .padding(horizontal = 5.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .width(if (compact) 4.dp else 5.dp)
                    .height(if (compact) 7.dp else 8.dp)
                    .background(PocketFlipColors.screenAccent)
            )
        }
    }
}

@Composable
private fun PocketFlipArtwork(
    song: Song?,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (compact) 7.dp else 9.dp))
            .background(PocketFlipColors.artworkWell),
        contentAlignment = Alignment.Center
    ) {
        if (song?.albumArtUri != null) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = "Current album artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Album,
                contentDescription = null,
                tint = PocketFlipColors.screenTextMuted,
                modifier = Modifier.size(if (compact) 42.dp else 54.dp)
            )
        }
    }
}

@Composable
private fun PocketFlipMetadata(
    currentSong: Song?,
    isPlaying: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isPlaying) "NOW PLAYING" else "PLAYBACK PAUSED",
            color = PocketFlipColors.screenAccent,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 9.sp else 10.sp,
            letterSpacing = 0.7.sp,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(if (compact) 5.dp else 8.dp))
        Text(
            text = currentSong?.title ?: "No track loaded",
            color = PocketFlipColors.screenText,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 15.sp else 18.sp,
            lineHeight = if (compact) 18.sp else 21.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = currentSong?.artist?.ifBlank { "Unknown artist" } ?: "",
            color = PocketFlipColors.screenText,
            fontFamily = FontFamily.Monospace,
            fontSize = if (compact) 11.sp else 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = currentSong?.album?.ifBlank { "Unknown album" } ?: "",
            color = PocketFlipColors.screenTextMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = if (compact) 10.sp else 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PocketFlipSeekBar(
    currentPosition: Int,
    duration: Int,
    onSeekChange: (Int) -> Unit,
    compact: Boolean
) {
    val safeDuration = duration.coerceAtLeast(1)
    val safePosition = currentPosition.coerceIn(0, safeDuration)

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Slider(
            value = safePosition.toFloat(),
            onValueChange = { value -> onSeekChange(value.toInt()) },
            valueRange = 0f..safeDuration.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = PocketFlipColors.screenAccent,
                activeTrackColor = PocketFlipColors.screenAccent,
                inactiveTrackColor = PocketFlipColors.seekInactive
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 24.dp else 28.dp)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatPocketFlipTime(safePosition),
                color = PocketFlipColors.bezelText,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 10.sp else 11.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatPocketFlipTime(duration),
                color = PocketFlipColors.bezelText,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 10.sp else 11.sp
            )
        }
    }
}

private fun formatPocketFlipTime(milliseconds: Int): String {
    val totalSeconds = (milliseconds / 1_000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
