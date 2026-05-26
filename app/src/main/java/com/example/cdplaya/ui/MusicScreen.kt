package com.example.cdplaya.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = currentSong.albumArtUri,
                contentDescription = "Album art for ${currentSong.title}",
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop,
                error = painterResource(android.R.drawable.ic_media_play),
                placeholder = painterResource(android.R.drawable.ic_media_play)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = currentSong.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = currentSong.artist,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = currentPosition.toFloat(),
                onValueChange = { newPosition ->
                    onSeekChange(newPosition.toInt())
                },
                valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatDuration(currentPosition))
                Text(text = formatDuration(duration))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = onPreviousClick) {
                    Text(text = "Previous")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(onClick = onPlayPauseClick) {
                    Text(text = if (isPlaying) "Pause" else "Play")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(onClick = onNextClick) {
                    Text(text = "Next")
                }
            }
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