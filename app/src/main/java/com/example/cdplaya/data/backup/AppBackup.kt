package com.example.cdplaya.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class AppBackup(
    val schemaVersion: Int = AppBackupJson.CURRENT_SCHEMA_VERSION,
    val createdAt: Long,
    val appName: String = "CDPlaya",
    val favorites: List<BackupFavoriteSong> = emptyList(),
    val playlists: List<BackupPlaylist> = emptyList(),
    val listeningHistory: List<BackupListeningHistoryEntry> = emptyList(),
    val preferences: BackupPreferences = BackupPreferences()
)

@Serializable
data class BackupPreferences(
    val selectedLibraryFolders: List<String> = emptyList(),
    val selectedPlayerThemeId: String = "",
    val replayGainMode: String = ""
)

@Serializable
data class BackupFavoriteSong(
    val songKey: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val createdAt: Long
)

@Serializable
data class BackupPlaylist(
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val songs: List<BackupPlaylistSong> = emptyList()
)

@Serializable
data class BackupPlaylistSong(
    val songKey: String,
    val position: Int,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val addedAt: Long
)

@Serializable
data class BackupListeningHistoryEntry(
    val songKey: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val playCount: Int,
    val firstPlayedAt: Long,
    val lastPlayedAt: Long
)
