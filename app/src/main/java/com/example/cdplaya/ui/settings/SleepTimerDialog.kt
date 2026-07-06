package com.example.cdplaya.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cdplaya.controller.SleepTimerController

@Composable
fun SleepTimerDialog(
    isTimerActive: Boolean,
    sleepTimerDisplayText: String,
    onStartTimerClick: (Int) -> Unit,
    onCancelTimerClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Sleep Timer")
        },
        text = {
            Column {
                Text(text = sleepTimerDisplayText)

                SleepTimerController.TIMER_OPTIONS_MINUTES.forEach { minutes ->
                    Button(
                        onClick = {
                            onStartTimerClick(minutes)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(text = "$minutes minutes")
                    }
                }

                if (isTimerActive) {
                    OutlinedButton(
                        onClick = {
                            onCancelTimerClick()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Text(text = "Cancel Timer")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss
            ) {
                Text(text = "Close")
            }
        }
    )
}