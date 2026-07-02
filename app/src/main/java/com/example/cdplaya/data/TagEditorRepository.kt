package com.example.cdplaya.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.flac.FlacTag
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.max

class TagEditorRepository {

    init {
        Logger.getLogger("org.jaudiotagger").level = Level.OFF
    }

    fun readTags(song: Song): EditableSongTags {
        val file = File(song.filePath)

        if (!file.exists()) {
            return EditableSongTags(
                title = song.title,
                artist = song.artist,
                album = song.album,
                trackNumber = song.trackNumber.takeIf { trackNumber ->
                    trackNumber > 0
                }?.toString() ?: "",
                year = ""
            )
        }

        return try {
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tag

            EditableSongTags(
                title = tag?.getFirst(FieldKey.TITLE).orEmpty()
                    .ifBlank { song.title },
                artist = tag?.getFirst(FieldKey.ARTIST).orEmpty()
                    .ifBlank { song.artist },
                album = tag?.getFirst(FieldKey.ALBUM).orEmpty()
                    .ifBlank { song.album },
                trackNumber = tag?.getFirst(FieldKey.TRACK).orEmpty()
                    .ifBlank {
                        song.trackNumber.takeIf { trackNumber ->
                            trackNumber > 0
                        }?.toString().orEmpty()
                    },
                year = tag?.getFirst(FieldKey.YEAR).orEmpty()
            )
        } catch (exception: Exception) {
            EditableSongTags(
                title = song.title,
                artist = song.artist,
                album = song.album,
                trackNumber = song.trackNumber.takeIf { trackNumber ->
                    trackNumber > 0
                }?.toString() ?: "",
                year = ""
            )
        }
    }

    fun writeTags(
        song: Song,
        editedTags: EditableSongTags
    ): TagEditorResult {
        val file = File(song.filePath)

        if (!file.exists()) {
            return TagEditorResult(
                wasSuccessful = false,
                message = "The audio file could not be found."
            )
        }

        return try {
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tagOrCreateAndSetDefault

            writeTextTagsToTag(
                tag = tag,
                editedTags = editedTags
            )

            AudioFileIO.write(audioFile)

            TagEditorResult(
                wasSuccessful = true,
                message = "Tags saved successfully."
            )
        } catch (exception: Exception) {
            TagEditorResult(
                wasSuccessful = false,
                message = exception.message ?: "Could not save tags."
            )
        } catch (error: LinkageError) {
            TagEditorResult(
                wasSuccessful = false,
                message = error.message ?: "Could not save tags on this Android device."
            )
        }
    }

    fun writeTagsAndArtwork(
        context: Context,
        song: Song,
        editedTags: EditableSongTags,
        artworkUri: Uri?
    ): TagEditorResult {
        val audioFileOnDisk = File(song.filePath)

        if (!audioFileOnDisk.exists()) {
            return TagEditorResult(
                wasSuccessful = false,
                message = "The audio file could not be found."
            )
        }

        var temporaryArtworkFile: File? = null
        var selectedFlacArtworkHash: String? = null
        var isFlacArtworkSave = false

        return try {
            val audioFile = AudioFileIO.read(audioFileOnDisk)
            val tag = audioFile.tagOrCreateAndSetDefault

            writeTextTagsToTag(
                tag = tag,
                editedTags = editedTags
            )

            if (artworkUri != null) {
                val optimizedArtwork = createOptimizedArtworkImageData(
                    context = context,
                    artworkUri = artworkUri
                ) ?: return TagEditorResult(
                    wasSuccessful = false,
                    message = "The selected artwork could not be read."
                )

                if (tag is FlacTag) {
                    selectedFlacArtworkHash = optimizedArtwork.bytes.sha256()
                    isFlacArtworkSave = true

                    setFlacArtwork(
                        flacTag = tag,
                        artworkImageData = optimizedArtwork
                    )
                } else {
                    temporaryArtworkFile = createTemporaryArtworkFile(
                        context = context,
                        artworkImageData = optimizedArtwork
                    )

                    if (temporaryArtworkFile == null || !temporaryArtworkFile.exists()) {
                        return TagEditorResult(
                            wasSuccessful = false,
                            message = "The selected artwork could not be prepared."
                        )
                    }

                    val artwork = ArtworkFactory.createArtworkFromFile(temporaryArtworkFile)

                    deleteExistingArtwork(tag)

                    tag.setField(artwork)
                }
            }

            AudioFileIO.write(audioFile)

            if (isFlacArtworkSave && selectedFlacArtworkHash != null) {
                val flacArtworkWasSaved = flacArtworkMatches(
                    audioFileOnDisk = audioFileOnDisk,
                    expectedArtworkHash = selectedFlacArtworkHash
                )

                if (!flacArtworkWasSaved) {
                    return TagEditorResult(
                        wasSuccessful = false,
                        message = "FLAC artwork could not be verified after saving."
                    )
                }
            }

            TagEditorResult(
                wasSuccessful = true,
                message = if (artworkUri == null) {
                    "Tags saved successfully."
                } else {
                    "Tags and artwork saved successfully."
                }
            )
        } catch (exception: Exception) {
            TagEditorResult(
                wasSuccessful = false,
                message = exception.message ?: "Could not save tags and artwork."
            )
        } catch (error: LinkageError) {
            TagEditorResult(
                wasSuccessful = false,
                message = error.message ?: "Could not save artwork on this Android device."
            )
        } finally {
            temporaryArtworkFile?.delete()
        }
    }

    fun getUnsupportedEditingMessage(song: Song): String? {
        val file = File(song.filePath)

        if (!file.exists()) {
            return "The audio file could not be found."
        }

        val extension = file.extension.lowercase()

        if (extension.isBlank()) {
            return "This file does not have a recognizable audio extension."
        }

        val supportedExtensions = setOf(
            "mp3",
            "flac",
            "m4a",
            "mp4",
            "ogg",
            "opus",
            "wav",
            "aif",
            "aiff"
        )

        if (extension !in supportedExtensions) {
            return "Tag editing is not enabled for .$extension files yet."
        }

        return null
    }

    private fun writeTextTagsToTag(
        tag: Tag,
        editedTags: EditableSongTags
    ) {
        tag.setField(FieldKey.TITLE, editedTags.title.trim())
        tag.setField(FieldKey.ARTIST, editedTags.artist.trim())
        tag.setField(FieldKey.ALBUM, editedTags.album.trim())

        val cleanedTrackNumber = editedTags.trackNumber.trim()
        if (cleanedTrackNumber.isNotBlank()) {
            tag.setField(FieldKey.TRACK, cleanedTrackNumber)
        }

        val cleanedYear = editedTags.year.trim()
        if (cleanedYear.isNotBlank()) {
            tag.setField(FieldKey.YEAR, cleanedYear)
        }
    }

    private fun setFlacArtwork(
        flacTag: FlacTag,
        artworkImageData: ArtworkImageData
    ) {
        deleteExistingArtwork(flacTag)

        val artworkField = flacTag.createArtworkField(
            artworkImageData.bytes,
            FRONT_COVER_PICTURE_TYPE,
            artworkImageData.mimeType,
            "Cover",
            artworkImageData.width,
            artworkImageData.height,
            DEFAULT_COLOUR_DEPTH,
            DEFAULT_INDEXED_COLOUR_COUNT
        )

        flacTag.addField(artworkField)
    }

    private fun deleteExistingArtwork(tag: Tag) {
        try {
            tag.deleteArtworkField()
        } catch (exception: Exception) {
            // Some files may not have artwork yet. In that case, we can continue.
        }
    }

    private fun createOptimizedArtworkImageData(
        context: Context,
        artworkUri: Uri
    ): ArtworkImageData? {
        val originalBytes = context.contentResolver.openInputStream(artworkUri)
            ?.use { inputStream ->
                inputStream.readBytes()
            }
            ?: return null

        if (originalBytes.isEmpty()) {
            return null
        }

        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeByteArray(
            originalBytes,
            0,
            originalBytes.size,
            boundsOptions
        )

        val originalWidth = boundsOptions.outWidth
        val originalHeight = boundsOptions.outHeight

        if (originalWidth <= 0 || originalHeight <= 0) {
            return null
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(
                width = originalWidth,
                height = originalHeight,
                maxSize = MAX_ARTWORK_SIZE_PX
            )
        }

        val decodedBitmap = BitmapFactory.decodeByteArray(
            originalBytes,
            0,
            originalBytes.size,
            decodeOptions
        ) ?: return null

        val scaledBitmap = scaleBitmapIfNeeded(
            bitmap = decodedBitmap,
            maxSize = MAX_ARTWORK_SIZE_PX
        )

        if (scaledBitmap !== decodedBitmap) {
            decodedBitmap.recycle()
        }

        val outputStream = ByteArrayOutputStream()

        scaledBitmap.compress(
            Bitmap.CompressFormat.JPEG,
            ARTWORK_JPEG_QUALITY,
            outputStream
        )

        val optimizedBytes = outputStream.toByteArray()

        val width = scaledBitmap.width
        val height = scaledBitmap.height

        scaledBitmap.recycle()

        if (optimizedBytes.isEmpty()) {
            return null
        }

        return ArtworkImageData(
            bytes = optimizedBytes,
            mimeType = OPTIMIZED_ARTWORK_MIME_TYPE,
            width = width,
            height = height
        )
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        maxSize: Int
    ): Int {
        var sampleSize = 1

        var sampledWidth = width
        var sampledHeight = height

        while (sampledWidth / 2 >= maxSize || sampledHeight / 2 >= maxSize) {
            sampleSize *= 2
            sampledWidth /= 2
            sampledHeight /= 2
        }

        return sampleSize
    }

    private fun scaleBitmapIfNeeded(
        bitmap: Bitmap,
        maxSize: Int
    ): Bitmap {
        val largestSide = max(bitmap.width, bitmap.height)

        if (largestSide <= maxSize) {
            return bitmap
        }

        val scale = maxSize.toFloat() / largestSide.toFloat()
        val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)

        return Bitmap.createScaledBitmap(
            bitmap,
            newWidth,
            newHeight,
            true
        )
    }

    private fun createTemporaryArtworkFile(
        context: Context,
        artworkImageData: ArtworkImageData
    ): File {
        val temporaryArtworkFile = File.createTempFile(
            "selected_artwork_",
            ".jpg",
            context.cacheDir
        )

        temporaryArtworkFile.writeBytes(artworkImageData.bytes)

        return temporaryArtworkFile
    }

    private fun flacArtworkMatches(
        audioFileOnDisk: File,
        expectedArtworkHash: String
    ): Boolean {
        return try {
            val updatedAudioFile = AudioFileIO.read(audioFileOnDisk)
            val updatedTag = updatedAudioFile.tag ?: return false

            updatedTag.artworkList.any { artwork ->
                val artworkBytes = artwork.binaryData

                artworkBytes != null &&
                        artworkBytes.isNotEmpty() &&
                        artworkBytes.sha256() == expectedArtworkHash
            }
        } catch (exception: Exception) {
            false
        } catch (error: LinkageError) {
            false
        }
    }

    private fun ByteArray.sha256(): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(this)

        return bytes.joinToString("") { byte ->
            "%02x".format(byte)
        }
    }

    private data class ArtworkImageData(
        val bytes: ByteArray,
        val mimeType: String,
        val width: Int,
        val height: Int
    )

    companion object {
        private const val FRONT_COVER_PICTURE_TYPE = 3
        private const val DEFAULT_COLOUR_DEPTH = 24
        private const val DEFAULT_INDEXED_COLOUR_COUNT = 0

        private const val MAX_ARTWORK_SIZE_PX = 1000
        private const val ARTWORK_JPEG_QUALITY = 90
        private const val OPTIMIZED_ARTWORK_MIME_TYPE = "image/jpeg"
    }
}