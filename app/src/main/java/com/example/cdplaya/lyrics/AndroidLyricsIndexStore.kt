package com.example.cdplaya.lyrics

import android.content.Context
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

class AndroidLyricsIndexStore(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher
) : LyricsIndexStore {
    private val indexFile = File(context.applicationContext.filesDir, INDEX_FILE_NAME)

    override suspend fun load(): LyricsIndexSnapshot? = withContext(ioDispatcher) {
        if (!indexFile.isFile) return@withContext null
        runCatching {
            lyricsStorageJson.decodeFromString<LyricsIndexSnapshot>(indexFile.readText())
        }.getOrNull()
    }

    override suspend fun save(snapshot: LyricsIndexSnapshot) = withContext(ioDispatcher) {
        val temporary = File(indexFile.parentFile, "${indexFile.name}.tmp")
        temporary.writeText(lyricsStorageJson.encodeToString(snapshot))
        if (!temporary.renameTo(indexFile)) {
            indexFile.writeText(temporary.readText())
            temporary.delete()
        }
    }

    override suspend fun clear() = withContext(ioDispatcher) {
        indexFile.delete()
        Unit
    }

    private companion object {
        const val INDEX_FILE_NAME = "lyrics_index.json"
    }
}
