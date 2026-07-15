package com.example.cdplaya.data

import android.net.Uri
import com.example.cdplaya.data.local.CachedSongDao
import com.example.cdplaya.data.local.CachedSongEntity
import java.io.File

class LibraryCacheRepository(
    private val cachedSongDao: CachedSongDao
) {
    suspend fun hasCachedSongs(): Boolean {
        return cachedSongDao.getCachedSongCount() > 0
    }

    suspend fun getCachedLibraryData(
        selectedFolders: Set<String> = emptySet()
    ): MusicLibraryData {
        val allSongs = cachedSongDao
            .getAllCachedSongs()
            .map { cachedSong ->
                cachedSong.toSong()
            }

        val filteredSongs = if (selectedFolders.isEmpty()) {
            allSongs
        } else {
            allSongs.filter { song ->
                selectedFolders.contains(song.folderPath)
            }
        }

        return MusicLibraryData(
            songs = filteredSongs,
            libraryFolders = buildLibraryFolders(allSongs)
        )
    }

    suspend fun replaceCachedSongs(songs: List<Song>) {
        val cachedAt = System.currentTimeMillis()

        cachedSongDao.replaceCachedSongs(
            songs.map { song ->
                song.toCachedSongEntity(cachedAt = cachedAt)
            }
        )
    }

    suspend fun clearCachedSongs() {
        cachedSongDao.clearCachedSongs()
    }

    private fun buildLibraryFolders(songs: List<Song>): List<LibraryFolder> {
        return songs
            .groupBy { song -> song.folderPath }
            .map { entry ->
                val folderPath = entry.key
                val folderName = File(folderPath).name.ifBlank {
                    folderPath
                }

                LibraryFolder(
                    path = folderPath,
                    name = folderName,
                    songCount = entry.value.size
                )
            }
            .sortedBy { folder ->
                folder.name.lowercase()
            }
    }
}

fun CachedSongEntity.toSong(): Song {
    return Song(
        id = mediaStoreId,
        title = title,
        artist = artist,
        album = album,
        trackNumber = trackNumber,
        duration = duration,
        uri = Uri.parse(uriString),
        filePath = filePath,
        folderPath = folderPath,
        albumArtUri = albumArtUriString
            ?.takeIf { uriString ->
                uriString.isNotBlank()
            }
            ?.let { uriString ->
                Uri.parse(uriString)
            },
        albumArtist = albumArtist
    )
}

fun Song.toCachedSongEntity(cachedAt: Long): CachedSongEntity {
    return CachedSongEntity(
        mediaStoreId = id,
        title = title,
        artist = artist,
        album = album,
        trackNumber = trackNumber,
        duration = duration,
        uriString = uri.toString(),
        filePath = filePath,
        folderPath = folderPath,
        albumArtUriString = albumArtUri?.toString(),
        albumArtist = albumArtist,
        cachedAt = cachedAt
    )
}