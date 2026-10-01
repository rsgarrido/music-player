package io.github.rsgarrido.sazanami.ui.settings

import android.content.res.Resources
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.data.backup.BackupExportResult
import io.github.rsgarrido.sazanami.data.backup.BackupRestoreResult
import io.github.rsgarrido.sazanami.data.backup.BackupRestoreSummary

internal data class BackupPresentationCounts(
    val favorites: Int,
    val playlists: Int,
    val playlistSongs: Int,
    val historyEntries: Int,
    val selectedFolders: Int,
    val pictures: Int
)

internal fun BackupExportResult.presentationCounts() = BackupPresentationCounts(
    favoriteCount, playlistCount, playlistSongCount, listeningHistoryCount, 0, visualAssetCount
)

internal fun BackupRestoreResult.presentationCounts() = BackupPresentationCounts(
    favoriteCount, playlistCount, playlistSongCount, listeningHistoryCount,
    selectedFolderCount, visualAssetCount
)

internal fun BackupRestoreSummary.presentationCounts() = BackupPresentationCounts(
    favoriteCount, playlistCount, playlistSongCount, listeningHistoryCount,
    selectedFolderCount, visualAssetCount
)

private fun Resources.backupCountPhrases(counts: BackupPresentationCounts): List<String> = listOf(
    getQuantityString(R.plurals.backup_playlists, counts.playlists, counts.playlists),
    getQuantityString(R.plurals.backup_favorites, counts.favorites, counts.favorites),
    getQuantityString(R.plurals.backup_history_entries, counts.historyEntries, counts.historyEntries),
    getQuantityString(R.plurals.backup_pictures, counts.pictures, counts.pictures)
)

internal fun Resources.backupExportSuccessMessage(result: BackupExportResult): String {
    val phrases = backupCountPhrases(result.presentationCounts())
    return getString(R.string.backup_export_success, phrases[0], phrases[1], phrases[2], phrases[3])
}

internal fun Resources.backupRestoreSuccessMessage(result: BackupRestoreResult): String {
    val phrases = backupCountPhrases(result.presentationCounts())
    return getString(R.string.backup_restore_success, phrases[0], phrases[1], phrases[2], phrases[3])
}

internal fun Resources.backupRestoreSummaryText(summary: BackupRestoreSummary): String {
    val counts = summary.presentationCounts()
    return getString(
        R.string.backup_restore_summary,
        getQuantityString(R.plurals.backup_favorites, counts.favorites, counts.favorites),
        getQuantityString(R.plurals.backup_playlists, counts.playlists, counts.playlists),
        getQuantityString(R.plurals.backup_playlist_songs, counts.playlistSongs, counts.playlistSongs),
        getQuantityString(R.plurals.backup_history_entries, counts.historyEntries, counts.historyEntries),
        getQuantityString(R.plurals.backup_selected_folders, counts.selectedFolders, counts.selectedFolders),
        getQuantityString(R.plurals.backup_pictures, counts.pictures, counts.pictures)
    )
}
