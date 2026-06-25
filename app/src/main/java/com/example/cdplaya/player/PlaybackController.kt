package com.example.cdplaya.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.cdplaya.data.Song
import kotlin.random.Random

class PlaybackController(
    context: Context
) {
    private val musicPlayer = MusicPlayer(context)
    private val playerStateStorage = PlayerStateStorage(context)

    private var librarySongs: List<Song> = emptyList()
    private var playbackContextSongs: List<Song> = emptyList()

    private val previousSongHistory = mutableListOf<Song>()
    private val nextSongHistory = mutableListOf<Song>()

    val playbackQueue = mutableStateListOf<Song>()

    var currentSong by mutableStateOf<Song?>(null)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var isShuffleEnabled by mutableStateOf(playerStateStorage.isShuffleEnabled())
        private set

    var repeatMode by mutableStateOf(playerStateStorage.getRepeatMode())
        private set

    var upcomingSongs by mutableStateOf<List<Song>>(emptyList())
        private set

    var currentPosition by mutableStateOf(0)
        private set

    var duration by mutableStateOf(0)
        private set

    var isPlayerConnected = false
        private set

    private val progressHandler = Handler(Looper.getMainLooper())

    private val progressRunnable = object : Runnable {
        override fun run() {
            if (currentSong != null) {
                currentPosition = musicPlayer.getCurrentPosition()
                duration = musicPlayer.getDuration()
                progressHandler.postDelayed(this, 500)
            }
        }
    }

    fun connect() {
        musicPlayer.connect {
            isPlayerConnected = true
            musicPlayer.setShuffleEnabled(isShuffleEnabled)
            musicPlayer.setRepeatMode(repeatMode)

            if (librarySongs.isNotEmpty()) {
                restorePlayerState()
            }
        }

        musicPlayer.onSongCompleted = {
            handleSongCompleted()
        }

        musicPlayer.onPlaybackStateChanged = { playerIsPlaying ->
            isPlaying = playerIsPlaying
        }

        musicPlayer.onCurrentSongChanged = { songId ->
            handleServiceSongChanged(songId)
        }
    }

    fun setLibrarySongs(songs: List<Song>) {
        librarySongs = songs

        if (isPlayerConnected) {
            restorePlayerState()
        }
    }

    fun handleLibrarySongsChanged(updatedSongs: List<Song>) {
        librarySongs = updatedSongs

        val validSongIds = updatedSongs.map { song ->
            song.id
        }.toSet()

        if (currentSong != null && currentSong?.id !in validSongIds) {
            musicPlayer.stop()
            currentSong = null
            isPlaying = false
            currentPosition = 0
            duration = 0
            upcomingSongs = emptyList()
        }

        playbackQueue.removeAll { queuedSong ->
            queuedSong.id !in validSongIds
        }

        previousSongHistory.removeAll { historySong ->
            historySong.id !in validSongIds
        }

        nextSongHistory.removeAll { historySong ->
            historySong.id !in validSongIds
        }

        playbackContextSongs = playbackContextSongs.filter { song ->
            song.id in validSongIds
        }

        if (playbackContextSongs.isEmpty()) {
            playbackContextSongs = librarySongs
        }

        if (currentSong != null) {
            syncServicePlaylistKeepingCurrent()
        }

        savePlayerState()
    }

    fun playSongsFromContext(
        playbackContext: List<Song>,
        shuffle: Boolean
    ) {
        if (playbackContext.isEmpty()) {
            return
        }

        isShuffleEnabled = shuffle
        previousSongHistory.clear()
        nextSongHistory.clear()

        val songToPlay = if (shuffle && playbackContext.size > 1) {
            playbackContext[Random.nextInt(playbackContext.size)]
        } else {
            playbackContext.first()
        }

        playSelectedSong(
            song = songToPlay,
            playbackContext = playbackContext
        )
    }

    fun playSelectedSong(
        song: Song,
        playbackContext: List<Song>? = null,
        addCurrentToHistory: Boolean = true,
        clearForwardHistory: Boolean = true
    ) {
        val previousSong = currentSong

        if (addCurrentToHistory && previousSong != null && previousSong.id != song.id) {
            previousSongHistory.add(previousSong)
        }

        if (clearForwardHistory) {
            nextSongHistory.clear()
        }

        playbackContextSongs = playbackContext ?: getPlaybackSourceSongs()

        musicPlayer.playSong(
            song = song,
            playlist = buildPlaybackPlaylist(song)
        )

        currentSong = song
        isPlaying = true
        currentPosition = 0
        duration = song.duration.toInt()

        musicPlayer.setShuffleEnabled(isShuffleEnabled)
        musicPlayer.setRepeatMode(repeatMode)

        startProgressUpdates()
        savePlayerState()
    }

    fun togglePlayPause() {
        if (musicPlayer.isPlaying()) {
            musicPlayer.pause()
            isPlaying = false
        } else {
            musicPlayer.resume()
            isPlaying = true
        }
    }

    fun skipToPrevious() {
        musicPlayer.skipToPrevious()
    }

    fun skipToNext() {
        musicPlayer.skipToNext()
    }

    fun seekTo(position: Int) {
        musicPlayer.seekTo(position)
        currentPosition = position
        savePlayerState()
    }

    fun toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled
        previousSongHistory.clear()
        nextSongHistory.clear()
        syncServicePlaylistKeepingCurrent(preserveExistingShuffleOrder = false)
        savePlayerState()
    }

    fun cycleRepeatMode() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }

        syncServicePlaylistKeepingCurrent()
        savePlayerState()
    }

    fun addSongToQueue(song: Song) {
        playbackQueue.add(song)
        syncServicePlaylistKeepingCurrent()
        savePlayerState()
    }

    fun addSongToPlayNext(song: Song) {
        playbackQueue.add(0, song)
        syncServicePlaylistKeepingCurrent()
        savePlayerState()
    }

    fun removeSongFromQueue(index: Int) {
        if (index in playbackQueue.indices) {
            playbackQueue.removeAt(index)
            syncServicePlaylistKeepingCurrent()
            savePlayerState()
        }
    }

    fun moveQueuedSongUp(index: Int) {
        if (index > 0 && index in playbackQueue.indices) {
            val song = playbackQueue.removeAt(index)
            playbackQueue.add(index - 1, song)
            syncServicePlaylistKeepingCurrent()
            savePlayerState()
        }
    }

    fun moveQueuedSongDown(index: Int) {
        if (index >= 0 && index < playbackQueue.lastIndex) {
            val song = playbackQueue.removeAt(index)
            playbackQueue.add(index + 1, song)
            syncServicePlaylistKeepingCurrent()
            savePlayerState()
        }
    }

    fun clearQueue() {
        playbackQueue.clear()
        syncServicePlaylistKeepingCurrent()
        savePlayerState()
    }

    fun addSongsToPlayNext(songs: List<Song>) {
        if (songs.isEmpty()) {
            return
        }

        playbackQueue.addAll(0, songs)
        syncServicePlaylistKeepingCurrent()
        savePlayerState()
    }

    fun addSongsToQueue(songs: List<Song>) {
        if (songs.isEmpty()) {
            return
        }

        playbackQueue.addAll(songs)
        syncServicePlaylistKeepingCurrent()
        savePlayerState()
    }

    fun removeFirstMatchingSongsFromQueue(songs: List<Song>) {
        var removedAnySong = false

        songs.forEach { song ->
            val index = playbackQueue.indexOfFirst { queuedSong ->
                queuedSong.id == song.id
            }

            if (index != -1) {
                playbackQueue.removeAt(index)
                removedAnySong = true
            }
        }

        if (removedAnySong) {
            syncServicePlaylistKeepingCurrent()
            savePlayerState()
        }
    }

    fun removeLastMatchingSongsFromQueue(songs: List<Song>) {
        var removedAnySong = false

        songs.asReversed().forEach { song ->
            for (index in playbackQueue.lastIndex downTo 0) {
                if (playbackQueue[index].id == song.id) {
                    playbackQueue.removeAt(index)
                    removedAnySong = true
                    break
                }
            }
        }

        if (removedAnySong) {
            syncServicePlaylistKeepingCurrent()
            savePlayerState()
        }
    }

    fun removeLastMatchingSongFromQueue(song: Song) {
        for (index in playbackQueue.lastIndex downTo 0) {
            if (playbackQueue[index].id == song.id) {
                playbackQueue.removeAt(index)
                syncServicePlaylistKeepingCurrent()
                savePlayerState()
                return
            }
        }
    }

    fun removeFirstMatchingSongFromQueue(song: Song) {
        val index = playbackQueue.indexOfFirst { queuedSong ->
            queuedSong.id == song.id
        }

        if (index != -1) {
            playbackQueue.removeAt(index)
            syncServicePlaylistKeepingCurrent()
            savePlayerState()
        }
    }

    fun getComingUpSongsForDisplay(): List<Song> {
        val queuedSongCount = playbackQueue.count { queuedSong ->
            queuedSong.id != currentSong?.id
        }

        return upcomingSongs.drop(queuedSongCount)
    }

    fun savePlayerState() {
        playerStateStorage.saveState(
            currentSongId = currentSong?.id,
            currentPosition = musicPlayer.getCurrentPosition(),
            isShuffleEnabled = isShuffleEnabled,
            repeatMode = repeatMode,
            previousSongIds = previousSongHistory.map { song -> song.id },
            nextSongIds = nextSongHistory.map { song -> song.id },
            queueSongIds = playbackQueue.map { song -> song.id },
            playbackContextSongIds = playbackContextSongs.map { song -> song.id }
        )
    }

    fun release() {
        savePlayerState()
        progressHandler.removeCallbacks(progressRunnable)
        musicPlayer.release()
    }

    private fun restorePlayerState() {
        val savedSongId = playerStateStorage.getCurrentSongId() ?: return

        val restoredSong = librarySongs.firstOrNull { song ->
            song.id == savedSongId
        } ?: return

        currentSong = restoredSong
        currentPosition = playerStateStorage.getCurrentPosition()
        duration = restoredSong.duration.toInt()
        isPlaying = false

        isShuffleEnabled = playerStateStorage.isShuffleEnabled()
        repeatMode = playerStateStorage.getRepeatMode()

        val restoredPlaybackContextSongs = playerStateStorage
            .getPlaybackContextSongIds()
            .mapNotNull { savedId ->
                librarySongs.firstOrNull { song -> song.id == savedId }
            }

        playbackContextSongs = if (restoredPlaybackContextSongs.isNotEmpty()) {
            restoredPlaybackContextSongs
        } else {
            librarySongs
        }

        playbackQueue.clear()
        playbackQueue.addAll(
            playerStateStorage.getQueueSongIds().mapNotNull { savedId ->
                librarySongs.firstOrNull { song -> song.id == savedId }
            }
        )

        previousSongHistory.clear()
        previousSongHistory.addAll(
            playerStateStorage.getPreviousSongIds().mapNotNull { savedId ->
                librarySongs.firstOrNull { song -> song.id == savedId }
            }
        )

        nextSongHistory.clear()
        nextSongHistory.addAll(
            playerStateStorage.getNextSongIds().mapNotNull { savedId ->
                librarySongs.firstOrNull { song -> song.id == savedId }
            }
        )

        musicPlayer.playSong(
            song = restoredSong,
            shouldStart = false,
            startPosition = currentPosition,
            playlist = buildPlaybackPlaylist(restoredSong)
        )

        musicPlayer.setShuffleEnabled(isShuffleEnabled)
        musicPlayer.setRepeatMode(repeatMode)

        startProgressUpdates()
    }

    private fun playNextSong() {
        val playbackSourceSongs = getPlaybackSourceSongs()

        if (playbackSourceSongs.isEmpty()) {
            return
        }

        if (repeatMode == RepeatMode.ONE) {
            currentSong?.let { song ->
                playSelectedSong(
                    song = song,
                    playbackContext = playbackSourceSongs,
                    addCurrentToHistory = false,
                    clearForwardHistory = false
                )
            }
            return
        }

        if (playNextQueuedSong()) {
            return
        }

        val nextSong = if (isShuffleEnabled) {
            if (nextSongHistory.isNotEmpty()) {
                nextSongHistory.removeAt(nextSongHistory.lastIndex)
            } else {
                getRandomSongExceptCurrent(playbackSourceSongs)
            }
        } else {
            val currentIndex = playbackSourceSongs.indexOfFirst { song ->
                song.id == currentSong?.id
            }

            val nextIndex = if (currentIndex == -1 || currentIndex == playbackSourceSongs.lastIndex) {
                0
            } else {
                currentIndex + 1
            }

            playbackSourceSongs[nextIndex]
        }

        playSelectedSong(
            song = nextSong,
            playbackContext = playbackSourceSongs
        )
    }

    private fun playPreviousSong() {
        val playbackSourceSongs = getPlaybackSourceSongs()

        if (playbackSourceSongs.isEmpty()) {
            return
        }

        val previousSong = if (isShuffleEnabled && previousSongHistory.isNotEmpty()) {
            currentSong?.let { song ->
                nextSongHistory.add(song)
            }

            previousSongHistory.removeAt(previousSongHistory.lastIndex)
        } else {
            val currentIndex = playbackSourceSongs.indexOfFirst { song ->
                song.id == currentSong?.id
            }

            val previousIndex = if (currentIndex <= 0) {
                playbackSourceSongs.lastIndex
            } else {
                currentIndex - 1
            }

            playbackSourceSongs[previousIndex]
        }

        playSelectedSong(
            song = previousSong,
            playbackContext = playbackSourceSongs,
            addCurrentToHistory = false,
            clearForwardHistory = false
        )
    }

    private fun playNextQueuedSong(): Boolean {
        if (playbackQueue.isEmpty()) {
            return false
        }

        val playbackSourceSongs = getPlaybackSourceSongs()
        val nextQueuedSong = playbackQueue.removeAt(0)

        playSelectedSong(
            song = nextQueuedSong,
            playbackContext = playbackSourceSongs
        )

        savePlayerState()

        return true
    }

    private fun handleSongCompleted() {
        val playbackSourceSongs = getPlaybackSourceSongs()

        when (repeatMode) {
            RepeatMode.ONE -> {
                currentSong?.let { song ->
                    playSelectedSong(
                        song = song,
                        playbackContext = playbackSourceSongs,
                        addCurrentToHistory = false,
                        clearForwardHistory = false
                    )
                }
            }

            RepeatMode.ALL -> {
                playNextSong()
            }

            RepeatMode.OFF -> {
                if (isShuffleEnabled) {
                    playNextSong()
                    return
                }

                val currentIndex = playbackSourceSongs.indexOfFirst { song ->
                    song.id == currentSong?.id
                }

                if (currentIndex == playbackSourceSongs.lastIndex) {
                    isPlaying = false
                    currentPosition = duration
                } else {
                    playNextSong()
                }
            }
        }
    }

    private fun handleServiceSongChanged(songId: Long?) {
        val newSong = librarySongs.firstOrNull { song ->
            song.id == songId
        } ?: return

        if (currentSong?.id == newSong.id) {
            musicPlayer.setRepeatMode(repeatMode)
            musicPlayer.setShuffleEnabled(isShuffleEnabled)
            return
        }

        currentSong = newSong
        currentPosition = musicPlayer.getCurrentPosition()
        duration = newSong.duration.toInt()
        isPlaying = musicPlayer.isPlaying()

        if (playbackQueue.firstOrNull()?.id == newSong.id) {
            playbackQueue.removeAt(0)
        }

        syncServicePlaylistKeepingCurrent()

        startProgressUpdates()
        savePlayerState()
    }

    private fun getRandomSongExceptCurrent(playbackSourceSongs: List<Song>): Song {
        val availableSongs = playbackSourceSongs.filter { song ->
            song.id != currentSong?.id
        }

        return if (availableSongs.isNotEmpty()) {
            availableSongs[Random.nextInt(availableSongs.size)]
        } else {
            playbackSourceSongs.first()
        }
    }

    private fun startProgressUpdates() {
        progressHandler.removeCallbacks(progressRunnable)
        progressHandler.post(progressRunnable)
    }

    private fun buildPlaybackPlaylist(startSong: Song): List<Song> {
        val refreshedUpcomingSongs = refreshUpcomingSongs(
            startSong = startSong,
            preserveExistingShuffleOrder = false
        )

        return listOf(startSong) + refreshedUpcomingSongs
    }

    private fun refreshUpcomingSongs(
        startSong: Song,
        preserveExistingShuffleOrder: Boolean
    ): List<Song> {
        upcomingSongs = buildUpcomingPlaylistAfterCurrent(
            startSong = startSong,
            preserveExistingShuffleOrder = preserveExistingShuffleOrder
        )

        return upcomingSongs
    }

    private fun buildUpcomingPlaylistAfterCurrent(
        startSong: Song,
        preserveExistingShuffleOrder: Boolean
    ): List<Song> {
        val queuedSongsAfterCurrent = playbackQueue.filter { queuedSong ->
            queuedSong.id != startSong.id
        }

        val excludedSongIds = mutableSetOf<Long>()
        excludedSongIds.add(startSong.id)
        excludedSongIds.addAll(queuedSongsAfterCurrent.map { song -> song.id })

        val playbackSourceSongs = getPlaybackSourceSongs()

        val startIndex = playbackSourceSongs.indexOfFirst { song ->
            song.id == startSong.id
        }

        val songsAfterCurrent = when {
            startIndex == -1 -> {
                getRemainingSongsFromExistingUpcoming(startSong)
            }

            preserveExistingShuffleOrder && isShuffleEnabled -> {
                val remainingExistingUpcomingSongs = getRemainingSongsFromExistingUpcoming(startSong)

                if (
                    remainingExistingUpcomingSongs.isEmpty() &&
                    repeatMode == RepeatMode.ALL &&
                    playbackSourceSongs.size > 1
                ) {
                    playbackSourceSongs.filter { song ->
                        song.id != startSong.id
                    }
                } else {
                    remainingExistingUpcomingSongs
                }
            }

            else -> {
                playbackSourceSongs.drop(startIndex + 1) + playbackSourceSongs.take(startIndex)
            }
        }

        val remainingContextSongs = songsAfterCurrent.filter { song ->
            song.id !in excludedSongIds
        }

        val shouldCreateNewShuffleOrder =
            isShuffleEnabled &&
                    startIndex != -1 &&
                    (
                            !preserveExistingShuffleOrder ||
                                    upcomingSongs.isEmpty() && repeatMode == RepeatMode.ALL
                            )

        val orderedRemainingSongs = if (shouldCreateNewShuffleOrder) {
            remainingContextSongs.shuffled()
        } else {
            remainingContextSongs
        }

        return queuedSongsAfterCurrent + orderedRemainingSongs
    }

    private fun syncServicePlaylistKeepingCurrent(
        preserveExistingShuffleOrder: Boolean = true
    ) {
        val song = currentSong ?: return

        val refreshedUpcomingSongs = refreshUpcomingSongs(
            startSong = song,
            preserveExistingShuffleOrder = preserveExistingShuffleOrder
        )

        musicPlayer.updateUpcomingPlaylist(
            upcomingSongs = refreshedUpcomingSongs
        )

        musicPlayer.setShuffleEnabled(isShuffleEnabled)
        musicPlayer.setRepeatMode(repeatMode)
    }

    private fun getRemainingSongsFromExistingUpcoming(startSong: Song): List<Song> {
        val currentSongIndexInUpcoming = upcomingSongs.indexOfFirst { song ->
            song.id == startSong.id
        }

        return if (currentSongIndexInUpcoming == -1) {
            upcomingSongs
        } else {
            upcomingSongs.drop(currentSongIndexInUpcoming + 1)
        }
    }

    private fun getPlaybackSourceSongs(): List<Song> {
        return if (playbackContextSongs.isNotEmpty()) {
            playbackContextSongs
        } else {
            librarySongs
        }
    }
}