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
    val replayGainMode: String = "",
    val modernArtworkTransitionStyle: String = "slide",
    val modernSeekbarStyle: String = "classic_bar",
    val playerThemeTokenOverrides: Map<String, BackupPlayerThemeTokenOverrides> = emptyMap(),
    val songsViewMode: String = "list",
    val albumsViewMode: String = "list",
    val artistsViewMode: String = "list",
    val songsGridColumnCount: Int = 2,
    val albumsGridColumnCount: Int = 2,
    val artistsGridColumnCount: Int = 2
)

@Serializable
data class BackupPlayerThemeTokenOverrides(
    val shellArgb: Long? = null,
    val accentArgb: Long? = null,
    val displayBackgroundArgb: Long? = null,
    val displayTextArgb: Long? = null,
    val secondaryAccentArgb: Long? = null
)

@Serializable
data class BackupFavoriteSong(
    val songKey: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val createdAt: Long,
    val reference: BackupSongReference? = null
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
    val addedAt: Long,
    val reference: BackupSongReference? = null
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
    val lastPlayedAt: Long,
    val reference: BackupSongReference? = null
)

@Serializable
data class BackupSongReference(
    val relativePath: String = "",
    val displayName: String = "",
    val fileSizeBytes: Long = 0L,
    val duration: Long = 0L,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArtist: String = "",
    val legacyStableKey: String = "",
    val portableKey: String = "",
    val portableKeyVersion: Int = 1
)
