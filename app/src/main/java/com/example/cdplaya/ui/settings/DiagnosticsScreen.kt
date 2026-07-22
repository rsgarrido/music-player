package com.example.cdplaya.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cdplaya.R
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.replaygain.ReplayGainMode
import com.example.cdplaya.player.waveform.WaveformCache
import com.example.cdplaya.player.waveform.WaveformCacheStats
import com.example.cdplaya.player.waveform.WaveformRepository
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal data class DiagnosticsSnapshot(
    val appVersionName: String,
    val appVersionCode: Long,
    val librarySongCount: Int,
    val selectedFolderCount: Int,
    val playerTheme: String,
    val replayGainMode: String,
    val isPlaybackConnected: Boolean,
    val currentSongTitle: String?,
    val currentSongArtist: String?,
    val isPlaying: Boolean,
    val currentPositionMs: Int,
    val durationMs: Int,
    val queueCount: Int,
    val upcomingCount: Int,
    val previousCount: Int,
    val forwardCount: Int,
    val waveformFileCount: Int,
    val waveformTotalBytes: Long,
    val unresolvedFavoriteCount: Int = 0,
    val unresolvedPlaylistRowCount: Int = 0,
    val unresolvedListeningHistoryCount: Int = 0
)

internal fun formatDiagnosticsSummary(snapshot: DiagnosticsSnapshot): String = buildString {
    appendLine("CDPlaya diagnostics")
    appendLine("App: ${snapshot.appVersionName} (${snapshot.appVersionCode})")
    appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
    appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
    appendLine("Library songs: ${snapshot.librarySongCount}")
    appendLine("Selected folders: ${snapshot.selectedFolderCount}")
    appendLine("Unresolved favorites: ${snapshot.unresolvedFavoriteCount}")
    appendLine("Unresolved playlist rows: ${snapshot.unresolvedPlaylistRowCount}")
    appendLine("Unresolved history rows: ${snapshot.unresolvedListeningHistoryCount}")
    appendLine("Player theme: ${snapshot.playerTheme}")
    appendLine("ReplayGain: ${snapshot.replayGainMode}")
    appendLine("Playback connected: ${snapshot.isPlaybackConnected}")
    appendLine("Current song: ${snapshot.currentSongTitle ?: "None"}")
    appendLine("Current artist: ${snapshot.currentSongArtist ?: "None"}")
    appendLine("Playback state: ${if (snapshot.isPlaying) "Playing" else "Paused"}")
    appendLine("Position: ${snapshot.currentPositionMs} / ${snapshot.durationMs} ms")
    appendLine("Queue / upcoming: ${snapshot.queueCount} / ${snapshot.upcomingCount}")
    appendLine("Previous / forward: ${snapshot.previousCount} / ${snapshot.forwardCount}")
    appendLine("Waveform cache: ${snapshot.waveformFileCount} files, ${snapshot.waveformTotalBytes} bytes")
    appendLine("Waveform format: ${WaveformCache.CACHE_FORMAT_VERSION}")
    append("Waveform buckets: ${WaveformRepository.DEFAULT_ANALYZED_BAR_COUNT}")
}

@Composable
internal fun DiagnosticsScreen(
    librarySongCount: Int,
    selectedFolderCount: Int,
    selectedPlayerTheme: PlayerTheme,
    selectedReplayGainMode: ReplayGainMode,
    isPlaybackConnected: Boolean,
    currentSong: Song?,
    isPlaying: Boolean,
    currentPosition: Int,
    duration: Int,
    queueCount: Int,
    upcomingCount: Int,
    previousCount: Int,
    forwardCount: Int,
    unresolvedFavoriteCount: Int = 0,
    unresolvedPlaylistRowCount: Int = 0,
    unresolvedListeningHistoryCount: Int = 0,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val repository = remember(appContext) { WaveformRepository.shared(appContext) }
    val scope = rememberCoroutineScope()
    var cacheStats by remember { mutableStateOf(WaveformCacheStats(0, 0L)) }
    var refreshRequest by remember { mutableIntStateOf(0) }
    var isCacheOperationRunning by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val version = remember(context) { context.appVersion() }
    val copiedMessage = stringResource(R.string.diagnostics_copied)
    val cacheClearedMessage = stringResource(R.string.diagnostics_cache_cleared)

    val snapshot = DiagnosticsSnapshot(
        appVersionName = version.first,
        appVersionCode = version.second,
        librarySongCount = librarySongCount,
        selectedFolderCount = selectedFolderCount,
        playerTheme = selectedPlayerTheme.displayName,
        replayGainMode = selectedReplayGainMode.displayName,
        isPlaybackConnected = isPlaybackConnected,
        currentSongTitle = currentSong?.title,
        currentSongArtist = currentSong?.artist,
        isPlaying = isPlaying,
        currentPositionMs = currentPosition,
        durationMs = duration,
        queueCount = queueCount,
        upcomingCount = upcomingCount,
        previousCount = previousCount,
        forwardCount = forwardCount,
        waveformFileCount = cacheStats.fileCount,
        waveformTotalBytes = cacheStats.totalBytes,
        unresolvedFavoriteCount = unresolvedFavoriteCount,
        unresolvedPlaylistRowCount = unresolvedPlaylistRowCount,
        unresolvedListeningHistoryCount = unresolvedListeningHistoryCount
    )

    LaunchedEffect(repository, refreshRequest) {
        cacheStats = repository.getCacheStats()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.diagnostics_back))
            }
            Text(
                text = stringResource(R.string.diagnostics_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        DiagnosticValue(stringResource(R.string.diagnostics_app_version), "${version.first} (${version.second})")
        DiagnosticValue(
            stringResource(R.string.diagnostics_library),
            pluralStringResource(R.plurals.diagnostics_song_count, librarySongCount, librarySongCount)
        )
        DiagnosticValue(stringResource(R.string.diagnostics_selected_folders), selectedFolderCount.toString())
        DiagnosticValue(stringResource(R.string.diagnostics_unresolved_favorites), unresolvedFavoriteCount.toString())
        DiagnosticValue(stringResource(R.string.diagnostics_unresolved_playlist_rows), unresolvedPlaylistRowCount.toString())
        DiagnosticValue(stringResource(R.string.diagnostics_unresolved_history_rows), unresolvedListeningHistoryCount.toString())
        DiagnosticValue(stringResource(R.string.diagnostics_player_theme), selectedPlayerTheme.displayName)
        DiagnosticValue(stringResource(R.string.diagnostics_replay_gain), selectedReplayGainMode.displayName)
        DiagnosticValue(
            stringResource(R.string.diagnostics_connection),
            stringResource(if (isPlaybackConnected) R.string.diagnostics_connected else R.string.diagnostics_disconnected)
        )
        DiagnosticValue(
            stringResource(R.string.diagnostics_current_song),
            currentSong?.let { "${it.title} — ${it.artist}" } ?: stringResource(R.string.diagnostics_none)
        )
        DiagnosticValue(
            stringResource(R.string.diagnostics_playback_state),
            stringResource(if (isPlaying) R.string.diagnostics_playing else R.string.diagnostics_paused)
        )
        DiagnosticValue(stringResource(R.string.diagnostics_position), "${formatDuration(currentPosition)} / ${formatDuration(duration)}")
        DiagnosticValue(stringResource(R.string.diagnostics_queue), queueCount.toString())
        DiagnosticValue(stringResource(R.string.diagnostics_upcoming), upcomingCount.toString())
        DiagnosticValue(stringResource(R.string.diagnostics_history), "$previousCount / $forwardCount")

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            text = stringResource(R.string.diagnostics_waveform_cache),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        DiagnosticValue(
            stringResource(R.string.diagnostics_cache_files),
            pluralStringResource(R.plurals.diagnostics_file_count, cacheStats.fileCount, cacheStats.fileCount)
        )
        DiagnosticValue(stringResource(R.string.diagnostics_cache_size), formatBytes(cacheStats.totalBytes))
        DiagnosticValue(stringResource(R.string.diagnostics_cache_format), WaveformCache.CACHE_FORMAT_VERSION.toString())
        DiagnosticValue(stringResource(R.string.diagnostics_analysis_buckets), WaveformRepository.DEFAULT_ANALYZED_BAR_COUNT.toString())

        statusMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { refreshRequest++ },
                enabled = !isCacheOperationRunning,
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.diagnostics_refresh)) }
            OutlinedButton(
                onClick = {
                    context.copyToClipboard(formatDiagnosticsSummary(snapshot))
                    statusMessage = copiedMessage
                },
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.diagnostics_copy)) }
        }
        Button(
            onClick = { showClearConfirmation = true },
            enabled = !isCacheOperationRunning,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        ) { Text(stringResource(R.string.diagnostics_clear_cache)) }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!isCacheOperationRunning) showClearConfirmation = false },
            title = { Text(stringResource(R.string.diagnostics_clear_cache_title)) },
            text = { Text(stringResource(R.string.diagnostics_clear_cache_message)) },
            confirmButton = {
                TextButton(
                    enabled = !isCacheOperationRunning,
                    onClick = {
                        showClearConfirmation = false
                        isCacheOperationRunning = true
                        scope.launch {
                            try {
                                cacheStats = repository.clearDiskCache()
                                statusMessage = cacheClearedMessage
                            } catch (cancellation: CancellationException) {
                                throw cancellation
                            } finally {
                                isCacheOperationRunning = false
                            }
                        }
                    }
                ) { Text(stringResource(R.string.diagnostics_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text(stringResource(R.string.diagnostics_cancel))
                }
            }
        )
    }
}

@Composable
private fun DiagnosticValue(label: String, value: String) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(value) }
    )
}

private fun Context.appVersion(): Pair<String, Long> {
    return runCatching {
        val info = packageManager.getPackageInfo(packageName, 0)
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        info.versionName.orEmpty().ifBlank { getString(R.string.diagnostics_unknown) } to code
    }.getOrDefault(getString(R.string.diagnostics_unknown) to 0L)
}

private fun Context.copyToClipboard(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.diagnostics_clip_label), text))
}

private fun formatDuration(milliseconds: Int): String {
    val totalSeconds = milliseconds.coerceAtLeast(0) / 1_000
    return String.format(Locale.ROOT, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

private fun formatBytes(bytes: Long): String {
    val safeBytes = bytes.coerceAtLeast(0L)
    return if (safeBytes < 1024L) "$safeBytes B" else String.format(
        Locale.ROOT,
        "%.1f MiB",
        safeBytes / (1024.0 * 1024.0)
    )
}
