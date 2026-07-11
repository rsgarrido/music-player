package com.example.cdplaya.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.cdplaya.data.ListeningHistoryRepository
import com.example.cdplaya.data.Song
import com.example.cdplaya.player.replaygain.ReplayGainMode
import com.example.cdplaya.player.replaygain.ReplayGainRepository
import com.example.cdplaya.player.replaygain.replayGainTrackMultiplier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random

class PlaybackController(
    context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val musicPlayer = MusicPlayer(context)
    private val playerStateStorage = PlayerStateStorage(context)
    private val playbackHistoryRecorder = PlaybackHistoryRecorder(coroutineScope)
    private val playbackQueueManager = PlaybackQueueManager()
    private val playbackNavigationHistory = PlaybackNavigationHistory()
    private val upcomingPlaylistBuilder = UpcomingPlaylistBuilder()
    private val replayGainRepository = ReplayGainRepository()
    private var librarySongs: List<Song> = emptyList()
    private var playbackContextSongs: List<Song> = emptyList()
    private var replayGainMode: ReplayGainMode = ReplayGainMode.OFF
    private var replayGainRequestId = 0
    val playbackQueue = playbackQueueManager.playbackQueue

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
                val updatedPosition = musicPlayer.getCurrentPosition()

                currentPosition = updatedPosition
                duration = musicPlayer.getDuration()

                playbackHistoryRecorder.onProgressUpdated(
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    updatedPosition = updatedPosition,
                    duration = duration
                )

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

    fun setReplayGainMode(mode: ReplayGainMode) {
        replayGainMode = mode
        applyReplayGainForCurrentSong()
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
            musicPlayer.setVolume(1f)
            currentSong = null
            isPlaying = false
            currentPosition = 0
            duration = 0
            upcomingSongs = emptyList()
        }

        playbackQueueManager.removeInvalidSongs(validSongIds)

        playbackNavigationHistory.removeInvalidSongs(validSongIds)

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
        playbackNavigationHistory.clearAll()

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
            playbackNavigationHistory.addPreviousSong(previousSong)
        }

        if (clearForwardHistory) {
            playbackNavigationHistory.clearForwardHistory()
        }

        playbackContextSongs = playbackContext ?: getPlaybackSourceSongs()

        musicPlayer.playSong(
            song = song,
            playlist = buildPlaybackPlaylist(song)
        )

        currentSong = song
        playbackHistoryRecorder.resetForNewSong()
        isPlaying = true
        currentPosition = 0
        duration = song.duration.toInt()

        musicPlayer.setShuffleEnabled(isShuffleEnabled)
        musicPlayer.setRepeatMode(repeatMode)
        applyReplayGainForCurrentSong()

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

    fun pausePlayback() {
        musicPlayer.pause()
        isPlaying = false
        savePlayerState()
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
        playbackHistoryRecorder.onSeek(position)
        savePlayerState()
    }

    fun toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled
        playbackNavigationHistory.clearAll()
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
        playbackQueueManager.addSongToQueue(song)
        syncServicePlaylistKeepingCurrent()
        savePlayerState()
    }

    fun addSongToPlayNext(song: Song) {
        playbackQueueManager.addSongToPlayNext(song)
        syncServicePlaylistKeepingCurrent()
        savePlayerState()
    }

    fun removeSongFromQueue(index: Int) {
        if (playbackQueueManager.removeSongFromQueue(index)) {
            syncServicePlaylistKeepingCurrent()
            savePlayerState()
        }
    }

    fun moveQueuedSongUp(index: Int) {
        if (playbackQueueManager.moveQueuedSongUp(index)) {
            syncServicePlaylistKeepingCurrent()
            savePlayerState()
        }
    }

    fun moveQueuedSongDown(index: Int) {
        if (playbackQueueManager.moveQueuedSongDown(index)) {
            syncServicePlaylistKeepingCurrent()
            savePlayerState()
        }
    }

    fun clearQueue() {
        if (playbackQueueManager.clearQueue()) {
            syncServicePlaylistKeepingCurrent()
            savePlayerState()
        }
    }

    fun addSongsToPlayNext(songs: List<Song>) {
        if (playbackQueueManager.addSongsToPlayNext(songs)) {
            syncServicePlaylistKeepingCurrent()
            savePlayerState()
        }
    }

    fun addSongsToQueue(songs: List<Song>) {
        if (playbackQueueManager.addSongsToQueue(songs)) {
            syncServicePlaylistKeepingCurrent()
            savePlayerState()
        }
    }

    fun removeFirstMatchingSongsFromQueue(songs: List<Song>) {
        if (playbackQueueManager.removeFirstMatchingSongsFromQueue(songs)) {
            syncServicePlaylistKeepingCurrent()
            savePlayerState()
        }
    }

    fun removeLastMatchingSongsFromQueue(songs: List<Song>) {
        if (playbackQueueManager.removeLastMatchingSongsFromQueue(songs)) {
            syncServicePlaylistKeepingCurrent()
            savePlayerState()
        }
    }

    fun removeLastMatchingSongFromQueue(song: Song) {
        if (playbackQueueManager.removeLastMatchingSongFromQueue(song)) {
            syncServicePlaylistKeepingCurrent()
            savePlayerState()
        }
    }

    fun removeFirstMatchingSongFromQueue(song: Song) {
        if (playbackQueueManager.removeFirstMatchingSongFromQueue(song)) {
            syncServicePlaylistKeepingCurrent()
            savePlayerState()
        }
    }

    fun getComingUpSongsForDisplay(): List<Song> {
        val queuedSongCount = playbackQueueManager.getQueuedSongCountExcludingCurrent(
            currentSongId = currentSong?.id
        )

        return upcomingSongs.drop(queuedSongCount)
    }

    fun savePlayerState() {
        playerStateStorage.saveState(
            currentSongId = currentSong?.id,
            currentPosition = musicPlayer.getCurrentPosition(),
            isShuffleEnabled = isShuffleEnabled,
            repeatMode = repeatMode,
            previousSongIds = playbackNavigationHistory.getPreviousSongIds(),
            nextSongIds = playbackNavigationHistory.getNextSongIds(),
            queueSongIds = playbackQueueManager.getQueuedSongIds(),
            playbackContextSongIds = playbackContextSongs.map { song -> song.id }
        )
    }

    fun release() {
        savePlayerState()
        progressHandler.removeCallbacks(progressRunnable)
        musicPlayer.release()
    }

    fun setListeningHistoryRepository(repository: ListeningHistoryRepository) {
        playbackHistoryRecorder.setListeningHistoryRepository(repository)
    }

    fun setOnListeningHistoryChanged(listener: () -> Unit) {
        playbackHistoryRecorder.setOnListeningHistoryChanged(listener)
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
        playbackHistoryRecorder.resetForNewSong()

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

        playbackQueueManager.replaceQueue(
            playerStateStorage.getQueueSongIds().mapNotNull { savedId ->
                librarySongs.firstOrNull { song -> song.id == savedId }
            }
        )

        playbackNavigationHistory.replacePreviousSongs(
            playerStateStorage.getPreviousSongIds().mapNotNull { savedId ->
                librarySongs.firstOrNull { song -> song.id == savedId }
            }
        )

        playbackNavigationHistory.replaceNextSongs(
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
        applyReplayGainForCurrentSong()

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
            playbackNavigationHistory.popNextSong()
                ?: getRandomSongExceptCurrent(playbackSourceSongs)
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

        val previousSong = if (isShuffleEnabled) {
            playbackNavigationHistory.popPreviousSongAndPushCurrent(currentSong)
                ?: run {
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
        val playbackSourceSongs = getPlaybackSourceSongs()
        val nextQueuedSong = playbackQueueManager.removeNextQueuedSong()
            ?: return false

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
            applyReplayGainForCurrentSong()
            return
        }

        currentSong = newSong
        playbackHistoryRecorder.resetForNewSong()
        currentPosition = musicPlayer.getCurrentPosition()
        duration = newSong.duration.toInt()
        isPlaying = musicPlayer.isPlaying()

        if (playbackQueue.firstOrNull()?.id == newSong.id) {
            playbackQueue.removeAt(0)
        }

        syncServicePlaylistKeepingCurrent()
        applyReplayGainForCurrentSong()

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
        val refreshedUpcomingSongs = upcomingPlaylistBuilder.buildUpcomingPlaylistAfterCurrent(
            startSong = startSong,
            playbackSourceSongs = getPlaybackSourceSongs(),
            queuedSongsAfterCurrent = playbackQueueManager.getQueuedSongsAfterCurrent(
                currentSongId = startSong.id
            ),
            currentUpcomingSongs = upcomingSongs,
            isShuffleEnabled = isShuffleEnabled,
            repeatMode = repeatMode,
            preserveExistingShuffleOrder = preserveExistingShuffleOrder
        )

        upcomingSongs = refreshedUpcomingSongs

        return refreshedUpcomingSongs
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

    private fun applyReplayGainForCurrentSong() {
        val song = currentSong
        val requestedMode = replayGainMode

        replayGainRequestId += 1
        val requestId = replayGainRequestId

        if (song == null || requestedMode == ReplayGainMode.OFF) {
            musicPlayer.setVolume(1f)
            return
        }

        musicPlayer.setVolume(1f)

        coroutineScope.launch {
            val replayGainInfo = replayGainRepository.getReplayGainInfo(song)

            val volumeMultiplier = replayGainTrackMultiplier(
                replayGainInfo = replayGainInfo,
                replayGainMode = requestedMode
            )

            val isStillCurrentRequest = replayGainRequestId == requestId
            val isStillSameSong = currentSong?.id == song.id
            val isStillSameMode = replayGainMode == requestedMode

            if (isStillCurrentRequest && isStillSameSong && isStillSameMode) {
                musicPlayer.setVolume(volumeMultiplier)
            }
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