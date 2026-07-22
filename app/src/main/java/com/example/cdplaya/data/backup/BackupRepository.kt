package com.example.cdplaya.data.backup

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.cdplaya.data.FavoritesRepository
import com.example.cdplaya.data.LibraryPreferences
import com.example.cdplaya.data.ListeningHistoryRepository
import com.example.cdplaya.data.ModernPlayerPreferences
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.data.PlayerThemePreferences
import com.example.cdplaya.data.PlayerThemeTokenPreferences
import com.example.cdplaya.data.PlaylistsRepository
import com.example.cdplaya.player.replaygain.ReplayGainMode
import com.example.cdplaya.player.replaygain.ReplayGainPreferences
import com.example.cdplaya.ui.library.LibraryViewCategory
import com.example.cdplaya.ui.library.LibraryViewMode
import com.example.cdplaya.ui.library.LibraryViewPreferences
import com.example.cdplaya.ui.player.modern.ModernArtworkTransitionStyle
import com.example.cdplaya.ui.player.modern.ModernSeekbarStyle
import com.example.cdplaya.ui.player.theme.PlayerThemeTokenField
import com.example.cdplaya.ui.player.theme.PlayerThemeTokenOverrides
import com.example.cdplaya.ui.player.theme.customizationOptions
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
    private val replayGainPreferences: ReplayGainPreferences,
    private val modernPlayerPreferences: ModernPlayerPreferences = ModernPlayerPreferences(context),
    private val playerThemeTokenPreferences: PlayerThemeTokenPreferences =
        PlayerThemeTokenPreferences(context),
    private val libraryViewPreferences: LibraryViewPreferences = LibraryViewPreferences(context)
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
                selectedLibraryFolders = libraryPreferences.getSelectedFolders()
                    .filterNot { it.isAbsolutePathLike() }
                    .sorted(),
                selectedPlayerThemeId = playerThemePreferences.getSelectedPlayerThemeId(),
                replayGainMode = replayGainPreferences.getReplayGainModeName(),
                modernArtworkTransitionStyle =
                    modernPlayerPreferences.getArtworkTransitionStyle().storageValue,
                modernSeekbarStyle = modernPlayerPreferences.getSeekbarStyle().storageValue,
                playerThemeTokenOverrides = createThemeTokenBackup(),
                songsViewMode = libraryViewPreferences
                    .getViewMode(LibraryViewCategory.SONGS).storageValue,
                albumsViewMode = libraryViewPreferences
                    .getViewMode(LibraryViewCategory.ALBUMS).storageValue,
                artistsViewMode = libraryViewPreferences
                    .getViewMode(LibraryViewCategory.ARTISTS).storageValue,
                songsGridColumnCount = libraryViewPreferences
                    .getGridColumnCount(LibraryViewCategory.SONGS),
                albumsGridColumnCount = libraryViewPreferences
                    .getGridColumnCount(LibraryViewCategory.ALBUMS),
                artistsGridColumnCount = libraryViewPreferences
                    .getGridColumnCount(LibraryViewCategory.ARTISTS)
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
            restorePreferences(backup.preferences)

            BackupRestoreResult(
                favoriteCount = summary.favoriteCount,
                playlistCount = summary.playlistCount,
                playlistSongCount = summary.playlistSongCount,
                listeningHistoryCount = summary.listeningHistoryCount,
                selectedFolderCount = summary.selectedFolderCount
            )
        }

    private fun createThemeTokenBackup(): Map<String, BackupPlayerThemeTokenOverrides> {
        return PlayerTheme.entries.mapNotNull { theme ->
            val supportedFields = theme.customizationOptions().map { it.field }.toSet()
            if (supportedFields.isEmpty()) return@mapNotNull null
            val overrides = playerThemeTokenPreferences.getOverrides(theme)
            val backup = BackupPlayerThemeTokenOverrides(
                shellArgb = overrides.shellColor.toBackupArgbIfSupported(
                    PlayerThemeTokenField.SHELL in supportedFields
                ),
                accentArgb = overrides.accentColor.toBackupArgbIfSupported(
                    PlayerThemeTokenField.ACCENT in supportedFields
                ),
                displayBackgroundArgb = overrides.displayBackgroundColor.toBackupArgbIfSupported(
                    PlayerThemeTokenField.DISPLAY_BACKGROUND in supportedFields
                ),
                displayTextArgb = overrides.displayTextColor.toBackupArgbIfSupported(
                    PlayerThemeTokenField.DISPLAY_TEXT in supportedFields
                ),
                secondaryAccentArgb = overrides.secondaryAccentColor.toBackupArgbIfSupported(
                    PlayerThemeTokenField.SECONDARY_ACCENT in supportedFields
                )
            )
            if (backup.hasAnyValue()) theme.id to backup else null
        }.toMap()
    }

    private fun restorePreferences(preferences: BackupPreferences) {
        playerThemePreferences.saveSelectedPlayerTheme(
            PlayerTheme.fromId(preferences.selectedPlayerThemeId)
        )
        replayGainPreferences.setReplayGainMode(
            runCatching { ReplayGainMode.valueOf(preferences.replayGainMode) }
                .getOrDefault(ReplayGainMode.OFF)
        )
        modernPlayerPreferences.saveArtworkTransitionStyle(
            ModernArtworkTransitionStyle.fromStorageValue(
                preferences.modernArtworkTransitionStyle
            )
        )
        modernPlayerPreferences.saveSeekbarStyle(
            ModernSeekbarStyle.fromStorageValue(preferences.modernSeekbarStyle)
        )
        playerThemeTokenPreferences.clearAllOverrides()
        PlayerTheme.entries.forEach { theme ->
            val backup = preferences.playerThemeTokenOverrides[theme.id] ?: return@forEach
            val supportedFields = theme.customizationOptions().map { it.field }.toSet()
            if (supportedFields.isEmpty()) return@forEach
            playerThemeTokenPreferences.saveOverrides(
                theme,
                PlayerThemeTokenOverrides(
                    shellColor = backup.shellArgb.toColorIfSupported(
                        PlayerThemeTokenField.SHELL in supportedFields
                    ),
                    accentColor = backup.accentArgb.toColorIfSupported(
                        PlayerThemeTokenField.ACCENT in supportedFields
                    ),
                    displayBackgroundColor = backup.displayBackgroundArgb.toColorIfSupported(
                        PlayerThemeTokenField.DISPLAY_BACKGROUND in supportedFields
                    ),
                    displayTextColor = backup.displayTextArgb.toColorIfSupported(
                        PlayerThemeTokenField.DISPLAY_TEXT in supportedFields
                    ),
                    secondaryAccentColor = backup.secondaryAccentArgb.toColorIfSupported(
                        PlayerThemeTokenField.SECONDARY_ACCENT in supportedFields
                    )
                )
            )
        }
        restoreLibraryView(
            LibraryViewCategory.SONGS,
            preferences.songsViewMode,
            preferences.songsGridColumnCount
        )
        restoreLibraryView(
            LibraryViewCategory.ALBUMS,
            preferences.albumsViewMode,
            preferences.albumsGridColumnCount
        )
        restoreLibraryView(
            LibraryViewCategory.ARTISTS,
            preferences.artistsViewMode,
            preferences.artistsGridColumnCount
        )
    }

    private fun restoreLibraryView(
        category: LibraryViewCategory,
        mode: String,
        columns: Int
    ) {
        libraryViewPreferences.saveViewMode(category, LibraryViewMode.fromStorageValue(mode))
        libraryViewPreferences.saveGridColumnCount(category, columns)
    }

    private companion object {
        const val APP_NAME = "CDPlaya"
    }
}

private fun Color?.toBackupArgbIfSupported(isSupported: Boolean): Long? {
    return if (isSupported && this != null) toArgb().toUInt().toLong() else null
}

private fun Long?.toColorIfSupported(isSupported: Boolean): Color? {
    return if (isSupported && this != null && this in 0..0xFFFF_FFFFL) {
        Color(toInt())
    } else {
        null
    }
}

private fun BackupPlayerThemeTokenOverrides.hasAnyValue(): Boolean {
    return shellArgb != null || accentArgb != null || displayBackgroundArgb != null ||
        displayTextArgb != null || secondaryAccentArgb != null
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
