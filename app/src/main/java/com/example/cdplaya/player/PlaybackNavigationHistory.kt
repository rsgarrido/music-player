package com.example.cdplaya.player

import com.example.cdplaya.data.Song

class PlaybackNavigationHistory {
    private val previousSongs = mutableListOf<Song>()
    private val nextSongs = mutableListOf<Song>()

    fun addPreviousSong(song: Song) {
        if (previousSongs.lastOrNull()?.id == song.id) {
            return
        }

        previousSongs.add(song)
    }

    fun clearForwardHistory() {
        nextSongs.clear()
    }

    fun clearAll() {
        previousSongs.clear()
        nextSongs.clear()
    }

    fun removeInvalidSongs(validSongIds: Set<Long>) {
        previousSongs.removeAll { song ->
            song.id !in validSongIds
        }

        nextSongs.removeAll { song ->
            song.id !in validSongIds
        }
    }

    fun getPreviousSongIds(): List<Long> {
        return previousSongs.map { song ->
            song.id
        }
    }

    fun getNextSongIds(): List<Long> {
        return nextSongs.map { song ->
            song.id
        }
    }

    fun replacePreviousSongs(songs: List<Song>) {
        previousSongs.clear()
        previousSongs.addAll(songs)
    }

    fun replaceNextSongs(songs: List<Song>) {
        nextSongs.clear()
        nextSongs.addAll(songs)
    }

    fun popNextSong(): Song? {
        if (nextSongs.isEmpty()) {
            return null
        }

        return nextSongs.removeAt(nextSongs.lastIndex)
    }

    fun popPreviousSongAndPushCurrent(currentSong: Song?): Song? {
        if (previousSongs.isEmpty()) {
            return null
        }

        if (currentSong != null) {
            nextSongs.add(currentSong)
        }

        return previousSongs.removeAt(previousSongs.lastIndex)
    }
}
