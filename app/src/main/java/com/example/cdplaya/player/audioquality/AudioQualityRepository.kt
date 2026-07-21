package com.example.cdplaya.player.audioquality

import com.example.cdplaya.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import java.util.LinkedHashMap
import java.util.logging.Level
import java.util.logging.Logger

class AudioQualityRepository {

    init {
        Logger.getLogger("org.jaudiotagger").level = Level.OFF
    }

    suspend fun getAudioQualityInfo(song: Song): AudioQualityInfo {
        return withContext(Dispatchers.IO) {
            val audioFile = File(song.filePath)
            val cacheKey = AudioQualityCacheKey(
                songId = song.id,
                filePath = song.filePath,
                lastModified = audioFile
                    .takeIf { file -> file.exists() }
                    ?.lastModified()
                    ?: 0L
            )

            synchronized(audioQualityCache) {
                audioQualityCache[cacheKey]
            }?.let { cachedInfo ->
                return@withContext cachedInfo
            }

            val loadedInfo = readAudioQualityInfo(audioFile)

            synchronized(audioQualityCache) {
                audioQualityCache[cacheKey] = loadedInfo
            }

            loadedInfo
        }
    }

    private fun readAudioQualityInfo(file: File): AudioQualityInfo {
        if (!file.isFile) {
            return emptyAudioQualityInfo()
        }

        val fallbackFormat = normalizeAudioFormat(file.extension)

        return try {
            val audioHeader = AudioFileIO.read(file).audioHeader

            AudioQualityInfo(
                format = resolveAudioFormat(
                    headerFormat = audioHeader.format,
                    encodingType = audioHeader.encodingType,
                    fileExtension = file.extension
                ),
                bitDepth = audioHeader.bitsPerSample.takeIf { value -> value > 0 },
                sampleRateHz = audioHeader.sampleRateAsNumber.takeIf { value -> value > 0 },
                bitrateKbps = audioHeader.bitRateAsNumber
                    .takeIf { value -> value in 1..Int.MAX_VALUE.toLong() }
                    ?.toInt()
            )
        } catch (exception: Exception) {
            emptyAudioQualityInfo(format = fallbackFormat)
        } catch (error: LinkageError) {
            emptyAudioQualityInfo(format = fallbackFormat)
        }
    }

    private fun emptyAudioQualityInfo(format: String? = null): AudioQualityInfo {
        return AudioQualityInfo(
            format = format,
            bitDepth = null,
            sampleRateHz = null,
            bitrateKbps = null
        )
    }

    private data class AudioQualityCacheKey(
        val songId: Long,
        val filePath: String,
        val lastModified: Long
    )

    private companion object {
        private const val MAX_CACHE_ENTRIES = 64

        private val audioQualityCache = object :
            LinkedHashMap<AudioQualityCacheKey, AudioQualityInfo>(
                MAX_CACHE_ENTRIES,
                0.75f,
                true
            ) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<AudioQualityCacheKey, AudioQualityInfo>?
            ): Boolean {
                return size > MAX_CACHE_ENTRIES
            }
        }
    }
}
