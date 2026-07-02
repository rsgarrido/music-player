package com.example.cdplaya.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.flac.FlacTag
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.security.MessageDigest
import java.util.logging.Level
import java.util.logging.Logger

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
                if (tag is FlacTag) {
                    val artworkImageData = readArtworkImageData(
                        context = context,
                        artworkUri = artworkUri
                    ) ?: return TagEditorResult(
                        wasSuccessful = false,
                        message = "The selected artwork could not be read."
                    )

                    selectedFlacArtworkHash = artworkImageData.bytes.sha256()
                    isFlacArtworkSave = true

                    setFlacArtwork(
                        flacTag = tag,
                        artworkImageData = artworkImageData
                    )
                } else {
                    temporaryArtworkFile = createTemporaryArtworkFile(
                        context = context,
                        artworkUri = artworkUri
                    )

                    if (temporaryArtworkFile == null || !temporaryArtworkFile.exists()) {
                        return TagEditorResult(
                            wasSuccessful = false,
                            message = "The selected artwork could not be read."
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

    private fun createTemporaryArtworkFile(
        context: Context,
        artworkUri: Uri
    ): File? {
        val mimeType = context.contentResolver.getType(artworkUri)
        val extension = getImageFileExtension(mimeType)

        val temporaryArtworkFile = File.createTempFile(
            "selected_artwork_",
            ".$extension",
            context.cacheDir
        )

        val inputStream = context.contentResolver.openInputStream(artworkUri)
            ?: return null

        inputStream.use { input ->
            temporaryArtworkFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return temporaryArtworkFile
    }

    private fun readArtworkImageData(
        context: Context,
        artworkUri: Uri
    ): ArtworkImageData? {
        val mimeType = context.contentResolver.getType(artworkUri)
            ?: DEFAULT_IMAGE_MIME_TYPE

        val artworkBytes = context.contentResolver.openInputStream(artworkUri)
            ?.use { inputStream ->
                inputStream.readBytes()
            }
            ?: return null

        if (artworkBytes.isEmpty()) {
            return null
        }

        val bitmapOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeByteArray(
            artworkBytes,
            0,
            artworkBytes.size,
            bitmapOptions
        )

        val width = bitmapOptions.outWidth
        val height = bitmapOptions.outHeight

        if (width <= 0 || height <= 0) {
            return null
        }

        return ArtworkImageData(
            bytes = artworkBytes,
            mimeType = mimeType,
            width = width,
            height = height
        )
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

    private fun getImageFileExtension(mimeType: String?): String {
        return when (mimeType?.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
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
        private const val DEFAULT_IMAGE_MIME_TYPE = "image/jpeg"
    }
}