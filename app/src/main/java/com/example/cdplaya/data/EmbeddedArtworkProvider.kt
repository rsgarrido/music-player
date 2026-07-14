package com.example.cdplaya.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

class EmbeddedArtworkProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String? {
        return when (uri.lastPathSegment?.substringAfterLast('.')?.lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "jpg", "jpeg" -> "image/jpeg"
            else -> null
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        require(mode == "r") { "Embedded artwork is read-only" }
        val fileName = requireNotNull(uri.lastPathSegment)
        require(fileName == File(fileName).name) { "Invalid artwork path" }
        val cacheDirectory = File(
            requireNotNull(context).cacheDir,
            EMBEDDED_ARTWORK_CACHE_DIRECTORY
        )
        val artworkFile = File(cacheDirectory, fileName)
        require(artworkFile.parentFile?.canonicalFile == cacheDirectory.canonicalFile) {
            "Invalid artwork path"
        }
        return ParcelFileDescriptor.open(artworkFile, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    private companion object {
        const val EMBEDDED_ARTWORK_CACHE_DIRECTORY = "embedded_album_art"
    }
}
