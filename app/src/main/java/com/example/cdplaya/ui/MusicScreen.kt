package com.example.cdplaya.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cdplaya.data.Song

@Composable
fun MusicScreen(
    songs: List<Song>,
    permissionGranted: Boolean,
    currentSong: Song?,
    isPlaying: Boolean,
    currentPosition: Int,
    duration: Int,
    modifier: Modifier = Modifier,
    onSongClick: (Song) -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekChange: (Int) -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "CDPlaya",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        if (!permissionGranted) {
            Text(
                text = "Audio and image permissions are needed to show your music.",
                modifier = Modifier.padding(16.dp)
            )
        } else if (songs.isEmpty()) {
            Text(
                text = "No songs found.",
                modifier = Modifier.padding(16.dp)
            )
        } else {
            NowPlayingSection(
                currentSong = currentSong,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                onPlayPauseClick = onPlayPauseClick,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onSeekChange = onSeekChange
            )

            SongList(
                songs = songs,
                onSongClick = onSongClick
            )
        }
    }
}

@Composable
fun NowPlayingSection(
    currentSong: Song?,
    isPlaying: Boolean,
    currentPosition: Int,
    duration: Int,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekChange: (Int) -> Unit
) {
    if (currentSong == null) {
        return
    }

    AsyncImage(
        model = currentSong.albumArtUri,
        contentDescription = "Album art for ${currentSong.title}",
        modifier = Modifier
            .padding(16.dp)
            .size(180.dp)
            .clip(RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Crop,
        error = painterResource(android.R.drawable.ic_media_play),
        placeholder = painterResource(android.R.drawable.ic_media_play)
    )

    Text(
        text = "Now playing: ${currentSong.title}",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp)
    )

    Text(
        text = currentSong.artist,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp)
    )

    Slider(
        value = currentPosition.toFloat(),
        onValueChange = { newPosition ->
            onSeekChange(newPosition.toInt())
        },
        valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = formatDuration(currentPosition))
        Text(text = formatDuration(duration))
    }

    Row(
        modifier = Modifier.padding(16.dp)
    ) {
        Button(
            onClick = onPreviousClick,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(text = "Previous")
        }

        Button(
            onClick = onPlayPauseClick,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(text = if (isPlaying) "Pause" else "Play")
        }

        Button(
            onClick = onNextClick
        ) {
            Text(text = "Next")
        }
    }
}

@Composable
fun SongList(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    LazyColumn {
        items(songs) { song ->
            ListItem(
                leadingContent = {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = "Album art for ${song.title}",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(android.R.drawable.ic_media_play),
                        placeholder = painterResource(android.R.drawable.ic_media_play)
                    )
                },
                headlineContent = {
                    Text(text = song.title)
                },
                supportingContent = {
                    Text(text = song.artist)
                },
                modifier = Modifier.clickable {
                    onSongClick(song)
                }
            )
        }
    }
}

fun formatDuration(milliseconds: Int): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "%d:%02d".format(minutes, seconds)
}