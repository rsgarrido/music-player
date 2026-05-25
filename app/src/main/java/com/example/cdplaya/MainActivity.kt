package com.example.cdplaya

import android.Manifest
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cdplaya.data.MusicRepository
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.MusicPlayer
import com.example.cdplaya.ui.theme.CdplayaTheme
import com.example.cdplaya.ui.MusicScreen

class MainActivity : ComponentActivity() {

    private var songs by mutableStateOf<List<Song>>(emptyList())
    private var permissionGranted by mutableStateOf(false)
    private lateinit var musicPlayer: MusicPlayer
    private var currentSong by mutableStateOf<Song?>(null)
    private var isPlaying by mutableStateOf(false)
    private var currentPosition by mutableStateOf(0)
    private var duration by mutableStateOf(0)

    private val progressHandler = Handler(Looper.getMainLooper())

    private val progressRunnable = object : Runnable {
        override fun run() {
            if (currentSong != null) {
                currentPosition = musicPlayer.getCurrentPosition()
                duration = musicPlayer.getDuration()
                progressHandler.postDelayed(this, 500)
            }
        }
    }

    private val mediaPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.READ_MEDIA_AUDIO] == true
        val imagesGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES] == true

        permissionGranted = audioGranted && imagesGranted

        if (permissionGranted) {
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
                        currentPosition = currentPosition,
                        duration = duration,
                        modifier = Modifier.padding(innerPadding),
                        onSongClick = { song ->
                            playSelectedSong(song)
                        },
                        onPlayPauseClick = {
                            if (musicPlayer.isPlaying()) {
                                musicPlayer.pause()
                                isPlaying = false
                            } else {
                                musicPlayer.resume()
                                isPlaying = true
                                startProgressUpdates()
                            }
                        },
                        onPreviousClick = {
                            playPreviousSong()
                        },
                        onNextClick = {
                            playNextSong()
                        },
                        onSeekChange = { position ->
                            musicPlayer.seekTo(position)
                            currentPosition = position
                        }
                    )
                }
            }
        }
    }

    private fun requestAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mediaPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            )
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
        currentPosition = 0
        duration = musicPlayer.getDuration()
        startProgressUpdates()
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

    private fun startProgressUpdates() {
        progressHandler.removeCallbacks(progressRunnable)
        progressHandler.post(progressRunnable)
    }


    override fun onDestroy() {
        super.onDestroy()
        progressHandler.removeCallbacks(progressRunnable)
        musicPlayer.stop()
    }


}
