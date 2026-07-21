package com.example.cdplaya

import android.Manifest
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.cdplaya.ui.MusicRoute
import com.example.cdplaya.ui.appShellBackgroundBrush
import com.example.cdplaya.ui.theme.CdplayaTheme
import com.example.cdplaya.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {

    private var permissionGranted by mutableStateOf(false)

    private val musicViewModel: MusicViewModel by viewModels()

    private val mediaPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.READ_MEDIA_AUDIO] == true
        val imagesGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES] == true

        permissionGranted = audioGranted && imagesGranted

        if (permissionGranted) {
            musicViewModel.loadSongs()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transparentSystemBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        enableEdgeToEdge(
            statusBarStyle = transparentSystemBarStyle,
            navigationBarStyle = transparentSystemBarStyle
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        requestAudioPermission()

        setContent {
            CdplayaTheme {
                val snackbarHostState = remember { SnackbarHostState() }

                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onBackground
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(appShellBackgroundBrush())
                    ) {
                        MusicRoute(
                            musicViewModel = musicViewModel,
                            permissionGranted = permissionGranted,
                            snackbarHostState = snackbarHostState,
                            modifier = Modifier.fillMaxSize()
                        )

                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                        )
                    }
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
            musicViewModel.loadSongs()
        }
    }

    override fun onPause() {
        super.onPause()
        musicViewModel.savePlayerState()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
