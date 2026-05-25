package com.example.cdplaya

import android.Manifest
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.MusicRepository
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.MusicPlayer
import com.example.cdplaya.ui.theme.CdplayaTheme

class MainActivity : ComponentActivity() {

    private var songs by mutableStateOf<List<Song>>(emptyList())
    private var permissionGranted by mutableStateOf(false)
    private lateinit var musicPlayer: MusicPlayer
    private var currentSong by mutableStateOf<Song?>(null)
    private var isPlaying by mutableStateOf(false)

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted

        if (isGranted) {
            loadSongs()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        musicPlayer = MusicPlayer(this)

        musicPlayer.onSongCompleted = {
            runOnUiThread {
                playNextSong()
            }
        }

        requestAudioPermission()

        setContent {
            CdplayaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MusicScreen(
                        songs = songs,
                        permissionGranted = permissionGranted,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        modifier = Modifier.padding(innerPadding),
                        onSongClick = { song ->
                            musicPlayer.playSong(song)
                            currentSong = song
                            isPlaying = true
                        },
                        onPlayPauseClick = {
                            if (musicPlayer.isPlaying()) {
                                musicPlayer.pause()
                                isPlaying = false
                            } else {
                                musicPlayer.resume()
                                isPlaying = true
                            }
                        },
                        onPreviousClick = {
                            playPreviousSong()
                        },
                        onNextClick = {
                            playNextSong()
                        }
                    )
                }
            }
        }
    }

    private fun requestAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            audioPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissionGranted = true
            loadSongs()
        }
    }

    private fun loadSongs() {
        val repository = MusicRepository(this)
        songs = repository.getSongs()
    }

    private fun playSelectedSong(song: Song) {
        musicPlayer.playSong(song)
        currentSong = song
        isPlaying = true
    }

    private fun playNextSong() {
        if (songs.isEmpty()) {
            return
        }

        val currentIndex = songs.indexOfFirst { it.id == currentSong?.id }

        val nextIndex = if (currentIndex == -1 || currentIndex == songs.lastIndex) {
            0
        } else {
            currentIndex + 1
        }

        playSelectedSong(songs[nextIndex])
    }

    private fun playPreviousSong() {
        if (songs.isEmpty()) {
            return
        }

        val currentIndex = songs.indexOfFirst { it.id == currentSong?.id }

        val previousIndex = if (currentIndex <= 0) {
            songs.lastIndex
        } else {
            currentIndex - 1
        }

        playSelectedSong(songs[previousIndex])
    }

    override fun onDestroy() {
        super.onDestroy()
        musicPlayer.stop()
    }
}

@Composable
fun MusicScreen(
    songs: List<Song>,
    permissionGranted: Boolean,
    currentSong: Song?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onSongClick: (Song) -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "CDPlaya",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        if (currentSong != null) {
            Text(
                text = "Now playing: ${currentSong.title}",
                modifier = Modifier.padding(horizontal = 16.dp)
            )

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

        if (!permissionGranted) {
            Text(
                text = "Audio permission is needed to show your music.",
                modifier = Modifier.padding(16.dp)
            )
        } else if (songs.isEmpty()) {
            Text(
                text = "No songs found.",
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn {
                items(songs) { song ->
                    ListItem(
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
    }
}