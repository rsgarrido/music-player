package io.github.rsgarrido.sazanami.ui.state

import android.content.res.Resources
import io.github.rsgarrido.sazanami.R

data class SleepTimerUiState(
    val isActive: Boolean = false,
    val remainingSeconds: Int = 0,
    val timerOptionsMinutes: List<Int> = emptyList()
) {
    companion object {
        val Inactive = SleepTimerUiState()
    }
}

val SLEEP_TIMER_OPTIONS_MINUTES = listOf(5, 10, 15, 30, 45, 60)

fun SleepTimerUiState.displayText(resources: Resources): String {
    if (!isActive || remainingSeconds <= 0) return resources.getString(R.string.sleep_timer_inactive)
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    return if (minutes > 0) {
        resources.getString(R.string.sleep_timer_minutes_remaining, minutes, seconds)
    } else {
        resources.getString(R.string.sleep_timer_seconds_remaining, seconds)
    }
}
