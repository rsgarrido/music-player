package com.example.cdplaya.data.backup

import android.content.Context
import android.net.Uri
import com.example.cdplaya.data.FavoritesRepository
import com.example.cdplaya.data.LibraryPreferences
import com.example.cdplaya.data.ListeningHistoryRepository
import com.example.cdplaya.data.PlayerThemePreferences
import com.example.cdplaya.data.PlaylistsRepository
import com.example.cdplaya.player.replaygain.ReplayGainPreferences
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupRepository(
    context: Context,
    private val favoritesRepository: FavoritesRepository,
    private val playlistsRepository: PlaylistsRepository,
    private val listeningHistoryRepository: ListeningHistoryRepository,
    private val libraryPreferences: LibraryPreferences,
    private val playerThemePreferences: PlayerThemePreferences,
    private val replayGainPreferences: ReplayGainPreferences
) {
    private val context = context.applicationContext ?: context

    suspend fun createBackup(): AppBackup = withContext(Dispatchers.IO) {
        AppBackup(
            schemaVersion = AppBackupJson.CURRENT_SCHEMA_VERSION,
            createdAt = System.currentTimeMillis(),
            appName = APP_NAME,
            favorites = favoritesRepository.getFavoritesForBackup(),
            playlists = playlistsRepository.getPlaylistsForBackup(),
            listeningHistory = listeningHistoryRepository.getListeningHistoryForBackup(),
            preferences = BackupPreferences(
                selectedLibraryFolders = libraryPreferences.getSelectedFolders().sorted(),
                selectedPlayerThemeId = playerThemePreferences.getSelectedPlayerThemeId(),
                replayGainMode = replayGainPreferences.getReplayGainModeName()
            )
        )
    }

    suspend fun writeBackupToUri(uri: Uri): BackupExportResult = withContext(Dispatchers.IO) {
        val backup = createBackup()
        val jsonText = AppBackupJson.encodeBackup(backup)
        val outputStream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("Unable to open backup destination.")

        OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
            writer.write(jsonText)
        }

        backup.toBackupExportResult()
    }

    suspend fun readBackupFromUri(uri: Uri): AppBackup = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open backup source.")
        val jsonText = InputStreamReader(inputStream, Charsets.UTF_8).use { reader ->
            reader.readText()
        }

        AppBackupJson.decodeBackup(jsonText)
    }

    fun summarizeRestore(backup: AppBackup): BackupRestoreSummary {
        return backup.toBackupRestoreSummary()
    }

    suspend fun restoreBackup(backup: AppBackup): BackupRestoreResult =
        withContext(Dispatchers.IO) {
            val summary = summarizeRestore(backup)

            favoritesRepository.restoreFavoritesFromBackup(backup.favorites)
            playlistsRepository.restorePlaylistsFromBackup(backup.playlists)
            listeningHistoryRepository.restoreListeningHistoryFromBackup(
                backup.listeningHistory
            )
            libraryPreferences.saveSelectedFolders(
                backup.preferences.selectedLibraryFolders.toSet()
            )
            playerThemePreferences.saveSelectedPlayerThemeId(
                backup.preferences.selectedPlayerThemeId
            )
            replayGainPreferences.setReplayGainModeName(
                backup.preferences.replayGainMode
            )

            BackupRestoreResult(
                favoriteCount = summary.favoriteCount,
                playlistCount = summary.playlistCount,
                playlistSongCount = summary.playlistSongCount,
                listeningHistoryCount = summary.listeningHistoryCount,
                selectedFolderCount = summary.selectedFolderCount
            )
        }

    private companion object {
        const val APP_NAME = "CDPlaya"
    }
}

data class BackupExportResult(
    val favoriteCount: Int,
    val playlistCount: Int,
    val playlistSongCount: Int,
    val listeningHistoryCount: Int
)

data class BackupRestoreSummary(
    val favoriteCount: Int,
    val playlistCount: Int,
    val playlistSongCount: Int,
    val listeningHistoryCount: Int,
    val selectedFolderCount: Int
)

data class BackupRestoreResult(
    val favoriteCount: Int,
    val playlistCount: Int,
    val playlistSongCount: Int,
    val listeningHistoryCount: Int,
    val selectedFolderCount: Int
)

internal fun AppBackup.toBackupExportResult(): BackupExportResult {
    return BackupExportResult(
        favoriteCount = favorites.size,
        playlistCount = playlists.size,
        playlistSongCount = playlists.sumOf { playlist -> playlist.songs.size },
        listeningHistoryCount = listeningHistory.size
    )
}

internal fun AppBackup.toBackupRestoreSummary(): BackupRestoreSummary {
    return BackupRestoreSummary(
        favoriteCount = favorites.size,
        playlistCount = playlists.size,
        playlistSongCount = playlists.sumOf { playlist -> playlist.songs.size },
        listeningHistoryCount = listeningHistory.size,
        selectedFolderCount = preferences.selectedLibraryFolders.size
    )
}
