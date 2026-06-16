package com.example.cdplaya.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File

class MusicRepository(private val context: Context) {

    fun getSongs(selectedFolders: Set<String> = emptySet()): List<Song> {
        val allSongs = getAllSongs()

        if (selectedFolders.isEmpty()) {
            return allSongs
        }

        return allSongs.filter { song ->
            selectedFolders.contains(song.folderPath)
        }
    }

    fun getLibraryFolders(): List<LibraryFolder> {
        val songs = getAllSongs()

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

                val albumArtUri = albumArtByFolder[folderPath]

                val song = Song(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    trackNumber = trackNumber,
                    duration = duration,
                    uri = uri,
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