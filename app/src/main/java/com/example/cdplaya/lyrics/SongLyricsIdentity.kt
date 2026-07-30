package com.example.cdplaya.lyrics

import com.example.cdplaya.data.Song

fun Song.toLyricsIdentity(): SongLyricsIdentity {
    val fileName = displayName.ifBlank {
        filePath.substringAfterLast('/').substringAfterLast('\\')
    }
    return SongLyricsIdentity(
        audioFileName = fileName,
        relativeDirectory = relativePath,
        fallbackDirectory = folderPath.ifBlank {
            filePath.substringBeforeLast('/', "").substringBeforeLast('\\', "")
        },
        volumeId = volumeName
    )
}
