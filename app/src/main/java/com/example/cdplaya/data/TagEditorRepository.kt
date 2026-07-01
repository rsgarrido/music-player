package com.example.cdplaya.data

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
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
        }
    }
}