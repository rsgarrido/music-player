package com.example.cdplaya.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File
import com.example.cdplaya.performance.PerformanceTraceNames
import com.example.cdplaya.performance.tracePerformance


class MusicRepository(private val context: Context) {
    fun getLibraryData(selectedFolders: Set<String> = emptySet()): MusicLibraryData {
        return buildMusicLibraryData(
            allSongs = refreshLibrary(emptyList()).songs,
            selectedFolders = selectedFolders
        )
    }

    fun getSongs(selectedFolders: Set<String> = emptySet()): List<Song> {
        return getLibraryData(selectedFolders).songs
    }

    fun refreshLibrary(
        cachedSongs: List<Song>,
        forceArtworkRefreshIds: Set<Long> = emptySet()
    ): LibraryRefreshResult {
        val indexSongs = runCatching {
            tracePerformance(PerformanceTraceNames.MEDIASTORE_INDEX_QUERY) { querySongIndex() }
        }.getOrNull()
        LibraryRefreshEngine.fallbackForIncompleteScan(cachedSongs, indexSongs)?.let { return it }
        checkNotNull(indexSongs)
        val embeddedArtworkResolver = EmbeddedArtworkResolver(context)
        tracePerformance(PerformanceTraceNames.ARTWORK_REPAIR_BATCH) {
            cachedSongs.filter { it.id in forceArtworkRefreshIds }
                .forEach(embeddedArtworkResolver::invalidate)
        }
        var albumArtByFolder: Map<String, Uri>? = null
        val artworkRepairKeys = mutableSetOf<String>()
        val result = tracePerformance(PerformanceTraceNames.LIBRARY_CLASSIFICATION) {
            LibraryRefreshEngine.refresh(
            cachedSongs = cachedSongs,
            indexSongs = indexSongs,
            requiresEnrichment = { cached, current ->
                val requiresRepair = cached.id in forceArtworkRefreshIds ||
                    cached.requiresArtworkRepair(current) ||
                    embeddedArtworkResolver.requiresReconstruction(cached)
                if (requiresRepair) artworkRepairKeys += current.uri.toString()
                requiresRepair
            },
            enrich = { indexSong ->
                val folderArtwork = albumArtByFolder ?: runCatching { getAlbumArtByFolder() }
                    .getOrDefault(emptyMap())
                    .also { albumArtByFolder = it }
                indexSong.copy(
                    albumArtUri = selectArtwork(
                        embedded = embeddedArtworkResolver.resolve(indexSong),
                        folder = folderArtwork[indexSong.folderPath]
                    ),
                    artworkEnrichmentVersion = CURRENT_ARTWORK_ENRICHMENT_VERSION
                )
            })
        }
        return result.copy(artworkRepairCount = artworkRepairKeys.size)
    }

    fun prepareCachedSongsForPublication(cachedSongs: List<Song>): List<Song> {
        val resolver = EmbeddedArtworkResolver(context)
        return hideUnavailableEmbeddedArtwork(cachedSongs, resolver::isMaterialized)
    }

    private fun querySongIndex(): List<Song>? {
        val songs = mutableListOf<Song>()

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED
        ) + if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                MediaStore.Audio.Media.VOLUME_NAME,
                MediaStore.Audio.Media.RELATIVE_PATH
            )
        } else {
            emptyArray()
        }

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val query = context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val displayNameColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
            val fileSizeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
            val dateAddedColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
            val dateModifiedColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
            val volumeNameColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Audio.Media.VOLUME_NAME)
            } else {
                -1
            }
            val relativePathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                -1
            }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown Title"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val duration = cursor.getLong(durationColumn)
                val filePath = cursor.getString(dataColumn) ?: ""
                val trackNumber = cursor.getInt(trackColumn)
                val displayName = cursor.stringOrEmpty(displayNameColumn)
                val fileSizeBytes = cursor.longOrZero(fileSizeColumn)
                val dateAddedEpochSeconds = cursor.longOrZero(dateAddedColumn)
                val dateModifiedEpochSeconds = cursor.longOrZero(dateModifiedColumn)
                val volumeName = cursor.stringOrEmpty(volumeNameColumn)
                val relativePath = cursor.stringOrEmpty(relativePathColumn)

                val folderPath = (File(filePath).parent ?: "")
                    .ifBlank { relativePath.trimEnd('/', '\\') }

                if (folderPath.isBlank()) {
                    continue
                }

                val audioCollection = if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && volumeName.isNotBlank()
                ) {
                    MediaStore.Audio.Media.getContentUri(volumeName)
                } else {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val uri = ContentUris.withAppendedId(
                    audioCollection,
                    id
                )

                val albumArtist = ""

                val song = Song(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    trackNumber = trackNumber,
                    duration = duration,
                    uri = uri,
                    filePath = filePath,
                    folderPath = folderPath,
                    albumArtUri = null,
                    albumArtist = albumArtist,
                    volumeName = volumeName,
                    displayName = displayName,
                    relativePath = relativePath,
                    fileSizeBytes = fileSizeBytes,
                    dateAddedEpochSeconds = dateAddedEpochSeconds,
                    dateModifiedEpochSeconds = dateModifiedEpochSeconds
                )

                songs.add(song)
            }
        }
        return if (query == null) null else songs
    }

    private fun getAlbumArtByFolder(): Map<String, Uri> {
        val albumArtByFolder = mutableMapOf<String, Uri>()

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME
        )

        val sortOrder = "${MediaStore.Images.Media.DISPLAY_NAME} ASC"

        val query = context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortOrder
        )

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val displayNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val imagePath = cursor.getString(dataColumn) ?: ""
                val displayName = cursor.getString(displayNameColumn) ?: ""

                if (!isLikelyAlbumCover(displayName)) {
                    continue
                }

                val folderPath = File(imagePath).parent ?: ""

                if (folderPath.isBlank()) {
                    continue
                }

                if (albumArtByFolder.containsKey(folderPath)) {
                    continue
                }

                val imageUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                albumArtByFolder[folderPath] = imageUri
            }
        }

        return albumArtByFolder
    }

    private fun isLikelyAlbumCover(fileName: String): Boolean {
        val normalizedName = fileName.lowercase()

        return normalizedName == "cover.jpg" ||
                normalizedName == "cover.png" ||
                normalizedName == "folder.jpg" ||
                normalizedName == "folder.png" ||
                normalizedName == "front.jpg" ||
                normalizedName == "front.png" ||
                normalizedName == "album.jpg" ||
                normalizedName == "album.png"
    }

}

internal fun selectArtwork(embedded: Uri?, folder: Uri?): Uri? = embedded ?: folder

internal fun hideUnavailableEmbeddedArtwork(
    songs: List<Song>,
    isMaterialized: (Uri?) -> Boolean
): List<Song> {
    var changed = false
    val prepared = songs.map { song ->
        if (
            EmbeddedArtworkContract.isEmbeddedArtworkUri(song.albumArtUri) &&
            !isMaterialized(song.albumArtUri)
        ) {
            changed = true
            song.copy(albumArtUri = null)
        } else {
            song
        }
    }
    return if (changed) prepared else songs
}

private fun android.database.Cursor.stringOrEmpty(columnIndex: Int): String {
    return if (columnIndex >= 0 && !isNull(columnIndex)) getString(columnIndex).orEmpty() else ""
}

private fun android.database.Cursor.longOrZero(columnIndex: Int): Long {
    return if (columnIndex >= 0 && !isNull(columnIndex)) getLong(columnIndex) else 0L
}
