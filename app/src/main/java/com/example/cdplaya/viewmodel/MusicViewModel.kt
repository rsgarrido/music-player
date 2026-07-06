package com.example.cdplaya.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cdplaya.controller.LibraryController
import com.example.cdplaya.controller.SleepTimerController
import com.example.cdplaya.data.ListeningHistoryRepository
import com.example.cdplaya.data.local.AppDatabase
import com.example.cdplaya.data.local.DatabaseProvider
import com.example.cdplaya.player.PlaybackController

class MusicViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

    private val appDatabase: AppDatabase = DatabaseProvider.getDatabase(appContext)

    val playbackController = PlaybackController(
        context = appContext,
        coroutineScope = viewModelScope
    )

    val sleepTimerController = SleepTimerController(
        coroutineScope = viewModelScope,
        onTimerFinished = {
            playbackController.pausePlayback()
        }
    )

    val libraryController = LibraryController(
        context = appContext,
        appDatabase = appDatabase,
        playbackController = playbackController,
        coroutineScope = viewModelScope
    )

    init {
        val listeningHistoryRepository = ListeningHistoryRepository(
            appDatabase.songPlayStatsDao()
        )

        playbackController.setListeningHistoryRepository(listeningHistoryRepository)

        playbackController.setOnListeningHistoryChanged {
            libraryController.refreshListeningHistory()
        }

        playbackController.connect()
        libraryController.loadSavedUserData()
    }

    fun loadSongs() {
        libraryController.loadSongs()
    }

    fun savePlayerState() {
        playbackController.savePlayerState()
    }

    override fun onCleared() {
        playbackController.release()
        sleepTimerController.release()
        super.onCleared()
    }
}