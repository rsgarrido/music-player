package com.example.cdplaya.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import java.security.MessageDigest

data class MusicLibraryData(
    val songs: List<Song>,
    val libraryFolders: List<LibraryFolder>
)

class MusicRepository(private val context: Context) {

    fun getLibraryData(selectedFolders: Set<String> = emptySet()): MusicLibraryData {
        val allSongs = getAllSongs()

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

    fun getSongs(selectedFolders: Set<String> = emptySet()): List<Song> {
        return getLibraryData(selectedFolders).songs
    }

    fun getLibraryFolders(): List<LibraryFolder> {
        return getLibraryData().libraryFolders
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
            MediaStore.Audio.Media.DATA
        )

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

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown Title"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val duration = cursor.getLong(durationColumn)
                val filePath = cursor.getString(dataColumn) ?: ""
                val trackNumber = cursor.getInt(trackColumn)

                val folderPath = File(filePath).parent ?: ""

                if (folderPath.isBlank()) {
                    continue
                }

                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val albumArtUri =
                    getEmbeddedAlbumArtUri(filePath) ?: albumArtByFolder[folderPath]

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
                    albumArtUri = albumArtUri
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

        return try {
            val tag = AudioFileIO.read(audioFile).tag ?: return null
            val artwork = tag.firstArtwork ?: return null
            val artworkBytes = artwork.binaryData ?: return null

            if (artworkBytes.isEmpty()) {
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

            Uri.fromFile(cachedArtworkFile)
        } catch (exception: Exception) {
            null
        }
    }

    private fun findCachedEmbeddedArtworkUri(
        audioFile: File,
        cacheDirectory: File
    ): Uri? {
        val cacheKey = buildEmbeddedArtworkCacheKey(audioFile)

        val cachedArtworkFile = cacheDirectory.listFiles()?.firstOrNull { file ->
            file.isFile && file.nameWithoutExtension == cacheKey
        }

        return cachedArtworkFile?.let { file ->
            Uri.fromFile(file)
        }
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