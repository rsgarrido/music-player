package com.example.cdplaya.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import java.security.MessageDigest


class MusicRepository(private val context: Context) {
    fun getLibraryData(selectedFolders: Set<String> = emptySet()): MusicLibraryData {
        return buildMusicLibraryData(
            allSongs = getAllSongs(),
            selectedFolders = selectedFolders
        )
    }

    fun getSongs(selectedFolders: Set<String> = emptySet()): List<Song> {
        return getLibraryData(selectedFolders).songs
    }

    private fun getAllSongs(): List<Song> {
        val songs = mutableListOf<Song>()

        val albumArtByFolder = getAlbumArtByFolder()

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

                val albumArtUri =
                    getEmbeddedAlbumArtUri(filePath) ?: albumArtByFolder[folderPath]

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
                    albumArtUri = albumArtUri,
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
    return songs
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

    private fun getEmbeddedAlbumArtUri(filePath: String): Uri? {
        val audioFile = File(filePath)

        if (!audioFile.exists()) {
            return null
        }

        val cacheDirectory = File(context.cacheDir, EMBEDDED_ARTWORK_CACHE_DIRECTORY)

        if (!cacheDirectory.exists()) {
            cacheDirectory.mkdirs()
        }

        findCachedEmbeddedArtworkUri(
            audioFile = audioFile,
            cacheDirectory = cacheDirectory
        )?.let { cachedArtworkUri ->
            return cachedArtworkUri
        }

        if (hasNoEmbeddedArtworkCacheMarker(audioFile, cacheDirectory)) {
            return null
        }

        return try {

            val tag = AudioFileIO.read(audioFile).tag

            if (tag == null) {
                writeNoEmbeddedArtworkCacheMarker(audioFile, cacheDirectory)
                return null
            }

            val artwork = tag.firstArtwork

            if (artwork == null) {
                writeNoEmbeddedArtworkCacheMarker(audioFile, cacheDirectory)
                return null
            }

            val artworkBytes = artwork.binaryData

            if (artworkBytes == null || artworkBytes.isEmpty()) {
                writeNoEmbeddedArtworkCacheMarker(audioFile, cacheDirectory)
                return null
            }

            val artworkFileExtension = getArtworkFileExtension(artwork.mimeType)
            val cacheFileName = buildEmbeddedArtworkCacheFileName(
                audioFile = audioFile,
                extension = artworkFileExtension
            )

            val cachedArtworkFile = File(cacheDirectory, cacheFileName)

            if (!cachedArtworkFile.exists()) {
                cachedArtworkFile.writeBytes(artworkBytes)
            }

            buildEmbeddedArtworkContentUri(cachedArtworkFile)
        } catch (exception: Exception) {
            writeNoEmbeddedArtworkCacheMarker(audioFile, cacheDirectory)
            null
        } catch (error: LinkageError) {
            writeNoEmbeddedArtworkCacheMarker(audioFile, cacheDirectory)
            null
        }
    }

    private fun hasNoEmbeddedArtworkCacheMarker(
        audioFile: File,
        cacheDirectory: File
    ): Boolean {
        val markerFile = File(
            cacheDirectory,
            buildNoEmbeddedArtworkCacheFileName(audioFile)
        )

        return markerFile.exists() && markerFile.isFile
    }

    private fun writeNoEmbeddedArtworkCacheMarker(
        audioFile: File,
        cacheDirectory: File
    ) {
        val markerFile = File(
            cacheDirectory,
            buildNoEmbeddedArtworkCacheFileName(audioFile)
        )

        if (markerFile.exists()) {
            return
        }

        try {
            markerFile.writeText("no_embedded_artwork")
        } catch (exception: Exception) {
        }
    }

    private fun buildNoEmbeddedArtworkCacheFileName(audioFile: File): String {
        return "${buildEmbeddedArtworkCacheKey(audioFile)}.no_artwork"
    }

    private fun findCachedEmbeddedArtworkUri(
        audioFile: File,
        cacheDirectory: File
    ): Uri? {
        val cacheKey = buildEmbeddedArtworkCacheKey(audioFile)

        val possibleCachedFiles = listOf(
            File(cacheDirectory, "$cacheKey.jpg"),
            File(cacheDirectory, "$cacheKey.png"),
            File(cacheDirectory, "$cacheKey.webp")
        )

        val cachedArtworkFile = possibleCachedFiles.firstOrNull { file ->
            file.exists() && file.isFile
        }

        return cachedArtworkFile?.let { file ->
            buildEmbeddedArtworkContentUri(file)
        }
    }

    private fun buildEmbeddedArtworkContentUri(artworkFile: File): Uri {
        return Uri.Builder()
            .scheme("content")
            .authority("${context.packageName}.embeddedartwork")
            .appendPath(artworkFile.name)
            .build()
    }

    private fun buildEmbeddedArtworkCacheKey(audioFile: File): String {
        val rawKey = listOf(
            audioFile.absolutePath,
            audioFile.lastModified().toString(),
            audioFile.length().toString()
        ).joinToString("|")

        return rawKey.sha256()
    }

    private fun buildEmbeddedArtworkCacheFileName(
        audioFile: File,
        extension: String
    ): String {
        return "${buildEmbeddedArtworkCacheKey(audioFile)}.$extension"
    }

    private fun getArtworkFileExtension(mimeType: String?): String {
        return when (mimeType?.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
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

    private fun String.sha256(): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(toByteArray())

        return bytes.joinToString("") { byte ->
            "%02x".format(byte)
        }
    }

    companion object {
        private const val EMBEDDED_ARTWORK_CACHE_DIRECTORY = "embedded_album_art"
    }
}

private fun android.database.Cursor.stringOrEmpty(columnIndex: Int): String {
    return if (columnIndex >= 0 && !isNull(columnIndex)) getString(columnIndex).orEmpty() else ""
}

private fun android.database.Cursor.longOrZero(columnIndex: Int): Long {
    return if (columnIndex >= 0 && !isNull(columnIndex)) getLong(columnIndex) else 0L
}
