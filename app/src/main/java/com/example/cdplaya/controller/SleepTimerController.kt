package com.example.cdplaya.controller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SleepTimerController(
    private val coroutineScope: CoroutineScope,
    private val onTimerFinished: () -> Unit
) {
    var isTimerActive by mutableStateOf(false)
        private set

    var remainingSeconds by mutableStateOf(0)
        private set

    private var timerJob: Job? = null

    fun startTimer(minutes: Int) {
        if (minutes <= 0) {
            return
        }

        timerJob?.cancel()

        remainingSeconds = minutes * SECONDS_PER_MINUTE
        isTimerActive = true

        timerJob = coroutineScope.launch {
            while (remainingSeconds > 0) {
                delay(ONE_SECOND_MS)
                remainingSeconds -= 1
            }

            isTimerActive = false
            timerJob = null

            onTimerFinished()
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        remainingSeconds = 0
        isTimerActive = false
    }

    fun getDisplayText(): String {
        if (!isTimerActive || remainingSeconds <= 0) {
            return "No sleep timer"
        }

        val minutes = remainingSeconds / SECONDS_PER_MINUTE
        val seconds = remainingSeconds % SECONDS_PER_MINUTE

        return if (minutes > 0) {
            "${minutes}m ${seconds.toString().padStart(2, '0')}s remaining"
        } else {
            "${seconds}s remaining"
        }
    }

    fun release() {
        cancelTimer()
    }

    companion object {
        val TIMER_OPTIONS_MINUTES = listOf(5, 10, 15, 30, 45, 60)

        private const val SECONDS_PER_MINUTE = 60
        private const val ONE_SECOND_MS = 1_000L
    }
}